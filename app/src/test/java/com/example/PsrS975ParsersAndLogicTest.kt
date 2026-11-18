package com.example

import com.example.audio.Sf2Parser
import com.example.audio.StyParser
import com.example.audio.WavParser
import com.example.audio.YamahaLibrary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class PsrS975ParsersAndLogicTest {

  @Test
  fun `wav parser parses valid 16-bit PCM mono wav`() {
    val samples = shortArrayOf(0, 16384, 32767, -16384, -32768, 0)
    val dataSize = samples.size * 2
    val buf = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)
    buf.put("RIFF".toByteArray(Charsets.US_ASCII))
    buf.putInt(36 + dataSize)
    buf.put("WAVE".toByteArray(Charsets.US_ASCII))
    buf.put("fmt ".toByteArray(Charsets.US_ASCII))
    buf.putInt(16)
    buf.putShort(1)
    buf.putShort(1)
    buf.putInt(44100)
    buf.putInt(88200)
    buf.putShort(2)
    buf.putShort(16)
    buf.put("data".toByteArray(Charsets.US_ASCII))
    buf.putInt(dataSize)
    samples.forEach { buf.putShort(it) }

    val parsed = WavParser.parse(ByteArrayInputStream(buf.array()), "w1", "SnareHQ.wav")
    assertNotNull(parsed)
    assertEquals("SnareHQ", parsed!!.displayName)
    assertEquals(44100, parsed.sampleRate)
    assertEquals(6, parsed.samples.size)
    assertTrue(parsed.samples[2] > 0.99f)
  }

  @Test
  fun `sty parser extracts tempo, markers and channel events`() {
    val trackOut = ByteArrayOutputStream()
    // Delta 0: Meta Tempo 128 BPM (468750 us) = 07 27 0E
    trackOut.write(byteArrayOf(0x00, 0xFF.toByte(), 0x51, 0x03, 0x07, 0x27, 0x0E))
    // Delta 0: Marker "Main A"
    val marker = "Main A".toByteArray(Charsets.US_ASCII)
    trackOut.write(byteArrayOf(0x00, 0xFF.toByte(), 0x06, marker.size.toByte()))
    trackOut.write(marker)
    // Delta 0: Note On Ch 9 (RHY1) Kick 36 vel 110
    trackOut.write(byteArrayOf(0x00, 0x99.toByte(), 36, 110))
    // Delta 120 (16th at 480 PPQ): Note Off
    trackOut.write(byteArrayOf(120, 0x89.toByte(), 36, 0))
    // End of track
    trackOut.write(byteArrayOf(0x00, 0xFF.toByte(), 0x2F, 0x00))

    val trkBytes = trackOut.toByteArray()
    val full = ByteBuffer.allocate(14 + 8 + trkBytes.size).order(ByteOrder.BIG_ENDIAN)
    full.put("MThd".toByteArray(Charsets.US_ASCII))
    full.putInt(6)
    full.putShort(0)
    full.putShort(1)
    full.putShort(480)
    full.put("MTrk".toByteArray(Charsets.US_ASCII))
    full.putInt(trkBytes.size)
    full.put(trkBytes)

    val parsed = StyParser.parse(ByteArrayInputStream(full.array()), "sty1", "PopGroove.sty")
    assertNotNull(parsed)
    assertEquals("PopGroove", parsed!!.style.name)
    assertEquals(128, parsed.style.defaultTempo)
    assertTrue(parsed.style.sections.isNotEmpty())
  }

  @Test
  fun `chord recognizer detects minor, major and 7th chords`() {
    val cm = YamahaLibrary.detectChord(listOf(48, 51, 55))
    assertEquals("Cm", cm?.displayName)

    val g7 = YamahaLibrary.detectChord(listOf(43, 47, 50, 53))
    assertEquals("G7", g7?.displayName)

    val fMaj7 = YamahaLibrary.detectChord(listOf(41, 45, 48, 52))
    assertEquals("Fmaj7", fMaj7?.displayName)
  }

  @Test
  fun `sf2 parser gracefully handles invalid file`() {
    val tmp = File.createTempFile("bad", ".sf2")
    tmp.writeText("NOT_A_SOUNDFONT")
    val res = Sf2Parser.parse(tmp, "b1", "bad.sf2")
    tmp.delete()
    assertEquals(null, res)
  }

  @Test
  fun `drum auto player grooves have valid 16-step patterns and fills`() {
    val grooves = YamahaLibrary.drumGrooves
    assertTrue(grooves.size >= 8)
    for (g in grooves) {
      assertTrue(g.steps.isNotEmpty())
      assertTrue(g.fillSteps.isNotEmpty())
      assertTrue(g.steps.all { it.step in 0..15 && it.velocity in 1..127 })
      assertTrue(g.fillSteps.all { it.step in 0..15 && it.velocity in 1..127 })
    }
  }
}
