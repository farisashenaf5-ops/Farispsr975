package com.example.ui

import com.example.audio.FreezeOptions
import com.example.audio.ImportedSf2Bank
import com.example.audio.ImportedStyFile
import com.example.audio.ImportedWavSample
import com.example.audio.MultiPadItem
import com.example.audio.PartId
import com.example.audio.PartState
import com.example.audio.PsrStyle
import com.example.audio.PsrVoice
import com.example.audio.RegistrationSlot
import com.example.audio.StyleCategory
import com.example.audio.StyleChannel
import com.example.audio.StyleSection
import com.example.audio.VoiceCategory
import com.example.audio.YamahaLibrary

enum class LcdScreenMode {
  HOME_LIVE,
  VOICE_BROWSER,
  STYLE_BROWSER,
  EXPANSION_IMPORTER,
  DRUM_AUTO_PLAYER,
  REGISTRATION_BANK,
  FREEZE_SETTINGS,
  SONG_PLAYER,
  MIXER_EFFECTS,
  SETTINGS,
  HELP_GUIDE,
}

enum class KnobsAssignBank { A, B }

data class PsrS975UiState(
  val isPowerOn: Boolean = true,
  val lcdMode: LcdScreenMode = LcdScreenMode.HOME_LIVE,
  val isLcdZoomed: Boolean = false,
  val statusBanner: String? = null,

  // Master & Analog Knobs
  val masterVolume: Float = 0.85f,
  val stereoBalance: Float = 0.0f, // -1f .. +1f
  val micGain: Float = 0.35f,
  val vuMeterLevel: Float = 0f,

  // Pitch Bend & Modulation Wheels
  val pitchBend: Float = 0.0f, // -1f .. +1f (springs to 0)
  val modulation: Float = 0.0f, // 0f .. 1f (holds)
  val pitchBendRangeSemitones: Int = 2,

  // Live Control Knobs
  val knobsAssignBank: KnobsAssignBank = KnobsAssignBank.A,
  val liveKnob1: Float = 0.85f, // Bank A: Cutoff, Bank B: Reverb
  val liveKnob2: Float = 0.25f, // Bank A: Resonance, Bank B: Chorus

  // Transpose & Octave
  val transpose: Int = 0, // -12..+12
  val upperOctave: Int = 0, // -2..+2
  val baseKeyboardMidi: Int = 48, // C3..C6 (37 keys default)
  val keyboardKeyCount: Int = 37,
  val splitNote: Int = 59, // B3 split between LEFT and RIGHT

  // Voices & Parts
  val selectedPartForEdit: PartId = PartId.RIGHT1,
  val activeVoiceCategory: VoiceCategory = VoiceCategory.PIANO,
  val parts: Map<PartId, PartState> = mapOf(
    PartId.LEFT to PartState(PartId.LEFT, isEnabled = false, voiceId = "v_warm_pad", volume = 0.72f),
    PartId.RIGHT1 to PartState(PartId.RIGHT1, isEnabled = true, voiceId = "v_concert_grand", volume = 0.90f),
    PartId.RIGHT2 to PartState(PartId.RIGHT2, isEnabled = false, voiceId = "v_warm_pad", volume = 0.65f),
    PartId.RIGHT3 to PartState(PartId.RIGHT3, isEnabled = false, voiceId = "v_concert_strings", volume = 0.60f),
  ),
  val allVoices: List<PsrVoice> = YamahaLibrary.factoryVoices,

  // Voice Control
  val harmonyEnabled: Boolean = false,
  val sustainEnabled: Boolean = false,
  val dspEnabled: Boolean = true,

  // Style & Arranger
  val activeStyleCategory: StyleCategory = StyleCategory.POP_ROCK,
  val allStyles: List<PsrStyle> = YamahaLibrary.factoryStyles,
  val currentStyleId: String = "st_acoustic_pop",
  val activeVariation: StyleSection = StyleSection.MAIN_A,
  val tempoBpm: Int = 120,
  val isStylePlaying: Boolean = false,
  val acmpEnabled: Boolean = true,
  val syncStartEnabled: Boolean = false,
  val syncStopEnabled: Boolean = false,
  val fadeInOutActive: Boolean = false,
  val currentBeatInBar: Int = 0,
  val currentBar: Int = 1,
  val detectedChordName: String = "Cm",
  val channelEnabled: Map<StyleChannel, Boolean> = StyleChannel.entries.associateWith { true },
  val tapTempoCount: Int = 0,

  // Registration Memory & Freeze
  val currentBankNumber: Int = 1, // 1..8
  val activeRegSlot: Int? = 1, // 1..8
  val isMemoryArmed: Boolean = false,
  val isFreezeEnabled: Boolean = false,
  val freezeOptions: FreezeOptions = FreezeOptions(),
  val registrationBanks: Map<Int, List<RegistrationSlot>> = emptyMap(),

  // Multi Pads & OTS
  val multiPads: List<MultiPadItem> = YamahaLibrary.defaultMultiPads,
  val activeMultiPadId: Int? = null,
  val activeOtsSlot: Int? = 1,

  // Song & Recorder
  val songTitle: String = "NewSong",
  val isSongPlaying: Boolean = false,
  val songRepeat: Boolean = false,
  val isRecording: Boolean = false,
  val isDemoPlaying: Boolean = false,

  // Imported SF2 / STY / WAV
  val importedSf2Banks: List<ImportedSf2Bank> = emptyList(),
  val importedWavSamples: List<ImportedWavSample> = emptyList(),
  val importedStyFiles: List<ImportedStyFile> = emptyList(),
  val isImporting: Boolean = false,

  // Drum Kit Auto Player
  val drumGrooves: List<com.example.audio.DrumGroovePreset> = YamahaLibrary.drumGrooves,
  val selectedDrumGrooveId: String = "dg_8beat_pop",
  val selectedDrumKitId: String = "v_power_kit",
  val isDrumAutoPlaying: Boolean = false,
  val drumAutoCurrentStep: Int = 0,
  val drumAutoFillPending: Boolean = false,
  val drumAutoVolume: Float = 0.90f,

  // Active pressed keys for visual key depression
  val pressedMidiNotes: Set<Int> = emptySet(),
  val selectedBrowserIndex: Int = 0,
  val trackControlActiveIndex: Int = 1,
) {
  val currentStyle: PsrStyle
    get() = allStyles.firstOrNull { it.id == currentStyleId } ?: allStyles.first()

  val currentDrumGroove: com.example.audio.DrumGroovePreset
    get() = drumGrooves.firstOrNull { it.id == selectedDrumGrooveId } ?: drumGrooves.first()

  fun voiceById(id: String): PsrVoice =
    allVoices.firstOrNull { it.id == id } ?: allVoices.first()
}
