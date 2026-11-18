package com.example.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.PartId
import com.example.audio.StyleCategory
import com.example.audio.StyleSection
import com.example.audio.VoiceCategory
import com.example.ui.components.LcdDisplayUnit
import com.example.ui.components.LedColor
import com.example.ui.components.PianoKeyboard
import com.example.ui.components.PitchBendAndModWheels
import com.example.ui.components.PsrHardwareButton
import com.example.ui.components.PsrJogWheel
import com.example.ui.components.PsrRotaryKnob
import com.example.ui.components.PsrStartStopButton
import com.example.ui.components.VolumeLedMeter
import com.example.ui.components.YamahaLogoMark
import com.example.ui.theme.PsrColors

@Composable
fun PsrS975WorkstationScreen(viewModel: PsrS975ViewModel) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  val filePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument(),
  ) { uri: Uri? ->
    if (uri != null) {
      viewModel.importFileFromUri(uri)
    }
  }

  val launchImporter: () -> Unit = {
    filePickerLauncher.launch(arrayOf("*/*"))
  }

  BackHandler(enabled = state.isLcdZoomed || state.lcdMode != LcdScreenMode.HOME_LIVE) {
    viewModel.exitToHome()
  }

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          listOf(PsrColors.ChassisDark, PsrColors.ChassisMid, PsrColors.ChassisDark),
        )
      )
      .windowInsetsPadding(WindowInsets.safeDrawing),
  ) {
    val compactHeight = maxHeight < 420.dp
    val btnHeight = if (compactHeight) 18.dp else 21.dp
    val btnFont = if (compactHeight) 6.5.sp else 7.2.sp

    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 6.dp, vertical = 3.dp),
      verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
      // 1. TOP YAMAHA PSR-S975 HEADER BAR
      TopHeaderBar(
        state = state,
        onMenuClick = { viewModel.setLcdMode(LcdScreenMode.EXPANSION_IMPORTER) },
        onDemoClick = viewModel::toggleDemo,
        onRecClick = viewModel::toggleRecord,
        onHelpClick = { viewModel.setLcdMode(LcdScreenMode.HELP_GUIDE) },
        onSettingsClick = { viewModel.setLcdMode(LcdScreenMode.SETTINGS) },
        onVolumeChange = viewModel::setMasterVolume,
      )

      // 2. UPPER CONTROL CONSOLE (Knobs | Song+Style | Central LCD | Voice+Part | Data Entry)
      Row(
        modifier = Modifier
          .weight(0.46f)
          .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        // Left Analog Strip: POWER, MASTER VOLUME, BALANCE, MIC GAIN
        PanelWellBox(
          modifier = Modifier.width(56.dp).fillMaxHeight(),
        ) {
          Column(
            modifier = Modifier.fillMaxSize().padding(vertical = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
          ) {
            PsrHardwareButton(
              label = "⏻",
              subLabel = "POWER",
              onClick = viewModel::togglePower,
              ledColor = LedColor.RED,
              isLit = state.isPowerOn,
              buttonHeight = 16.dp,
              fontSize = 8.sp,
              testTag = "power_button",
            )
            PsrRotaryKnob(
              label = "MASTER VOL",
              value = state.masterVolume,
              onValueChange = viewModel::setMasterVolume,
              knobSize = if (compactHeight) 22.dp else 26.dp,
              testTag = "master_volume_knob",
            )
            PsrRotaryKnob(
              label = "BALANCE",
              value = (state.stereoBalance + 1f) / 2f,
              onValueChange = { viewModel.setStereoBalance(it * 2f - 1f) },
              knobSize = if (compactHeight) 22.dp else 26.dp,
              leftHint = "L",
              rightHint = "R",
              testTag = "balance_knob",
            )
            PsrRotaryKnob(
              label = "MIC GAIN",
              value = state.micGain,
              onValueChange = viewModel::setMicGain,
              knobSize = if (compactHeight) 22.dp else 26.dp,
              testTag = "mic_gain_knob",
            )
          }
        }

        // Left-Center: SONG + STYLE Matrices
        Column(
          modifier = Modifier.weight(0.24f).fillMaxHeight(),
          verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
          // SONG Section
          PanelWellBox(
            title = "SONG",
            modifier = Modifier.weight(0.46f).fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.fillMaxSize(),
              verticalArrangement = Arrangement.SpaceEvenly,
            ) {
              Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                PsrHardwareButton(
                  "SELECT",
                  { viewModel.setLcdMode(LcdScreenMode.SONG_PLAYER) },
                  Modifier.weight(1f),
                  buttonHeight = btnHeight,
                  fontSize = btnFont,
                )
                PsrHardwareButton(
                  "DRUM AUTO",
                  {
                    if (state.lcdMode == LcdScreenMode.DRUM_AUTO_PLAYER) {
                      viewModel.toggleDrumAutoPlay()
                    } else {
                      viewModel.setLcdMode(LcdScreenMode.DRUM_AUTO_PLAYER)
                    }
                  },
                  Modifier.weight(1f),
                  ledColor = LedColor.GREEN,
                  isLit = state.isPowerOn && state.isDrumAutoPlaying,
                  buttonHeight = btnHeight,
                  fontSize = 6.sp,
                  testTag = "drum_auto_button",
                )
                PsrHardwareButton(
                  "IMPORT",
                  { viewModel.setLcdMode(LcdScreenMode.EXPANSION_IMPORTER) },
                  Modifier.weight(1f),
                  ledColor = LedColor.BLUE,
                  isLit = state.lcdMode == LcdScreenMode.EXPANSION_IMPORTER,
                  buttonHeight = btnHeight,
                  fontSize = btnFont,
                  testTag = "song_import_button",
                )
              }
              Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                PsrHardwareButton(
                  "REPEAT",
                  viewModel::toggleSongRepeat,
                  Modifier.weight(1f),
                  ledColor = LedColor.AMBER,
                  isLit = state.songRepeat,
                  buttonHeight = btnHeight,
                  fontSize = btnFont,
                )
                PsrHardwareButton(
                  "|<</REW",
                  viewModel::rewindSong,
                  Modifier.weight(1f),
                  buttonHeight = btnHeight,
                  fontSize = btnFont,
                )
                PsrHardwareButton(
                  "▶/||",
                  viewModel::toggleSongPlayPause,
                  Modifier.weight(1f),
                  ledColor = LedColor.GREEN,
                  isLit = state.isSongPlaying,
                  buttonHeight = btnHeight,
                  fontSize = btnFont,
                )
                PsrHardwareButton(
                  "■",
                  viewModel::stopSong,
                  Modifier.weight(1f),
                  buttonHeight = btnHeight,
                  fontSize = btnFont,
                )
              }
            }
          }

          // STYLE Section
          PanelWellBox(
            title = "STYLE",
            modifier = Modifier.weight(0.54f).fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.fillMaxSize(),
              verticalArrangement = Arrangement.SpaceEvenly,
            ) {
              val cats = listOf(
                StyleCategory.POP_ROCK, StyleCategory.DANCE, StyleCategory.BALLAD,
                StyleCategory.SWING_JAZZ, StyleCategory.RNB, StyleCategory.COUNTRY,
                StyleCategory.LATIN, StyleCategory.ENTERTAINER, StyleCategory.WORLD,
              )
              for (row in 0 until 3) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                  for (col in 0 until 3) {
                    val cat = cats[row * 3 + col]
                    PsrHardwareButton(
                      label = cat.label,
                      onClick = { viewModel.selectStyleCategory(cat) },
                      modifier = Modifier.weight(1f),
                      ledColor = LedColor.GREEN,
                      isLit = state.isPowerOn && state.activeStyleCategory == cat && state.lcdMode == LcdScreenMode.STYLE_BROWSER,
                      buttonHeight = btnHeight,
                      fontSize = 6.2.sp,
                      testTag = "style_cat_${cat.name.lowercase()}",
                    )
                  }
                }
              }
              Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                PsrHardwareButton(
                  "TEMPO",
                  { viewModel.setLcdMode(LcdScreenMode.MIXER_EFFECTS) },
                  Modifier.weight(1f),
                  buttonHeight = btnHeight,
                  fontSize = 6.2.sp,
                )
                PsrHardwareButton(
                  "TAP TEMPO",
                  viewModel::onTapTempo,
                  Modifier.weight(1f),
                  ledColor = LedColor.AMBER,
                  isLit = state.tapTempoCount > 0,
                  buttonHeight = btnHeight,
                  fontSize = 6.sp,
                  testTag = "tap_tempo_button",
                )
                PsrHardwareButton(
                  "RESET",
                  viewModel::resetStyleToBar1,
                  Modifier.weight(1f),
                  buttonHeight = btnHeight,
                  fontSize = 6.2.sp,
                )
              }
            }
          }
        }

        // Center: Iconic PSR-S975 TFT LCD + Soft Keys + Channel ON/OFF
        LcdDisplayUnit(
          state = state,
          onSoftLeftClick = { idx ->
            when (idx) {
              0 -> viewModel.setLcdMode(LcdScreenMode.SONG_PLAYER)
              1 -> viewModel.setLcdMode(LcdScreenMode.STYLE_BROWSER)
              2 -> viewModel.setLcdMode(LcdScreenMode.MIXER_EFFECTS)
              3 -> viewModel.setLcdMode(LcdScreenMode.FREEZE_SETTINGS)
              4 -> viewModel.exitToHome()
            }
          },
          onSoftRightClick = { idx ->
            when (idx) {
              0 -> { viewModel.selectPart(PartId.RIGHT1); viewModel.setLcdMode(LcdScreenMode.VOICE_BROWSER) }
              1 -> { viewModel.selectPart(PartId.RIGHT2); viewModel.setLcdMode(LcdScreenMode.VOICE_BROWSER) }
              2 -> { viewModel.selectPart(PartId.RIGHT3); viewModel.setLcdMode(LcdScreenMode.VOICE_BROWSER) }
              3 -> viewModel.setLcdMode(LcdScreenMode.EXPANSION_IMPORTER)
              4 -> viewModel.setLcdMode(LcdScreenMode.REGISTRATION_BANK)
            }
          },
          onChannelToggle = viewModel::toggleChannel,
          onSelectVoice = viewModel::selectVoiceForCurrentPart,
          onSelectVoiceCategory = viewModel::selectVoiceCategory,
          onSelectStyle = viewModel::selectStyle,
          onSelectStyleCategory = viewModel::selectStyleCategory,
          onSelectPart = viewModel::selectPart,
          onRequestFileImport = launchImporter,
          onDeleteImport = viewModel::deleteImport,
          onUpdateFreezeOptions = viewModel::updateFreezeOptions,
          onToggleLcdZoom = viewModel::toggleLcdZoom,
          onChangeMode = viewModel::setLcdMode,
          onShiftTranspose = viewModel::shiftTranspose,
          onShiftOctave = viewModel::shiftUpperOctave,
          onChangePitchBendRange = viewModel::setPitchBendRange,
          onChangeKeyCount = viewModel::setKeyboardKeyCount,
          onSetTempo = viewModel::setTempo,
          onSelectRegSlot = viewModel::onRegistrationSlotClicked,
          onLiveKnob1 = viewModel::setLiveKnob1,
          onLiveKnob2 = viewModel::setLiveKnob2,
          onToggleDrumAutoPlay = viewModel::toggleDrumAutoPlay,
          onSelectDrumGroove = viewModel::selectDrumGroove,
          onSelectDrumKit = viewModel::selectDrumKitForAutoPlayer,
          onTriggerDrumAutoFill = viewModel::triggerDrumAutoFill,
          onToggleDrumGridCell = viewModel::toggleDrumGridCell,
          onAuditionDrumHit = viewModel::auditionDrumHit,
          modifier = Modifier.weight(0.42f).fillMaxHeight(),
        )

        // Right-Center: VOICE + VOICE CONTROL + PART SELECT
        PanelWellBox(
          title = "VOICE",
          modifier = Modifier.weight(0.24f).fillMaxHeight(),
        ) {
          Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
          ) {
            val vCats = listOf(
              VoiceCategory.PIANO, VoiceCategory.E_PIANO, VoiceCategory.ORGAN,
              VoiceCategory.GUITAR, VoiceCategory.STRINGS, VoiceCategory.BRASS,
              VoiceCategory.WOODWIND, VoiceCategory.SYNTH, VoiceCategory.PERC_DRUM,
            )
            for (row in 0 until 3) {
              Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                for (col in 0 until 3) {
                  val cat = vCats[row * 3 + col]
                  val curVoice = state.parts[state.selectedPartForEdit]?.voiceId?.let { state.voiceById(it) }
                  PsrHardwareButton(
                    label = cat.label,
                    onClick = { viewModel.selectVoiceCategory(cat) },
                    modifier = Modifier.weight(1f),
                    ledColor = LedColor.GREEN,
                    isLit = state.isPowerOn && curVoice?.category == cat,
                    buttonHeight = btnHeight,
                    fontSize = 6.sp,
                    testTag = "voice_cat_${cat.name.lowercase()}",
                  )
                }
              }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
              PsrHardwareButton(
                label = "EXPANSION/USER",
                onClick = { viewModel.selectVoiceCategory(VoiceCategory.EXPANSION_USER) },
                modifier = Modifier.weight(1f),
                ledColor = LedColor.BLUE,
                isLit = state.isPowerOn && state.lcdMode == LcdScreenMode.EXPANSION_IMPORTER,
                buttonHeight = btnHeight,
                fontSize = 5.8.sp,
                testTag = "expansion_user_button",
              )
              PsrHardwareButton(
                label = "VOICE EFFECT",
                onClick = { viewModel.setLcdMode(LcdScreenMode.MIXER_EFFECTS) },
                modifier = Modifier.weight(1f),
                buttonHeight = btnHeight,
                fontSize = 5.8.sp,
              )
            }

            // VOICE CONTROL
            Text(
              text = "VOICE CONTROL",
              color = PsrColors.LabelMuted,
              fontSize = 5.8.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
              PsrHardwareButton(
                label = "HARMONY/ARP",
                onClick = viewModel::toggleHarmony,
                modifier = Modifier.weight(1.2f),
                ledColor = LedColor.AMBER,
                isLit = state.isPowerOn && state.harmonyEnabled,
                buttonHeight = btnHeight,
                fontSize = 5.8.sp,
                testTag = "harmony_button",
              )
              PsrHardwareButton(
                label = "SUSTAIN",
                onClick = viewModel::toggleSustain,
                modifier = Modifier.weight(1f),
                ledColor = LedColor.GREEN,
                isLit = state.isPowerOn && state.sustainEnabled,
                buttonHeight = btnHeight,
                fontSize = 6.sp,
                testTag = "sustain_button",
              )
              PsrHardwareButton(
                label = "DSP",
                onClick = viewModel::toggleDsp,
                modifier = Modifier.weight(0.9f),
                ledColor = LedColor.AMBER,
                isLit = state.isPowerOn && state.dspEnabled,
                buttonHeight = btnHeight,
                fontSize = 6.sp,
                testTag = "dsp_button",
              )
            }

            // PART SELECT
            Text(
              text = "PART SELECT",
              color = PsrColors.LabelMuted,
              fontSize = 5.8.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
              PartId.entries.forEach { part ->
                val pState = state.parts[part]
                val isSelected = state.selectedPartForEdit == part
                val isOn = pState?.isEnabled == true
                PsrHardwareButton(
                  label = part.label,
                  onClick = { viewModel.selectPart(part) },
                  modifier = Modifier.weight(1f),
                  ledColor = if (isSelected) LedColor.AMBER else LedColor.GREEN,
                  isLit = state.isPowerOn && (isOn || isSelected),
                  buttonHeight = btnHeight,
                  fontSize = 6.sp,
                  testTag = "part_${part.name.lowercase()}",
                )
              }
            }
          }
        }

        // Far-Right: DATA ENTRY Jog Wheel + ENTER / EXIT
        PanelWellBox(
          modifier = Modifier.width(74.dp).fillMaxHeight(),
        ) {
          Column(
            modifier = Modifier.fillMaxSize().padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
          ) {
            PsrJogWheel(
              onStep = viewModel::onJogWheelStep,
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              PsrHardwareButton(
                label = "ENTER",
                onClick = launchImporter,
                modifier = Modifier.weight(1f),
                buttonHeight = 20.dp,
                fontSize = 6.sp,
                testTag = "enter_import_button",
              )
              PsrHardwareButton(
                label = "EXIT",
                onClick = viewModel::exitToHome,
                modifier = Modifier.weight(1f),
                buttonHeight = 20.dp,
                fontSize = 6.sp,
                testTag = "exit_button",
              )
            }
          }
        }
      }

      // 3. MIDDLE STRIP: LIVE CONTROL | KNOBS ASSIGN | SECTION | TRACK CONTROL | REGISTRATION MEMORY (BANK, 1-8, FREEZE, MEMORY)
      Row(
        modifier = Modifier.fillMaxWidth().height(if (compactHeight) 36.dp else 42.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // LIVE CONTROL + KNOBS ASSIGN
        PanelWellBox(modifier = Modifier.weight(0.18f).fillMaxHeight()) {
          Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
          ) {
            PsrRotaryKnob(
              label = if (state.knobsAssignBank == KnobsAssignBank.A) "CUTOFF" else "REVERB",
              value = state.liveKnob1,
              onValueChange = viewModel::setLiveKnob1,
              knobSize = 22.dp,
              leftHint = "",
              rightHint = "",
              testTag = "live_knob_1",
            )
            PsrRotaryKnob(
              label = if (state.knobsAssignBank == KnobsAssignBank.A) "RESO" else "CHORUS",
              value = state.liveKnob2,
              onValueChange = viewModel::setLiveKnob2,
              knobSize = 22.dp,
              leftHint = "",
              rightHint = "",
              testTag = "live_knob_2",
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.width(24.dp)) {
              PsrHardwareButton(
                "A",
                { viewModel.selectKnobsAssign(KnobsAssignBank.A) },
                ledColor = LedColor.GREEN,
                isLit = state.knobsAssignBank == KnobsAssignBank.A,
                buttonHeight = 14.dp,
                fontSize = 6.5.sp,
              )
              PsrHardwareButton(
                "B",
                { viewModel.selectKnobsAssign(KnobsAssignBank.B) },
                ledColor = LedColor.AMBER,
                isLit = state.knobsAssignBank == KnobsAssignBank.B,
                buttonHeight = 14.dp,
                fontSize = 6.5.sp,
              )
            }
          }
        }

        // OCTAVE / TRANSPOSE SECTION (< / >)
        PanelWellBox(title = "OCTAVE - / +", modifier = Modifier.weight(0.10f).fillMaxHeight()) {
          Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            PsrHardwareButton(
              "◀",
              { viewModel.shiftUpperOctave(-1) },
              Modifier.weight(1f),
              buttonHeight = 20.dp,
              testTag = "octave_down_button",
            )
            PsrHardwareButton(
              "▶",
              { viewModel.shiftUpperOctave(1) },
              Modifier.weight(1f),
              buttonHeight = 20.dp,
              testTag = "octave_up_button",
            )
          }
        }

        // TRACK CONTROL 1..8
        PanelWellBox(title = "TRACK CONTROL", modifier = Modifier.weight(0.26f).fillMaxHeight()) {
          Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            for (idx in 1..8) {
              PsrHardwareButton(
                label = "$idx",
                onClick = { viewModel.setTrackControlIndex(idx) },
                modifier = Modifier.weight(1f),
                ledColor = LedColor.GREEN,
                isLit = state.isPowerOn && state.trackControlActiveIndex == idx,
                buttonHeight = 20.dp,
                fontSize = 6.5.sp,
              )
            }
          }
        }

        // REGISTRATION MEMORY: BANK -/+, 1..8, FREEZE, MEMORY
        PanelWellBox(title = "REGISTRATION MEMORY (BANK ${state.currentBankNumber})", modifier = Modifier.weight(0.46f).fillMaxHeight()) {
          Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(2.5.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            PsrHardwareButton(
              "−",
              { viewModel.shiftRegistrationBank(-1) },
              Modifier.weight(0.8f),
              buttonHeight = 21.dp,
              fontSize = 9.sp,
              testTag = "reg_bank_minus",
            )
            PsrHardwareButton(
              "+",
              { viewModel.shiftRegistrationBank(1) },
              Modifier.weight(0.8f),
              buttonHeight = 21.dp,
              fontSize = 9.sp,
              testTag = "reg_bank_plus",
            )
            for (slot in 1..8) {
              PsrHardwareButton(
                label = "$slot",
                onClick = { viewModel.onRegistrationSlotClicked(slot) },
                modifier = Modifier.weight(1f),
                ledColor = if (state.isMemoryArmed) LedColor.RED else LedColor.GREEN,
                isLit = state.isPowerOn && (state.activeRegSlot == slot),
                isBigNumber = true,
                buttonHeight = 21.dp,
                fontSize = 8.sp,
                testTag = "reg_slot_$slot",
              )
            }
            PsrHardwareButton(
              label = "FREEZE",
              onClick = viewModel::toggleFreeze,
              modifier = Modifier.weight(1.35f),
              ledColor = LedColor.BLUE,
              isLit = state.isPowerOn && state.isFreezeEnabled,
              buttonHeight = 21.dp,
              fontSize = 6.sp,
              testTag = "reg_freeze_button",
            )
            PsrHardwareButton(
              label = "MEMORY",
              onClick = viewModel::toggleMemoryArmed,
              modifier = Modifier.weight(1.4f),
              ledColor = LedColor.RED,
              isLit = state.isPowerOn && state.isMemoryArmed,
              buttonHeight = 21.dp,
              fontSize = 6.sp,
              testTag = "reg_memory_button",
            )
          }
        }
      }

      // 4. LOWER STRIP: ACMP + STYLE CONTROL + MULTI PAD CONTROL + ONE TOUCH SETTING
      Row(
        modifier = Modifier.fillMaxWidth().height(if (compactHeight) 36.dp else 40.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // STYLE CONTROL
        PanelWellBox(title = "STYLE CONTROL", modifier = Modifier.weight(0.62f).fillMaxHeight()) {
          Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            PsrHardwareButton(
              "ACMP",
              viewModel::toggleAcmp,
              Modifier.weight(1f),
              ledColor = LedColor.AMBER,
              isLit = state.isPowerOn && state.acmpEnabled,
              buttonHeight = 22.dp,
              fontSize = 6.sp,
              testTag = "acmp_button",
            )
            listOf(
              StyleSection.INTRO_1 to "I",
              StyleSection.INTRO_2 to "II",
              StyleSection.INTRO_3 to "III",
            ).forEach { (sec, lbl) ->
              PsrHardwareButton(
                lbl,
                { viewModel.selectStyleSection(sec) },
                Modifier.weight(0.8f),
                ledColor = LedColor.AMBER,
                isLit = state.activeVariation == sec,
                buttonHeight = 22.dp,
                fontSize = 6.5.sp,
              )
            }
            listOf(
              StyleSection.MAIN_A to "A",
              StyleSection.MAIN_B to "B",
              StyleSection.MAIN_C to "C",
              StyleSection.MAIN_D to "D",
            ).forEach { (sec, lbl) ->
              PsrHardwareButton(
                lbl,
                { viewModel.selectStyleSection(sec) },
                Modifier.weight(0.95f),
                ledColor = LedColor.GREEN,
                isLit = state.isPowerOn && state.activeVariation == sec,
                isBigNumber = true,
                buttonHeight = 22.dp,
                fontSize = 8.sp,
                testTag = "main_var_${lbl.lowercase()}",
              )
            }
            PsrHardwareButton(
              "BRK",
              { viewModel.selectStyleSection(StyleSection.BREAK) },
              Modifier.weight(0.85f),
              ledColor = LedColor.AMBER,
              isLit = state.activeVariation == StyleSection.BREAK,
              buttonHeight = 22.dp,
              fontSize = 6.sp,
            )
            listOf(
              StyleSection.ENDING_1 to "E1",
              StyleSection.ENDING_2 to "E2",
              StyleSection.ENDING_3 to "E3",
            ).forEach { (sec, lbl) ->
              PsrHardwareButton(
                lbl,
                { viewModel.selectStyleSection(sec) },
                Modifier.weight(0.8f),
                ledColor = LedColor.RED,
                isLit = state.activeVariation == sec,
                buttonHeight = 22.dp,
                fontSize = 6.sp,
              )
            }
            PsrHardwareButton(
              "S.STOP",
              viewModel::toggleSyncStop,
              Modifier.weight(1f),
              ledColor = LedColor.AMBER,
              isLit = state.syncStopEnabled,
              buttonHeight = 22.dp,
              fontSize = 5.8.sp,
            )
            PsrHardwareButton(
              "S.STRT",
              viewModel::toggleSyncStart,
              Modifier.weight(1f),
              ledColor = LedColor.RED,
              isLit = state.syncStartEnabled,
              buttonHeight = 22.dp,
              fontSize = 5.8.sp,
              testTag = "sync_start_button",
            )
            PsrStartStopButton(
              isPlaying = state.isStylePlaying,
              beatInBar = state.currentBeatInBar,
              onClick = viewModel::toggleStyleStartStop,
              modifier = Modifier.weight(1.35f),
            )
            PsrHardwareButton(
              "FADE",
              viewModel::triggerFadeInOut,
              Modifier.weight(0.95f),
              ledColor = LedColor.AMBER,
              isLit = state.fadeInOutActive,
              buttonHeight = 22.dp,
              fontSize = 5.8.sp,
            )
          }
        }

        // MULTI PAD CONTROL
        PanelWellBox(title = "MULTI PAD CONTROL", modifier = Modifier.weight(0.20f).fillMaxHeight()) {
          Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(2.5.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            for (padId in 1..4) {
              PsrHardwareButton(
                label = "$padId",
                onClick = { viewModel.triggerMultiPad(padId) },
                modifier = Modifier.weight(1f),
                ledColor = LedColor.GREEN,
                isLit = state.isPowerOn && state.activeMultiPadId == padId,
                isBigNumber = true,
                buttonHeight = 22.dp,
                fontSize = 8.sp,
                testTag = "multi_pad_$padId",
              )
            }
            PsrHardwareButton(
              "STOP",
              viewModel::stopMultiPads,
              Modifier.weight(1.1f),
              buttonHeight = 22.dp,
              fontSize = 5.8.sp,
            )
          }
        }

        // ONE TOUCH SETTING
        PanelWellBox(title = "ONE TOUCH SETTING", modifier = Modifier.weight(0.18f).fillMaxHeight()) {
          Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(2.5.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            for (ots in 1..4) {
              PsrHardwareButton(
                label = "$ots",
                onClick = { viewModel.selectOtsSlot(ots) },
                modifier = Modifier.weight(1f),
                ledColor = LedColor.GREEN,
                isLit = state.isPowerOn && state.activeOtsSlot == ots,
                isBigNumber = true,
                buttonHeight = 22.dp,
                fontSize = 8.sp,
                testTag = "ots_slot_$ots",
              )
            }
          }
        }
      }

      // 5. BOTTOM KEYBOARD AREA: PITCH BEND + MODULATION WHEELS (Left) + MULTI-TOUCH PIANO KEYBOARD
      Row(
        modifier = Modifier
          .weight(0.32f)
          .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        PitchBendAndModWheels(
          pitchBend = state.pitchBend,
          modulation = state.modulation,
          onPitchBendChange = viewModel::setPitchBend,
          onModulationChange = viewModel::setModulation,
          modifier = Modifier.width(62.dp).fillMaxHeight(),
        )
        PianoKeyboard(
          baseMidi = state.baseKeyboardMidi,
          keyCount = state.keyboardKeyCount,
          splitNote = state.splitNote,
          acmpEnabled = state.acmpEnabled,
          pressedNotes = state.pressedMidiNotes,
          onNoteOn = viewModel::onKeyPressed,
          onNoteOff = viewModel::onKeyReleased,
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(RoundedCornerShape(3.dp))
            .border(1.dp, PsrColors.PanelBorder, RoundedCornerShape(3.dp)),
        )
      }
    }

    // Optional Expanded Zoomed LCD Overlay when user clicks Zoom on the LCD bezel
    if (state.isLcdZoomed && state.isPowerOn) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color(0xCC07080A))
          .padding(horizontal = 32.dp, vertical = 14.dp),
      ) {
        LcdDisplayUnit(
          state = state,
          onSoftLeftClick = { idx ->
            when (idx) {
              0 -> viewModel.setLcdMode(LcdScreenMode.SONG_PLAYER)
              1 -> viewModel.setLcdMode(LcdScreenMode.STYLE_BROWSER)
              2 -> viewModel.setLcdMode(LcdScreenMode.MIXER_EFFECTS)
              3 -> viewModel.setLcdMode(LcdScreenMode.FREEZE_SETTINGS)
              4 -> viewModel.exitToHome()
            }
          },
          onSoftRightClick = { idx ->
            when (idx) {
              0 -> { viewModel.selectPart(PartId.RIGHT1); viewModel.setLcdMode(LcdScreenMode.VOICE_BROWSER) }
              1 -> { viewModel.selectPart(PartId.RIGHT2); viewModel.setLcdMode(LcdScreenMode.VOICE_BROWSER) }
              2 -> { viewModel.selectPart(PartId.RIGHT3); viewModel.setLcdMode(LcdScreenMode.VOICE_BROWSER) }
              3 -> viewModel.setLcdMode(LcdScreenMode.EXPANSION_IMPORTER)
              4 -> viewModel.setLcdMode(LcdScreenMode.REGISTRATION_BANK)
            }
          },
          onChannelToggle = viewModel::toggleChannel,
          onSelectVoice = viewModel::selectVoiceForCurrentPart,
          onSelectVoiceCategory = viewModel::selectVoiceCategory,
          onSelectStyle = viewModel::selectStyle,
          onSelectStyleCategory = viewModel::selectStyleCategory,
          onSelectPart = viewModel::selectPart,
          onRequestFileImport = launchImporter,
          onDeleteImport = viewModel::deleteImport,
          onUpdateFreezeOptions = viewModel::updateFreezeOptions,
          onToggleLcdZoom = viewModel::toggleLcdZoom,
          onChangeMode = viewModel::setLcdMode,
          onShiftTranspose = viewModel::shiftTranspose,
          onShiftOctave = viewModel::shiftUpperOctave,
          onChangePitchBendRange = viewModel::setPitchBendRange,
          onChangeKeyCount = viewModel::setKeyboardKeyCount,
          onSetTempo = viewModel::setTempo,
          onSelectRegSlot = viewModel::onRegistrationSlotClicked,
          onLiveKnob1 = viewModel::setLiveKnob1,
          onLiveKnob2 = viewModel::setLiveKnob2,
          onToggleDrumAutoPlay = viewModel::toggleDrumAutoPlay,
          onSelectDrumGroove = viewModel::selectDrumGroove,
          onSelectDrumKit = viewModel::selectDrumKitForAutoPlayer,
          onTriggerDrumAutoFill = viewModel::triggerDrumAutoFill,
          onToggleDrumGridCell = viewModel::toggleDrumGridCell,
          onAuditionDrumHit = viewModel::auditionDrumHit,
          modifier = Modifier.fillMaxSize(),
        )
      }
    }
  }
}

@Composable
private fun TopHeaderBar(
  state: PsrS975UiState,
  onMenuClick: () -> Unit,
  onDemoClick: () -> Unit,
  onRecClick: () -> Unit,
  onHelpClick: () -> Unit,
  onSettingsClick: () -> Unit,
  onVolumeChange: (Float) -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(28.dp)
      .background(
        Brush.verticalGradient(listOf(Color(0xFF1F2227), Color(0xFF121417))),
        RoundedCornerShape(4.dp),
      )
      .border(0.8.dp, PsrColors.PanelBorder, RoundedCornerShape(4.dp))
      .padding(horizontal = 8.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Icon(
        imageVector = Icons.Filled.Menu,
        contentDescription = "Menu",
        tint = PsrColors.SilverBright,
        modifier = Modifier.size(16.dp).clickable(onClick = onMenuClick),
      )
      YamahaLogoMark(showModelBadge = true, logoSize = 16.dp)
    }

    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      HeaderToolIcon("MENU", Icons.Filled.Menu, false, onMenuClick, "header_menu_button")
      HeaderToolIcon("DEMO", Icons.Filled.Slideshow, state.isDemoPlaying, onDemoClick, "header_demo_button")
      HeaderToolIcon("REC", Icons.Filled.FiberManualRecord, state.isRecording, onRecClick, "header_rec_button", tintActive = PsrColors.LedRed)
      HeaderToolIcon("HELP", Icons.Filled.HelpOutline, state.lcdMode == LcdScreenMode.HELP_GUIDE, onHelpClick, "header_help_button")
      HeaderToolIcon("SETTINGS", Icons.Filled.Settings, state.lcdMode == LcdScreenMode.SETTINGS, onSettingsClick, "header_settings_button")
      VolumeLedMeter(
        level = if (state.isPowerOn) state.vuMeterLevel else 0f,
        volume = state.masterVolume,
        onVolumeChange = onVolumeChange,
      )
    }
  }
}

@Composable
private fun HeaderToolIcon(
  label: String,
  icon: ImageVector,
  isActive: Boolean,
  onClick: () -> Unit,
  testTag: String,
  tintActive: Color = PsrColors.LedGreen,
) {
  Row(
    modifier = Modifier
      .clip(RoundedCornerShape(3.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 3.dp, vertical = 1.dp)
      .testTag(testTag),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(2.dp),
  ) {
    Icon(
      imageVector = icon,
      contentDescription = label,
      tint = if (isActive) tintActive else PsrColors.SilverMid,
      modifier = Modifier.size(12.dp),
    )
    Text(
      text = label,
      color = if (isActive) tintActive else PsrColors.LabelMuted,
      fontSize = 7.sp,
      fontWeight = FontWeight.Bold,
    )
  }
}

@Composable
private fun PanelWellBox(
  modifier: Modifier = Modifier,
  title: String? = null,
  content: @Composable () -> Unit,
) {
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(4.dp))
      .background(PsrColors.PanelWell)
      .border(0.8.dp, PsrColors.PanelBorder, RoundedCornerShape(4.dp))
      .padding(horizontal = 4.dp, vertical = 2.dp),
  ) {
    if (title != null) {
      Text(
        text = title,
        color = PsrColors.LabelMuted,
        fontSize = 6.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(bottom = 1.dp),
      )
    }
    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
      content()
    }
  }
}
