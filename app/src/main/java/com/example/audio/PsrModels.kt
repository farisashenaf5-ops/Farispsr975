package com.example.audio

enum class VoiceCategory(val label: String) {
  PIANO("PIANO"),
  E_PIANO("E.PIANO"),
  ORGAN("ORGAN"),
  GUITAR("GUITAR"),
  STRINGS("STRINGS"),
  BRASS("BRASS"),
  WOODWIND("WOODWIND"),
  SYNTH("SYNTH"),
  PERC_DRUM("PERC.&DRUM"),
  EXPANSION_USER("EXPANSION/USER"),
}

enum class SynthTimbre {
  CONCERT_GRAND,
  BRIGHT_GRAND,
  SUITCASE_EP,
  FM_EP,
  JAZZ_ORGAN,
  PIPE_ORGAN,
  STEEL_GUITAR,
  NYLON_GUITAR,
  CLEAN_GUITAR,
  CONCERT_STRINGS,
  WARM_PAD,
  BIG_BRASS,
  SAX_BRASS,
  SWEET_FLUTE,
  TENOR_SAX,
  SAW_LEAD,
  ANALOG_PAD,
  SUB_BASS,
  HQ_DRUM_KIT,
  SF2_ZONE,
  WAV_SAMPLE,
}

data class PsrVoice(
  val id: String,
  val name: String,
  val category: VoiceCategory,
  val isSuperArticulation: Boolean,
  val timbre: SynthTimbre,
  val attackSec: Float = 0.008f,
  val decaySec: Float = 0.35f,
  val sustainLevel: Float = 0.65f,
  val releaseSec: Float = 0.35f,
  val brightness: Float = 0.72f,
  val vibratoSensitivity: Float = 1.0f,
  val isDrumKit: Boolean = false,
  val sf2BankId: String? = null,
  val sf2PresetIndex: Int = -1,
  val wavId: String? = null,
)

enum class StyleCategory(val label: String) {
  POP_ROCK("POP&ROCK"),
  DANCE("DANCE"),
  BALLAD("BALLAD"),
  SWING_JAZZ("SWING&JAZZ"),
  RNB("R&B"),
  COUNTRY("COUNTRY"),
  LATIN("LATIN"),
  ENTERTAINER("ENTERTAINER"),
  WORLD("WORLD"),
  EXPANSION_USER("EXPANSION/USER"),
}

enum class StyleSection(val label: String) {
  INTRO_1("INTRO I"),
  INTRO_2("INTRO II"),
  INTRO_3("INTRO III"),
  MAIN_A("MAIN A"),
  MAIN_B("MAIN B"),
  MAIN_C("MAIN C"),
  MAIN_D("MAIN D"),
  BREAK("BREAK"),
  ENDING_1("ENDING I"),
  ENDING_2("ENDING II"),
  ENDING_3("ENDING III"),
}

enum class StyleChannel(val label: String) {
  RHY1("RHY1"),
  RHY2("RHY2"),
  BASS("BASS"),
  CHD1("CHD1"),
  CHD2("CHD2"),
  PAD("PAD"),
  PHR1("PHR1"),
  PHR2("PHR2"),
}

data class StyleNoteEvent(
  val step: Int, // 16th-note step within section (0 until lengthSteps)
  val durationSteps: Int,
  val channel: StyleChannel,
  val note: Int, // MIDI note relative to C major root (for tonal channels) or GM drum note
  val velocity: Int,
)

data class StyleSectionData(
  val section: StyleSection,
  val lengthSteps: Int, // e.g., 16, 32, 64 (16th notes)
  val events: List<StyleNoteEvent>,
)

data class PsrStyle(
  val id: String,
  val name: String,
  val category: StyleCategory,
  val defaultTempo: Int,
  val timeSigNum: Int = 4,
  val timeSigDen: Int = 4,
  val isImportedSty: Boolean = false,
  val sections: Map<StyleSection, StyleSectionData>,
  val otsVoiceIds: List<String> = emptyList(),
)

data class Sf2Zone(
  val samplePool: ShortArray,
  val start: Int,
  val end: Int,
  val startLoop: Int,
  val endLoop: Int,
  val sampleRate: Int,
  val rootKey: Int,
  val fineTuneCents: Int,
  val keyLow: Int,
  val keyHigh: Int,
  val velLow: Int,
  val velHigh: Int,
  val hasLoop: Boolean,
)

data class Sf2Preset(
  val name: String,
  val bank: Int,
  val presetNumber: Int,
  val zones: List<Sf2Zone>,
)

data class ImportedSf2Bank(
  val id: String,
  val fileName: String,
  val bankName: String,
  val presets: List<Sf2Preset>,
)

data class ImportedWavSample(
  val id: String,
  val fileName: String,
  val displayName: String,
  val sampleRate: Int,
  val samples: FloatArray,
  val durationMs: Long,
)

data class ImportedStyFile(
  val id: String,
  val fileName: String,
  val style: PsrStyle,
)

enum class PartId(val label: String) {
  LEFT("LEFT"),
  RIGHT1("RIGHT1"),
  RIGHT2("RIGHT2"),
  RIGHT3("RIGHT3"),
}

data class PartState(
  val partId: PartId,
  val isEnabled: Boolean,
  val voiceId: String,
  val volume: Float = 0.85f,
  val octaveShift: Int = 0,
)

data class FreezeOptions(
  val freezeStyle: Boolean = true,
  val freezeTempo: Boolean = true,
  val freezeTranspose: Boolean = true,
  val freezeVoices: Boolean = false,
  val freezeHarmony: Boolean = false,
)

data class RegistrationSlot(
  val slotNumber: Int, // 1..8
  val label: String,
  val isEmpty: Boolean,
  val styleId: String,
  val variation: StyleSection,
  val tempo: Int,
  val transpose: Int,
  val upperOctave: Int,
  val acmpEnabled: Boolean,
  val parts: Map<PartId, PartState>,
  val sustainEnabled: Boolean,
  val dspEnabled: Boolean,
  val harmonyEnabled: Boolean,
  val modulation: Float,
  val channelEnabled: Map<StyleChannel, Boolean>,
)

data class MultiPadItem(
  val id: Int, // 1..4
  val title: String,
  val drumNotes: List<Int> = emptyList(),
  val tonalNotes: List<Int> = emptyList(),
  val wavId: String? = null,
)

data class DrumPatternStep(
  val step: Int, // 0..15
  val drumNote: Int, // GM drum MIDI note (36=Kick, 38=Snare, 42=CHat, 46=OHat, 49=Crash, 45/48=Tom, 60/61=Perc)
  val velocity: Int,
)

data class DrumGroovePreset(
  val id: String,
  val name: String,
  val kitId: String,
  val defaultTempo: Int,
  val steps: List<DrumPatternStep>,
  val fillSteps: List<DrumPatternStep>,
)

