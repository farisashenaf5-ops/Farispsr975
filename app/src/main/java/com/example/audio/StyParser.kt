package com.example.audio

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Yamaha .sty (SFF1 / SFF2 Standard MIDI + CASM) parser.
 * Extracts tempo, time signature, section markers (Intro/Main A-D/Break/Ending),
 * and multi-channel accompaniment note events mapped to PSR-S975 StyleChannels.
 */
object StyParser {

  private data class RawMidiNote(
    val tick: Long,
    val channel: Int,
    val note: Int,
    val velocity: Int,
    val isNoteOn: Boolean,
  )

  private data class MarkerTick(val tick: Long, val name: String)

  fun parse(inputStream: InputStream, id: String, fileName: String): ImportedStyFile? {
    val bytes = inputStream.readBytes()
    if (bytes.size < 22) return null

    val buf = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
    if (readFourCC(bytes, 0) != "MThd") return null

    val headerLen = buf.getInt(4)
    val trackCount = buf.getShort(10).toInt() and 0xFFFF
    val ppq = (buf.getShort(12).toInt() and 0x7FFF).coerceAtLeast(24)

    var pos = 8 + headerLen
    var tempoBpm = 120
    var timeSigNum = 4
    var timeSigDen = 4
    val markers = mutableListOf<MarkerTick>()
    val rawNotes = mutableListOf<RawMidiNote>()

    var tracksRead = 0
    while (pos + 8 <= bytes.size && tracksRead < trackCount) {
      val chunkId = readFourCC(bytes, pos)
      val chunkSize = buf.getInt(pos + 4).coerceAtLeast(0)
      val dataStart = pos + 8
      val dataEnd = (dataStart + chunkSize).coerceAtMost(bytes.size)

      if (chunkId == "MTrk") {
        tracksRead++
        var idx = dataStart
        var tick = 0L
        var runningStatus = 0

        while (idx < dataEnd) {
          val (delta, nextIdx) = readVarLen(bytes, idx, dataEnd)
          tick += delta
          idx = nextIdx
          if (idx >= dataEnd) break

          var status = bytes[idx].toInt() and 0xFF
          if (status < 0x80) {
            status = runningStatus
          } else {
            idx++
            if (status < 0xF0) runningStatus = status
          }

          when {
            status in 0x80..0xEF -> {
              val cmd = status and 0xF0
              val ch = status and 0x0F
              if (cmd == 0xC0 || cmd == 0xD0) {
                idx += 1
              } else {
                if (idx + 1 >= dataEnd) break
                val d1 = bytes[idx].toInt() and 0x7F
                val d2 = bytes[idx + 1].toInt() and 0x7F
                idx += 2
                if (cmd == 0x90 && d2 > 0) {
                  rawNotes.add(RawMidiNote(tick, ch, d1, d2, true))
                } else if (cmd == 0x80 || (cmd == 0x90 && d2 == 0)) {
                  rawNotes.add(RawMidiNote(tick, ch, d1, 0, false))
                }
              }
            }
            status == 0xFF -> {
              if (idx >= dataEnd) break
              val metaType = bytes[idx].toInt() and 0xFF
              idx++
              val (lenLong, afterLen) = readVarLen(bytes, idx, dataEnd)
              val len = lenLong.toInt().coerceAtLeast(0)
              idx = afterLen
              if (idx + len <= dataEnd) {
                when (metaType) {
                  0x51 -> if (len >= 3) {
                    val usPerBeat = ((bytes[idx].toInt() and 0xFF) shl 16) or
                      ((bytes[idx + 1].toInt() and 0xFF) shl 8) or
                      (bytes[idx + 2].toInt() and 0xFF)
                    if (usPerBeat > 0) tempoBpm = (60_000_000 / usPerBeat).coerceIn(40, 240)
                  }
                  0x58 -> if (len >= 2) {
                    timeSigNum = (bytes[idx].toInt() and 0xFF).coerceIn(2, 12)
                    val denPow = bytes[idx + 1].toInt() and 0xFF
                    timeSigDen = (1 shl denPow.coerceIn(1, 4))
                  }
                  0x06, 0x03 -> {
                    val text = String(bytes, idx, len, Charsets.US_ASCII).trim()
                    if (metaType == 0x06 && text.isNotBlank()) {
                      markers.add(MarkerTick(tick, text))
                    }
                  }
                }
              }
              idx += len
            }
            status == 0xF0 || status == 0xF7 -> {
              val (lenLong, afterLen) = readVarLen(bytes, idx, dataEnd)
              idx = afterLen + lenLong.toInt().coerceAtLeast(0)
            }
            else -> idx++
          }
        }
      }
      pos = dataEnd
    }

    if (rawNotes.isEmpty()) return null

    // Pair NoteOn with NoteOff to build duration
    val ticksPer16th = max(1f, ppq / 4f)
    val sortedMarkers = markers.sortedBy { it.tick }
    val maxTick = rawNotes.maxOf { it.tick } + ppq * 4L

    val sectionsMap = mutableMapOf<StyleSection, StyleSectionData>()

    if (sortedMarkers.isNotEmpty()) {
      for (i in sortedMarkers.indices) {
        val m = sortedMarkers[i]
        val section = mapMarkerToSection(m.name) ?: continue
        val startTick = m.tick
        val endTick = if (i + 1 < sortedMarkers.size) sortedMarkers[i + 1].tick else maxTick
        val lengthSteps = (((endTick - startTick) / ticksPer16th).roundToInt()).coerceIn(16, 128)
        val sectionNotes = rawNotes.filter { it.isNoteOn && it.tick in startTick until endTick }
        val events = convertNotesToEvents(sectionNotes, rawNotes, startTick, lengthSteps, ticksPer16th)
        if (events.isNotEmpty() && !sectionsMap.containsKey(section)) {
          sectionsMap[section] = StyleSectionData(section, lengthSteps, events)
        }
      }
    }

    // Fallback if no SFF markers were found: slice the pattern into MAIN_A..D
    if (sectionsMap.isEmpty()) {
      val lengthSteps = 32
      val events = convertNotesToEvents(
        rawNotes.filter { it.isNoteOn && it.tick < (lengthSteps * ticksPer16th).toLong() },
        rawNotes,
        0L,
        lengthSteps,
        ticksPer16th,
      )
      sectionsMap[StyleSection.MAIN_A] = StyleSectionData(StyleSection.MAIN_A, lengthSteps, events)
    }

    // Ensure MAIN_A..D exist by falling back to available main sections
    val fallbackMain = sectionsMap[StyleSection.MAIN_A] ?: sectionsMap.values.first()
    for (sec in listOf(StyleSection.MAIN_A, StyleSection.MAIN_B, StyleSection.MAIN_C, StyleSection.MAIN_D)) {
      if (!sectionsMap.containsKey(sec)) {
        sectionsMap[sec] = fallbackMain.copy(section = sec)
      }
    }

    val styleName = fileName.substringBeforeLast('.').take(22).ifBlank { "Imported Style" }
    val style = PsrStyle(
      id = id,
      name = styleName,
      category = StyleCategory.EXPANSION_USER,
      defaultTempo = tempoBpm,
      timeSigNum = timeSigNum,
      timeSigDen = timeSigDen,
      isImportedSty = true,
      sections = sectionsMap,
    )
    return ImportedStyFile(id = id, fileName = fileName, style = style)
  }

  private fun convertNotesToEvents(
    onNotes: List<RawMidiNote>,
    allNotes: List<RawMidiNote>,
    baseTick: Long,
    lengthSteps: Int,
    ticksPer16th: Float,
  ): List<StyleNoteEvent> {
    val out = mutableListOf<StyleNoteEvent>()
    for (n in onNotes) {
      val step = (((n.tick - baseTick) / ticksPer16th).roundToInt()).coerceIn(0, lengthSteps - 1)
      val off = allNotes.firstOrNull { !it.isNoteOn && it.channel == n.channel && it.note == n.note && it.tick > n.tick }
      val durSteps = if (off != null) {
        (((off.tick - n.tick) / ticksPer16th).roundToInt()).coerceIn(1, 16)
      } else {
        2
      }
      val styleCh = mapMidiChannelToStyleChannel(n.channel)
      out.add(
        StyleNoteEvent(
          step = step,
          durationSteps = durSteps,
          channel = styleCh,
          note = n.note,
          velocity = n.velocity.coerceIn(20, 127),
        )
      )
    }
    return out
  }

  private fun mapMidiChannelToStyleChannel(midiCh: Int): StyleChannel =
    when (midiCh) {
      9 -> StyleChannel.RHY1
      8 -> StyleChannel.RHY2
      10 -> StyleChannel.BASS
      11 -> StyleChannel.CHD1
      12 -> StyleChannel.CHD2
      13 -> StyleChannel.PAD
      14 -> StyleChannel.PHR1
      15 -> StyleChannel.PHR2
      0, 1 -> StyleChannel.BASS
      2, 3 -> StyleChannel.CHD1
      else -> StyleChannel.CHD2
    }

  private fun mapMarkerToSection(marker: String): StyleSection? {
    val m = marker.uppercase().trim()
    return when {
      m.contains("MAIN A") -> StyleSection.MAIN_A
      m.contains("MAIN B") -> StyleSection.MAIN_B
      m.contains("MAIN C") -> StyleSection.MAIN_C
      m.contains("MAIN D") -> StyleSection.MAIN_D
      m.contains("INTRO A") || m.contains("INTRO 1") -> StyleSection.INTRO_1
      m.contains("INTRO B") || m.contains("INTRO 2") -> StyleSection.INTRO_2
      m.contains("INTRO C") || m.contains("INTRO 3") -> StyleSection.INTRO_3
      m.contains("BREAK") -> StyleSection.BREAK
      m.contains("ENDING A") || m.contains("ENDING 1") -> StyleSection.ENDING_1
      m.contains("ENDING B") || m.contains("ENDING 2") -> StyleSection.ENDING_2
      m.contains("ENDING C") || m.contains("ENDING 3") -> StyleSection.ENDING_3
      else -> null
    }
  }

  private fun readVarLen(bytes: ByteArray, start: Int, end: Int): Pair<Long, Int> {
    var value = 0L
    var idx = start
    var count = 0
    while (idx < end && count < 4) {
      val b = bytes[idx].toInt() and 0xFF
      idx++
      count++
      value = (value shl 7) or (b and 0x7F).toLong()
      if ((b and 0x80) == 0) break
    }
    return value to idx
  }

  private fun readFourCC(bytes: ByteArray, offset: Int): String {
    if (offset + 4 > bytes.size) return ""
    return String(bytes, offset, 4, Charsets.US_ASCII)
  }
}
