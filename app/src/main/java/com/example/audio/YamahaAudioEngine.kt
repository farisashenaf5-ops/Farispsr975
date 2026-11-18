package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.Process
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh
import kotlin.random.Random

class YamahaAudioEngine {

  companion object {
    const val SAMPLE_RATE = 44100
    private const val BLOCK_FRAMES = 256
    private const val MAX_VOICES = 36
    private const val TWO_PI = (2.0 * PI).toFloat()
  }

  private sealed class EngineCmd {
    data class NoteOn(val tag: String, val midiNote: Int, val velocity: Int, val voice: PsrVoice, val volume: Float) : EngineCmd()
    data class NoteOff(val tag: String, val midiNote: Int) : EngineCmd()
    data class DrumHit(val drumNote: Int, val velocity: Int, val kitId: String, val volume: Float) : EngineCmd()
    data class WavOneShot(val wav: ImportedWavSample, val volume: Float) : EngineCmd()
    data object AllNotesOff : EngineCmd()
    data object ReleaseSustain : EngineCmd()
  }

  private enum class EnvStage { ATTACK, DECAY, SUSTAIN, RELEASE, DONE }

  private class SynthVoice {
    var active = false
    var tag = ""
    var midiNote = 60
    var velocityGain = 0.8f
    var partVolume = 0.85f
    var voice: PsrVoice? = null
    var held = false
    var sustained = false
    var envStage = EnvStage.DONE
    var envVal = 0f
    var phase = 0f
    var phase2 = 0f
    var phase3 = 0f
    var ageSamples = 0
    var samplePos = 0.0
    var sf2Zone: Sf2Zone? = null
    var wavSample: ImportedWavSample? = null
    var lpState = 0f
    var lpState2 = 0f
    var panL = 0.707f
    var panR = 0.707f
    var isDrum = false
    var drumNote = 36
    var drumPitch = 1f
    var drumNoiseSeed = 12345
    var drumRemainingSamples = 0
    var drumTotalSamples = 1
  }

  private val cmdQueue = ConcurrentLinkedQueue<EngineCmd>()
  private val voices = Array(MAX_VOICES) { SynthVoice() }
  private val sf2Banks = ConcurrentHashMap<String, ImportedSf2Bank>()
  private val wavSamples = ConcurrentHashMap<String, ImportedWavSample>()

  // Live synth parameters (volatile for lock-free audio thread access)
  @Volatile var isPowerOn = true
  @Volatile var masterVolume = 0.85f
  @Volatile var stereoBalance = 0.0f // -1 (L) .. +1 (R)
  @Volatile var micGain = 0.35f
  @Volatile var pitchBendSemitones = 0.0f
  @Volatile var modulationDepth = 0.0f // 0..1
  @Volatile var sustainPedal = false
    set(value) {
      field = value
      if (!value) cmdQueue.add(EngineCmd.ReleaseSustain)
    }
  @Volatile var dspEnabled = true
  @Volatile var filterCutoffNorm = 0.85f // 0..1 from Live Control Knob 1
  @Volatile var resonanceNorm = 0.25f    // 0..1 from Live Control Knob 2
  @Volatile var reverbDepth = 0.34f
  @Volatile var chorusDepth = 0.24f
  @Volatile var currentDrumKitId = "v_power_kit"

  // Style sequencer state
  @Volatile var isStylePlaying = false
  @Volatile var tempoBpm = 120
  @Volatile var activeStyle: PsrStyle? = null
  @Volatile var activeSection: StyleSection = StyleSection.MAIN_A
  @Volatile var pendingMainSection: StyleSection? = null
  @Volatile var chordRootPc = 0 // 0=C
  @Volatile var chordIntervals: List<Int> = listOf(0, 4, 7)
  @Volatile var channelEnabled: Map<StyleChannel, Boolean> = StyleChannel.entries.associateWith { true }
  @Volatile var fadeMultiplier = 1.0f
  @Volatile var fadeTarget = 1.0f

  // Song / WAV playback state
  @Volatile var songPlaybackSamples: FloatArray? = null
  @Volatile var songPlaybackRate: Int = SAMPLE_RATE
  @Volatile var isSongPlaying = false
  @Volatile var songRepeat = false
  private var songCursor = 0.0

  // Recording state
  @Volatile var isRecording = false
  private val recordedBuffer = ArrayList<Short>(SAMPLE_RATE * 10)
  private val recordLock = Any()

  // Drum Kit Auto Player state
  @Volatile var isDrumAutoPlaying = false
  @Volatile var activeDrumGroove: DrumGroovePreset? = null
  @Volatile var drumAutoFillPending = false
  @Volatile var drumAutoVolume = 0.90f
  private var drumAutoFillActive = false
  private var drumAutoAccum = 0.0
  private var drumAutoStep = 0

  // Metering & sequencer callbacks
  @Volatile var peakMeterLevel = 0f
  var onStepChanged: ((step: Int, beatInBar: Int, bar: Int, autoSwitchedSection: StyleSection?) -> Unit)? = null
  var onDrumAutoStepChanged: ((step: Int, fillCompleted: Boolean) -> Unit)? = null

  private var audioThread: Thread? = null
  @Volatile private var running = false

  // Reverb comb delay lines (mono/stereo Schroeder)
  private val revBufL = FloatArray(4799)
  private val revBufR = FloatArray(5113)
  private var revIdxL = 0
  private var revIdxR = 0
  private val rev2BufL = FloatArray(1613)
  private val rev2BufR = FloatArray(1777)
  private var rev2IdxL = 0
  private var rev2IdxR = 0

  // Chorus delay line
  private val chorusBuf = FloatArray(4096)
  private var chorusWriteIdx = 0
  private var chorusLfoPhase = 0f
  private var modLfoPhase = 0f

  // Sequencer step tracking
  private var sampleAccum = 0.0
  private var currentStep = 0
  private var totalStepsPlayed = 0

  fun start() {
    if (running) return
    running = true
    audioThread = Thread(::audioLoop, "PsrS975AudioEngine").apply {
      priority = Thread.MAX_PRIORITY
      start()
    }
  }

  fun stop() {
    running = false
    audioThread?.join(500)
    audioThread = null
  }

  fun registerSf2Bank(bank: ImportedSf2Bank) {
    sf2Banks[bank.id] = bank
  }

  fun registerWavSample(wav: ImportedWavSample) {
    wavSamples[wav.id] = wav
  }

  fun unregisterImport(id: String) {
    sf2Banks.remove(id)
    wavSamples.remove(id)
  }

  fun noteOn(tag: String, midiNote: Int, velocity: Int, voice: PsrVoice, volume: Float = 0.85f) {
    if (!isPowerOn) return
    cmdQueue.add(EngineCmd.NoteOn(tag, midiNote.coerceIn(0, 127), velocity.coerceIn(1, 127), voice, volume))
  }

  fun noteOff(tag: String, midiNote: Int) {
    cmdQueue.add(EngineCmd.NoteOff(tag, midiNote.coerceIn(0, 127)))
  }

  fun triggerDrum(drumNote: Int, velocity: Int, volume: Float = 0.9f) {
    if (!isPowerOn) return
    cmdQueue.add(EngineCmd.DrumHit(drumNote, velocity.coerceIn(1, 127), currentDrumKitId, volume))
  }

  fun triggerWavOneShot(wav: ImportedWavSample, volume: Float = 0.9f) {
    if (!isPowerOn) return
    cmdQueue.add(EngineCmd.WavOneShot(wav, volume))
  }

  fun allNotesOff() {
    cmdQueue.add(EngineCmd.AllNotesOff)
  }

  fun resetSequencerPosition() {
    sampleAccum = 0.0
    currentStep = 0
    totalStepsPlayed = 0
  }

  fun resetDrumAutoPosition() {
    drumAutoAccum = 0.0
    drumAutoStep = 0
    drumAutoFillActive = false
  }

  fun startRecording() {
    synchronized(recordLock) {
      recordedBuffer.clear()
      isRecording = true
    }
  }

  fun stopRecordingAndSave(targetFile: File): ImportedWavSample? {
    val pcm: ShortArray
    synchronized(recordLock) {
      isRecording = false
      if (recordedBuffer.isEmpty()) return null
      pcm = recordedBuffer.toShortArray()
      recordedBuffer.clear()
    }
    WavParser.writePcm16StereoWav(targetFile, pcm, SAMPLE_RATE)
    return targetFile.inputStream().use { WavParser.parse(it, "rec_${System.currentTimeMillis()}", targetFile.name) }
  }

  fun loadSongForPlayback(wav: ImportedWavSample) {
    songPlaybackSamples = wav.samples
    songPlaybackRate = wav.sampleRate
    songCursor = 0.0
  }

  fun seekSongRelative(deltaSeconds: Float) {
    val samples = songPlaybackSamples ?: return
    val next = (songCursor + deltaSeconds * songPlaybackRate).coerceIn(0.0, (samples.size - 1).coerceAtLeast(0).toDouble())
    songCursor = next
  }

  private fun audioLoop() {
    Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
    val minBuf = AudioTrack.getMinBufferSize(
      SAMPLE_RATE,
      AudioFormat.CHANNEL_OUT_STEREO,
      AudioFormat.ENCODING_PCM_16BIT,
    ).coerceAtLeast(BLOCK_FRAMES * 8)

    val builder = AudioTrack.Builder()
      .setAudioAttributes(
        AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_MEDIA)
          .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
          .build()
      )
      .setAudioFormat(
        AudioFormat.Builder()
          .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
          .setSampleRate(SAMPLE_RATE)
          .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
          .build()
      )
      .setBufferSizeInBytes(minBuf)
      .setTransferMode(AudioTrack.MODE_STREAM)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      builder.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
    }

    val track = builder.build()
    try {
      track.play()
    } catch (_: Exception) {
      return
    }

    val mixL = FloatArray(BLOCK_FRAMES)
    val mixR = FloatArray(BLOCK_FRAMES)
    val pcmOut = ShortArray(BLOCK_FRAMES * 2)

    while (running) {
      processCommands()
      stepStyleSequencer(BLOCK_FRAMES)
      stepDrumAutoPlayer(BLOCK_FRAMES)

      mixL.fill(0f)
      mixR.fill(0f)

      if (isPowerOn) {
        renderBlock(mixL, mixR, BLOCK_FRAMES)
      }

      // Master volume, balance, soft clipping & PCM16 conversion
      val bal = stereoBalance.coerceIn(-1f, 1f)
      val gainL = masterVolume * (if (bal > 0f) 1f - bal * 0.85f else 1f)
      val gainR = masterVolume * (if (bal < 0f) 1f + bal * 0.85f else 1f)

      var peak = 0f
      for (i in 0 until BLOCK_FRAMES) {
        val sL = tanh((mixL[i] * gainL).toDouble()).toFloat()
        val sR = tanh((mixR[i] * gainR).toDouble()).toFloat()
        val absPeak = max(abs(sL), abs(sR))
        if (absPeak > peak) peak = absPeak

        val pL = (sL * 32767f).toInt().coerceIn(-32768, 32767).toShort()
        val pR = (sR * 32767f).toInt().coerceIn(-32768, 32767).toShort()
        pcmOut[i * 2] = pL
        pcmOut[i * 2 + 1] = pR
      }
      peakMeterLevel = peakMeterLevel * 0.65f + peak * 0.35f

      if (isRecording) {
        synchronized(recordLock) {
          if (recordedBuffer.size < SAMPLE_RATE * 2 * 180) { // max 3 minutes recording buffer
            for (s in pcmOut) recordedBuffer.add(s)
          }
        }
      }

      track.write(pcmOut, 0, pcmOut.size)
    }

    try {
      track.stop()
      track.release()
    } catch (_: Exception) {}
  }

  private fun processCommands() {
    while (true) {
      val cmd = cmdQueue.poll() ?: break
      when (cmd) {
        is EngineCmd.NoteOn -> allocateVoice(cmd)
        is EngineCmd.NoteOff -> releaseVoice(cmd.tag, cmd.midiNote)
        is EngineCmd.DrumHit -> allocateDrum(cmd)
        is EngineCmd.WavOneShot -> allocateWavOneShot(cmd)
        EngineCmd.AllNotesOff -> {
          for (v in voices) {
            if (v.active) {
              v.held = false
              v.sustained = false
              v.envStage = EnvStage.RELEASE
            }
          }
        }
        EngineCmd.ReleaseSustain -> {
          for (v in voices) {
            if (v.active && !v.held && v.sustained) {
              v.sustained = false
              v.envStage = EnvStage.RELEASE
            }
          }
        }
      }
    }
  }

  private fun findFreeVoice(): SynthVoice {
    for (v in voices) {
      if (!v.active) return v
    }
    // Steal oldest releasing or oldest active voice
    var oldest = voices[0]
    for (v in voices) {
      if (v.envStage == EnvStage.RELEASE && oldest.envStage != EnvStage.RELEASE) {
        oldest = v
      } else if (v.ageSamples > oldest.ageSamples) {
        oldest = v
      }
    }
    return oldest
  }

  private fun allocateVoice(cmd: EngineCmd.NoteOn) {
    if (cmd.voice.isDrumKit) {
      allocateDrum(EngineCmd.DrumHit(cmd.midiNote, cmd.velocity, cmd.voice.id, cmd.volume))
      return
    }
    val v = findFreeVoice()
    v.active = true
    v.isDrum = false
    v.tag = cmd.tag
    v.midiNote = cmd.midiNote
    v.velocityGain = (cmd.velocity / 127f).pow(1.25f)
    v.partVolume = cmd.volume
    v.voice = cmd.voice
    v.held = true
    v.sustained = false
    v.envStage = EnvStage.ATTACK
    v.envVal = 0f
    v.phase = 0f
    v.phase2 = 0.17f
    v.phase3 = 0.43f
    v.ageSamples = 0
    v.samplePos = 0.0
    v.lpState = 0f
    v.lpState2 = 0f

    // Subtle stereo key panning (bass slightly left, treble slightly right)
    val pan = ((cmd.midiNote - 60) / 48f).coerceIn(-0.45f, 0.45f)
    v.panL = ((1f - pan) * 0.7f).coerceIn(0.25f, 1f)
    v.panR = ((1f + pan) * 0.7f).coerceIn(0.25f, 1f)

    // Bind SF2 zone or WAV sample if applicable
    v.sf2Zone = null
    v.wavSample = null
    if (cmd.voice.timbre == SynthTimbre.SF2_ZONE && cmd.voice.sf2BankId != null) {
      val bank = sf2Banks[cmd.voice.sf2BankId]
      val preset = bank?.presets?.getOrNull(cmd.voice.sf2PresetIndex)
      if (preset != null) {
        v.sf2Zone = preset.zones.firstOrNull {
          cmd.midiNote in it.keyLow..it.keyHigh && cmd.velocity in it.velLow..it.velHigh
        } ?: preset.zones.firstOrNull()
        v.samplePos = (v.sf2Zone?.start ?: 0).toDouble()
      }
    } else if (cmd.voice.timbre == SynthTimbre.WAV_SAMPLE && cmd.voice.wavId != null) {
      v.wavSample = wavSamples[cmd.voice.wavId]
    }
  }

  private fun releaseVoice(tag: String, midiNote: Int) {
    for (v in voices) {
      if (v.active && !v.isDrum && v.tag == tag && v.midiNote == midiNote && v.held) {
        v.held = false
        if (sustainPedal && !tag.startsWith("STYLE_")) {
          v.sustained = true
        } else {
          v.envStage = EnvStage.RELEASE
        }
      }
    }
  }

  private fun allocateDrum(cmd: EngineCmd.DrumHit) {
    val v = findFreeVoice()
    v.active = true
    v.isDrum = true
    v.tag = "DRUM"
    v.drumNote = cmd.drumNote
    v.velocityGain = (cmd.velocity / 127f).pow(1.1f)
    v.partVolume = cmd.volume
    v.ageSamples = 0
    v.phase = 0f
    v.phase2 = 0f
    v.phase3 = 0f
    v.lpState = 0f
    v.drumNoiseSeed = (cmd.drumNote * 48271 + cmd.velocity * 997) or 1
    v.voice = null
    v.sf2Zone = null
    v.wavSample = null

    val kitBoost = when (cmd.kitId) {
      "v_edm_kit" -> 1.15f
      "v_real_kit" -> 0.95f
      else -> 1.0f
    }
    v.drumPitch = kitBoost

    v.drumTotalSamples = when (cmd.drumNote) {
      35, 36 -> (SAMPLE_RATE * 0.34f).toInt() // Kick
      38, 40, 37, 39 -> (SAMPLE_RATE * 0.24f).toInt() // Snare/Rim/Clap
      42, 44 -> (SAMPLE_RATE * 0.085f).toInt() // Closed Hat
      46 -> (SAMPLE_RATE * 0.36f).toInt() // Open Hat
      49, 51, 52, 55, 57, 59 -> (SAMPLE_RATE * 0.85f).toInt() // Cymbals
      else -> (SAMPLE_RATE * 0.26f).toInt() // Toms / Perc
    }
    v.drumRemainingSamples = v.drumTotalSamples

    val pan = when (cmd.drumNote) {
      42, 44, 46 -> -0.25f
      49 -> -0.35f
      51, 59 -> 0.35f
      48, 47 -> -0.15f
      43, 41 -> 0.22f
      else -> 0.0f
    }
    v.panL = ((1f - pan) * 0.72f).coerceIn(0.3f, 1f)
    v.panR = ((1f + pan) * 0.72f).coerceIn(0.3f, 1f)
  }

  private fun allocateWavOneShot(cmd: EngineCmd.WavOneShot) {
    val v = findFreeVoice()
    v.active = true
    v.isDrum = false
    v.tag = "PAD_WAV"
    v.midiNote = 60
    v.velocityGain = 1.0f
    v.partVolume = cmd.volume
    v.voice = PsrVoice(
      id = cmd.wav.id,
      name = cmd.wav.displayName,
      category = VoiceCategory.EXPANSION_USER,
      isSuperArticulation = false,
      timbre = SynthTimbre.WAV_SAMPLE,
      attackSec = 0.002f,
      decaySec = 0.1f,
      sustainLevel = 1.0f,
      releaseSec = 0.05f,
      wavId = cmd.wav.id,
    )
    v.wavSample = cmd.wav
    v.sf2Zone = null
    v.held = true
    v.sustained = false
    v.envStage = EnvStage.SUSTAIN
    v.envVal = 1.0f
    v.samplePos = 0.0
    v.ageSamples = 0
    v.panL = 0.707f
    v.panR = 0.707f
  }

  private fun stepStyleSequencer(frames: Int) {
    if (!isStylePlaying) return
    val style = activeStyle ?: return

    // Fade In/Out smoothing
    if (abs(fadeMultiplier - fadeTarget) > 0.0005f) {
      val stepFade = (frames.toFloat() / (SAMPLE_RATE * 2.5f))
      fadeMultiplier = if (fadeMultiplier < fadeTarget) {
        min(fadeTarget, fadeMultiplier + stepFade)
      } else {
        max(fadeTarget, fadeMultiplier - stepFade)
      }
      if (fadeTarget == 0f && fadeMultiplier <= 0.002f) {
        isStylePlaying = false
        fadeMultiplier = 1f
        fadeTarget = 1f
        return
      }
    }

    val samplesPer16th = (SAMPLE_RATE * 60.0) / (tempoBpm.coerceIn(40, 240) * 4.0)
    sampleAccum += frames
    while (sampleAccum >= samplesPer16th) {
      sampleAccum -= samplesPer16th
      val secData = style.sections[activeSection]
        ?: style.sections[StyleSection.MAIN_A]
        ?: return

      val stepIdx = currentStep % secData.lengthSteps
      triggerSectionStep(secData, stepIdx)

      val beatInBar = (stepIdx / 4) % style.timeSigNum
      val bar = (totalStepsPlayed / (style.timeSigNum * 4)) + 1
      var switched: StyleSection? = null

      currentStep++
      totalStepsPlayed++
      if (currentStep >= secData.lengthSteps) {
        currentStep = 0
        // Auto transitions from Intro/Break -> Main, Ending -> Stop
        when {
          activeSection == StyleSection.INTRO_1 || activeSection == StyleSection.INTRO_2 ||
            activeSection == StyleSection.INTRO_3 || activeSection == StyleSection.BREAK -> {
            val target = pendingMainSection ?: StyleSection.MAIN_A
            activeSection = target
            pendingMainSection = null
            switched = target
          }
          activeSection == StyleSection.ENDING_1 || activeSection == StyleSection.ENDING_2 ||
            activeSection == StyleSection.ENDING_3 -> {
            isStylePlaying = false
            switched = activeSection
          }
        }
      }
      onStepChanged?.invoke(stepIdx, beatInBar, bar, switched)
    }
  }

  private fun stepDrumAutoPlayer(frames: Int) {
    if (!isDrumAutoPlaying || !isPowerOn) return
    val groove = activeDrumGroove ?: return
    val samplesPer16th = (SAMPLE_RATE * 60.0) / (tempoBpm.coerceIn(40, 240) * 4.0)
    drumAutoAccum += frames
    while (drumAutoAccum >= samplesPer16th) {
      drumAutoAccum -= samplesPer16th
      val stepIdx = drumAutoStep % 16
      if (stepIdx == 0 && drumAutoFillPending) {
        drumAutoFillActive = true
        drumAutoFillPending = false
      }
      val list = if (drumAutoFillActive) groove.fillSteps else groove.steps
      for (ev in list) {
        if (ev.step == stepIdx) {
          allocateDrum(EngineCmd.DrumHit(ev.drumNote, ev.velocity, currentDrumKitId, drumAutoVolume))
        }
      }
      var fillDone = false
      drumAutoStep++
      if (drumAutoStep >= 16) {
        drumAutoStep = 0
        if (drumAutoFillActive) {
          drumAutoFillActive = false
          fillDone = true
        }
      }
      onDrumAutoStepChanged?.invoke(stepIdx, fillDone)
    }
  }

  private fun triggerSectionStep(secData: StyleSectionData, step: Int) {
    val styleVol = 0.68f * fadeMultiplier
    val enabledMap = channelEnabled
    for (ev in secData.events) {
      if (ev.step != step) continue
      if (enabledMap[ev.channel] != true) continue

      when (ev.channel) {
        StyleChannel.RHY1, StyleChannel.RHY2 -> {
          allocateDrum(EngineCmd.DrumHit(ev.note, ev.velocity, currentDrumKitId, styleVol * 0.95f))
        }
        StyleChannel.BASS -> {
          val bassNote = transposeStyleNote(ev.note, isBass = true)
          triggerStyleTonal(ev.channel, bassNote, ev.velocity, styleBassVoice, styleVol * 0.85f, ev.durationSteps)
        }
        StyleChannel.CHD1 -> {
          val n = transposeStyleNote(ev.note, isBass = false)
          triggerStyleTonal(ev.channel, n, ev.velocity, styleGuitarVoice, styleVol * 0.60f, ev.durationSteps)
        }
        StyleChannel.CHD2 -> {
          val n = transposeStyleNote(ev.note, isBass = false)
          triggerStyleTonal(ev.channel, n, ev.velocity, stylePianoVoice, styleVol * 0.55f, ev.durationSteps)
        }
        StyleChannel.PAD -> {
          val n = transposeStyleNote(ev.note, isBass = false)
          triggerStyleTonal(ev.channel, n, ev.velocity, stylePadVoice, styleVol * 0.45f, ev.durationSteps)
        }
        StyleChannel.PHR1, StyleChannel.PHR2 -> {
          val n = transposeStyleNote(ev.note, isBass = false)
          triggerStyleTonal(ev.channel, n, ev.velocity, styleBrassVoice, styleVol * 0.55f, ev.durationSteps)
        }
      }
    }
  }

  private fun transposeStyleNote(sourceNote: Int, isBass: Boolean): Int {
    val pc = ((sourceNote % 12) + 12) % 12
    val octaveBase = sourceNote - pc
    if (isBass) {
      return (octaveBase + chordRootPc).coerceIn(24, 60)
    }
    // Map C-major triad notes (C=0, E=4, G=7) to active chord intervals
    val mappedInterval = when (pc) {
      0 -> chordIntervals.getOrElse(0) { 0 }
      4 -> chordIntervals.getOrElse(1) { 4 }
      7 -> chordIntervals.getOrElse(2) { 7 }
      11, 10 -> chordIntervals.getOrElse(3) { pc }
      else -> pc
    }
    return (octaveBase + chordRootPc + mappedInterval).coerceIn(36, 96)
  }

  private fun triggerStyleTonal(
    ch: StyleChannel,
    note: Int,
    vel: Int,
    voice: PsrVoice,
    vol: Float,
    durSteps: Int,
  ) {
    val v = findFreeVoice()
    v.active = true
    v.isDrum = false
    v.tag = "STYLE_${ch.name}"
    v.midiNote = note
    v.velocityGain = (vel / 127f).pow(1.2f)
    v.partVolume = vol
    v.voice = voice
    v.held = false
    v.sustained = false
    v.envStage = EnvStage.ATTACK
    v.envVal = 0f
    v.phase = 0f
    v.phase2 = 0.25f
    v.phase3 = 0.5f
    v.ageSamples = 0
    v.sf2Zone = null
    v.wavSample = null
    v.lpState = 0f
    v.lpState2 = 0f
    v.panL = 0.68f
    v.panR = 0.68f
    // Use drumRemainingSamples as an auto-gate timer in samples for style notes
    val samplesPer16th = ((SAMPLE_RATE * 60.0) / (tempoBpm.coerceIn(40, 240) * 4.0)).toInt()
    v.drumTotalSamples = max(256, durSteps * samplesPer16th)
    v.drumRemainingSamples = v.drumTotalSamples
  }

  private fun renderBlock(outL: FloatArray, outR: FloatArray, frames: Int) {
    val bendFactor = 2.0.pow((pitchBendSemitones / 12.0)).toFloat()
    val modDepth = modulationDepth.coerceIn(0f, 1f)
    val cutoffCtrl = filterCutoffNorm.coerceIn(0.08f, 1.0f)
    val resoCtrl = (resonanceNorm * 0.72f).coerceIn(0f, 0.75f)
    val invSr = 1f / SAMPLE_RATE

    for (i in 0 until frames) {
      modLfoPhase += 5.6f * invSr
      if (modLfoPhase >= 1f) modLfoPhase -= 1f
      val lfo = sin((modLfoPhase * TWO_PI).toDouble()).toFloat()
      val vibratoMult = 1f + lfo * modDepth * 0.022f

      var dryL = 0f
      var dryR = 0f

      for (v in voices) {
        if (!v.active) continue
        v.ageSamples++

        if (v.isDrum) {
          val sample = renderDrumSample(v)
          val g = sample * v.velocityGain * v.partVolume
          dryL += g * v.panL
          dryR += g * v.panR
          v.drumRemainingSamples--
          if (v.drumRemainingSamples <= 0) v.active = false
          continue
        }

        // Auto-gate for style accompaniment notes
        if (v.tag.startsWith("STYLE_") && v.envStage != EnvStage.RELEASE) {
          v.drumRemainingSamples--
          if (v.drumRemainingSamples <= 0) {
            v.envStage = EnvStage.RELEASE
          }
        }

        val voiceCfg = v.voice ?: continue
        val env = advanceEnvelope(v, voiceCfg, invSr)
        if (!v.active) continue

        val freq = (440.0 * 2.0.pow((v.midiNote - 69) / 12.0)).toFloat() * bendFactor * vibratoMult

        var rawSample = when (voiceCfg.timbre) {
          SynthTimbre.SF2_ZONE -> renderSf2Sample(v, bendFactor * vibratoMult)
          SynthTimbre.WAV_SAMPLE -> renderWavSample(v, bendFactor * vibratoMult)
          SynthTimbre.CONCERT_GRAND, SynthTimbre.BRIGHT_GRAND -> renderPiano(v, freq, invSr, voiceCfg.brightness)
          SynthTimbre.SUITCASE_EP, SynthTimbre.FM_EP -> renderEPiano(v, freq, invSr, voiceCfg.timbre == SynthTimbre.FM_EP)
          SynthTimbre.JAZZ_ORGAN, SynthTimbre.PIPE_ORGAN -> renderOrgan(v, freq, invSr, lfo, modDepth, voiceCfg.timbre == SynthTimbre.PIPE_ORGAN)
          SynthTimbre.STEEL_GUITAR, SynthTimbre.NYLON_GUITAR, SynthTimbre.CLEAN_GUITAR -> renderGuitar(v, freq, invSr, voiceCfg.brightness)
          SynthTimbre.CONCERT_STRINGS, SynthTimbre.WARM_PAD -> renderStringsPad(v, freq, invSr, lfo, modDepth)
          SynthTimbre.BIG_BRASS, SynthTimbre.SAX_BRASS -> renderBrass(v, freq, invSr)
          SynthTimbre.SWEET_FLUTE, SynthTimbre.TENOR_SAX -> renderWoodwind(v, freq, invSr, lfo, modDepth, voiceCfg.timbre == SynthTimbre.TENOR_SAX)
          SynthTimbre.SAW_LEAD, SynthTimbre.ANALOG_PAD -> renderSynth(v, freq, invSr)
          SynthTimbre.SUB_BASS -> renderSubBass(v, freq, invSr)
          SynthTimbre.HQ_DRUM_KIT -> 0f
        }

        // State-variable / 2-pole resonant low-pass filter shaped by Live Control Knobs + Mod Wheel
        val modFilter = (1f - modDepth * 0.28f * (1f - lfo))
        val fc = (voiceCfg.brightness * cutoffCtrl * modFilter).coerceIn(0.06f, 0.96f)
        val fb = resoCtrl * (1f + 0.1f / (1f - fc + 0.1f))
        v.lpState += fc * (rawSample - v.lpState + fb * (v.lpState - v.lpState2))
        v.lpState2 += fc * (v.lpState - v.lpState2)
        rawSample = v.lpState2

        val outSample = rawSample * env * v.velocityGain * v.partVolume * 0.36f
        dryL += outSample * v.panL
        dryR += outSample * v.panR
      }

      // Mix Song / Recording WAV playback if active
      if (isSongPlaying) {
        val song = songPlaybackSamples
        if (song != null && song.isNotEmpty()) {
          val idx0 = songCursor.toInt()
          if (idx0 < song.size - 1) {
            val frac = (songCursor - idx0).toFloat()
            val s = song[idx0] + (song[idx0 + 1] - song[idx0]) * frac
            dryL += s * 0.72f
            dryR += s * 0.72f
            songCursor += songPlaybackRate.toDouble() / SAMPLE_RATE.toDouble()
          } else if (songRepeat) {
            songCursor = 0.0
          } else {
            isSongPlaying = false
            songCursor = 0.0
          }
        }
      }

      // Apply Stereo Chorus + Schroeder Reverb when DSP is enabled
      var wetL = dryL
      var wetR = dryR
      if (dspEnabled) {
        val mono = (dryL + dryR) * 0.5f
        // Chorus
        chorusLfoPhase += 0.85f * invSr
        if (chorusLfoPhase >= 1f) chorusLfoPhase -= 1f
        val cMod = 180f + 95f * sin((chorusLfoPhase * TWO_PI).toDouble()).toFloat()
        val readIdx = (chorusWriteIdx - cMod.toInt() + chorusBuf.size) % chorusBuf.size
        val cSample = chorusBuf[readIdx]
        chorusBuf[chorusWriteIdx] = mono
        chorusWriteIdx = (chorusWriteIdx + 1) % chorusBuf.size

        wetL += cSample * chorusDepth * 0.45f
        wetR -= cSample * chorusDepth * 0.45f

        // Stereo Reverb
        val rOutL = revBufL[revIdxL]
        val rOutR = revBufR[revIdxR]
        revBufL[revIdxL] = dryL + rOutL * 0.58f
        revBufR[revIdxR] = dryR + rOutR * 0.58f
        revIdxL = (revIdxL + 1) % revBufL.size
        revIdxR = (revIdxR + 1) % revBufR.size

        val r2L = rev2BufL[rev2IdxL]
        val r2R = rev2BufR[rev2IdxR]
        rev2BufL[rev2IdxL] = rOutL + r2L * 0.48f
        rev2BufR[rev2IdxR] = rOutR + r2R * 0.48f
        rev2IdxL = (rev2IdxL + 1) % rev2BufL.size
        rev2IdxR = (rev2IdxR + 1) % rev2BufR.size

        val rv = reverbDepth.coerceIn(0f, 0.75f)
        wetL += (rOutL + r2R * 0.6f) * rv * 0.42f
        wetR += (rOutR + r2L * 0.6f) * rv * 0.42f
      }

      outL[i] = wetL
      outR[i] = wetR
    }
  }

  private fun advanceEnvelope(v: SynthVoice, cfg: PsrVoice, invSr: Float): Float {
    when (v.envStage) {
      EnvStage.ATTACK -> {
        val inc = invSr / max(0.0015f, cfg.attackSec)
        v.envVal += inc
        if (v.envVal >= 1f) {
          v.envVal = 1f
          v.envStage = EnvStage.DECAY
        }
      }
      EnvStage.DECAY -> {
        val dec = invSr / max(0.01f, cfg.decaySec)
        v.envVal -= dec * (1f - cfg.sustainLevel * 0.5f)
        if (v.envVal <= cfg.sustainLevel) {
          v.envVal = cfg.sustainLevel
          v.envStage = EnvStage.SUSTAIN
        }
      }
      EnvStage.SUSTAIN -> {
        v.envVal = cfg.sustainLevel
      }
      EnvStage.RELEASE -> {
        val rel = invSr / max(0.015f, cfg.releaseSec)
        v.envVal -= rel * max(0.08f, v.envVal)
        if (v.envVal <= 0.0008f) {
          v.envVal = 0f
          v.envStage = EnvStage.DONE
          v.active = false
        }
      }
      EnvStage.DONE -> v.active = false
    }
    return v.envVal
  }

  private fun renderPiano(v: SynthVoice, freq: Float, invSr: Float, brightness: Float): Float {
    v.phase = (v.phase + freq * invSr) % 1f
    v.phase2 = (v.phase2 + freq * 1.0018f * invSr) % 1f // Slight unison detuning for stereo richness
    val tSec = v.ageSamples * invSr
    val hammer = exp(-tSec * 28f) * brightness
    val f1 = sin((v.phase * TWO_PI).toDouble()).toFloat()
    val f1b = sin((v.phase2 * TWO_PI).toDouble()).toFloat()
    val f2 = sin((v.phase * 2f * TWO_PI).toDouble()).toFloat() * (0.45f + hammer * 0.45f)
    val f3 = sin((v.phase * 3f * TWO_PI).toDouble()).toFloat() * (0.18f + hammer * 0.35f)
    val f4 = sin((v.phase * 4f * TWO_PI).toDouble()).toFloat() * hammer * 0.28f
    return (f1 * 0.55f + f1b * 0.25f + f2 * 0.32f + f3 * 0.18f + f4 * 0.14f)
  }

  private fun renderEPiano(v: SynthVoice, freq: Float, invSr: Float, isFm: Boolean): Float {
    v.phase = (v.phase + freq * invSr) % 1f
    val modRatio = if (isFm) 14f else 2f
    v.phase2 = (v.phase2 + freq * modRatio * invSr) % 1f
    val tSec = v.ageSamples * invSr
    val tineDecay = exp(-tSec * (if (isFm) 9f else 5.5f))
    val modIdx = tineDecay * (if (isFm) 1.8f else 0.9f)
    val modSig = sin((v.phase2 * TWO_PI).toDouble()).toFloat() * modIdx
    val carrier = sin(((v.phase * TWO_PI) + modSig).toDouble()).toFloat()
    val warmSub = sin((v.phase * TWO_PI).toDouble()).toFloat() * 0.45f
    return carrier * 0.65f + warmSub
  }

  private fun renderOrgan(v: SynthVoice, freq: Float, invSr: Float, lfo: Float, modDepth: Float, isPipe: Boolean): Float {
    v.phase = (v.phase + freq * invSr) % 1f
    val p = v.phase * TWO_PI
    val h1 = sin((p * 0.5f).toDouble()).toFloat() * 0.35f
    val h2 = sin(p.toDouble()).toFloat() * 0.55f
    val h3 = sin((p * 1.5f).toDouble()).toFloat() * 0.30f
    val h4 = sin((p * 2f).toDouble()).toFloat() * 0.35f
    val h6 = sin((p * 3f).toDouble()).toFloat() * (if (isPipe) 0.25f else 0.18f)
    val h8 = sin((p * 4f).toDouble()).toFloat() * 0.14f
    val rotary = 1f - modDepth * 0.22f * (0.5f + 0.5f * lfo)
    return (h1 + h2 + h3 + h4 + h6 + h8) * 0.65f * rotary
  }

  private fun renderGuitar(v: SynthVoice, freq: Float, invSr: Float, brightness: Float): Float {
    v.phase = (v.phase + freq * invSr) % 1f
    val tSec = v.ageSamples * invSr
    val pluckEnv = exp(-tSec * 6.5f)
    val saw = (v.phase * 2f - 1f)
    val tri = 1f - 4f * abs(v.phase - 0.5f)
    val harmonic = sin((v.phase * 3f * TWO_PI).toDouble()).toFloat() * pluckEnv * brightness * 0.35f
    return (saw * (0.28f + pluckEnv * 0.45f) + tri * 0.55f + harmonic) * 0.72f
  }

  private fun renderStringsPad(v: SynthVoice, freq: Float, invSr: Float, lfo: Float, modDepth: Float): Float {
    v.phase = (v.phase + freq * 0.9965f * invSr) % 1f
    v.phase2 = (v.phase2 + freq * 1.000f * invSr) % 1f
    v.phase3 = (v.phase3 + freq * 1.0038f * invSr) % 1f
    val s1 = (v.phase * 2f - 1f)
    val s2 = (v.phase2 * 2f - 1f)
    val s3 = (v.phase3 * 2f - 1f)
    val ensemble = (s1 + s2 + s3) * 0.28f
    val shimmer = sin((v.phase2 * TWO_PI).toDouble()).toFloat() * (0.25f + modDepth * 0.15f * lfo)
    return ensemble + shimmer
  }

  private fun renderBrass(v: SynthVoice, freq: Float, invSr: Float): Float {
    v.phase = (v.phase + freq * invSr) % 1f
    v.phase2 = (v.phase2 + freq * 1.002f * invSr) % 1f
    val saw1 = v.phase * 2f - 1f
    val saw2 = v.phase2 * 2f - 1f
    val pulse = if (v.phase < 0.35f) 0.5f else -0.5f
    return (saw1 * 0.45f + saw2 * 0.35f + pulse * 0.25f)
  }

  private fun renderWoodwind(v: SynthVoice, freq: Float, invSr: Float, lfo: Float, modDepth: Float, isSax: Boolean): Float {
    v.phase = (v.phase + freq * invSr) % 1f
    val fund = sin((v.phase * TWO_PI).toDouble()).toFloat()
    val odd3 = sin((v.phase * 3f * TWO_PI).toDouble()).toFloat() * (if (isSax) 0.32f else 0.14f)
    val even2 = sin((v.phase * 2f * TWO_PI).toDouble()).toFloat() * (if (isSax) 0.28f else 0.10f)
    v.drumNoiseSeed = v.drumNoiseSeed * 1664525 + 1013904223
    val breathNoise = ((v.drumNoiseSeed shr 16).toShort() / 32768f) * 0.045f * (1f + modDepth * 0.4f * abs(lfo))
    return (fund * 0.72f + even2 + odd3 + breathNoise)
  }

  private fun renderSynth(v: SynthVoice, freq: Float, invSr: Float): Float {
    v.phase = (v.phase + freq * 0.995f * invSr) % 1f
    v.phase2 = (v.phase2 + freq * 1.005f * invSr) % 1f
    val s1 = v.phase * 2f - 1f
    val s2 = v.phase2 * 2f - 1f
    val sub = sin((v.phase * 0.5f * TWO_PI).toDouble()).toFloat() * 0.35f
    return (s1 + s2) * 0.38f + sub
  }

  private fun renderSubBass(v: SynthVoice, freq: Float, invSr: Float): Float {
    v.phase = (v.phase + freq * invSr) % 1f
    val fund = sin((v.phase * TWO_PI).toDouble()).toFloat()
    val sat = tanh((fund * 1.6f).toDouble()).toFloat()
    val oct = sin((v.phase * 2f * TWO_PI).toDouble()).toFloat() * 0.25f
    return sat * 0.78f + oct
  }

  private fun renderSf2Sample(v: SynthVoice, pitchMult: Float): Float {
    val zone = v.sf2Zone ?: return 0f
    val pool = zone.samplePool
    val idx = v.samplePos.toInt()
    if (idx < zone.start || idx >= zone.end - 1) {
      if (zone.hasLoop && zone.endLoop > zone.startLoop) {
        val loopLen = (zone.endLoop - zone.startLoop).toDouble()
        v.samplePos = zone.startLoop + ((v.samplePos - zone.startLoop) % loopLen)
      } else {
        v.active = false
        return 0f
      }
    }
    val i0 = v.samplePos.toInt().coerceIn(zone.start, zone.end - 2)
    val frac = (v.samplePos - i0).toFloat()
    val s0 = pool[i0] / 32768f
    val s1 = pool[i0 + 1] / 32768f
    val sample = s0 + (s1 - s0) * frac

    val semitones = (v.midiNote - zone.rootKey) + (zone.fineTuneCents / 100.0)
    val ratio = (zone.sampleRate.toDouble() / SAMPLE_RATE.toDouble()) * 2.0.pow(semitones / 12.0) * pitchMult
    v.samplePos += ratio

    if (zone.hasLoop && v.samplePos >= zone.endLoop && v.envStage != EnvStage.RELEASE) {
      val loopLen = (zone.endLoop - zone.startLoop).toDouble()
      if (loopLen > 1.0) v.samplePos -= loopLen
    }
    return sample * 1.4f
  }

  private fun renderWavSample(v: SynthVoice, pitchMult: Float): Float {
    val wav = v.wavSample ?: return 0f
    val samples = wav.samples
    val idx = v.samplePos.toInt()
    if (idx >= samples.size - 1) {
      v.active = false
      return 0f
    }
    val frac = (v.samplePos - idx).toFloat()
    val s0 = samples[idx]
    val s1 = samples[idx + 1]
    val out = s0 + (s1 - s0) * frac

    val semitones = if (v.tag == "PAD_WAV") 0.0 else (v.midiNote - 60).toDouble()
    val ratio = (wav.sampleRate.toDouble() / SAMPLE_RATE.toDouble()) * 2.0.pow(semitones / 12.0) * pitchMult
    v.samplePos += ratio
    return out * 1.35f
  }

  private fun renderDrumSample(v: SynthVoice): Float {
    val tSec = v.ageSamples.toFloat() / SAMPLE_RATE
    v.drumNoiseSeed = v.drumNoiseSeed * 1664525 + 1013904223
    val noise = ((v.drumNoiseSeed shr 16).toShort() / 32768f)
    val pBoost = v.drumPitch

    return when (v.drumNote) {
      35, 36 -> { // High-Quality Punchy Kick
        val freq = (46f + 125f * exp(-tSec * 42f)) * pBoost
        v.phase = (v.phase + freq / SAMPLE_RATE) % 1f
        val sub = sin((v.phase * TWO_PI).toDouble()).toFloat() * exp(-tSec * 9.5f)
        val click = noise * exp(-tSec * 190f) * 0.45f
        tanh(((sub + click) * 1.55f).toDouble()).toFloat() * 0.85f
      }
      38, 40 -> { // Crisp Dual-Body Acoustic/Studio Snare
        val f1 = 185f * pBoost
        val f2 = 332f * pBoost
        v.phase = (v.phase + f1 / SAMPLE_RATE) % 1f
        v.phase2 = (v.phase2 + f2 / SAMPLE_RATE) % 1f
        val body = (sin((v.phase * TWO_PI).toDouble()).toFloat() * 0.6f +
          sin((v.phase2 * TWO_PI).toDouble()).toFloat() * 0.4f) * exp(-tSec * 22f)
        // High-pass flavor on snare wires
        v.lpState = v.lpState * 0.62f + noise * 0.38f
        val wire = (noise - v.lpState * 0.55f) * exp(-tSec * 15f)
        (body * 0.65f + wire * 0.85f) * 0.72f
      }
      37 -> { // Rimshot / Sidestick
        v.phase = (v.phase + 480f / SAMPLE_RATE) % 1f
        v.phase2 = (v.phase2 + 1240f / SAMPLE_RATE) % 1f
        val rim = (sin((v.phase * TWO_PI).toDouble()).toFloat() + sin((v.phase2 * TWO_PI).toDouble()).toFloat() * 0.6f) * exp(-tSec * 55f)
        (rim + noise * exp(-tSec * 85f) * 0.3f) * 0.65f
      }
      39 -> { // Studio Clap
        val burst = ((sin((tSec * 210f * TWO_PI).toDouble()).toFloat() * 0.4f + 0.6f) * exp(-tSec * 14f))
        v.lpState = v.lpState * 0.5f + noise * 0.5f
        (noise - v.lpState * 0.7f) * burst * 0.9f
      }
      42, 44, 46 -> { // Metallic FM Hi-Hat (Closed / Pedal / Open)
        val decayRate = if (v.drumNote == 46) 6.5f else 36f
        v.phase = (v.phase + 295f / SAMPLE_RATE) % 1f
        v.phase2 = (v.phase2 + 542f / SAMPLE_RATE) % 1f
        v.phase3 = (v.phase3 + 811f / SAMPLE_RATE) % 1f
        val sq1 = if (v.phase < 0.5f) 1f else -1f
        val sq2 = if (v.phase2 < 0.5f) 1f else -1f
        val sq3 = if (v.phase3 < 0.5f) 1f else -1f
        val metal = (sq1 * sq2 + sq3) * 0.35f + noise * 0.65f
        v.lpState = v.lpState * 0.35f + metal * 0.65f
        val hp = metal - v.lpState * 0.85f
        hp * exp(-tSec * decayRate) * 0.58f
      }
      49, 51, 52, 55, 57, 59 -> { // Shimmering Crash / Ride Cymbal
        val decay = if (v.drumNote == 51 || v.drumNote == 59) 4.5f else 2.8f
        v.phase = (v.phase + 312f / SAMPLE_RATE) % 1f
        v.phase2 = (v.phase2 + 647f / SAMPLE_RATE) % 1f
        val bell = sin((v.phase * 4.2f * TWO_PI).toDouble()).toFloat() * 0.3f
        val shimmer = ((if (v.phase < 0.5f) 1f else -1f) * (if (v.phase2 < 0.5f) 1f else -1f)) * 0.35f + noise * 0.65f
        v.lpState = v.lpState * 0.42f + shimmer * 0.58f
        ((shimmer - v.lpState * 0.7f) + bell) * exp(-tSec * decay) * 0.50f
      }
      else -> { // Toms & Percussion (Conga, Bongo, Tambourine, Cowbell)
        val baseFreq = when (v.drumNote) {
          41 -> 88f
          43 -> 104f
          45 -> 124f
          47 -> 148f
          48 -> 174f
          50 -> 208f
          54 -> 2800f
          56 -> 560f
          60 -> 340f
          61 -> 260f
          69 -> 4400f
          else -> 160f + (v.drumNote % 12) * 22f
        }
        if (v.drumNote == 54 || v.drumNote == 69) {
          // Shaker / Tambourine
          v.lpState = v.lpState * 0.3f + noise * 0.7f
          (noise - v.lpState * 0.8f) * exp(-tSec * 22f) * 0.48f
        } else {
          val sweep = baseFreq * (1f + 0.45f * exp(-tSec * 28f)) * pBoost
          v.phase = (v.phase + sweep / SAMPLE_RATE) % 1f
          sin((v.phase * TWO_PI).toDouble()).toFloat() * exp(-tSec * 12f) * 0.72f
        }
      }
    }
  }

  // Internal accompaniment voices used by Style Sequencer
  private val styleBassVoice = PsrVoice("sb", "StyleBass", VoiceCategory.SYNTH, false, SynthTimbre.SUB_BASS, 0.004f, 0.35f, 0.60f, 0.14f, 0.62f)
  private val styleGuitarVoice = PsrVoice("sg", "StyleGuitar", VoiceCategory.GUITAR, false, SynthTimbre.STEEL_GUITAR, 0.002f, 0.35f, 0.20f, 0.16f, 0.78f)
  private val stylePianoVoice = PsrVoice("sp", "StylePiano", VoiceCategory.PIANO, false, SynthTimbre.BRIGHT_GRAND, 0.002f, 0.45f, 0.25f, 0.18f, 0.78f)
  private val stylePadVoice = PsrVoice("spd", "StylePad", VoiceCategory.STRINGS, false, SynthTimbre.WARM_PAD, 0.08f, 0.25f, 0.80f, 0.35f, 0.56f)
  private val styleBrassVoice = PsrVoice("sbr", "StyleBrass", VoiceCategory.BRASS, false, SynthTimbre.BIG_BRASS, 0.015f, 0.18f, 0.80f, 0.18f, 0.82f)
}
