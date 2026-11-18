package com.example.audio

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min

/**
 * SoundFont 2 (.sf2) RIFF binary parser.
 * Parses INFO (INAM), sdta (smpl 16-bit sample pool), and pdta (phdr/pbag/pgen/inst/ibag/igen/shdr).
 */
object Sf2Parser {

  private data class PhdrEntry(val name: String, val preset: Int, val bank: Int, val bagNdx: Int)
  private data class BagEntry(val genNdx: Int)
  private data class GenEntry(val oper: Int, val amountLo: Int, val amountHi: Int, val amountSigned: Int)
  private data class InstEntry(val name: String, val bagNdx: Int)
  private data class ShdrEntry(
    val name: String,
    val start: Int,
    val end: Int,
    val startLoop: Int,
    val endLoop: Int,
    val sampleRate: Int,
    val originalPitch: Int,
    val pitchCorrection: Int,
  )

  fun parse(file: File, bankId: String, fileName: String): ImportedSf2Bank? {
    if (!file.exists() || file.length() < 64) return null

    RandomAccessFile(file, "r").use { raf ->
      val head = ByteArray(12)
      raf.readFully(head)
      val riff = String(head, 0, 4, Charsets.US_ASCII)
      val sfbk = String(head, 8, 4, Charsets.US_ASCII)
      if (riff != "RIFF" || sfbk != "sfbk") return null

      var bankName = fileName.substringBeforeLast('.')
      var smplOffset = -1L
      var smplSize = 0
      var phdrBytes: ByteArray? = null
      var pbagBytes: ByteArray? = null
      var pgenBytes: ByteArray? = null
      var instBytes: ByteArray? = null
      var ibagBytes: ByteArray? = null
      var igenBytes: ByteArray? = null
      var shdrBytes: ByteArray? = null

      val fileLen = raf.length()
      var pos = 12L

      while (pos + 8 <= fileLen) {
        raf.seek(pos)
        val chunkHeader = ByteArray(8)
        raf.readFully(chunkHeader)
        val id = String(chunkHeader, 0, 4, Charsets.US_ASCII)
        val size = ByteBuffer.wrap(chunkHeader, 4, 4).order(ByteOrder.LITTLE_ENDIAN).int.toLong() and 0xFFFFFFFFL
        val payloadPos = pos + 8

        if (id == "LIST" && payloadPos + 4 <= fileLen) {
          val listTypeBuf = ByteArray(4)
          raf.readFully(listTypeBuf)
          val listType = String(listTypeBuf, Charsets.US_ASCII)
          var subPos = payloadPos + 4
          val listEnd = min(fileLen, payloadPos + size)

          while (subPos + 8 <= listEnd) {
            raf.seek(subPos)
            val subHead = ByteArray(8)
            raf.readFully(subHead)
            val subId = String(subHead, 0, 4, Charsets.US_ASCII)
            val subSize = ByteBuffer.wrap(subHead, 4, 4).order(ByteOrder.LITTLE_ENDIAN).int.toLong() and 0xFFFFFFFFL
            val subDataPos = subPos + 8
            val safeSize = min(subSize, listEnd - subDataPos).toInt().coerceAtLeast(0)

            when (listType) {
              "INFO" -> {
                if (subId == "INAM" && safeSize > 0) {
                  val b = ByteArray(min(safeSize, 128))
                  raf.readFully(b)
                  val parsed = String(b, Charsets.UTF_8).trim('\u0000', ' ')
                  if (parsed.isNotBlank()) bankName = parsed
                }
              }
              "sdta" -> {
                if (subId == "smpl" && safeSize > 0) {
                  smplOffset = subDataPos
                  // Cap sample pool at 24M samples (~48MB) for safe mobile memory footprint
                  smplSize = min(safeSize, 48_000_000)
                }
              }
              "pdta" -> {
                if (safeSize in 1..4_000_000) {
                  val b = ByteArray(safeSize)
                  raf.readFully(b)
                  when (subId) {
                    "phdr" -> phdrBytes = b
                    "pbag" -> pbagBytes = b
                    "pgen" -> pgenBytes = b
                    "inst" -> instBytes = b
                    "ibag" -> ibagBytes = b
                    "igen" -> igenBytes = b
                    "shdr" -> shdrBytes = b
                  }
                }
              }
            }
            subPos = subDataPos + subSize + (subSize and 1L)
          }
        }
        pos = payloadPos + size + (size and 1L)
      }

      if (smplOffset < 0 || smplSize < 2 || phdrBytes == null || pbagBytes == null ||
        pgenBytes == null || instBytes == null || ibagBytes == null || igenBytes == null || shdrBytes == null
      ) {
        return null
      }

      // Read 16-bit PCM sample pool
      val totalSamples = smplSize / 2
      val rawBytes = ByteArray(totalSamples * 2)
      raf.seek(smplOffset)
      raf.readFully(rawBytes)
      val samplePool = ShortArray(totalSamples)
      ByteBuffer.wrap(rawBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(samplePool)

      val phdrs = parsePhdr(phdrBytes)
      val pbags = parseBag(pbagBytes)
      val pgens = parseGen(pgenBytes)
      val insts = parseInst(instBytes)
      val ibags = parseBag(ibagBytes)
      val igens = parseGen(igenBytes)
      val shdrs = parseShdr(shdrBytes)

      if (phdrs.size < 2 || shdrs.isEmpty()) return null

      val presets = mutableListOf<Sf2Preset>()
      // Last phdr is terminal EOP
      for (pIdx in 0 until phdrs.size - 1) {
        val phdr = phdrs[pIdx]
        val nextPhdr = phdrs[pIdx + 1]
        val pBagStart = phdr.bagNdx.coerceIn(0, pbags.size)
        val pBagEnd = nextPhdr.bagNdx.coerceIn(pBagStart, pbags.size)

        val zones = mutableListOf<Sf2Zone>()
        var globalInstKeyLow = 0
        var globalInstKeyHigh = 127

        for (b in pBagStart until pBagEnd) {
          val gStart = pbags[b].genNdx.coerceIn(0, pgens.size)
          val gEnd = if (b + 1 < pbags.size) pbags[b + 1].genNdx.coerceIn(gStart, pgens.size) else pgens.size

          var instIdx = -1
          var pKeyLo = 0
          var pKeyHi = 127
          for (g in gStart until gEnd) {
            val gen = pgens[g]
            when (gen.oper) {
              43 -> { // keyRange
                pKeyLo = gen.amountLo
                pKeyHi = gen.amountHi
              }
              41 -> { // instrument
                instIdx = gen.amountSigned and 0xFFFF
              }
            }
          }
          if (instIdx < 0 && b == pBagStart) {
            globalInstKeyLow = pKeyLo
            globalInstKeyHigh = pKeyHi
            continue
          }
          if (instIdx !in 0 until (insts.size - 1)) continue

          val inst = insts[instIdx]
          val nextInst = insts[instIdx + 1]
          val iBagStart = inst.bagNdx.coerceIn(0, ibags.size)
          val iBagEnd = nextInst.bagNdx.coerceIn(iBagStart, ibags.size)

          var globalRootOverride = -1
          var globalFineTune = 0
          var globalSampleMode = 0

          for (ib in iBagStart until iBagEnd) {
            val igStart = ibags[ib].genNdx.coerceIn(0, igens.size)
            val igEnd = if (ib + 1 < ibags.size) ibags[ib + 1].genNdx.coerceIn(igStart, igens.size) else igens.size

            var keyLo = maxOf(pKeyLo, globalInstKeyLow)
            var keyHi = minOf(pKeyHi, globalInstKeyHigh)
            var velLo = 0
            var velHi = 127
            var rootOverride = globalRootOverride
            var fineTune = globalFineTune
            var sampleMode = globalSampleMode
            var sampleId = -1

            for (ig in igStart until igEnd) {
              val gen = igens[ig]
              when (gen.oper) {
                43 -> {
                  keyLo = maxOf(keyLo, gen.amountLo)
                  keyHi = minOf(keyHi, gen.amountHi)
                }
                44 -> {
                  velLo = gen.amountLo
                  velHi = gen.amountHi
                }
                52 -> fineTune = gen.amountSigned
                54 -> sampleMode = gen.amountSigned and 0x3
                58 -> rootOverride = gen.amountSigned and 0xFF
                53 -> sampleId = gen.amountSigned and 0xFFFF
              }
            }

            if (sampleId < 0 && ib == iBagStart) {
              globalRootOverride = rootOverride
              globalFineTune = fineTune
              globalSampleMode = sampleMode
              continue
            }
            if (sampleId !in shdrs.indices) continue
            val shdr = shdrs[sampleId]
            val start = shdr.start.coerceIn(0, samplePool.size - 1)
            val end = shdr.end.coerceIn(start + 1, samplePool.size)
            if (end - start < 4) continue

            val sLoop = shdr.startLoop.coerceIn(start, end)
            val eLoop = shdr.endLoop.coerceIn(sLoop, end)
            val hasLoop = (sampleMode == 1 || sampleMode == 3) && (eLoop - sLoop >= 8)
            val root = if (rootOverride in 0..127) rootOverride else shdr.originalPitch.coerceIn(0, 127)

            zones.add(
              Sf2Zone(
                samplePool = samplePool,
                start = start,
                end = end,
                startLoop = sLoop,
                endLoop = eLoop,
                sampleRate = shdr.sampleRate.coerceIn(8000, 96000),
                rootKey = root,
                fineTuneCents = fineTune + shdr.pitchCorrection,
                keyLow = keyLo.coerceIn(0, 127),
                keyHigh = keyHi.coerceIn(0, 127),
                velLow = velLo.coerceIn(0, 127),
                velHigh = velHi.coerceIn(0, 127),
                hasLoop = hasLoop,
              )
            )
          }
        }

        if (zones.isNotEmpty()) {
          presets.add(
            Sf2Preset(
              name = phdr.name.ifBlank { "Preset ${phdr.preset}" },
              bank = phdr.bank,
              presetNumber = phdr.preset,
              zones = zones,
            )
          )
        }
        if (presets.size >= 48) break
      }

      if (presets.isEmpty()) return null
      return ImportedSf2Bank(
        id = bankId,
        fileName = fileName,
        bankName = bankName,
        presets = presets,
      )
    }
  }

  private fun parsePhdr(bytes: ByteArray): List<PhdrEntry> {
    val count = bytes.size / 38
    val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    return List(count) { i ->
      val off = i * 38
      val name = String(bytes, off, 20, Charsets.US_ASCII).trim('\u0000', ' ')
      val preset = buf.getShort(off + 20).toInt() and 0xFFFF
      val bank = buf.getShort(off + 22).toInt() and 0xFFFF
      val bagNdx = buf.getShort(off + 24).toInt() and 0xFFFF
      PhdrEntry(name, preset, bank, bagNdx)
    }
  }

  private fun parseBag(bytes: ByteArray): List<BagEntry> {
    val count = bytes.size / 4
    val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    return List(count) { i ->
      BagEntry(buf.getShort(i * 4).toInt() and 0xFFFF)
    }
  }

  private fun parseGen(bytes: ByteArray): List<GenEntry> {
    val count = bytes.size / 4
    val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    return List(count) { i ->
      val off = i * 4
      val oper = buf.getShort(off).toInt() and 0xFFFF
      val lo = bytes[off + 2].toInt() and 0xFF
      val hi = bytes[off + 3].toInt() and 0xFF
      val signed = buf.getShort(off + 2).toInt()
      GenEntry(oper, lo, hi, signed)
    }
  }

  private fun parseInst(bytes: ByteArray): List<InstEntry> {
    val count = bytes.size / 22
    val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    return List(count) { i ->
      val off = i * 22
      val name = String(bytes, off, 20, Charsets.US_ASCII).trim('\u0000', ' ')
      val bagNdx = buf.getShort(off + 20).toInt() and 0xFFFF
      InstEntry(name, bagNdx)
    }
  }

  private fun parseShdr(bytes: ByteArray): List<ShdrEntry> {
    val count = bytes.size / 46
    val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    return List(count) { i ->
      val off = i * 46
      val name = String(bytes, off, 20, Charsets.US_ASCII).trim('\u0000', ' ')
      val start = buf.getInt(off + 20)
      val end = buf.getInt(off + 24)
      val startLoop = buf.getInt(off + 28)
      val endLoop = buf.getInt(off + 32)
      val sampleRate = buf.getInt(off + 36)
      val originalPitch = bytes[off + 40].toInt() and 0xFF
      val pitchCorr = bytes[off + 41].toInt()
      ShdrEntry(name, start, end, startLoop, endLoop, sampleRate, originalPitch, pitchCorr)
    }
  }
}
