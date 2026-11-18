package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.FreezeOptions
import com.example.audio.ImportAndStorageRepository
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
import com.example.audio.SynthTimbre
import com.example.audio.VoiceCategory
import com.example.audio.YamahaAudioEngine
import com.example.audio.YamahaLibrary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PsrS975ViewModel(application: Application) : AndroidViewModel(application) {

  private val engine = YamahaAudioEngine()
  private val repo = ImportAndStorageRepository(application)

  private val _uiState = MutableStateFlow(PsrS975UiState())
  val uiState: StateFlow<PsrS975UiState> = _uiState.asStateFlow()

  private var lastTapMs = 0L
  private val tapIntervals = ArrayDeque<Long>(4)
  private var demoJob: Job? = null
  private var bannerJob: Job? = null
  private val activeLeftKeys = mutableSetOf<Int>()
  private val activeRightKeys = mutableMapOf<Int, List<Pair<String, Int>>>()

  init {
    engine.onStepChanged = { _, beatInBar, bar, autoSwitched ->
      _uiState.update { st ->
        val stillPlaying = engine.isStylePlaying
        st.copy(
          currentBeatInBar = beatInBar,
          currentBar = bar,
          isStylePlaying = stillPlaying,
          activeVariation = autoSwitched ?: st.activeVariation,
        )
      }
    }

    engine.onDrumAutoStepChanged = { step, fillDone ->
      _uiState.update { st ->
        st.copy(
          drumAutoCurrentStep = step,
          drumAutoFillPending = if (fillDone) false else st.drumAutoFillPending,
        )
      }
    }

    engine.start()
    loadPersistedState()
    startMeterPolling()
  }

  private fun startMeterPolling() {
    viewModelScope.launch {
      while (true) {
        delay(50)
        val level = engine.peakMeterLevel
        if (_uiState.value.vuMeterLevel != level) {
          _uiState.update { it.copy(vuMeterLevel = level) }
        }
      }
    }
  }

  private fun loadPersistedState() {
    viewModelScope.launch(Dispatchers.IO) {
      val imports = repo.loadAllImports()
      imports.sf2Banks.forEach { engine.registerSf2Bank(it) }
      imports.wavSamples.forEach { engine.registerWavSample(it) }

      val regs = repo.loadRegistrations { createFactoryRegistrations() }
      val freeze = repo.loadFreezeOptions()

      withContext(Dispatchers.Main) {
        _uiState.update { st ->
          val mergedVoices = buildMergedVoices(imports.sf2Banks, imports.wavSamples)
          val mergedStyles = buildMergedStyles(imports.styFiles)
          val updatedPads = updatePadsWithWavs(imports.wavSamples)
          st.copy(
            importedSf2Banks = imports.sf2Banks,
            importedWavSamples = imports.wavSamples,
            importedStyFiles = imports.styFiles,
            allVoices = mergedVoices,
            allStyles = mergedStyles,
            multiPads = updatedPads,
            registrationBanks = regs,
            freezeOptions = freeze,
          )
        }
        syncEngineFromState()
      }
    }
  }

  private fun createFactoryRegistrations(): Map<Int, List<RegistrationSlot>> {
    val defaultChannels = StyleChannel.entries.associateWith { true }
    val presets = listOf(
      Triple("GrandBallad", "st_piano_ballad", "v_concert_grand"),
      Triple("AcousticPop", "st_acoustic_pop", "v_grand_piano"),
      Triple("WarmStrings", "st_epic_love", "v_concert_strings"),
      Triple("SteelGuitar", "st_country_pop", "v_steel_guitar"),
      Triple("BigBandSwing", "st_big_band", "v_big_brass"),
      Triple("EuroClubDance", "st_euro_dance", "v_saw_lead"),
      Triple("BossaFlute", "st_bossa_nova", "v_sweet_flute"),
      Triple("PowerDrums", "st_brit_rock", "v_power_kit"),
    )
    return (1..8).associateWith { bank ->
      (1..8).map { slot ->
        val p = presets[(slot - 1) % presets.size]
        RegistrationSlot(
          slotNumber = slot,
          label = if (bank == 1) p.first else "Bank$bank-$slot",
          isEmpty = bank > 1,
          styleId = p.second,
          variation = StyleSection.MAIN_A,
          tempo = YamahaLibrary.factoryStyles.firstOrNull { it.id == p.second }?.defaultTempo ?: 120,
          transpose = 0,
          upperOctave = 0,
          acmpEnabled = true,
          parts = mapOf(
            PartId.LEFT to PartState(PartId.LEFT, false, "v_warm_pad", 0.72f),
            PartId.RIGHT1 to PartState(PartId.RIGHT1, true, p.third, 0.90f),
            PartId.RIGHT2 to PartState(PartId.RIGHT2, slot == 1 || slot == 3, "v_warm_pad", 0.62f),
            PartId.RIGHT3 to PartState(PartId.RIGHT3, false, "v_concert_strings", 0.60f),
          ),
          sustainEnabled = slot == 1 || slot == 3,
          dspEnabled = true,
          harmonyEnabled = false,
          modulation = 0f,
          channelEnabled = defaultChannels,
        )
      }
    }
  }

  private fun buildMergedVoices(
    sf2Banks: List<ImportedSf2Bank>,
    wavSamples: List<ImportedWavSample>,
  ): List<PsrVoice> {
    val list = YamahaLibrary.factoryVoices.toMutableList()
    for (bank in sf2Banks) {
      bank.presets.forEachIndexed { idx, preset ->
        list.add(
          PsrVoice(
            id = "sf2_${bank.id}_$idx",
            name = preset.name.take(16),
            category = VoiceCategory.EXPANSION_USER,
            isSuperArticulation = true,
            timbre = SynthTimbre.SF2_ZONE,
            attackSec = 0.004f,
            decaySec = 0.4f,
            sustainLevel = 0.85f,
            releaseSec = 0.35f,
            brightness = 0.92f,
            sf2BankId = bank.id,
            sf2PresetIndex = idx,
          )
        )
      }
    }
    for (wav in wavSamples) {
      list.add(
        PsrVoice(
          id = "wav_${wav.id}",
          name = wav.displayName.take(16),
          category = VoiceCategory.EXPANSION_USER,
          isSuperArticulation = false,
          timbre = SynthTimbre.WAV_SAMPLE,
          attackSec = 0.002f,
          decaySec = 0.2f,
          sustainLevel = 0.95f,
          releaseSec = 0.25f,
          brightness = 0.95f,
          wavId = wav.id,
        )
      )
    }
    return list
  }

  private fun buildMergedStyles(styFiles: List<ImportedStyFile>): List<PsrStyle> {
    return YamahaLibrary.factoryStyles + styFiles.map { it.style }
  }

  private fun updatePadsWithWavs(wavs: List<ImportedWavSample>): List<MultiPadItem> {
    val base = YamahaLibrary.defaultMultiPads.toMutableList()
    wavs.take(4).forEachIndexed { idx, wav ->
      base[idx] = MultiPadItem(
        id = idx + 1,
        title = wav.displayName.take(12),
        wavId = wav.id,
      )
    }
    return base
  }

  private fun syncEngineFromState() {
    val st = _uiState.value
    engine.isPowerOn = st.isPowerOn
    engine.masterVolume = st.masterVolume
    engine.stereoBalance = st.stereoBalance
    engine.micGain = st.micGain
    engine.pitchBendSemitones = st.pitchBend * st.pitchBendRangeSemitones
    engine.modulationDepth = st.modulation
    engine.sustainPedal = st.sustainEnabled
    engine.dspEnabled = st.dspEnabled
    engine.activeStyle = st.currentStyle
    engine.activeSection = st.activeVariation
    engine.tempoBpm = st.tempoBpm
    engine.channelEnabled = st.channelEnabled
    engine.activeDrumGroove = st.currentDrumGroove
    engine.drumAutoVolume = st.drumAutoVolume
    val r1Voice = st.parts[PartId.RIGHT1]?.voiceId?.let { st.voiceById(it) }
    engine.currentDrumKitId = if (r1Voice?.isDrumKit == true) r1Voice.id else st.selectedDrumKitId
  }

  // --- Power & Screen ---
  fun togglePower() {
    val next = !_uiState.value.isPowerOn
    if (!next) {
      engine.isStylePlaying = false
      engine.isDrumAutoPlaying = false
      engine.isSongPlaying = false
      demoJob?.cancel()
      engine.allNotesOff()
    }
    engine.isPowerOn = next
    _uiState.update {
      it.copy(
        isPowerOn = next,
        isStylePlaying = if (next) it.isStylePlaying else false,
        isDrumAutoPlaying = if (next) it.isDrumAutoPlaying else false,
        isSongPlaying = if (next) it.isSongPlaying else false,
        isDemoPlaying = if (next) it.isDemoPlaying else false,
        pressedMidiNotes = emptySet(),
      )
    }
  }

  fun setLcdMode(mode: LcdScreenMode) {
    if (!_uiState.value.isPowerOn) return
    _uiState.update { it.copy(lcdMode = mode, selectedBrowserIndex = 0) }
  }

  fun toggleLcdZoom() {
    if (!_uiState.value.isPowerOn) return
    _uiState.update { it.copy(isLcdZoomed = !it.isLcdZoomed) }
  }

  fun exitToHome() {
    if (!_uiState.value.isPowerOn) return
    _uiState.update { it.copy(lcdMode = LcdScreenMode.HOME_LIVE, isLcdZoomed = false, isMemoryArmed = false) }
  }

  // --- Knobs & Wheels ---
  fun setMasterVolume(v: Float) {
    val clamped = v.coerceIn(0f, 1f)
    engine.masterVolume = clamped
    _uiState.update { it.copy(masterVolume = clamped) }
  }

  fun setStereoBalance(v: Float) {
    val clamped = v.coerceIn(-1f, 1f)
    engine.stereoBalance = clamped
    _uiState.update { it.copy(stereoBalance = clamped) }
  }

  fun setMicGain(v: Float) {
    val clamped = v.coerceIn(0f, 1f)
    engine.micGain = clamped
    _uiState.update { it.copy(micGain = clamped) }
  }

  fun setPitchBend(normalized: Float) {
    val clamped = normalized.coerceIn(-1f, 1f)
    val st = _uiState.value
    engine.pitchBendSemitones = clamped * st.pitchBendRangeSemitones
    _uiState.update { it.copy(pitchBend = clamped) }
  }

  fun setModulation(normalized: Float) {
    val clamped = normalized.coerceIn(0f, 1f)
    engine.modulationDepth = clamped
    _uiState.update { it.copy(modulation = clamped) }
  }

  fun setPitchBendRange(semitones: Int) {
    val clamped = semitones.coerceIn(1, 12)
    engine.pitchBendSemitones = _uiState.value.pitchBend * clamped
    _uiState.update { it.copy(pitchBendRangeSemitones = clamped) }
  }

  fun selectKnobsAssign(bank: KnobsAssignBank) {
    _uiState.update { it.copy(knobsAssignBank = bank) }
  }

  fun setLiveKnob1(v: Float) {
    val clamped = v.coerceIn(0f, 1f)
    if (_uiState.value.knobsAssignBank == KnobsAssignBank.A) {
      engine.filterCutoffNorm = clamped
    } else {
      engine.reverbDepth = clamped * 0.75f
    }
    _uiState.update { it.copy(liveKnob1 = clamped) }
  }

  fun setLiveKnob2(v: Float) {
    val clamped = v.coerceIn(0f, 1f)
    if (_uiState.value.knobsAssignBank == KnobsAssignBank.A) {
      engine.resonanceNorm = clamped
    } else {
      engine.chorusDepth = clamped * 0.75f
    }
    _uiState.update { it.copy(liveKnob2 = clamped) }
  }

  // --- Data Entry Jog Wheel ---
  fun onJogWheelStep(delta: Int) {
    val st = _uiState.value
    if (!st.isPowerOn || delta == 0) return
    when (st.lcdMode) {
      LcdScreenMode.HOME_LIVE -> setTempo(st.tempoBpm + delta)
      LcdScreenMode.VOICE_BROWSER -> {
        val catVoices = st.allVoices.filter { it.category == st.activeVoiceCategory }
        if (catVoices.isNotEmpty()) {
          val curVoiceId = st.parts[st.selectedPartForEdit]?.voiceId
          val curIdx = catVoices.indexOfFirst { it.id == curVoiceId }.coerceAtLeast(0)
          val nextIdx = (curIdx + delta).mod(catVoices.size)
          selectVoiceForCurrentPart(catVoices[nextIdx].id)
        }
      }
      LcdScreenMode.STYLE_BROWSER -> {
        val catStyles = st.allStyles.filter { it.category == st.activeStyleCategory }
        if (catStyles.isNotEmpty()) {
          val curIdx = catStyles.indexOfFirst { it.id == st.currentStyleId }.coerceAtLeast(0)
          val nextIdx = (curIdx + delta).mod(catStyles.size)
          selectStyle(catStyles[nextIdx].id)
        }
      }
      else -> setTempo(st.tempoBpm + delta)
    }
  }

  // --- Voices & Parts ---
  fun selectPart(partId: PartId) {
    if (!_uiState.value.isPowerOn) return
    _uiState.update { st ->
      val current = st.parts[partId] ?: return@update st
      val nextParts = if (st.selectedPartForEdit == partId && partId != PartId.RIGHT1) {
        st.parts + (partId to current.copy(isEnabled = !current.isEnabled))
      } else {
        st.parts + (partId to current.copy(isEnabled = true))
      }
      st.copy(selectedPartForEdit = partId, parts = nextParts)
    }
  }

  fun togglePartEnabled(partId: PartId) {
    _uiState.update { st ->
      val cur = st.parts[partId] ?: return@update st
      st.copy(parts = st.parts + (partId to cur.copy(isEnabled = !cur.isEnabled)))
    }
  }

  fun selectVoiceCategory(cat: VoiceCategory) {
    if (!_uiState.value.isPowerOn) return
    _uiState.update {
      it.copy(
        activeVoiceCategory = cat,
        lcdMode = if (cat == VoiceCategory.EXPANSION_USER) LcdScreenMode.EXPANSION_IMPORTER else LcdScreenMode.VOICE_BROWSER,
      )
    }
  }

  fun selectVoiceForCurrentPart(voiceId: String) {
    _uiState.update { st ->
      val voice = st.voiceById(voiceId)
      val partId = st.selectedPartForEdit
      val cur = st.parts[partId] ?: PartState(partId, true, voiceId)
      if (voice.isDrumKit) {
        engine.currentDrumKitId = voice.id
      }
      st.copy(
        parts = st.parts + (partId to cur.copy(isEnabled = true, voiceId = voiceId)),
        activeVoiceCategory = voice.category,
      )
    }
  }

  fun toggleHarmony() {
    _uiState.update { it.copy(harmonyEnabled = !it.harmonyEnabled) }
  }

  fun toggleSustain() {
    val next = !_uiState.value.sustainEnabled
    engine.sustainPedal = next
    _uiState.update { it.copy(sustainEnabled = next) }
  }

  fun toggleDsp() {
    val next = !_uiState.value.dspEnabled
    engine.dspEnabled = next
    _uiState.update { it.copy(dspEnabled = next) }
  }

  fun shiftTranspose(delta: Int) {
    _uiState.update { it.copy(transpose = (it.transpose + delta).coerceIn(-12, 12)) }
  }

  fun shiftUpperOctave(delta: Int) {
    _uiState.update {
      val nextOct = (it.upperOctave + delta).coerceIn(-2, 2)
      val nextBase = (48 + nextOct * 12).coerceIn(24, 72)
      it.copy(upperOctave = nextOct, baseKeyboardMidi = nextBase)
    }
  }

  // --- Style & Arranger Controls ---
  fun selectStyleCategory(cat: StyleCategory) {
    if (!_uiState.value.isPowerOn) return
    _uiState.update {
      it.copy(
        activeStyleCategory = cat,
        lcdMode = LcdScreenMode.STYLE_BROWSER,
      )
    }
  }

  fun selectStyle(styleId: String) {
    val st = _uiState.value
    val style = st.allStyles.firstOrNull { it.id == styleId } ?: return
    engine.activeStyle = style
    engine.tempoBpm = style.defaultTempo
    _uiState.update {
      it.copy(
        currentStyleId = styleId,
        activeStyleCategory = style.category,
        tempoBpm = style.defaultTempo,
      )
    }
  }

  fun selectStyleSection(section: StyleSection) {
    if (!_uiState.value.isPowerOn) return
    if (section in listOf(StyleSection.MAIN_A, StyleSection.MAIN_B, StyleSection.MAIN_C, StyleSection.MAIN_D)) {
      engine.activeSection = section
      engine.pendingMainSection = section
    } else {
      if (engine.activeVariationForMain(_uiState.value.activeVariation) != null) {
        engine.pendingMainSection = engine.activeVariationForMain(_uiState.value.activeVariation)
      }
      engine.activeSection = section
      engine.resetSequencerPosition()
    }
    _uiState.update { it.copy(activeVariation = section) }
  }

  private fun YamahaAudioEngine.activeVariationForMain(cur: StyleSection): StyleSection? =
    if (cur in listOf(StyleSection.MAIN_A, StyleSection.MAIN_B, StyleSection.MAIN_C, StyleSection.MAIN_D)) cur else null

  fun toggleStyleStartStop() {
    if (!_uiState.value.isPowerOn) return
    val next = !engine.isStylePlaying
    if (next) {
      engine.activeStyle = _uiState.value.currentStyle
      engine.activeSection = _uiState.value.activeVariation
      engine.fadeMultiplier = 1f
      engine.fadeTarget = 1f
      engine.resetSequencerPosition()
      engine.isStylePlaying = true
    } else {
      engine.isStylePlaying = false
      engine.fadeMultiplier = 1f
      engine.fadeTarget = 1f
    }
    _uiState.update {
      it.copy(
        isStylePlaying = next,
        fadeInOutActive = false,
        currentBeatInBar = 0,
      )
    }
  }

  fun toggleAcmp() {
    _uiState.update { it.copy(acmpEnabled = !it.acmpEnabled) }
  }

  fun toggleSyncStart() {
    _uiState.update { it.copy(syncStartEnabled = !it.syncStartEnabled) }
  }

  fun toggleSyncStop() {
    _uiState.update { it.copy(syncStopEnabled = !it.syncStopEnabled) }
  }

  fun triggerFadeInOut() {
    if (!_uiState.value.isPowerOn) return
    if (!engine.isStylePlaying) {
      engine.fadeMultiplier = 0.05f
      engine.fadeTarget = 1f
      engine.resetSequencerPosition()
      engine.isStylePlaying = true
      _uiState.update { it.copy(isStylePlaying = true, fadeInOutActive = true) }
    } else {
      engine.fadeTarget = 0f
      _uiState.update { it.copy(fadeInOutActive = true) }
    }
  }

  fun setTempo(bpm: Int) {
    val clamped = bpm.coerceIn(40, 240)
    engine.tempoBpm = clamped
    _uiState.update { it.copy(tempoBpm = clamped) }
  }

  fun onTapTempo() {
    val now = System.currentTimeMillis()
    if (lastTapMs > 0L && now - lastTapMs < 2000L) {
      tapIntervals.addLast(now - lastTapMs)
      if (tapIntervals.size > 4) tapIntervals.removeFirst()
      val avg = tapIntervals.average()
      if (avg > 200.0) {
        setTempo((60_000.0 / avg).toInt())
      }
    } else {
      tapIntervals.clear()
    }
    lastTapMs = now
    _uiState.update { it.copy(tapTempoCount = it.tapTempoCount + 1) }
  }

  fun resetStyleToBar1() {
    engine.resetSequencerPosition()
    _uiState.update { it.copy(currentBeatInBar = 0, currentBar = 1) }
  }

  fun toggleChannel(ch: StyleChannel) {
    _uiState.update { st ->
      val next = !(st.channelEnabled[ch] ?: true)
      val nextMap = st.channelEnabled + (ch to next)
      engine.channelEnabled = nextMap
      st.copy(channelEnabled = nextMap)
    }
  }

  // --- Registration Memory & Freeze ---
  fun shiftRegistrationBank(delta: Int) {
    _uiState.update { st ->
      val next = ((st.currentBankNumber - 1 + delta).mod(8)) + 1
      st.copy(currentBankNumber = next, activeRegSlot = null)
    }
  }

  fun toggleMemoryArmed() {
    if (!_uiState.value.isPowerOn) return
    _uiState.update { it.copy(isMemoryArmed = !it.isMemoryArmed) }
    if (_uiState.value.isMemoryArmed) {
      showBanner("MEMORY ARMED: Tap Registration 1-8 to store")
    }
  }

  fun toggleFreeze() {
    if (!_uiState.value.isPowerOn) return
    val next = !_uiState.value.isFreezeEnabled
    _uiState.update { it.copy(isFreezeEnabled = next) }
    showBanner(if (next) "FREEZE ON (Style/Tempo locked)" else "FREEZE OFF")
  }

  fun updateFreezeOptions(opts: FreezeOptions) {
    _uiState.update { it.copy(freezeOptions = opts) }
    viewModelScope.launch(Dispatchers.IO) {
      repo.saveFreezeOptions(opts)
    }
  }

  fun onRegistrationSlotClicked(slotNumber: Int) {
    val st = _uiState.value
    if (!st.isPowerOn) return

    if (st.isMemoryArmed) {
      // Save current state to Bank + Slot
      val snapshot = RegistrationSlot(
        slotNumber = slotNumber,
        label = st.voiceById(st.parts[PartId.RIGHT1]?.voiceId ?: "v_grand_piano").name.take(12),
        isEmpty = false,
        styleId = st.currentStyleId,
        variation = st.activeVariation,
        tempo = st.tempoBpm,
        transpose = st.transpose,
        upperOctave = st.upperOctave,
        acmpEnabled = st.acmpEnabled,
        parts = st.parts,
        sustainEnabled = st.sustainEnabled,
        dspEnabled = st.dspEnabled,
        harmonyEnabled = st.harmonyEnabled,
        modulation = st.modulation,
        channelEnabled = st.channelEnabled,
      )
      val bankList = st.registrationBanks[st.currentBankNumber]?.toMutableList()
        ?: createFactoryRegistrations()[st.currentBankNumber]!!.toMutableList()
      val idx = bankList.indexOfFirst { it.slotNumber == slotNumber }
      if (idx >= 0) bankList[idx] = snapshot
      val nextBanks = st.registrationBanks + (st.currentBankNumber to bankList)

      _uiState.update {
        it.copy(
          registrationBanks = nextBanks,
          activeRegSlot = slotNumber,
          isMemoryArmed = false,
        )
      }
      viewModelScope.launch(Dispatchers.IO) { repo.saveRegistrations(nextBanks) }
      showBanner("Stored to Bank ${st.currentBankNumber} - Slot $slotNumber")
      return
    }

    // Recall registration respecting FREEZE flags
    val slot = st.registrationBanks[st.currentBankNumber]?.firstOrNull { it.slotNumber == slotNumber } ?: return
    val freeze = st.isFreezeEnabled
    val fOpts = st.freezeOptions

    _uiState.update { cur ->
      val nextStyleId = if (freeze && fOpts.freezeStyle) cur.currentStyleId else slot.styleId
      val nextVar = if (freeze && fOpts.freezeStyle) cur.activeVariation else slot.variation
      val nextTempo = if (freeze && fOpts.freezeTempo) cur.tempoBpm else slot.tempo
      val nextTrans = if (freeze && fOpts.freezeTranspose) cur.transpose else slot.transpose
      val nextOct = if (freeze && fOpts.freezeTranspose) cur.upperOctave else slot.upperOctave
      val nextParts = if (freeze && fOpts.freezeVoices) cur.parts else slot.parts
      val nextHarm = if (freeze && fOpts.freezeHarmony) cur.harmonyEnabled else slot.harmonyEnabled
      val nextCh = if (freeze && fOpts.freezeStyle) cur.channelEnabled else slot.channelEnabled

      cur.copy(
        activeRegSlot = slotNumber,
        currentStyleId = nextStyleId,
        activeVariation = nextVar,
        tempoBpm = nextTempo,
        transpose = nextTrans,
        upperOctave = nextOct,
        acmpEnabled = if (freeze && fOpts.freezeStyle) cur.acmpEnabled else slot.acmpEnabled,
        parts = nextParts,
        sustainEnabled = slot.sustainEnabled,
        dspEnabled = slot.dspEnabled,
        harmonyEnabled = nextHarm,
        modulation = slot.modulation,
        channelEnabled = nextCh,
      )
    }
    syncEngineFromState()
    showBanner(if (freeze) "Recalled Slot $slotNumber (FREEZE active)" else "Recalled Slot $slotNumber")
  }

  // --- Multi Pads & One Touch Setting ---
  fun triggerMultiPad(padId: Int) {
    val st = _uiState.value
    if (!st.isPowerOn) return
    val pad = st.multiPads.firstOrNull { it.id == padId } ?: return
    _uiState.update { it.copy(activeMultiPadId = padId) }

    if (pad.wavId != null) {
      val wav = st.importedWavSamples.firstOrNull { it.id == pad.wavId }
      if (wav != null) {
        engine.triggerWavOneShot(wav, 0.92f)
        return
      }
    }
    viewModelScope.launch {
      if (pad.drumNotes.isNotEmpty()) {
        pad.drumNotes.forEach { d ->
          engine.triggerDrum(d, 108, 0.85f)
          delay(125)
        }
      } else if (pad.tonalNotes.isNotEmpty()) {
        val voice = st.voiceById(st.parts[PartId.RIGHT1]?.voiceId ?: "v_steel_guitar")
        pad.tonalNotes.forEach { n ->
          engine.noteOn("MPAD", n, 95, voice, 0.65f)
          delay(90)
        }
        delay(320)
        pad.tonalNotes.forEach { n -> engine.noteOff("MPAD", n) }
      }
      _uiState.update { if (it.activeMultiPadId == padId) it.copy(activeMultiPadId = null) else it }
    }
  }

  fun stopMultiPads() {
    _uiState.update { it.copy(activeMultiPadId = null) }
    engine.allNotesOff()
  }

  fun selectOtsSlot(otsNumber: Int) {
    val st = _uiState.value
    if (!st.isPowerOn) return
    val otsList = st.currentStyle.otsVoiceIds
    val voiceId = otsList.getOrNull(otsNumber - 1) ?: return
    _uiState.update { cur ->
      val r1 = cur.parts[PartId.RIGHT1] ?: PartState(PartId.RIGHT1, true, voiceId)
      cur.copy(
        activeOtsSlot = otsNumber,
        parts = cur.parts + (PartId.RIGHT1 to r1.copy(isEnabled = true, voiceId = voiceId)),
      )
    }
    showBanner("OTS $otsNumber: ${st.voiceById(voiceId).name}")
  }

  // --- Piano Keyboard Touch Handling ---
  fun onKeyPressed(midiNote: Int, velocity: Int = 104) {
    val st = _uiState.value
    if (!st.isPowerOn) return
    val playedMidi = (midiNote + st.transpose).coerceIn(12, 115)

    _uiState.update { it.copy(pressedMidiNotes = it.pressedMidiNotes + midiNote) }

    val isLeftZone = st.acmpEnabled && midiNote <= st.splitNote
    if (isLeftZone) {
      activeLeftKeys.add(playedMidi)
      val detected = YamahaLibrary.detectChord(activeLeftKeys)
      if (detected != null) {
        engine.chordRootPc = detected.rootPc
        engine.chordIntervals = detected.intervals
        _uiState.update { it.copy(detectedChordName = detected.displayName) }
      }
      if (st.syncStartEnabled && !engine.isStylePlaying) {
        engine.activeStyle = st.currentStyle
        engine.activeSection = st.activeVariation
        engine.resetSequencerPosition()
        engine.isStylePlaying = true
        _uiState.update { it.copy(isStylePlaying = true, syncStartEnabled = false) }
      }
      val leftPart = st.parts[PartId.LEFT]
      if (leftPart != null && leftPart.isEnabled) {
        val voice = st.voiceById(leftPart.voiceId)
        engine.noteOn("LEFT", playedMidi, velocity, voice, leftPart.volume)
      }
      return
    }

    // Right Zone (RIGHT1 / RIGHT2 / RIGHT3 + Harmony/Arpeggio)
    val spawned = mutableListOf<Pair<String, Int>>()
    for (pId in listOf(PartId.RIGHT1, PartId.RIGHT2, PartId.RIGHT3)) {
      val part = st.parts[pId] ?: continue
      if (!part.isEnabled) continue
      val voice = st.voiceById(part.voiceId)
      val tag = pId.name
      engine.noteOn(tag, playedMidi, velocity, voice, part.volume)
      spawned.add(tag to playedMidi)

      if (st.harmonyEnabled && pId == PartId.RIGHT1 && !voice.isDrumKit) {
        val harmNote = (playedMidi + 4).coerceAtMost(115)
        engine.noteOn("${tag}_H", harmNote, (velocity * 0.72f).toInt(), voice, part.volume * 0.7f)
        spawned.add("${tag}_H" to harmNote)
      }
    }
    activeRightKeys[midiNote] = spawned
  }

  fun onKeyReleased(midiNote: Int) {
    val st = _uiState.value
    val playedMidi = (midiNote + st.transpose).coerceIn(12, 115)
    _uiState.update { it.copy(pressedMidiNotes = it.pressedMidiNotes - midiNote) }

    if (activeLeftKeys.remove(playedMidi)) {
      engine.noteOff("LEFT", playedMidi)
      if (activeLeftKeys.isEmpty() && st.syncStopEnabled && engine.isStylePlaying) {
        engine.isStylePlaying = false
        _uiState.update { it.copy(isStylePlaying = false) }
      }
    }

    val rightSpawned = activeRightKeys.remove(midiNote)
    if (rightSpawned != null) {
      for ((tag, note) in rightSpawned) {
        engine.noteOff(tag, note)
      }
    } else {
      for (pId in listOf(PartId.RIGHT1, PartId.RIGHT2, PartId.RIGHT3)) {
        engine.noteOff(pId.name, playedMidi)
      }
    }
  }

  // --- SF2 / STY / WAV Importer ---
  fun importFileFromUri(uri: Uri) {
    _uiState.update { it.copy(isImporting = true) }
    viewModelScope.launch(Dispatchers.IO) {
      val outcome = repo.importFromUri(uri)
      withContext(Dispatchers.Main) {
        when (outcome) {
          is ImportAndStorageRepository.ImportOutcome.Sf2Imported -> {
            engine.registerSf2Bank(outcome.bank)
            _uiState.update { st ->
              val nextSf2 = st.importedSf2Banks + outcome.bank
              val merged = buildMergedVoices(nextSf2, st.importedWavSamples)
              val firstPresetVoiceId = "sf2_${outcome.bank.id}_0"
              val r1 = st.parts[PartId.RIGHT1]!!
              st.copy(
                isImporting = false,
                importedSf2Banks = nextSf2,
                allVoices = merged,
                activeVoiceCategory = VoiceCategory.EXPANSION_USER,
                parts = st.parts + (PartId.RIGHT1 to r1.copy(isEnabled = true, voiceId = firstPresetVoiceId)),
              )
            }
            showBanner("SF2 Loaded: ${outcome.bank.bankName} (${outcome.bank.presets.size} presets)")
          }
          is ImportAndStorageRepository.ImportOutcome.WavImported -> {
            engine.registerWavSample(outcome.sample)
            _uiState.update { st ->
              val nextWav = st.importedWavSamples + outcome.sample
              val merged = buildMergedVoices(st.importedSf2Banks, nextWav)
              val wavVoiceId = "wav_${outcome.sample.id}"
              val r1 = st.parts[PartId.RIGHT1]!!
              st.copy(
                isImporting = false,
                importedWavSamples = nextWav,
                allVoices = merged,
                multiPads = updatePadsWithWavs(nextWav),
                activeVoiceCategory = VoiceCategory.EXPANSION_USER,
                parts = st.parts + (PartId.RIGHT1 to r1.copy(isEnabled = true, voiceId = wavVoiceId)),
              )
            }
            showBanner("WAV Loaded: ${outcome.sample.displayName}")
          }
          is ImportAndStorageRepository.ImportOutcome.StyImported -> {
            _uiState.update { st ->
              val nextSty = st.importedStyFiles + outcome.sty
              val mergedStyles = buildMergedStyles(nextSty)
              engine.activeStyle = outcome.sty.style
              engine.tempoBpm = outcome.sty.style.defaultTempo
              st.copy(
                isImporting = false,
                importedStyFiles = nextSty,
                allStyles = mergedStyles,
                currentStyleId = outcome.sty.style.id,
                activeStyleCategory = StyleCategory.EXPANSION_USER,
                tempoBpm = outcome.sty.style.defaultTempo,
              )
            }
            showBanner("STY Loaded: ${outcome.sty.style.name}")
          }
          is ImportAndStorageRepository.ImportOutcome.Error -> {
            _uiState.update { it.copy(isImporting = false) }
            showBanner("Import Error: ${outcome.message}")
          }
        }
      }
    }
  }

  fun deleteImport(id: String) {
    engine.unregisterImport(id)
    viewModelScope.launch(Dispatchers.IO) {
      repo.deleteImport(id)
      withContext(Dispatchers.Main) {
        _uiState.update { st ->
          val nextSf2 = st.importedSf2Banks.filterNot { it.id == id }
          val nextWav = st.importedWavSamples.filterNot { it.id == id }
          val nextSty = st.importedStyFiles.filterNot { it.id == id }
          st.copy(
            importedSf2Banks = nextSf2,
            importedWavSamples = nextWav,
            importedStyFiles = nextSty,
            allVoices = buildMergedVoices(nextSf2, nextWav),
            allStyles = buildMergedStyles(nextSty),
            multiPads = updatePadsWithWavs(nextWav),
          )
        }
        showBanner("Removed imported item")
      }
    }
  }

  // --- Song / REC / Demo ---
  fun toggleRecord() {
    val st = _uiState.value
    if (!st.isPowerOn) return
    if (!st.isRecording) {
      engine.startRecording()
      _uiState.update { it.copy(isRecording = true, songTitle = "REC_Taking...") }
      showBanner("Recording started...")
    } else {
      _uiState.update { it.copy(isRecording = false) }
      viewModelScope.launch(Dispatchers.IO) {
        val file = repo.createRecordingFile()
        val wav = engine.stopRecordingAndSave(file)
        withContext(Dispatchers.Main) {
          if (wav != null) {
            engine.loadSongForPlayback(wav)
            _uiState.update { it.copy(songTitle = wav.displayName) }
            showBanner("Saved Recording: ${wav.displayName}")
          } else {
            _uiState.update { it.copy(songTitle = "NewSong") }
          }
        }
      }
    }
  }

  fun toggleSongPlayPause() {
    if (!_uiState.value.isPowerOn) return
    if (engine.songPlaybackSamples == null) {
      val firstWav = _uiState.value.importedWavSamples.firstOrNull()
      if (firstWav != null) {
        engine.loadSongForPlayback(firstWav)
        _uiState.update { it.copy(songTitle = firstWav.displayName) }
      } else {
        toggleDemo()
        return
      }
    }
    val next = !engine.isSongPlaying
    engine.isSongPlaying = next
    _uiState.update { it.copy(isSongPlaying = next) }
  }

  fun stopSong() {
    engine.isSongPlaying = false
    engine.songPlaybackSamples?.let { engine.seekSongRelative(-999f) }
    demoJob?.cancel()
    _uiState.update { it.copy(isSongPlaying = false, isDemoPlaying = false) }
  }

  fun rewindSong() {
    engine.seekSongRelative(-5f)
  }

  fun toggleSongRepeat() {
    val next = !_uiState.value.songRepeat
    engine.songRepeat = next
    _uiState.update { it.copy(songRepeat = next) }
  }

  fun toggleDemo() {
    if (!_uiState.value.isPowerOn) return
    if (_uiState.value.isDemoPlaying) {
      demoJob?.cancel()
      engine.isStylePlaying = false
      engine.allNotesOff()
      _uiState.update { it.copy(isDemoPlaying = false, isStylePlaying = false) }
      return
    }
    _uiState.update { it.copy(isDemoPlaying = true, songTitle = "YamahaPSRDemo") }
    demoJob = viewModelScope.launch {
      val melody = listOf(60, 64, 67, 72, 71, 67, 64, 69, 67, 65, 64, 62, 60)
      val voice = _uiState.value.voiceById("v_concert_grand")
      if (!engine.isStylePlaying) {
        engine.activeStyle = _uiState.value.currentStyle
        engine.resetSequencerPosition()
        engine.isStylePlaying = true
        _uiState.update { it.copy(isStylePlaying = true) }
      }
      for (note in melody) {
        if (!_uiState.value.isDemoPlaying) break
        engine.noteOn("DEMO", note, 98, voice, 0.8f)
        delay(320)
        engine.noteOff("DEMO", note)
        delay(40)
      }
      _uiState.update { it.copy(isDemoPlaying = false) }
    }
  }

  fun setKeyboardKeyCount(count: Int) {
    _uiState.update { it.copy(keyboardKeyCount = count.coerceIn(25, 61)) }
  }

  fun setTrackControlIndex(idx: Int) {
    _uiState.update { it.copy(trackControlActiveIndex = idx) }
  }

  // --- Drum Kit Auto Player ---
  fun toggleDrumAutoPlay() {
    val st = _uiState.value
    if (!st.isPowerOn) return
    val next = !st.isDrumAutoPlaying
    if (next) {
      engine.activeDrumGroove = st.currentDrumGroove
      engine.currentDrumKitId = st.selectedDrumKitId
      engine.drumAutoVolume = st.drumAutoVolume
      engine.resetDrumAutoPosition()
      engine.isDrumAutoPlaying = true
    } else {
      engine.isDrumAutoPlaying = false
    }
    _uiState.update {
      it.copy(
        isDrumAutoPlaying = next,
        drumAutoCurrentStep = if (next) 0 else it.drumAutoCurrentStep,
        drumAutoFillPending = false,
      )
    }
    showBanner(if (next) "Drum Auto: ${st.currentDrumGroove.name} ON" else "Drum Auto Stopped")
  }

  fun selectDrumGroove(grooveId: String) {
    val st = _uiState.value
    val groove = st.drumGrooves.firstOrNull { it.id == grooveId } ?: return
    engine.activeDrumGroove = groove
    engine.currentDrumKitId = groove.kitId
    if (!st.isStylePlaying) {
      engine.tempoBpm = groove.defaultTempo
    }
    _uiState.update {
      it.copy(
        selectedDrumGrooveId = grooveId,
        selectedDrumKitId = groove.kitId,
        tempoBpm = if (!it.isStylePlaying) groove.defaultTempo else it.tempoBpm,
      )
    }
  }

  fun selectDrumKitForAutoPlayer(kitId: String) {
    engine.currentDrumKitId = kitId
    _uiState.update { it.copy(selectedDrumKitId = kitId) }
  }

  fun triggerDrumAutoFill() {
    val st = _uiState.value
    if (!st.isPowerOn) return
    if (!st.isDrumAutoPlaying) {
      toggleDrumAutoPlay()
    }
    engine.drumAutoFillPending = true
    _uiState.update { it.copy(drumAutoFillPending = true) }
    showBanner("Drum Auto Fill-In armed")
  }

  fun toggleDrumGridCell(drumNote: Int, step: Int) {
    val st = _uiState.value
    val groove = st.currentDrumGroove
    val existing = groove.steps.indexOfFirst { it.step == step && it.drumNote == drumNote }
    val nextSteps = if (existing >= 0) {
      groove.steps.toMutableList().apply { removeAt(existing) }
    } else {
      engine.triggerDrum(drumNote, 108, st.drumAutoVolume)
      groove.steps + com.example.audio.DrumPatternStep(step, drumNote, 108)
    }
    val updatedGroove = groove.copy(steps = nextSteps)
    val nextList = st.drumGrooves.map { if (it.id == groove.id) updatedGroove else it }
    engine.activeDrumGroove = updatedGroove
    _uiState.update { it.copy(drumGrooves = nextList) }
  }

  fun auditionDrumHit(drumNote: Int) {
    val st = _uiState.value
    if (!st.isPowerOn) return
    engine.currentDrumKitId = st.selectedDrumKitId
    engine.triggerDrum(drumNote, 115, st.drumAutoVolume)
  }

  fun setDrumAutoVolume(vol: Float) {
    val clamped = vol.coerceIn(0.1f, 1f)
    engine.drumAutoVolume = clamped
    _uiState.update { it.copy(drumAutoVolume = clamped) }
  }

  fun showBanner(msg: String) {
    bannerJob?.cancel()
    _uiState.update { it.copy(statusBanner = msg) }
    bannerJob = viewModelScope.launch {
      delay(2600)
      _uiState.update { it.copy(statusBanner = null) }
    }
  }

  override fun onCleared() {
    super.onCleared()
    engine.stop()
  }
}
