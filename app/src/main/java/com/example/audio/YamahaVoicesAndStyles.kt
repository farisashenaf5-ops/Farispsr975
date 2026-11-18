package com.example.audio

object YamahaLibrary {

  val factoryVoices: List<PsrVoice> = listOf(
    // PIANO
    PsrVoice("v_grand_piano", "GrandPiano", VoiceCategory.PIANO, true, SynthTimbre.CONCERT_GRAND, 0.003f, 0.9f, 0.35f, 0.45f, 0.80f, 0.4f),
    PsrVoice("v_concert_grand", "ConcertGrand", VoiceCategory.PIANO, true, SynthTimbre.CONCERT_GRAND, 0.002f, 1.1f, 0.32f, 0.50f, 0.85f, 0.35f),
    PsrVoice("v_bright_piano", "BrightPiano", VoiceCategory.PIANO, false, SynthTimbre.BRIGHT_GRAND, 0.002f, 0.85f, 0.30f, 0.40f, 0.94f, 0.35f),
    PsrVoice("v_mellow_grand", "MellowGrand", VoiceCategory.PIANO, false, SynthTimbre.CONCERT_GRAND, 0.005f, 0.95f, 0.38f, 0.55f, 0.55f, 0.35f),

    // E.PIANO
    PsrVoice("v_suitcase_ep", "SuitcaseEP", VoiceCategory.E_PIANO, true, SynthTimbre.SUITCASE_EP, 0.003f, 0.70f, 0.38f, 0.42f, 0.78f, 0.9f),
    PsrVoice("v_dx_ep", "DX7ModernEP", VoiceCategory.E_PIANO, false, SynthTimbre.FM_EP, 0.002f, 0.80f, 0.32f, 0.48f, 0.86f, 0.8f),
    PsrVoice("v_wurli_ep", "WurliClassic", VoiceCategory.E_PIANO, true, SynthTimbre.SUITCASE_EP, 0.004f, 0.65f, 0.42f, 0.35f, 0.84f, 0.85f),

    // ORGAN
    PsrVoice("v_jazz_organ", "JazzOrgan", VoiceCategory.ORGAN, true, SynthTimbre.JAZZ_ORGAN, 0.005f, 0.05f, 0.95f, 0.12f, 0.78f, 1.4f),
    PsrVoice("v_pipe_organ", "CathedralOrgan", VoiceCategory.ORGAN, false, SynthTimbre.PIPE_ORGAN, 0.045f, 0.10f, 0.95f, 0.45f, 0.82f, 0.6f),
    PsrVoice("v_rock_organ", "RockDrawbar", VoiceCategory.ORGAN, false, SynthTimbre.JAZZ_ORGAN, 0.004f, 0.05f, 0.96f, 0.10f, 0.90f, 1.5f),

    // GUITAR
    PsrVoice("v_steel_guitar", "SteelGuitar", VoiceCategory.GUITAR, true, SynthTimbre.STEEL_GUITAR, 0.002f, 0.75f, 0.18f, 0.35f, 0.85f, 0.95f),
    PsrVoice("v_nylon_guitar", "NylonConcert", VoiceCategory.GUITAR, true, SynthTimbre.NYLON_GUITAR, 0.003f, 0.65f, 0.16f, 0.32f, 0.68f, 0.9f),
    PsrVoice("v_clean_strat", "CleanSolidGt", VoiceCategory.GUITAR, false, SynthTimbre.CLEAN_GUITAR, 0.002f, 0.80f, 0.25f, 0.30f, 0.88f, 1.1f),

    // STRINGS
    PsrVoice("v_warm_pad", "WarmPad", VoiceCategory.STRINGS, true, SynthTimbre.WARM_PAD, 0.14f, 0.30f, 0.88f, 0.65f, 0.60f, 0.8f),
    PsrVoice("v_concert_strings", "ConcertStrings", VoiceCategory.STRINGS, true, SynthTimbre.CONCERT_STRINGS, 0.095f, 0.25f, 0.90f, 0.55f, 0.78f, 1.1f),
    PsrVoice("v_slow_strings", "CinemaStrings", VoiceCategory.STRINGS, false, SynthTimbre.CONCERT_STRINGS, 0.22f, 0.30f, 0.92f, 0.75f, 0.72f, 1.0f),

    // BRASS
    PsrVoice("v_big_brass", "BigBandBrass", VoiceCategory.BRASS, true, SynthTimbre.BIG_BRASS, 0.025f, 0.18f, 0.85f, 0.22f, 0.88f, 1.0f),
    PsrVoice("v_french_horn", "FrenchHorns", VoiceCategory.BRASS, true, SynthTimbre.BIG_BRASS, 0.055f, 0.20f, 0.86f, 0.32f, 0.64f, 0.85f),
    PsrVoice("v_sax_brass", "BrassSection", VoiceCategory.BRASS, false, SynthTimbre.SAX_BRASS, 0.020f, 0.16f, 0.84f, 0.20f, 0.90f, 1.1f),

    // WOODWIND
    PsrVoice("v_sweet_flute", "SweetFlute", VoiceCategory.WOODWIND, true, SynthTimbre.SWEET_FLUTE, 0.042f, 0.15f, 0.88f, 0.24f, 0.74f, 1.35f),
    PsrVoice("v_tenor_sax", "SweetTenorSax", VoiceCategory.WOODWIND, true, SynthTimbre.TENOR_SAX, 0.032f, 0.20f, 0.85f, 0.22f, 0.80f, 1.3f),
    PsrVoice("v_clarinet", "ClarinetS.Art", VoiceCategory.WOODWIND, true, SynthTimbre.SWEET_FLUTE, 0.028f, 0.15f, 0.88f, 0.20f, 0.68f, 1.15f),

    // SYNTH
    PsrVoice("v_saw_lead", "SawLead", VoiceCategory.SYNTH, false, SynthTimbre.SAW_LEAD, 0.008f, 0.25f, 0.82f, 0.25f, 0.90f, 1.4f),
    PsrVoice("v_analog_pad", "AnalogSweep", VoiceCategory.SYNTH, false, SynthTimbre.ANALOG_PAD, 0.12f, 0.35f, 0.86f, 0.70f, 0.76f, 1.1f),
    PsrVoice("v_sub_bass", "FingerSubBass", VoiceCategory.SYNTH, false, SynthTimbre.SUB_BASS, 0.004f, 0.45f, 0.65f, 0.20f, 0.55f, 0.3f),

    // PERC.&DRUM (High Quality Yamaha Drum Kits)
    PsrVoice("v_power_kit", "PowerKit HQ", VoiceCategory.PERC_DRUM, true, SynthTimbre.HQ_DRUM_KIT, isDrumKit = true),
    PsrVoice("v_studio_kit", "StudioStdKit", VoiceCategory.PERC_DRUM, true, SynthTimbre.HQ_DRUM_KIT, isDrumKit = true),
    PsrVoice("v_real_kit", "RealAcousticKit", VoiceCategory.PERC_DRUM, true, SynthTimbre.HQ_DRUM_KIT, isDrumKit = true),
    PsrVoice("v_edm_kit", "EDM_T9_808Kit", VoiceCategory.PERC_DRUM, false, SynthTimbre.HQ_DRUM_KIT, isDrumKit = true),
  )

  val factoryStyles: List<PsrStyle> = listOf(
    buildStyle(
      id = "st_acoustic_pop",
      name = "AcousticPop",
      category = StyleCategory.POP_ROCK,
      tempo = 120,
      drumFeel = "POP",
      ots = listOf("v_grand_piano", "v_steel_guitar", "v_concert_strings", "v_sweet_flute"),
    ),
    buildStyle(
      id = "st_brit_rock",
      name = "BritStadiumRock",
      category = StyleCategory.POP_ROCK,
      tempo = 128,
      drumFeel = "ROCK",
      ots = listOf("v_bright_piano", "v_clean_strat", "v_rock_organ", "v_big_brass"),
    ),
    buildStyle(
      id = "st_euro_dance",
      name = "EuroClubDance",
      category = StyleCategory.DANCE,
      tempo = 128,
      drumFeel = "DANCE",
      ots = listOf("v_saw_lead", "v_analog_pad", "v_dx_ep", "v_sub_bass"),
    ),
    buildStyle(
      id = "st_modern_edm",
      name = "FutureHouse",
      category = StyleCategory.DANCE,
      tempo = 124,
      drumFeel = "DANCE",
      ots = listOf("v_analog_pad", "v_saw_lead", "v_bright_piano", "v_edm_kit"),
    ),
    buildStyle(
      id = "st_piano_ballad",
      name = "GrandBallad",
      category = StyleCategory.BALLAD,
      tempo = 76,
      drumFeel = "BALLAD",
      ots = listOf("v_concert_grand", "v_warm_pad", "v_concert_strings", "v_tenor_sax"),
    ),
    buildStyle(
      id = "st_epic_love",
      name = "8BeatSlowBallad",
      category = StyleCategory.BALLAD,
      tempo = 82,
      drumFeel = "BALLAD",
      ots = listOf("v_suitcase_ep", "v_slow_strings", "v_nylon_guitar", "v_sweet_flute"),
    ),
    buildStyle(
      id = "st_big_band",
      name = "BigBandSwing",
      category = StyleCategory.SWING_JAZZ,
      tempo = 138,
      drumFeel = "SWING",
      ots = listOf("v_big_brass", "v_jazz_organ", "v_tenor_sax", "v_grand_piano"),
    ),
    buildStyle(
      id = "st_smooth_jazz",
      name = "CoolJazzClub",
      category = StyleCategory.SWING_JAZZ,
      tempo = 108,
      drumFeel = "SWING",
      ots = listOf("v_suitcase_ep", "v_tenor_sax", "v_clean_strat", "v_jazz_organ"),
    ),
    buildStyle(
      id = "st_neo_soul",
      name = "NeoSoulGroove",
      category = StyleCategory.RNB,
      tempo = 94,
      drumFeel = "RNB",
      ots = listOf("v_suitcase_ep", "v_warm_pad", "v_jazz_organ", "v_tenor_sax"),
    ),
    buildStyle(
      id = "st_country_pop",
      name = "NashvillePop",
      category = StyleCategory.COUNTRY,
      tempo = 114,
      drumFeel = "POP",
      ots = listOf("v_steel_guitar", "v_bright_piano", "v_concert_strings", "v_clean_strat"),
    ),
    buildStyle(
      id = "st_bossa_nova",
      name = "BossaNova",
      category = StyleCategory.LATIN,
      tempo = 118,
      drumFeel = "LATIN",
      ots = listOf("v_nylon_guitar", "v_sweet_flute", "v_suitcase_ep", "v_warm_pad"),
    ),
    buildStyle(
      id = "st_salsa_caliente",
      name = "SalsaCaliente",
      category = StyleCategory.LATIN,
      tempo = 172,
      drumFeel = "LATIN",
      ots = listOf("v_big_brass", "v_bright_piano", "v_sax_brass", "v_jazz_organ"),
    ),
    buildStyle(
      id = "st_schlager_party",
      name = "ShowtimePolka",
      category = StyleCategory.ENTERTAINER,
      tempo = 132,
      drumFeel = "POP",
      ots = listOf("v_big_brass", "v_bright_piano", "v_clarinet", "v_pipe_organ"),
    ),
    buildStyle(
      id = "st_arabic_pop",
      name = "OrientalWorld",
      category = StyleCategory.WORLD,
      tempo = 106,
      drumFeel = "WORLD",
      ots = listOf("v_concert_strings", "v_nylon_guitar", "v_sweet_flute", "v_warm_pad"),
    ),
  )

  val defaultMultiPads: List<MultiPadItem> = listOf(
    MultiPadItem(1, "Perc Loop", drumNotes = listOf(54, 69, 60, 61)),
    MultiPadItem(2, "Crash & Tom", drumNotes = listOf(49, 48, 45, 36)),
    MultiPadItem(3, "Chord Arp", tonalNotes = listOf(60, 64, 67, 72)),
    MultiPadItem(4, "Brass Hit", tonalNotes = listOf(48, 60, 64, 67)),
  )

  val drumGrooves: List<DrumGroovePreset> = listOf(
    DrumGroovePreset(
      id = "dg_8beat_pop",
      name = "8-Beat Pop",
      kitId = "v_power_kit",
      defaultTempo = 116,
      steps = buildList {
        for (s in 0 until 16 step 2) add(DrumPatternStep(s, if (s == 14) 46 else 42, if (s % 4 == 0) 92 else 74))
        listOf(0, 6, 8, 10).forEach { add(DrumPatternStep(it, 36, 114)) }
        listOf(4, 12).forEach { add(DrumPatternStep(it, 38, 108)) }
      },
      fillSteps = standardTomSnareFill(),
    ),
    DrumGroovePreset(
      id = "dg_16beat_funk",
      name = "16-Beat Funk",
      kitId = "v_studio_kit",
      defaultTempo = 104,
      steps = buildList {
        for (s in 0 until 16) add(DrumPatternStep(s, if (s == 10 || s == 14) 46 else 42, if (s % 2 == 0) 88 else 64))
        listOf(0, 3, 6, 10, 14).forEach { add(DrumPatternStep(it, 36, 112)) }
        listOf(4, 7, 9, 12, 15).forEach { add(DrumPatternStep(it, 38, if (it == 4 || it == 12) 110 else 68)) }
        add(DrumPatternStep(2, 54, 72))
      },
      fillSteps = standardTomSnareFill(),
    ),
    DrumGroovePreset(
      id = "dg_modern_ballad",
      name = "Modern Ballad",
      kitId = "v_real_kit",
      defaultTempo = 76,
      steps = buildList {
        for (s in 0 until 16 step 2) add(DrumPatternStep(s, 42, 72))
        listOf(0, 10).forEach { add(DrumPatternStep(it, 36, 102)) }
        listOf(4, 12).forEach { add(DrumPatternStep(it, 38, 92)) }
        add(DrumPatternStep(6, 37, 74))
      },
      fillSteps = standardTomSnareFill(),
    ),
    DrumGroovePreset(
      id = "dg_edm_club",
      name = "EDM Club 4/4",
      kitId = "v_edm_kit",
      defaultTempo = 128,
      steps = buildList {
        for (s in 0 until 16 step 4) add(DrumPatternStep(s, 36, 122))
        for (s in 2 until 16 step 4) add(DrumPatternStep(s, 46, 94))
        listOf(4, 12).forEach { add(DrumPatternStep(it, 39, 112)) }
        listOf(1, 5, 9, 13).forEach { add(DrumPatternStep(it, 42, 68)) }
      },
      fillSteps = buildList {
        for (s in 0 until 16) add(DrumPatternStep(s, 38, 70 + s * 3))
        for (s in 0 until 16 step 2) add(DrumPatternStep(s, 36, 116))
        add(DrumPatternStep(15, 49, 118))
      },
    ),
    DrumGroovePreset(
      id = "dg_jazz_swing",
      name = "Jazz Swing",
      kitId = "v_real_kit",
      defaultTempo = 136,
      steps = buildList {
        listOf(0, 4, 7, 8, 12, 15).forEach { add(DrumPatternStep(it, 51, 90)) }
        listOf(4, 12).forEach { add(DrumPatternStep(it, 44, 82)) }
        listOf(0, 8).forEach { add(DrumPatternStep(it, 36, 86)) }
        listOf(7, 14).forEach { add(DrumPatternStep(it, 38, 76)) }
      },
      fillSteps = standardTomSnareFill(),
    ),
    DrumGroovePreset(
      id = "dg_latin_bossa",
      name = "Latin Bossa",
      kitId = "v_studio_kit",
      defaultTempo = 118,
      steps = buildList {
        for (s in 0 until 16 step 2) add(DrumPatternStep(s, 42, 78))
        listOf(0, 3, 6, 10, 14).forEach { add(DrumPatternStep(it, 37, 94)) }
        listOf(0, 6, 8, 14).forEach { add(DrumPatternStep(it, 36, 102)) }
        listOf(2, 5, 9, 13).forEach { add(DrumPatternStep(it, 69, 70)) }
      },
      fillSteps = standardTomSnareFill(),
    ),
    DrumGroovePreset(
      id = "dg_oriental_tabla",
      name = "Oriental Perc",
      kitId = "v_power_kit",
      defaultTempo = 108,
      steps = buildList {
        listOf(0, 6, 8).forEach { add(DrumPatternStep(it, 36, 112)) }
        listOf(4, 12).forEach { add(DrumPatternStep(it, 38, 102)) }
        for (s in 0 until 16 step 2) add(DrumPatternStep(s, 60, 86))
        listOf(2, 6, 10, 14).forEach { add(DrumPatternStep(it, 61, 84)) }
        listOf(1, 5, 9, 13).forEach { add(DrumPatternStep(it, 54, 72)) }
      },
      fillSteps = standardTomSnareFill(),
    ),
    DrumGroovePreset(
      id = "dg_rock_double",
      name = "Stadium Rock",
      kitId = "v_power_kit",
      defaultTempo = 132,
      steps = buildList {
        for (s in 0 until 16 step 2) add(DrumPatternStep(s, if (s % 4 == 0) 46 else 42, 92))
        listOf(0, 2, 6, 8, 10).forEach { add(DrumPatternStep(it, 36, 118)) }
        listOf(4, 12).forEach { add(DrumPatternStep(it, 38, 116)) }
        add(DrumPatternStep(0, 49, 106))
      },
      fillSteps = standardTomSnareFill(),
    ),
  )

  private fun standardTomSnareFill(): List<DrumPatternStep> = listOf(
    DrumPatternStep(0, 36, 112),
    DrumPatternStep(2, 38, 96),
    DrumPatternStep(4, 48, 100),
    DrumPatternStep(6, 48, 98),
    DrumPatternStep(8, 47, 104),
    DrumPatternStep(10, 45, 106),
    DrumPatternStep(12, 43, 108),
    DrumPatternStep(14, 38, 114),
    DrumPatternStep(15, 49, 118),
  )

  private fun buildStyle(
    id: String,
    name: String,
    category: StyleCategory,
    tempo: Int,
    drumFeel: String,
    ots: List<String>,
  ): PsrStyle {
    val sections = mutableMapOf<StyleSection, StyleSectionData>()
    sections[StyleSection.INTRO_1] = buildIntroSection(StyleSection.INTRO_1, 16, drumFeel)
    sections[StyleSection.INTRO_2] = buildIntroSection(StyleSection.INTRO_2, 32, drumFeel)
    sections[StyleSection.INTRO_3] = buildIntroSection(StyleSection.INTRO_3, 32, drumFeel)
    sections[StyleSection.MAIN_A] = buildMainSection(StyleSection.MAIN_A, 1, drumFeel)
    sections[StyleSection.MAIN_B] = buildMainSection(StyleSection.MAIN_B, 2, drumFeel)
    sections[StyleSection.MAIN_C] = buildMainSection(StyleSection.MAIN_C, 3, drumFeel)
    sections[StyleSection.MAIN_D] = buildMainSection(StyleSection.MAIN_D, 4, drumFeel)
    sections[StyleSection.BREAK] = buildBreakSection(drumFeel)
    sections[StyleSection.ENDING_1] = buildEndingSection(StyleSection.ENDING_1, 16)
    sections[StyleSection.ENDING_2] = buildEndingSection(StyleSection.ENDING_2, 32)
    sections[StyleSection.ENDING_3] = buildEndingSection(StyleSection.ENDING_3, 32)
    return PsrStyle(
      id = id,
      name = name,
      category = category,
      defaultTempo = tempo,
      sections = sections,
      otsVoiceIds = ots,
    )
  }

  private fun buildMainSection(sec: StyleSection, energy: Int, feel: String): StyleSectionData {
    val events = mutableListOf<StyleNoteEvent>()
    val len = 32 // 2 bars of 16th notes

    for (s in 0 until len) {
      val barStep = s % 16
      // RHY1: Kick & Snare
      when (feel) {
        "DANCE" -> {
          if (barStep % 4 == 0) events.add(StyleNoteEvent(s, 1, StyleChannel.RHY1, 36, 118))
          if (barStep == 4 || barStep == 12) events.add(StyleNoteEvent(s, 1, StyleChannel.RHY1, 39, 108))
          if (barStep % 4 == 2) events.add(StyleNoteEvent(s, 1, StyleChannel.RHY1, 46, 88))
        }
        "BALLAD" -> {
          if (barStep == 0 || barStep == 10) events.add(StyleNoteEvent(s, 1, StyleChannel.RHY1, 36, 98))
          if (barStep == 4 || barStep == 12) events.add(StyleNoteEvent(s, 1, StyleChannel.RHY1, 38, 86))
          if (barStep % 2 == 0) events.add(StyleNoteEvent(s, 1, StyleChannel.RHY1, 42, 68))
        }
        "LATIN" -> {
          if (barStep == 0 || barStep == 6 || barStep == 10) events.add(StyleNoteEvent(s, 1, StyleChannel.RHY1, 36, 106))
          if (barStep == 3 || barStep == 6 || barStep == 10 || barStep == 14) events.add(StyleNoteEvent(s, 1, StyleChannel.RHY1, 37, 94))
          if (barStep % 2 == 0) events.add(StyleNoteEvent(s, 1, StyleChannel.RHY1, 42, 82))
        }
        "SWING" -> {
          if (barStep == 0 || barStep == 8) events.add(StyleNoteEvent(s, 1, StyleChannel.RHY1, 36, 94))
          if (barStep == 0 || barStep == 4 || barStep == 7 || barStep == 8 || barStep == 12 || barStep == 15) {
            events.add(StyleNoteEvent(s, 1, StyleChannel.RHY1, 51, 88))
          }
          if (barStep == 4 || barStep == 12) events.add(StyleNoteEvent(s, 1, StyleChannel.RHY1, 44, 80))
        }
        "WORLD" -> {
          if (barStep == 0 || barStep == 6 || barStep == 8) events.add(StyleNoteEvent(s, 1, StyleChannel.RHY1, 36, 112))
          if (barStep == 4 || barStep == 12) events.add(StyleNoteEvent(s, 1, StyleChannel.RHY1, 38, 104))
          if (barStep % 2 == 0) events.add(StyleNoteEvent(s, 1, StyleChannel.RHY1, 60, 86))
          if (barStep % 4 == 2) events.add(StyleNoteEvent(s, 1, StyleChannel.RHY1, 61, 84))
        }
        else -> { // POP / ROCK / RNB
          if (barStep == 0 || barStep == 6 || (energy >= 2 && barStep == 10)) {
            events.add(StyleNoteEvent(s, 1, StyleChannel.RHY1, 36, 112))
          }
          if (barStep == 4 || barStep == 12) events.add(StyleNoteEvent(s, 1, StyleChannel.RHY1, 38, 106))
          if (barStep % 2 == 0) {
            val hat = if (energy >= 3 && barStep == 14) 46 else 42
            events.add(StyleNoteEvent(s, 1, StyleChannel.RHY1, hat, 80 + energy * 4))
          }
        }
      }

      // RHY2: Percussion layer (active from Main B onward)
      if (energy >= 2) {
        if (barStep % 2 == 1) events.add(StyleNoteEvent(s, 1, StyleChannel.RHY2, 69, 64))
        if (barStep == 4 || barStep == 12) events.add(StyleNoteEvent(s, 1, StyleChannel.RHY2, 54, 72))
        if (s == 0 && energy >= 3) events.add(StyleNoteEvent(s, 1, StyleChannel.RHY1, 49, 96))
      }

      // BASS (relative to C2 = 36)
      when {
        feel == "DANCE" && barStep % 2 == 1 -> events.add(StyleNoteEvent(s, 1, StyleChannel.BASS, 36, 102))
        barStep == 0 -> events.add(StyleNoteEvent(s, 4, StyleChannel.BASS, 36, 106))
        barStep == 6 -> events.add(StyleNoteEvent(s, 2, StyleChannel.BASS, 43, 94))
        barStep == 8 -> events.add(StyleNoteEvent(s, 4, StyleChannel.BASS, 36, 100))
        energy >= 2 && barStep == 12 -> events.add(StyleNoteEvent(s, 3, StyleChannel.BASS, 43, 92))
      }

      // CHD1 (Guitar / Piano rhythm)
      if (barStep == 0 || barStep == 6 || barStep == 12) {
        val vel = if (barStep == 0) 86 else 72
        events.add(StyleNoteEvent(s, 2, StyleChannel.CHD1, 60, vel))
        events.add(StyleNoteEvent(s, 2, StyleChannel.CHD1, 64, vel))
        events.add(StyleNoteEvent(s, 2, StyleChannel.CHD1, 67, vel))
      }

      // CHD2 (Arp/Sync comp)
      if (energy >= 2 && barStep % 4 == 2) {
        events.add(StyleNoteEvent(s, 2, StyleChannel.CHD2, 64, 66))
        events.add(StyleNoteEvent(s, 2, StyleChannel.CHD2, 67, 66))
      }

      // PAD (Sustained chord on bar starts)
      if (s % 16 == 0) {
        events.add(StyleNoteEvent(s, 14, StyleChannel.PAD, 60, 62))
        events.add(StyleNoteEvent(s, 14, StyleChannel.PAD, 64, 60))
        events.add(StyleNoteEvent(s, 14, StyleChannel.PAD, 67, 60))
      }

      // PHR1 & PHR2 (Melodic fills in Main C & D)
      if (energy >= 3 && s % 8 == 0) {
        events.add(StyleNoteEvent(s, 3, StyleChannel.PHR1, 72, 74))
      }
      if (energy >= 4 && s % 8 == 4) {
        events.add(StyleNoteEvent(s, 3, StyleChannel.PHR2, 76, 70))
      }
    }
    return StyleSectionData(sec, len, events)
  }

  private fun buildIntroSection(sec: StyleSection, len: Int, feel: String): StyleSectionData {
    val events = mutableListOf<StyleNoteEvent>()
    for (s in 0 until len) {
      val barStep = s % 16
      if (barStep == 0) events.add(StyleNoteEvent(s, 1, StyleChannel.RHY1, 49, 100))
      if (barStep % 4 == 0) events.add(StyleNoteEvent(s, 1, StyleChannel.RHY1, 36, 102))
      if (barStep == 4 || barStep == 12) events.add(StyleNoteEvent(s, 1, StyleChannel.RHY1, 38, 96))
      if (barStep % 2 == 0) events.add(StyleNoteEvent(s, 1, StyleChannel.RHY1, 42, 76))
      if (s % 8 == 0) {
        events.add(StyleNoteEvent(s, 6, StyleChannel.PAD, 60, 74))
        events.add(StyleNoteEvent(s, 6, StyleChannel.PAD, 64, 72))
        events.add(StyleNoteEvent(s, 6, StyleChannel.PAD, 67, 72))
        events.add(StyleNoteEvent(s, 4, StyleChannel.BASS, 36, 98))
      }
      if (s >= len - 4) {
        events.add(StyleNoteEvent(s, 1, StyleChannel.RHY1, 48 - (s - (len - 4)), 98))
      }
    }
    return StyleSectionData(sec, len, events)
  }

  private fun buildBreakSection(feel: String): StyleSectionData {
    val events = mutableListOf<StyleNoteEvent>()
    val len = 16
    events.add(StyleNoteEvent(0, 1, StyleChannel.RHY1, 49, 104))
    events.add(StyleNoteEvent(0, 2, StyleChannel.BASS, 36, 102))
    events.add(StyleNoteEvent(0, 4, StyleChannel.CHD1, 60, 86))
    events.add(StyleNoteEvent(0, 4, StyleChannel.CHD1, 64, 86))
    events.add(StyleNoteEvent(0, 4, StyleChannel.CHD1, 67, 86))
    events.add(StyleNoteEvent(8, 1, StyleChannel.RHY1, 38, 92))
    events.add(StyleNoteEvent(10, 1, StyleChannel.RHY1, 48, 94))
    events.add(StyleNoteEvent(12, 1, StyleChannel.RHY1, 45, 98))
    events.add(StyleNoteEvent(14, 1, StyleChannel.RHY1, 41, 104))
    events.add(StyleNoteEvent(15, 1, StyleChannel.RHY1, 38, 110))
    return StyleSectionData(StyleSection.BREAK, len, events)
  }

  private fun buildEndingSection(sec: StyleSection, len: Int): StyleSectionData {
    val events = mutableListOf<StyleNoteEvent>()
    for (s in 0 until len - 4 step 4) {
      events.add(StyleNoteEvent(s, 1, StyleChannel.RHY1, 36, 96))
      events.add(StyleNoteEvent(s + 2, 1, StyleChannel.RHY1, 42, 74))
      events.add(StyleNoteEvent(s, 3, StyleChannel.BASS, 36, 94))
    }
    val finalHit = len - 4
    events.add(StyleNoteEvent(finalHit, 1, StyleChannel.RHY1, 49, 114))
    events.add(StyleNoteEvent(finalHit, 1, StyleChannel.RHY1, 36, 116))
    events.add(StyleNoteEvent(finalHit, 4, StyleChannel.BASS, 36, 108))
    events.add(StyleNoteEvent(finalHit, 4, StyleChannel.PAD, 60, 84))
    events.add(StyleNoteEvent(finalHit, 4, StyleChannel.PAD, 64, 84))
    events.add(StyleNoteEvent(finalHit, 4, StyleChannel.PAD, 67, 84))
    return StyleSectionData(sec, len, events)
  }

  data class DetectedChord(
    val rootPc: Int, // 0..11 (C=0)
    val intervals: List<Int>,
    val displayName: String,
  )

  private val noteNames = arrayOf("C", "C#", "D", "Eb", "E", "F", "F#", "G", "Ab", "A", "Bb", "B")

  fun detectChord(midiNotes: Collection<Int>): DetectedChord? {
    if (midiNotes.isEmpty()) return null
    val sorted = midiNotes.sorted()
    val pcs = sorted.map { ((it % 12) + 12) % 12 }.distinct()
    val bassPc = ((sorted.first() % 12) + 12) % 12

    if (pcs.size == 1) {
      return DetectedChord(bassPc, listOf(0, 4, 7), noteNames[bassPc])
    }

    for (root in pcs) {
      val rel = pcs.map { (it - root + 12) % 12 }.toSet()
      val name = noteNames[root]
      when {
        rel.containsAll(setOf(0, 3, 7, 10)) -> return DetectedChord(root, listOf(0, 3, 7, 10), "${name}m7")
        rel.containsAll(setOf(0, 4, 7, 11)) -> return DetectedChord(root, listOf(0, 4, 7, 11), "${name}maj7")
        rel.containsAll(setOf(0, 4, 7, 10)) -> return DetectedChord(root, listOf(0, 4, 7, 10), "${name}7")
        rel.containsAll(setOf(0, 3, 7)) -> return DetectedChord(root, listOf(0, 3, 7), "${name}m")
        rel.containsAll(setOf(0, 4, 7)) -> return DetectedChord(root, listOf(0, 4, 7), name)
        rel.containsAll(setOf(0, 5, 7)) -> return DetectedChord(root, listOf(0, 5, 7), "${name}sus4")
        rel.containsAll(setOf(0, 3, 6)) -> return DetectedChord(root, listOf(0, 3, 6), "${name}dim")
      }
    }
    return DetectedChord(bassPc, listOf(0, 4, 7), noteNames[bassPc])
  }
}
