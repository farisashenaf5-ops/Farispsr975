package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PsrColors
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

enum class LedColor { NONE, GREEN, RED, AMBER, BLUE }

@Composable
fun YamahaLogoMark(
  modifier: Modifier = Modifier,
  showModelBadge: Boolean = true,
  logoSize: Dp = 18.dp,
) {
  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(5.dp),
  ) {
    // Yamaha Tuning Fork Circle Emblem
    Canvas(modifier = Modifier.size(logoSize)) {
      val r = size.minDimension / 2f
      val c = center
      drawCircle(
        color = PsrColors.SilverBright,
        radius = r - 1f,
        style = Stroke(width = 1.6f),
      )
      // Three crossed tuning forks (120 deg apart)
      for (deg in listOf(-90f, 30f, 150f)) {
        val rad = deg * PI.toFloat() / 180f
        val end = Offset(c.x + cos(rad) * (r * 0.78f), c.y + sin(rad) * (r * 0.78f))
        drawLine(
          color = PsrColors.SilverBright,
          start = c,
          end = end,
          strokeWidth = 1.6f,
          cap = StrokeCap.Round,
        )
      }
      drawCircle(color = PsrColors.SilverBright, radius = r * 0.16f, center = c)
    }
    Text(
      text = "YAMAHA",
      color = PsrColors.SilverBright,
      fontSize = 13.sp,
      fontWeight = FontWeight.Black,
      letterSpacing = 1.1.sp,
    )
    if (showModelBadge) {
      Spacer(modifier = Modifier.width(6.dp))
      Row(verticalAlignment = Alignment.Bottom) {
        Text(
          text = "PSR-",
          color = PsrColors.SilverMid,
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(bottom = 1.dp),
        )
        Text(
          text = "S975",
          color = PsrColors.SilverBright,
          fontSize = 15.sp,
          fontWeight = FontWeight.ExtraBold,
          fontStyle = FontStyle.Italic,
          letterSpacing = 0.8.sp,
        )
      }
    }
  }
}

@Composable
fun PsrHardwareButton(
  label: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  ledColor: LedColor = LedColor.NONE,
  isLit: Boolean = false,
  isBigNumber: Boolean = false,
  subLabel: String? = null,
  fontSize: TextUnit = 7.5.sp,
  buttonHeight: Dp = 22.dp,
  testTag: String? = null,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  val resolvedLed = when {
    !isLit -> PsrColors.LedOff
    ledColor == LedColor.GREEN -> PsrColors.LedGreen
    ledColor == LedColor.RED -> PsrColors.LedRed
    ledColor == LedColor.AMBER -> PsrColors.LedAmber
    ledColor == LedColor.BLUE -> PsrColors.LedBlue
    else -> PsrColors.LedOff
  }

  val topBg = when {
    isPressed -> PsrColors.ButtonPressed
    isLit && ledColor == LedColor.GREEN && isBigNumber -> Color(0xFF2E7D32)
    isLit && ledColor == LedColor.AMBER && isBigNumber -> Color(0xFF9A5B13)
    else -> PsrColors.ButtonSurfaceTop
  }
  val botBg = when {
    isPressed -> PsrColors.PanelWell
    isLit && ledColor == LedColor.GREEN && isBigNumber -> Color(0xFF1B4D20)
    isLit && ledColor == LedColor.AMBER && isBigNumber -> Color(0xFF6B3E0A)
    else -> PsrColors.ButtonSurfaceBottom
  }

  Column(
    modifier = modifier
      .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    if (subLabel != null) {
      Text(
        text = subLabel,
        color = PsrColors.LabelMuted,
        fontSize = 6.5.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Clip,
      )
    }
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(buttonHeight)
        .shadow(1.dp, RoundedCornerShape(3.dp))
        .clip(RoundedCornerShape(3.dp))
        .background(Brush.verticalGradient(listOf(topBg, botBg)))
        .border(
          width = 0.8.dp,
          color = if (isLit && ledColor != LedColor.NONE) resolvedLed.copy(alpha = 0.7f) else PsrColors.PanelHighlight,
          shape = RoundedCornerShape(3.dp),
        )
        .clickable(
          interactionSource = interactionSource,
          indication = null,
          onClick = onClick,
        )
        .padding(horizontal = 2.dp),
      contentAlignment = Alignment.Center,
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        if (ledColor != LedColor.NONE && !isBigNumber) {
          Box(
            modifier = Modifier
              .width(12.dp)
              .height(2.5.dp)
              .clip(RoundedCornerShape(1.dp))
              .background(resolvedLed),
          )
          Spacer(modifier = Modifier.height(1.5.dp))
        }
        Text(
          text = label,
          color = if (isLit && isBigNumber) Color.White else PsrColors.LabelWhite,
          fontSize = fontSize,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          lineHeight = 9.sp,
        )
      }
    }
  }
}

@Composable
fun PsrStartStopButton(
  isPlaying: Boolean,
  beatInBar: Int,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val ledColor = when {
    !isPlaying -> PsrColors.LedOff
    beatInBar == 0 -> PsrColors.LedRed
    else -> PsrColors.LedGreen
  }
  Column(
    modifier = modifier.testTag("style_start_stop_button"),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = "START/STOP",
      color = PsrColors.LabelWhite,
      fontSize = 6.5.sp,
      fontWeight = FontWeight.Bold,
      maxLines = 1,
    )
    Spacer(modifier = Modifier.height(1.dp))
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(24.dp)
        .clip(RoundedCornerShape(3.dp))
        .background(
          Brush.verticalGradient(
            listOf(PsrColors.StartStopTop, PsrColors.StartStopBottom),
          )
        )
        .border(1.dp, PsrColors.SilverBright, RoundedCornerShape(3.dp))
        .clickable(onClick = onClick),
      contentAlignment = Alignment.Center,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Box(
          modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(if (isPlaying) ledColor else Color(0xFF454A52)),
        )
        Text(
          text = if (isPlaying) "■" else "▶",
          color = Color(0xFF1A1D21),
          fontSize = 10.sp,
          fontWeight = FontWeight.Black,
        )
      }
    }
  }
}

@Composable
fun PsrRotaryKnob(
  label: String,
  value: Float, // 0f..1f
  onValueChange: (Float) -> Unit,
  modifier: Modifier = Modifier,
  knobSize: Dp = 32.dp,
  leftHint: String = "MIN",
  rightHint: String = "MAX",
  testTag: String? = null,
) {
  var currentVal by remember(value) { mutableFloatStateOf(value) }

  Column(
    modifier = modifier.then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    if (label.isNotEmpty()) {
      Text(
        text = label,
        color = PsrColors.LabelWhite,
        fontSize = 6.5.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        maxLines = 1,
      )
      Spacer(modifier = Modifier.height(1.dp))
    }
    Box(
      modifier = Modifier
        .size(knobSize)
        .pointerInput(Unit) {
          detectDragGestures { change, dragAmount ->
            change.consume()
            val delta = (-dragAmount.y + dragAmount.x * 0.5f) / 140f
            currentVal = (currentVal + delta).coerceIn(0f, 1f)
            onValueChange(currentVal)
          }
        },
      contentAlignment = Alignment.Center,
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val r = size.minDimension / 2f
        val c = center
        // Outer tick marks
        for (i in 0..10) {
          val deg = 135f + i * 27f
          val rad = deg * PI.toFloat() / 180f
          val p1 = Offset(c.x + cos(rad) * (r * 0.85f), c.y + sin(rad) * (r * 0.85f))
          val p2 = Offset(c.x + cos(rad) * (r * 0.98f), c.y + sin(rad) * (r * 0.98f))
          drawLine(
            color = if (i / 10f <= value) PsrColors.SilverBright else PsrColors.SilverDark,
            start = p1,
            end = p2,
            strokeWidth = 1.2f,
          )
        }
        // Knob body
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(Color(0xFF42464E), Color(0xFF16181B), Color(0xFF0B0C0E)),
            center = Offset(c.x - r * 0.2f, c.y - r * 0.2f),
            radius = r * 0.85f,
          ),
          radius = r * 0.72f,
        )
        drawCircle(
          color = PsrColors.SilverDark,
          radius = r * 0.72f,
          style = Stroke(width = 1.2f),
        )
        // Pointer notch
        val angleDeg = 135f + value * 270f
        val rad = angleDeg * PI.toFloat() / 180f
        drawLine(
          color = PsrColors.SilverBright,
          start = Offset(c.x + cos(rad) * (r * 0.25f), c.y + sin(rad) * (r * 0.25f)),
          end = Offset(c.x + cos(rad) * (r * 0.68f), c.y + sin(rad) * (r * 0.68f)),
          strokeWidth = 2.2f,
          cap = StrokeCap.Round,
        )
      }
    }
    if (leftHint.isNotEmpty() || rightHint.isNotEmpty()) {
      Row(
        modifier = Modifier.width(knobSize + 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(text = leftHint, color = PsrColors.LabelMuted, fontSize = 5.5.sp)
        Text(text = rightHint, color = PsrColors.LabelMuted, fontSize = 5.5.sp)
      }
    }
  }
}

@Composable
fun PsrJogWheel(
  onStep: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  var accumAngle by remember { mutableFloatStateOf(0f) }

  Column(
    modifier = modifier.testTag("data_entry_jog_wheel"),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = "DATA ENTRY",
      color = PsrColors.LabelWhite,
      fontSize = 7.sp,
      fontWeight = FontWeight.Bold,
    )
    Spacer(modifier = Modifier.height(3.dp))
    Box(
      modifier = Modifier
        .size(62.dp)
        .clip(CircleShape)
        .background(PsrColors.PanelWell)
        .border(1.5.dp, PsrColors.PanelBorder, CircleShape)
        .pointerInput(Unit) {
          var prevAngle: Float? = null
          detectDragGestures(
            onDragStart = { prevAngle = null },
            onDragEnd = { prevAngle = null },
            onDragCancel = { prevAngle = null },
          ) { change, dragAmount ->
            change.consume()
            val cx = size.width / 2f
            val cy = size.height / 2f
            val angle = atan2(change.position.y - cy, change.position.x - cx)
            val prev = prevAngle
            if (prev != null) {
              var diff = angle - prev
              if (diff > PI) diff -= (2 * PI).toFloat()
              if (diff < -PI) diff += (2 * PI).toFloat()
              accumAngle += diff
              val stepAngle = 0.35f
              if (kotlin.math.abs(accumAngle) >= stepAngle) {
                val steps = (accumAngle / stepAngle).roundToInt()
                if (steps != 0) {
                  onStep(steps)
                  accumAngle -= steps * stepAngle
                }
              }
            } else if (kotlin.math.abs(dragAmount.y) > 0.1f) {
              val steps = (-dragAmount.y / 14f).roundToInt()
              if (steps != 0) onStep(steps)
            }
            prevAngle = angle
          }
        },
      contentAlignment = Alignment.Center,
    ) {
      Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
        val r = size.minDimension / 2f
        // Brushed metallic aluminum dial
        drawCircle(
          brush = Brush.sweepGradient(
            listOf(
              Color(0xFF7B828C),
              Color(0xFFD5DAE0),
              Color(0xFF606670),
              Color(0xFFC5CBD3),
              Color(0xFF7B828C),
            )
          ),
          radius = r,
        )
        drawCircle(
          color = Color(0xFF2E3238),
          radius = r * 0.94f,
          style = Stroke(width = 1.5f),
        )
        // Finger dimple
        drawCircle(
          brush = Brush.radialGradient(
            listOf(Color(0xFF474C54), Color(0xFFA0A7B0)),
          ),
          radius = r * 0.24f,
          center = Offset(center.x + r * 0.42f, center.y - r * 0.32f),
        )
      }
    }
  }
}

@Composable
fun PitchBendAndModWheels(
  pitchBend: Float, // -1f .. +1f
  modulation: Float, // 0f .. 1f
  onPitchBendChange: (Float) -> Unit,
  onModulationChange: (Float) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .background(PsrColors.PanelWell, RoundedCornerShape(4.dp))
      .border(1.dp, PsrColors.PanelBorder, RoundedCornerShape(4.dp))
      .padding(horizontal = 4.dp, vertical = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Text(
      text = "PITCH / MOD",
      color = PsrColors.LabelMuted,
      fontSize = 6.sp,
      fontWeight = FontWeight.Bold,
      maxLines = 1,
    )
    Row(
      modifier = Modifier.weight(1f),
      horizontalArrangement = Arrangement.spacedBy(5.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // PITCH BEND (spring return to 0f)
      HardwareWheel(
        label = "PITCH",
        normalizedPos = (pitchBend + 1f) / 2f, // 0..1 with 0.5 center
        isBipolar = true,
        accentColor = PsrColors.LedBlue,
        onPositionChange = { pos ->
          onPitchBendChange((pos * 2f - 1f).coerceIn(-1f, 1f))
        },
        onRelease = { onPitchBendChange(0f) },
        modifier = Modifier.weight(1f).fillMaxHeight().testTag("pitch_bend_wheel"),
      )
      // MODULATION (holds value 0f..1f)
      HardwareWheel(
        label = "MOD",
        normalizedPos = modulation,
        isBipolar = false,
        accentColor = PsrColors.LedAmber,
        onPositionChange = { pos -> onModulationChange(pos.coerceIn(0f, 1f)) },
        onRelease = {},
        modifier = Modifier.weight(1f).fillMaxHeight().testTag("modulation_wheel"),
      )
    }
  }
}

@Composable
private fun HardwareWheel(
  label: String,
  normalizedPos: Float,
  isBipolar: Boolean,
  accentColor: Color,
  onPositionChange: (Float) -> Unit,
  onRelease: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var posState by remember(normalizedPos) { mutableFloatStateOf(normalizedPos) }

  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Box(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .clip(RoundedCornerShape(3.dp))
        .background(Color(0xFF070809))
        .border(0.8.dp, PsrColors.PanelHighlight, RoundedCornerShape(3.dp))
        .pointerInput(Unit) {
          detectDragGestures(
            onDragEnd = { onRelease() },
            onDragCancel = { onRelease() },
          ) { change, dragAmount ->
            change.consume()
            val delta = -dragAmount.y / size.height.coerceAtLeast(40).toFloat()
            posState = (posState + delta).coerceIn(0f, 1f)
            onPositionChange(posState)
          }
        },
    ) {
      Canvas(modifier = Modifier.fillMaxSize().padding(2.dp)) {
        val w = size.width
        val h = size.height
        // Wheel track
        drawRoundRect(
          brush = Brush.verticalGradient(
            listOf(Color(0xFF1D2024), Color(0xFF2D3138), Color(0xFF15171A)),
          ),
          cornerRadius = CornerRadius(4f, 4f),
        )
        // Center detent line for Pitch Bend
        if (isBipolar) {
          drawLine(
            color = PsrColors.SilverDark,
            start = Offset(0f, h / 2f),
            end = Offset(w, h / 2f),
            strokeWidth = 1.2f,
          )
        }
        // Ribbed thumb grip
        val thumbY = ((1f - normalizedPos) * (h - 14f)).coerceIn(2f, h - 14f)
        drawRoundRect(
          brush = Brush.verticalGradient(
            listOf(Color(0xFF4B515B), Color(0xFF262A30), Color(0xFF181A1E)),
          ),
          topLeft = Offset(1f, thumbY),
          size = Size(w - 2f, 12f),
          cornerRadius = CornerRadius(3f, 3f),
        )
        // Glowing indicator bar on grip
        val isActive = if (isBipolar) kotlin.math.abs(normalizedPos - 0.5f) > 0.03f else normalizedPos > 0.03f
        drawLine(
          color = if (isActive) accentColor else PsrColors.SilverBright,
          start = Offset(3f, thumbY + 6f),
          end = Offset(w - 3f, thumbY + 6f),
          strokeWidth = 2.2f,
          cap = StrokeCap.Round,
        )
      }
    }
    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text = label,
      color = PsrColors.LabelWhite,
      fontSize = 6.sp,
      fontWeight = FontWeight.Bold,
    )
  }
}

@Composable
fun VolumeLedMeter(
  level: Float,
  volume: Float,
  onVolumeChange: (Float) -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(5.dp),
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = "VOLUME",
        color = PsrColors.LabelMuted,
        fontSize = 6.5.sp,
        fontWeight = FontWeight.Bold,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Row(
        horizontalArrangement = Arrangement.spacedBy(1.5.dp),
        modifier = Modifier
          .background(PsrColors.PanelWell, RoundedCornerShape(2.dp))
          .padding(horizontal = 3.dp, vertical = 2.dp),
      ) {
        val segments = 16
        val litCount = (level * segments * 1.4f).roundToInt().coerceIn(0, segments)
        for (i in 0 until segments) {
          val active = i < litCount
          val segColor = when {
            !active -> Color(0xFF20242A)
            i >= 13 -> PsrColors.LedRed
            i >= 10 -> PsrColors.LedAmber
            else -> PsrColors.LedGreen
          }
          Box(
            modifier = Modifier
              .width(3.dp)
              .height(8.dp)
              .background(segColor, RoundedCornerShape(0.5.dp)),
          )
        }
      }
    }
    PsrRotaryKnob(
      label = "",
      value = volume,
      onValueChange = onVolumeChange,
      knobSize = 24.dp,
      leftHint = "",
      rightHint = "",
      testTag = "top_volume_knob",
    )
  }
}
