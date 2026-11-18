package com.example.audio

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min

object WavParser {

  fun parse(inputStream: InputStream, id: String, fileName: String): ImportedWavSample? {
    val bytes = inputStream.readBytes()
    if (bytes.size < 44) return null

    val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    val riff = readFourCC(buf, 0)
    val wave = readFourCC(buf, 8)
    if (riff != "RIFF" || wave != "WAVE") return null

    var pos = 12
    var audioFormat = 1
    var channels = 1
    var sampleRate = 44100
    var bitsPerSample = 16
    var dataOffset = -1
    var dataSize = 0

    while (pos + 8 <= bytes.size) {
      val chunkId = readFourCC(buf, pos)
      val chunkSize = buf.getInt(pos + 4).coerceAtLeast(0)
      val payloadStart = pos + 8
      if (payloadStart + chunkSize > bytes.size && chunkId != "data") break

      when (chunkId) {
        "fmt " -> {
          if (chunkSize >= 16 && payloadStart + 16 <= bytes.size) {
            audioFormat = buf.getShort(payloadStart).toInt() and 0xFFFF
            channels = (buf.getShort(payloadStart + 2).toInt() and 0xFFFF).coerceAtLeast(1)
            sampleRate = buf.getInt(payloadStart + 4).coerceIn(8000, 192000)
            bitsPerSample = buf.getShort(payloadStart + 14).toInt() and 0xFFFF
          }
        }
        "data" -> {
          dataOffset = payloadStart
          dataSize = min(chunkSize, bytes.size - payloadStart)
          break
        }
      }
      val paddedSize = chunkSize + (chunkSize and 1)
      pos = payloadStart + paddedSize
    }

    if (dataOffset < 0 || dataSize <= 0) return null
    val bytesPerSample = (bitsPerSample / 8).coerceAtLeast(1)
    val frameSize = bytesPerSample * channels
    val frameCount = (dataSize / frameSize).coerceAtMost(sampleRate * 30) // Cap at 30s per sample
    if (frameCount <= 0) return null

    val monoSamples = FloatArray(frameCount)
    var offset = dataOffset

    for (i in 0 until frameCount) {
      var sum = 0f
      for (ch in 0 until channels) {
        val sampleFloat = when {
          audioFormat == 3 && bitsPerSample == 32 -> {
            buf.getFloat(offset).coerceIn(-1f, 1f)
          }
          bitsPerSample == 8 -> {
            val u = bytes[offset].toInt() and 0xFF
            (u - 128) / 128f
          }
          bitsPerSample == 16 -> {
            buf.getShort(offset) / 32768f
          }
          bitsPerSample == 24 -> {
            val b0 = bytes[offset].toInt() and 0xFF
            val b1 = bytes[offset + 1].toInt() and 0xFF
            val b2 = bytes[offset + 2].toInt()
            val signed = (b2 shl 16) or (b1 shl 8) or b0
            signed / 8388608f
          }
          bitsPerSample == 32 -> {
            buf.getInt(offset) / 2147483648f
          }
          else -> 0f
        }
        sum += sampleFloat
        offset += bytesPerSample
      }
      monoSamples[i] = (sum / channels).coerceIn(-1f, 1f)
    }

    val cleanName = fileName.substringBeforeLast('.').take(24).ifBlank { "User WAV" }
    val durationMs = (frameCount * 1000L) / sampleRate
    return ImportedWavSample(
      id = id,
      fileName = fileName,
      displayName = cleanName,
      sampleRate = sampleRate,
      samples = monoSamples,
      durationMs = durationMs,
    )
  }

  fun writePcm16StereoWav(file: File, pcmInterleaved: ShortArray, sampleRate: Int = 44100) {
    val dataBytes = pcmInterleaved.size * 2
    val totalRiffSize = 36 + dataBytes
    val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
    header.put("RIFF".toByteArray(Charsets.US_ASCII))
    header.putInt(totalRiffSize)
    header.put("WAVE".toByteArray(Charsets.US_ASCII))
    header.put("fmt ".toByteArray(Charsets.US_ASCII))
    header.putInt(16) // PCM chunk size
    header.putShort(1) // PCM format
    header.putShort(2) // Stereo
    header.putInt(sampleRate)
    header.putInt(sampleRate * 4) // Byte rate
    header.putShort(4) // Block align
    header.putShort(16) // Bits per sample
    header.put("data".toByteArray(Charsets.US_ASCII))
    header.putInt(dataBytes)

    FileOutputStream(file).use { out ->
      out.write(header.array())
      val body = ByteBuffer.allocate(dataBytes).order(ByteOrder.LITTLE_ENDIAN)
      for (s in pcmInterleaved) {
        body.putShort(s)
      }
      out.write(body.array())
    }
  }

  private fun readFourCC(buf: ByteBuffer, offset: Int): String {
    if (offset + 4 > buf.limit()) return ""
    return String(
      byteArrayOf(buf.get(offset), buf.get(offset + 1), buf.get(offset + 2), buf.get(offset + 3)),
      Charsets.US_ASCII,
    )
  }
}
