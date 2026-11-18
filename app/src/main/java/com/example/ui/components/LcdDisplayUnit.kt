package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.FreezeOptions
import com.example.audio.PartId
import com.example.audio.StyleCategory
import com.example.audio.StyleChannel
import com.example.audio.VoiceCategory
import com.example.ui.LcdScreenMode
import com.example.ui.PsrS975UiState
import com.example.ui.theme.PsrColors

@Composable
fun LcdDisplayUnit(
  state: PsrS975UiState,
  onSoftLeftClick: (Int) -> Unit,
  onSoftRightClick: (Int) -> Unit,
  onChannelToggle: (StyleChannel) -> Unit,
  onSelectVoice: (String) -> Unit,
  onSelectVoiceCategory: (VoiceCategory) -> Unit,
  onSelectStyle: (String) -> Unit,
  onSelectStyleCategory: (StyleCategory) -> Unit,
  onSelectPart: (PartId) -> Unit,
  onRequestFileImport: () -> Unit,
  onDeleteImport: (String) -> Unit,
  onUpdateFreezeOptions: (FreezeOptions) -> Unit,
  onToggleLcdZoom: () -> Unit,
  onChangeMode: (LcdScreenMode) -> Unit,
  onShiftTranspose: (Int) -> Unit,
  onShiftOctave: (Int) -> Unit,
  onChangePitchBendRange: (Int) -> Unit,
  onChangeKeyCount: (Int) -> Unit,
  onSetTempo: (Int) -> Unit,
  onSelectRegSlot: (Int) -> Unit,
  onLiveKnob1: (Float) -> Unit,
  onLiveKnob2: (Float) -> Unit,
  onToggleDrumAutoPlay: () -> Unit,
  onSelectDrumGroove: (String) -> Unit,
  onSelectDrumKit: (String) -> Unit,
  onTriggerDrumAutoFill: () -> Unit,
  onToggleDrumGridCell: (Int, Int) -> Unit,
  onAuditionDrumHit: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(6.dp))
      .background(PsrColors.LcdBezel)
      .border(1.5.dp, PsrColors.PanelBorder, RoundedCornerShape(6.dp))
      .padding(horizontal = 6.dp, vertical = 4.dp),
  ) {
    // Top YAMAHA bezel emblem + quick screen zoom button
    Row(
      modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = if (state.isFreezeEnabled) "FREEZE" else if (state.isMemoryArmed) "MEM ARMED" else "PSR-S975 TFT",
        color = when {
          state.isMemoryArmed -> PsrColors.LedRed
          state.isFreezeEnabled -> PsrColors.LedBlue
          else -> PsrColors.LabelMuted
        },
        fontSize = 6.5.sp,
        fontWeight = FontWeight.Bold,
      )
      YamahaLogoMark(showModelBadge = false, logoSize = 11.dp)
      Row(
        modifier = Modifier
          .clip(RoundedCornerShape(3.dp))
          .clickable(enabled = state.isPowerOn, onClick = onToggleLcdZoom)
          .padding(horizontal = 3.dp, vertical = 1.dp)
          .testTag("lcd_zoom_button"),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          imageVector = if (state.isLcdZoomed) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
          contentDescription = "Zoom LCD",
          tint = PsrColors.SilverMid,
          modifier = Modifier.size(12.dp),
        )
      }
    }

    // Middle Row: Left 5 Soft Buttons + LCD TFT Panel + Right 5 Soft Buttons
    Row(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // Left 5 Soft Keys + Knob
      Column(
        modifier = Modifier.width(24.dp).fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        for (i in 0..4) {
          SoftHardwareButton(
            onClick = { onSoftLeftClick(i) },
            testTag = "lcd_soft_left_$i",
          )
        }
        PsrRotaryKnob(
          label = "",
          value = state.liveKnob1,
          onValueChange = onLiveKnob1,
          knobSize = 18.dp,
          leftHint = "",
          rightHint = "",
        )
      }

      // Main Backlit TFT LCD Screen
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxHeight()
          .clip(RoundedCornerShape(4.dp))
          .background(
            if (state.isPowerOn) {
              Brush.verticalGradient(listOf(PsrColors.LcdBgTop, PsrColors.LcdBgBottom))
            } else {
              Brush.verticalGradient(listOf(Color(0xFF0C1016), Color(0xFF07090D)))
            }
          )
          .border(1.dp, Color(0xFF283242), RoundedCornerShape(4.dp)),
      ) {
        if (state.isPowerOn) {
          LcdScreenContent(
            state = state,
            onSelectVoice = onSelectVoice,
            onSelectVoiceCategory = onSelectVoiceCategory,
            onSelectStyle = onSelectStyle,
            onSelectStyleCategory = onSelectStyleCategory,
            onSelectPart = onSelectPart,
            onRequestFileImport = onRequestFileImport,
            onDeleteImport = onDeleteImport,
            onUpdateFreezeOptions = onUpdateFreezeOptions,
            onChangeMode = onChangeMode,
            onShiftTranspose = onShiftTranspose,
            onShiftOctave = onShiftOctave,
            onChangePitchBendRange = onChangePitchBendRange,
            onChangeKeyCount = onChangeKeyCount,
            onSetTempo = onSetTempo,
            onSelectRegSlot = onSelectRegSlot,
            onToggleDrumAutoPlay = onToggleDrumAutoPlay,
            onSelectDrumGroove = onSelectDrumGroove,
            onSelectDrumKit = onSelectDrumKit,
            onTriggerDrumAutoFill = onTriggerDrumAutoFill,
            onToggleDrumGridCell = onToggleDrumGridCell,
            onAuditionDrumHit = onAuditionDrumHit,
          )
        } else {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
              text = "POWER OFF",
              color = Color(0xFF2B3544),
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }

      // Right 5 Soft Keys + Knob
      Column(
        modifier = Modifier.width(24.dp).fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        for (i in 0..4) {
          SoftHardwareButton(
            onClick = { onSoftRightClick(i) },
            testTag = "lcd_soft_right_$i",
          )
        }
        PsrRotaryKnob(
          label = "",
          value = state.liveKnob2,
          onValueChange = onLiveKnob2,
          knobSize = 18.dp,
          leftHint = "",
          rightHint = "",
        )
      }
    }

    Spacer(modifier = Modifier.height(3.dp))

    // Bottom Strip: CHANNEL ON/OFF (STYLE) 8 buttons
    Text(
      text = "CHANNEL ON/OFF (STYLE)",
      color = PsrColors.LabelMuted,
      fontSize = 6.sp,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.align(Alignment.CenterHorizontally),
    )
    Row(
      modifier = Modifier.fillMaxWidth().padding(top = 1.dp),
      horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
      StyleChannel.entries.forEach { ch ->
        val enabled = state.isPowerOn && (state.channelEnabled[ch] ?: true)
        Column(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(2.dp))
            .clickable(enabled = state.isPowerOn) { onChannelToggle(ch) }
            .testTag("channel_${ch.name.lowercase()}"),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Text(
            text = ch.label,
            color = PsrColors.LabelWhite,
            fontSize = 6.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
          )
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(12.dp)
              .background(
                if (enabled) Color(0xFF1F7A2E) else Color(0xFF20242A),
                RoundedCornerShape(2.dp),
              )
              .border(
                0.6.dp,
                if (enabled) PsrColors.LedGreen else PsrColors.PanelBorder,
                RoundedCornerShape(2.dp),
              ),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = if (enabled) "ON" else "OFF",
              color = if (enabled) Color.White else PsrColors.LabelMuted,
              fontSize = 6.sp,
              fontWeight = FontWeight.ExtraBold,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SoftHardwareButton(
  onClick: () -> Unit,
  testTag: String,
) {
  Box(
    modifier = Modifier
      .width(22.dp)
      .height(13.dp)
      .clip(RoundedCornerShape(3.dp))
      .background(Brush.verticalGradient(listOf(Color(0xFF343840), Color(0xFF181B20))))
      .border(0.7.dp, Color(0xFF4C525E), RoundedCornerShape(3.dp))
      .clickable(onClick = onClick)
      .testTag(testTag),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier = Modifier
        .width(10.dp)
        .height(1.5.dp)
        .background(Color(0xFF6B7280)),
    )
  }
}

@Composable
private fun LcdScreenContent(
  state: PsrS975UiState,
  onSelectVoice: (String) -> Unit,
  onSelectVoiceCategory: (VoiceCategory) -> Unit,
  onSelectStyle: (String) -> Unit,
  onSelectStyleCategory: (StyleCategory) -> Unit,
  onSelectPart: (PartId) -> Unit,
  onRequestFileImport: () -> Unit,
  onDeleteImport: (String) -> Unit,
  onUpdateFreezeOptions: (FreezeOptions) -> Unit,
  onChangeMode: (LcdScreenMode) -> Unit,
  onShiftTranspose: (Int) -> Unit,
  onShiftOctave: (Int) -> Unit,
  onChangePitchBendRange: (Int) -> Unit,
  onChangeKeyCount: (Int) -> Unit,
  onSetTempo: (Int) -> Unit,
  onSelectRegSlot: (Int) -> Unit,
  onToggleDrumAutoPlay: () -> Unit,
  onSelectDrumGroove: (String) -> Unit,
  onSelectDrumKit: (String) -> Unit,
  onTriggerDrumAutoFill: () -> Unit,
  onToggleDrumGridCell: (Int, Int) -> Unit,
  onAuditionDrumHit: (Int) -> Unit,
) {
  Column(modifier = Modifier.fillMaxSize()) {
    // Top LCD Status Bar (matches PSR-S975 header)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(PsrColors.LcdHeaderBg)
        .padding(horizontal = 6.dp, vertical = 2.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = when (state.lcdMode) {
          LcdScreenMode.HOME_LIVE -> "LIVE CONTROL"
          LcdScreenMode.VOICE_BROWSER -> "VOICE: ${state.selectedPartForEdit.label}"
          LcdScreenMode.STYLE_BROWSER -> "STYLE: ${state.activeStyleCategory.label}"
          LcdScreenMode.EXPANSION_IMPORTER -> "SF2 / STY / WAV IMPORTER"
          LcdScreenMode.DRUM_AUTO_PLAYER -> "DRUM KIT AUTO PLAYER"
          LcdScreenMode.FREEZE_SETTINGS -> "REGISTRATION FREEZE"
          LcdScreenMode.REGISTRATION_BANK -> "REGISTRATION BANK ${state.currentBankNumber}"
          LcdScreenMode.MIXER_EFFECTS -> "MIXER & PITCH/MOD"
          LcdScreenMode.SONG_PLAYER -> "SONG & RECORDER"
          LcdScreenMode.SETTINGS -> "UTILITY SETTINGS"
          LcdScreenMode.HELP_GUIDE -> "PSR-S975 GUIDE"
        },
        color = PsrColors.LcdHeaderText,
        fontSize = 8.sp,
        fontWeight = FontWeight.Bold,
      )
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "TRANSPOSE : ${state.transpose}",
          color = PsrColors.LcdHeaderText,
          fontSize = 7.5.sp,
          fontWeight = FontWeight.SemiBold,
        )
        Text(
          text = "U.OCTAVE : ${state.upperOctave}",
          color = PsrColors.LcdHeaderText,
          fontSize = 7.5.sp,
          fontWeight = FontWeight.SemiBold,
        )
      }
    }

    // Optional transient status banner inside LCD
    if (state.statusBanner != null) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(PsrColors.LcdSelectedTop)
          .padding(horizontal = 6.dp, vertical = 1.5.dp),
      ) {
        Text(
          text = state.statusBanner,
          color = Color.White,
          fontSize = 7.5.sp,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }

    // Body
    Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(4.dp)) {
      when (state.lcdMode) {
        LcdScreenMode.HOME_LIVE -> HomeLiveLcdPage(
          state = state,
          onOpenVoice = { onChangeMode(LcdScreenMode.VOICE_BROWSER) },
          onOpenStyle = { onChangeMode(LcdScreenMode.STYLE_BROWSER) },
          onOpenImporter = { onChangeMode(LcdScreenMode.EXPANSION_IMPORTER) },
          onOpenMixer = { onChangeMode(LcdScreenMode.MIXER_EFFECTS) },
          onSelectPart = onSelectPart,
        )
        LcdScreenMode.VOICE_BROWSER -> VoiceBrowserLcdPage(
          state = state,
          onSelectVoiceCategory = onSelectVoiceCategory,
          onSelectVoice = onSelectVoice,
          onOpenDrumAuto = { onChangeMode(LcdScreenMode.DRUM_AUTO_PLAYER) },
        )
        LcdScreenMode.STYLE_BROWSER -> StyleBrowserLcdPage(
          state = state,
          onSelectStyleCategory = onSelectStyleCategory,
          onSelectStyle = onSelectStyle,
        )
        LcdScreenMode.EXPANSION_IMPORTER -> ExpansionImporterLcdPage(
          state = state,
          onRequestFileImport = onRequestFileImport,
          onSelectVoice = onSelectVoice,
          onSelectStyle = onSelectStyle,
          onDeleteImport = onDeleteImport,
        )
        LcdScreenMode.DRUM_AUTO_PLAYER -> DrumAutoPlayerLcdPage(
          state = state,
          onTogglePlay = onToggleDrumAutoPlay,
          onSelectGroove = onSelectDrumGroove,
          onSelectKit = onSelectDrumKit,
          onTriggerFill = onTriggerDrumAutoFill,
          onToggleCell = onToggleDrumGridCell,
          onAuditionHit = onAuditionDrumHit,
        )
        LcdScreenMode.FREEZE_SETTINGS -> FreezeSettingsLcdPage(
          options = state.freezeOptions,
          isFreezeEnabled = state.isFreezeEnabled,
          onUpdate = onUpdateFreezeOptions,
        )
        LcdScreenMode.REGISTRATION_BANK -> RegistrationBankLcdPage(
          state = state,
          onSelectSlot = onSelectRegSlot,
        )
        LcdScreenMode.MIXER_EFFECTS -> MixerEffectsLcdPage(
          state = state,
          onShiftTranspose = onShiftTranspose,
          onShiftOctave = onShiftOctave,
          onChangePitchBendRange = onChangePitchBendRange,
          onSetTempo = onSetTempo,
        )
        LcdScreenMode.SONG_PLAYER -> SongPlayerLcdPage(
          state = state,
          onOpenImporter = { onChangeMode(LcdScreenMode.EXPANSION_IMPORTER) },
        )
        LcdScreenMode.SETTINGS -> SettingsLcdPage(
          state = state,
          onChangeKeyCount = onChangeKeyCount,
          onChangePitchBendRange = onChangePitchBendRange,
        )
        LcdScreenMode.HELP_GUIDE -> HelpGuideLcdPage()
      }
    }
  }
}

@Composable
private fun HomeLiveLcdPage(
  state: PsrS975UiState,
  onOpenVoice: () -> Unit,
  onOpenStyle: () -> Unit,
  onOpenImporter: () -> Unit,
  onOpenMixer: () -> Unit,
  onSelectPart: (PartId) -> Unit,
) {
  val r1Voice = state.voiceById(state.parts[PartId.RIGHT1]?.voiceId ?: "v_concert_grand")
  val r2Voice = state.voiceById(state.parts[PartId.RIGHT2]?.voiceId ?: "v_warm_pad")
  val r3Voice = state.voiceById(state.parts[PartId.RIGHT3]?.voiceId ?: "v_concert_strings")
  val leftVoice = state.voiceById(state.parts[PartId.LEFT]?.voiceId ?: "v_warm_pad")

  Row(
    modifier = Modifier.fillMaxSize(),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    // Left Column: Song + Style + Chord + Import shortcut (exact PSR-S975 layout)
    Column(
      modifier = Modifier.weight(1f).fillMaxHeight(),
      verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
      // Song Card
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .clip(RoundedCornerShape(3.dp))
          .background(Brush.verticalGradient(listOf(PsrColors.LcdSelectedTop, PsrColors.LcdSelectedBottom)))
          .border(0.8.dp, PsrColors.LcdCardBorder, RoundedCornerShape(3.dp))
          .clickable(onClick = onOpenMixer)
          .padding(horizontal = 6.dp, vertical = 3.dp),
      ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              Icon(Icons.Filled.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
              Text(
                text = state.songTitle,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Text(
              text = "${state.currentStyle.timeSigNum}/${state.currentStyle.timeSigDen}  ♩=${state.tempoBpm}",
              color = Color(0xFFDCE7F3),
              fontSize = 7.5.sp,
              fontWeight = FontWeight.SemiBold,
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "BAR ${state.currentBar} • ${state.activeVariation.label}",
              color = Color(0xFFE5EEF8),
              fontSize = 7.5.sp,
              fontWeight = FontWeight.Medium,
            )
            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = state.detectedChordName,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
              )
              Text(
                text = "CHORD",
                color = Color(0xFFBFD4EC),
                fontSize = 6.sp,
              )
            }
          }
        }
      }

      // Style Card
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(0.9f)
          .clip(RoundedCornerShape(3.dp))
          .background(PsrColors.LcdCardBg)
          .border(0.8.dp, PsrColors.LcdCardBorder, RoundedCornerShape(3.dp))
          .clickable(onClick = onOpenStyle)
          .padding(horizontal = 6.dp, vertical = 3.dp),
      ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "STYLE",
              color = PsrColors.LcdTextSub,
              fontSize = 6.5.sp,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = state.currentStyle.category.label,
              color = PsrColors.LcdSelectedTop,
              fontSize = 6.5.sp,
              fontWeight = FontWeight.Bold,
            )
          }
          Text(
            text = state.currentStyle.name,
            color = PsrColors.LcdTextDark,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = "PB: ${(state.pitchBend * state.pitchBendRangeSemitones).let { String.format("%.1f", it) }}st",
              color = PsrColors.LcdTextSub,
              fontSize = 6.5.sp,
            )
            Text(
              text = "MOD: ${(state.modulation * 100).toInt()}%",
              color = PsrColors.LcdTextSub,
              fontSize = 6.5.sp,
            )
          }
        }
      }
    }

    // Right Column: 5 Voice / Expansion Slots (matches GrandPiano, WarmPad, ConcertStrings, NewBank, My Setting)
    Column(
      modifier = Modifier.weight(1f).fillMaxHeight(),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      LcdVoiceSlotCard(
        title = r1Voice.name,
        badge = if (r1Voice.isSuperArticulation) "S.Art!" else "R1",
        isActive = state.selectedPartForEdit == PartId.RIGHT1,
        icon = Icons.Filled.Piano,
        onClick = {
          onSelectPart(PartId.RIGHT1)
          onOpenVoice()
        },
        modifier = Modifier.weight(1f),
      )
      LcdVoiceSlotCard(
        title = r2Voice.name,
        badge = if (state.parts[PartId.RIGHT2]?.isEnabled == true) "R2 ON" else "S.Art!",
        isActive = state.selectedPartForEdit == PartId.RIGHT2,
        icon = Icons.Filled.LibraryMusic,
        onClick = {
          onSelectPart(PartId.RIGHT2)
          onOpenVoice()
        },
        modifier = Modifier.weight(1f),
      )
      LcdVoiceSlotCard(
        title = r3Voice.name,
        badge = if (state.parts[PartId.RIGHT3]?.isEnabled == true) "R3 ON" else "S.Art!",
        isActive = state.selectedPartForEdit == PartId.RIGHT3,
        icon = Icons.Filled.MusicNote,
        onClick = {
          onSelectPart(PartId.RIGHT3)
          onOpenVoice()
        },
        modifier = Modifier.weight(1f),
      )
      LcdVoiceSlotCard(
        title = "Import SF2/STY/WAV",
        badge = "${state.importedSf2Banks.size + state.importedStyFiles.size + state.importedWavSamples.size}",
        isActive = false,
        icon = Icons.Filled.UploadFile,
        onClick = onOpenImporter,
        modifier = Modifier.weight(1f).testTag("lcd_import_shortcut"),
      )
      LcdVoiceSlotCard(
        title = "LEFT: ${leftVoice.name}",
        badge = "B${state.currentBankNumber}",
        isActive = state.selectedPartForEdit == PartId.LEFT,
        icon = Icons.Filled.FolderOpen,
        onClick = {
          onSelectPart(PartId.LEFT)
          onOpenVoice()
        },
        modifier = Modifier.weight(1f),
      )
    }
  }
}

@Composable
private fun LcdVoiceSlotCard(
  title: String,
  badge: String,
  isActive: Boolean,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(2.5.dp))
      .background(
        if (isActive) Color(0xFFD0E3F7) else PsrColors.LcdCardBg,
      )
      .border(
        0.7.dp,
        if (isActive) PsrColors.LcdSelectedTop else PsrColors.LcdCardBorder,
        RoundedCornerShape(2.5.dp),
      )
      .clickable(onClick = onClick)
      .padding(horizontal = 5.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      modifier = Modifier.weight(1f),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = PsrColors.LcdAccentAmber,
        modifier = Modifier.size(11.dp),
      )
      Text(
        text = title,
        color = PsrColors.LcdTextDark,
        fontSize = 8.5.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
    Box(
      modifier = Modifier
        .background(PsrColors.LcdSArtBadge, RoundedCornerShape(2.dp))
        .padding(horizontal = 4.dp, vertical = 1.dp),
    ) {
      Text(
        text = badge,
        color = Color.White,
        fontSize = 6.5.sp,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}

@Composable
private fun VoiceBrowserLcdPage(
  state: PsrS975UiState,
  onSelectVoiceCategory: (VoiceCategory) -> Unit,
  onSelectVoice: (String) -> Unit,
  onOpenDrumAuto: () -> Unit,
) {
  val currentVoiceId = state.parts[state.selectedPartForEdit]?.voiceId
  val filtered = state.allVoices.filter { it.category == state.activeVoiceCategory }

  Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
    // Category Column
    LazyColumn(
      modifier = Modifier.width(82.dp).fillMaxHeight(),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      items(VoiceCategory.entries) { cat ->
        val sel = cat == state.activeVoiceCategory
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(2.dp))
            .background(if (sel) PsrColors.LcdSelectedTop else PsrColors.LcdCardBg)
            .border(0.5.dp, PsrColors.LcdCardBorder, RoundedCornerShape(2.dp))
            .clickable { onSelectVoiceCategory(cat) }
            .padding(horizontal = 4.dp, vertical = 3.dp),
        ) {
          Text(
            text = cat.label,
            color = if (sel) Color.White else PsrColors.LcdTextDark,
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
          )
        }
      }
    }

    // Voices List
    LazyColumn(
      modifier = Modifier.weight(1f).fillMaxHeight(),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      if (state.activeVoiceCategory == VoiceCategory.PERC_DRUM) {
        item(key = "open_drum_auto_banner") {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(2.5.dp))
              .background(Brush.verticalGradient(listOf(PsrColors.LcdSelectedTop, PsrColors.LcdSelectedBottom)))
              .clickable(onClick = onOpenDrumAuto)
              .padding(horizontal = 6.dp, vertical = 4.dp)
              .testTag("open_drum_auto_from_voice"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "▶ OPEN DRUM KIT AUTO PLAYER",
              color = Color.White,
              fontSize = 8.sp,
              fontWeight = FontWeight.ExtraBold,
            )
            Text(
              text = if (state.isDrumAutoPlaying) "PLAYING" else "GROOVES",
              color = Color(0xFFFFD166),
              fontSize = 7.sp,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }
      items(filtered, key = { it.id }) { voice ->
        val selected = voice.id == currentVoiceId
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(2.5.dp))
            .background(if (selected) PsrColors.LcdSelectedTop else PsrColors.LcdCardBg)
            .border(0.6.dp, PsrColors.LcdCardBorder, RoundedCornerShape(2.5.dp))
            .clickable { onSelectVoice(voice.id) }
            .padding(horizontal = 6.dp, vertical = 4.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = voice.name,
            color = if (selected) Color.White else PsrColors.LcdTextDark,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          if (voice.isSuperArticulation || voice.isDrumKit) {
            Text(
              text = if (voice.isDrumKit) "HQ DRUM" else "S.Art!",
              color = if (selected) Color(0xFFFFD166) else PsrColors.LcdSelectedTop,
              fontSize = 7.sp,
              fontWeight = FontWeight.ExtraBold,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun StyleBrowserLcdPage(
  state: PsrS975UiState,
  onSelectStyleCategory: (StyleCategory) -> Unit,
  onSelectStyle: (String) -> Unit,
) {
  val filtered = state.allStyles.filter { it.category == state.activeStyleCategory }

  Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
    LazyColumn(
      modifier = Modifier.width(82.dp).fillMaxHeight(),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      items(StyleCategory.entries) { cat ->
        val sel = cat == state.activeStyleCategory
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(2.dp))
            .background(if (sel) PsrColors.LcdSelectedTop else PsrColors.LcdCardBg)
            .border(0.5.dp, PsrColors.LcdCardBorder, RoundedCornerShape(2.dp))
            .clickable { onSelectStyleCategory(cat) }
            .padding(horizontal = 4.dp, vertical = 3.dp),
        ) {
          Text(
            text = cat.label,
            color = if (sel) Color.White else PsrColors.LcdTextDark,
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
          )
        }
      }
    }

    LazyColumn(
      modifier = Modifier.weight(1f).fillMaxHeight(),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      items(filtered, key = { it.id }) { style ->
        val selected = style.id == state.currentStyleId
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(2.5.dp))
            .background(if (selected) PsrColors.LcdSelectedTop else PsrColors.LcdCardBg)
            .border(0.6.dp, PsrColors.LcdCardBorder, RoundedCornerShape(2.5.dp))
            .clickable { onSelectStyle(style.id) }
            .padding(horizontal = 6.dp, vertical = 4.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = style.name,
            color = if (selected) Color.White else PsrColors.LcdTextDark,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "${style.defaultTempo} BPM",
            color = if (selected) Color(0xFFDCE7F3) else PsrColors.LcdTextSub,
            fontSize = 7.5.sp,
            fontWeight = FontWeight.SemiBold,
          )
        }
      }
    }
  }
}

@Composable
private fun ExpansionImporterLcdPage(
  state: PsrS975UiState,
  onRequestFileImport: () -> Unit,
  onSelectVoice: (String) -> Unit,
  onSelectStyle: (String) -> Unit,
  onDeleteImport: (String) -> Unit,
) {
  Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
    // Top Import Button Bar
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        modifier = Modifier
          .clip(RoundedCornerShape(3.dp))
          .background(Brush.verticalGradient(listOf(PsrColors.LcdSelectedTop, PsrColors.LcdSelectedBottom)))
          .clickable(enabled = !state.isImporting, onClick = onRequestFileImport)
          .padding(horizontal = 8.dp, vertical = 4.dp)
          .testTag("import_sf2_sty_wav_button"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Icon(Icons.Filled.UploadFile, contentDescription = "Import file", tint = Color.White, modifier = Modifier.size(12.dp))
        Text(
          text = if (state.isImporting) "IMPORTING..." else "+ IMPORT .SF2 / .STY / .WAV",
          color = Color.White,
          fontSize = 8.sp,
          fontWeight = FontWeight.Bold,
        )
      }
      Text(
        text = "SF2:${state.importedSf2Banks.size} STY:${state.importedStyFiles.size} WAV:${state.importedWavSamples.size}",
        color = PsrColors.LcdTextDark,
        fontSize = 7.5.sp,
        fontWeight = FontWeight.Bold,
      )
    }

    // List of imported SF2 presets, STY styles, WAV samples
    if (state.importedSf2Banks.isEmpty() && state.importedStyFiles.isEmpty() && state.importedWavSamples.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(PsrColors.LcdCardBg, RoundedCornerShape(3.dp))
          .border(0.7.dp, PsrColors.LcdCardBorder, RoundedCornerShape(3.dp))
          .padding(8.dp),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "Tap '+ IMPORT .SF2 / .STY / .WAV' to load SoundFonts, Yamaha Styles, or WAV audio samples from your device.",
          color = PsrColors.LcdTextSub,
          fontSize = 8.sp,
          fontWeight = FontWeight.Medium,
        )
      }
    } else {
      LazyColumn(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        for (bank in state.importedSf2Banks) {
          item(key = "hdr_${bank.id}") {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFCEDDF0), RoundedCornerShape(2.dp))
                .padding(horizontal = 5.dp, vertical = 2.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "SF2: ${bank.bankName} (${bank.presets.size} presets)",
                color = PsrColors.LcdTextDark,
                fontSize = 7.5.sp,
                fontWeight = FontWeight.ExtraBold,
              )
              Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "Delete SF2",
                tint = Color(0xFFB91C1C),
                modifier = Modifier.size(13.dp).clickable { onDeleteImport(bank.id) },
              )
            }
          }
          items(bank.presets.take(16)) { preset ->
            val pIdx = bank.presets.indexOf(preset)
            val vId = "sf2_${bank.id}_$pIdx"
            val active = state.parts[state.selectedPartForEdit]?.voiceId == vId
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .background(if (active) PsrColors.LcdSelectedTop else PsrColors.LcdCardBg, RoundedCornerShape(2.dp))
                .clickable { onSelectVoice(vId) }
                .padding(horizontal = 6.dp, vertical = 3.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = "  ${preset.name}",
                color = if (active) Color.White else PsrColors.LcdTextDark,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "USE",
                color = if (active) Color(0xFFFFD166) else PsrColors.LcdSelectedTop,
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }

        items(state.importedStyFiles, key = { it.id }) { sty ->
          val active = state.currentStyleId == sty.style.id
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(if (active) PsrColors.LcdSelectedTop else PsrColors.LcdCardBg, RoundedCornerShape(2.dp))
              .border(0.5.dp, PsrColors.LcdCardBorder, RoundedCornerShape(2.dp))
              .clickable { onSelectStyle(sty.style.id) }
              .padding(horizontal = 6.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "STY: ${sty.style.name} (${sty.style.defaultTempo} BPM)",
              color = if (active) Color.White else PsrColors.LcdTextDark,
              fontSize = 8.sp,
              fontWeight = FontWeight.Bold,
            )
            Icon(
              imageVector = Icons.Filled.Delete,
              contentDescription = "Delete STY",
              tint = if (active) Color.White else Color(0xFFB91C1C),
              modifier = Modifier.size(13.dp).clickable { onDeleteImport(sty.id) },
            )
          }
        }

        items(state.importedWavSamples, key = { it.id }) { wav ->
          val vId = "wav_${wav.id}"
          val active = state.parts[state.selectedPartForEdit]?.voiceId == vId
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(if (active) PsrColors.LcdSelectedTop else PsrColors.LcdCardBg, RoundedCornerShape(2.dp))
              .border(0.5.dp, PsrColors.LcdCardBorder, RoundedCornerShape(2.dp))
              .clickable { onSelectVoice(vId) }
              .padding(horizontal = 6.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              Icon(Icons.Filled.AudioFile, contentDescription = null, tint = PsrColors.LcdAccentAmber, modifier = Modifier.size(11.dp))
              Text(
                text = "WAV: ${wav.displayName} (${wav.durationMs}ms)",
                color = if (active) Color.White else PsrColors.LcdTextDark,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
              )
            }
            Icon(
              imageVector = Icons.Filled.Delete,
              contentDescription = "Delete WAV",
              tint = if (active) Color.White else Color(0xFFB91C1C),
              modifier = Modifier.size(13.dp).clickable { onDeleteImport(wav.id) },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun FreezeSettingsLcdPage(
  options: FreezeOptions,
  isFreezeEnabled: Boolean,
  onUpdate: (FreezeOptions) -> Unit,
) {
  Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
    Text(
      text = "FREEZE STATUS: ${if (isFreezeEnabled) "ACTIVE (Locks checked items)" else "OFF"}",
      color = if (isFreezeEnabled) PsrColors.LcdSelectedTop else PsrColors.LcdTextSub,
      fontSize = 8.sp,
      fontWeight = FontWeight.Bold,
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
      FreezeToggleChip("STYLE", options.freezeStyle, Modifier.weight(1f)) {
        onUpdate(options.copy(freezeStyle = !options.freezeStyle))
      }
      FreezeToggleChip("TEMPO", options.freezeTempo, Modifier.weight(1f)) {
        onUpdate(options.copy(freezeTempo = !options.freezeTempo))
      }
      FreezeToggleChip("TRANSPOSE", options.freezeTranspose, Modifier.weight(1f)) {
        onUpdate(options.copy(freezeTranspose = !options.freezeTranspose))
      }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
      FreezeToggleChip("VOICE", options.freezeVoices, Modifier.weight(1f)) {
        onUpdate(options.copy(freezeVoices = !options.freezeVoices))
      }
      FreezeToggleChip("HARMONY", options.freezeHarmony, Modifier.weight(1f)) {
        onUpdate(options.copy(freezeHarmony = !options.freezeHarmony))
      }
    }
  }
}

@Composable
private fun FreezeToggleChip(
  label: String,
  checked: Boolean,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(3.dp))
      .background(if (checked) PsrColors.LcdSelectedTop else PsrColors.LcdCardBg)
      .border(0.7.dp, PsrColors.LcdCardBorder, RoundedCornerShape(3.dp))
      .clickable(onClick = onClick)
      .padding(vertical = 6.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = "${if (checked) "✓ " else ""}$label",
      color = if (checked) Color.White else PsrColors.LcdTextDark,
      fontSize = 8.sp,
      fontWeight = FontWeight.Bold,
    )
  }
}

@Composable
private fun RegistrationBankLcdPage(
  state: PsrS975UiState,
  onSelectSlot: (Int) -> Unit,
) {
  val slots = state.registrationBanks[state.currentBankNumber] ?: emptyList()
  LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
    items(slots) { slot ->
      val active = state.activeRegSlot == slot.slotNumber
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(if (active) PsrColors.LcdSelectedTop else PsrColors.LcdCardBg, RoundedCornerShape(2.dp))
          .border(0.5.dp, PsrColors.LcdCardBorder, RoundedCornerShape(2.dp))
          .clickable { onSelectSlot(slot.slotNumber) }
          .padding(horizontal = 6.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "REG ${slot.slotNumber}: ${slot.label}",
          color = if (active) Color.White else PsrColors.LcdTextDark,
          fontSize = 8.sp,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = "${slot.tempo} BPM",
          color = if (active) Color(0xFFDCE7F3) else PsrColors.LcdTextSub,
          fontSize = 7.5.sp,
        )
      }
    }
  }
}

@Composable
private fun MixerEffectsLcdPage(
  state: PsrS975UiState,
  onShiftTranspose: (Int) -> Unit,
  onShiftOctave: (Int) -> Unit,
  onChangePitchBendRange: (Int) -> Unit,
  onSetTempo: (Int) -> Unit,
) {
  Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
    LcdStepRow("TRANSPOSE", "${state.transpose}", { onShiftTranspose(-1) }, { onShiftTranspose(1) })
    LcdStepRow("UPPER OCTAVE", "${state.upperOctave}", { onShiftOctave(-1) }, { onShiftOctave(1) })
    LcdStepRow("TEMPO BPM", "${state.tempoBpm}", { onSetTempo(state.tempoBpm - 2) }, { onSetTempo(state.tempoBpm + 2) })
    LcdStepRow("PITCH BEND RANGE", "±${state.pitchBendRangeSemitones} st", { onChangePitchBendRange(state.pitchBendRangeSemitones - 1) }, { onChangePitchBendRange(state.pitchBendRangeSemitones + 1) })
  }
}

@Composable
private fun LcdStepRow(
  label: String,
  value: String,
  onMinus: () -> Unit,
  onPlus: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(PsrColors.LcdCardBg, RoundedCornerShape(2.5.dp))
      .border(0.6.dp, PsrColors.LcdCardBorder, RoundedCornerShape(2.5.dp))
      .padding(horizontal = 6.dp, vertical = 2.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(text = label, color = PsrColors.LcdTextDark, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier
          .background(PsrColors.LcdSelectedBottom, RoundedCornerShape(2.dp))
          .clickable(onClick = onMinus)
          .padding(horizontal = 7.dp, vertical = 1.dp),
      ) { Text("-", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
      Text(text = value, color = PsrColors.LcdTextDark, fontSize = 8.5.sp, fontWeight = FontWeight.ExtraBold)
      Box(
        modifier = Modifier
          .background(PsrColors.LcdSelectedBottom, RoundedCornerShape(2.dp))
          .clickable(onClick = onPlus)
          .padding(horizontal = 7.dp, vertical = 1.dp),
      ) { Text("+", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
    }
  }
}

@Composable
private fun SongPlayerLcdPage(
  state: PsrS975UiState,
  onOpenImporter: () -> Unit,
) {
  Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(PsrColors.LcdCardBg, RoundedCornerShape(3.dp))
        .border(0.6.dp, PsrColors.LcdCardBorder, RoundedCornerShape(3.dp))
        .padding(6.dp),
    ) {
      Text("ACTIVE SONG: ${state.songTitle}", color = PsrColors.LcdTextDark, fontSize = 9.sp, fontWeight = FontWeight.Bold)
      Text(
        "Status: ${if (state.isRecording) "RECORDING WAV..." else if (state.isSongPlaying) "PLAYING" else "STOPPED"} | Repeat: ${if (state.songRepeat) "ON" else "OFF"}",
        color = PsrColors.LcdTextSub,
        fontSize = 7.5.sp,
      )
    }
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(PsrColors.LcdSelectedTop, RoundedCornerShape(3.dp))
        .clickable(onClick = onOpenImporter)
        .padding(6.dp),
      contentAlignment = Alignment.Center,
    ) {
      Text("OPEN SF2 / STY / WAV IMPORTER", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
  }
}

@Composable
private fun SettingsLcdPage(
  state: PsrS975UiState,
  onChangeKeyCount: (Int) -> Unit,
  onChangePitchBendRange: (Int) -> Unit,
) {
  Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text("KEYBOARD SPAN: ${state.keyboardKeyCount} KEYS", color = PsrColors.LcdTextDark, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
      listOf(25, 37, 49, 61).forEach { cnt ->
        val sel = state.keyboardKeyCount == cnt
        Box(
          modifier = Modifier
            .weight(1f)
            .background(if (sel) PsrColors.LcdSelectedTop else PsrColors.LcdCardBg, RoundedCornerShape(2.dp))
            .border(0.6.dp, PsrColors.LcdCardBorder, RoundedCornerShape(2.dp))
            .clickable { onChangeKeyCount(cnt) }
            .padding(vertical = 4.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text("$cnt", color = if (sel) Color.White else PsrColors.LcdTextDark, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
    LcdStepRow("PITCH BEND RANGE", "±${state.pitchBendRangeSemitones} st", { onChangePitchBendRange(state.pitchBendRangeSemitones - 1) }, { onChangePitchBendRange(state.pitchBendRangeSemitones + 1) })
  }
}

@Composable
private fun HelpGuideLcdPage() {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(PsrColors.LcdCardBg, RoundedCornerShape(3.dp))
      .padding(6.dp),
    verticalArrangement = Arrangement.spacedBy(3.dp),
  ) {
    Text("PSR-S975 QUICK GUIDE", color = PsrColors.LcdTextDark, fontSize = 8.5.sp, fontWeight = FontWeight.ExtraBold)
    Text("• DRUM AUTO / PERC.&DRUM: Auto Drum Kit grooves + 16-step grid.", color = PsrColors.LcdTextDark, fontSize = 7.5.sp)
    Text("• EXPANSION/USER or MENU: Import .SF2, .STY, .WAV files.", color = PsrColors.LcdTextDark, fontSize = 7.5.sp)
    Text("• MEMORY + 1..8: Save setup. FREEZE: Lock Style/Tempo.", color = PsrColors.LcdTextDark, fontSize = 7.5.sp)
    Text("• PITCH & MOD Wheels (left of keys): Real-time bend & vibrato.", color = PsrColors.LcdTextDark, fontSize = 7.5.sp)
  }
}

@Composable
private fun DrumAutoPlayerLcdPage(
  state: PsrS975UiState,
  onTogglePlay: () -> Unit,
  onSelectGroove: (String) -> Unit,
  onSelectKit: (String) -> Unit,
  onTriggerFill: () -> Unit,
  onToggleCell: (Int, Int) -> Unit,
  onAuditionHit: (Int) -> Unit,
) {
  val groove = state.currentDrumGroove
  val kits = listOf(
    "v_power_kit" to "PowerKit",
    "v_studio_kit" to "Studio",
    "v_real_kit" to "Acoustic",
    "v_edm_kit" to "EDM 808",
  )
  val lanes = listOf(
    36 to "KICK",
    38 to "SNARE",
    42 to "HAT",
    49 to "PERC",
  )

  Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
    // Transport & Kit Row
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(3.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(2.5.dp))
          .background(if (state.isDrumAutoPlaying) Color(0xFF15803D) else PsrColors.LcdSelectedTop)
          .clickable(onClick = onTogglePlay)
          .padding(horizontal = 7.dp, vertical = 3.dp)
          .testTag("drum_auto_play_toggle"),
      ) {
        Text(
          text = if (state.isDrumAutoPlaying) "■ STOP" else "▶ AUTO PLAY",
          color = Color.White,
          fontSize = 7.5.sp,
          fontWeight = FontWeight.ExtraBold,
        )
      }
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(2.5.dp))
          .background(if (state.drumAutoFillPending) PsrColors.LcdAccentAmber else PsrColors.LcdSelectedBottom)
          .clickable(onClick = onTriggerFill)
          .padding(horizontal = 6.dp, vertical = 3.dp)
          .testTag("drum_auto_fill_button"),
      ) {
        Text(
          text = if (state.drumAutoFillPending) "FILL..." else "AUTO FILL",
          color = Color.White,
          fontSize = 7.5.sp,
          fontWeight = FontWeight.Bold,
        )
      }
      kits.forEach { (id, label) ->
        val sel = state.selectedDrumKitId == id
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(2.dp))
            .background(if (sel) PsrColors.LcdSelectedTop else PsrColors.LcdCardBg)
            .border(0.5.dp, PsrColors.LcdCardBorder, RoundedCornerShape(2.dp))
            .clickable { onSelectKit(id) }
            .padding(vertical = 3.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = label,
            color = if (sel) Color.White else PsrColors.LcdTextDark,
            fontSize = 6.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
          )
        }
      }
    }

    // Groove Preset Selector (2 rows of 4)
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
      for (row in 0..1) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
          for (col in 0..3) {
            val g = state.drumGrooves.getOrNull(row * 4 + col)
            if (g != null) {
              val sel = g.id == state.selectedDrumGrooveId
              Box(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(2.dp))
                  .background(if (sel) PsrColors.LcdSelectedBottom else PsrColors.LcdCardBg)
                  .border(0.5.dp, PsrColors.LcdCardBorder, RoundedCornerShape(2.dp))
                  .clickable { onSelectGroove(g.id) }
                  .padding(vertical = 2.dp, horizontal = 2.dp),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = g.name,
                  color = if (sel) Color.White else PsrColors.LcdTextDark,
                  fontSize = 6.5.sp,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
        }
      }
    }

    // Live 16-Step Drum Grid
    Column(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .background(PsrColors.LcdCardBg, RoundedCornerShape(2.5.dp))
        .border(0.6.dp, PsrColors.LcdCardBorder, RoundedCornerShape(2.5.dp))
        .padding(3.dp),
      verticalArrangement = Arrangement.SpaceEvenly,
    ) {
      lanes.forEach { (note, label) ->
        Row(
          modifier = Modifier.fillMaxWidth().weight(1f),
          horizontalArrangement = Arrangement.spacedBy(1.5.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            modifier = Modifier
              .width(34.dp)
              .fillMaxHeight()
              .padding(vertical = 0.5.dp)
              .clip(RoundedCornerShape(1.5.dp))
              .background(PsrColors.LcdHeaderBg)
              .clickable { onAuditionHit(note) },
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = label,
              color = Color.White,
              fontSize = 6.sp,
              fontWeight = FontWeight.Bold,
            )
          }
          for (step in 0 until 16) {
            val isHit = groove.steps.any {
              it.step == step && when (note) {
                42 -> it.drumNote == 42 || it.drumNote == 44 || it.drumNote == 46 || it.drumNote == 51
                49 -> it.drumNote >= 45 && it.drumNote != 46 && it.drumNote != 51
                else -> it.drumNote == note || (note == 38 && (it.drumNote == 37 || it.drumNote == 39))
              }
            }
            val isPlayhead = state.isDrumAutoPlaying && state.drumAutoCurrentStep == step
            val cellBg = when {
              isPlayhead && isHit -> PsrColors.LedGreen
              isPlayhead -> Color(0xFF93C5FD)
              isHit -> PsrColors.LcdSelectedTop
              step % 4 == 0 -> Color(0xFFD0DCEB)
              else -> Color(0xFFE2EBF5)
            }
            Box(
              modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(vertical = 0.5.dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(cellBg)
                .clickable { onToggleCell(note, step) },
            )
          }
        }
      }
    }
  }
}
