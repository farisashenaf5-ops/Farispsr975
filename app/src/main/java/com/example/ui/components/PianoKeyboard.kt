package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import com.example.ui.theme.PsrColors

@Composable
fun PianoKeyboard(
  baseMidi: Int,
  keyCount: Int,
  splitNote: Int,
  acmpEnabled: Boolean,
  pressedNotes: Set<Int>,
  onNoteOn: (Int) -> Unit,
  onNoteOff: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  val pointerNoteMap = remember { mutableStateMapOf<PointerId, Int>() }

  // Build ordered key lists
  val whiteNotes = remember(baseMidi, keyCount) {
    (baseMidi until (baseMidi + keyCount)).filter { !isBlackKey(it) }
  }
  val blackNotes = remember(baseMidi, keyCount) {
    (baseMidi until (baseMidi + keyCount)).filter { isBlackKey(it) }
  }

  Box(
    modifier = modifier
      .background(Color(0xFF0A0B0D))
      .testTag("piano_keyboard")
      .pointerInput(baseMidi, keyCount, whiteNotes) {
        awaitPointerEventScope {
          while (true) {
            val event = awaitPointerEvent()
            val w = size.width.toFloat().coerceAtLeast(1f)
            val h = size.height.toFloat().coerceAtLeast(1f)
            val whiteW = w / whiteNotes.size.coerceAtLeast(1)
            val blackH = h * 0.60f
            val blackW = whiteW * 0.60f

            for (change in event.changes) {
              val id = change.id
              if (!change.pressed) {
                val old = pointerNoteMap.remove(id)
                if (old != null && !pointerNoteMap.values.contains(old)) {
                  onNoteOff(old)
                }
                change.consume()
              } else {
                val pos = change.position
                val hitNote = findHitMidiNote(
                  x = pos.x,
                  y = pos.y,
                  whiteNotes = whiteNotes,
                  blackNotes = blackNotes,
                  whiteW = whiteW,
                  blackW = blackW,
                  blackH = blackH,
                )
                val prev = pointerNoteMap[id]
                if (prev != hitNote) {
                  if (prev != null && !pointerNoteMap.values.contains(prev)) {
                    onNoteOff(prev)
                  }
                  if (hitNote != null) {
                    pointerNoteMap[id] = hitNote
                    onNoteOn(hitNote)
                  } else {
                    pointerNoteMap.remove(id)
                  }
                }
                change.consume()
              }
            }
          }
        }
      },
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height
      val whiteCount = whiteNotes.size.coerceAtLeast(1)
      val whiteW = w / whiteCount
      val blackW = whiteW * 0.60f
      val blackH = h * 0.60f

      // Draw white keys
      whiteNotes.forEachIndexed { idx, midi ->
        val x = idx * whiteW
        val isPressed = pressedNotes.contains(midi)
        val isSplitMarker = acmpEnabled && midi <= splitNote && (idx + 1 < whiteNotes.size && whiteNotes[idx + 1] > splitNote)
        drawRoundRect(
          brush = Brush.verticalGradient(
            if (isPressed) {
              listOf(PsrColors.WhiteKeyPressed, Color(0xFFB3BCC8))
            } else {
              listOf(PsrColors.WhiteKeyTop, PsrColors.WhiteKeyBottom)
            }
          ),
          topLeft = Offset(x + 0.6f, 2f),
          size = Size(whiteW - 1.2f, h - 3f),
          cornerRadius = CornerRadius(2.5f, 2.5f),
        )
        // Subtle bottom lip shadow
        drawRect(
          color = Color(0x33000000),
          topLeft = Offset(x + 0.6f, h - 5f),
          size = Size(whiteW - 1.2f, 4f),
        )
        if (isSplitMarker) {
          drawLine(
            color = PsrColors.LedAmber,
            start = Offset(x + whiteW - 1f, 3f),
            end = Offset(x + whiteW - 1f, h - 4f),
            strokeWidth = 2.2f,
          )
        }
      }

      // Classic Yamaha red felt strip along the keybed back
      drawRect(
        color = Color(0xFF5A1519),
        topLeft = Offset(0f, 0f),
        size = Size(w, 3.2f),
      )

      // Draw black keys on top
      for (midi in blackNotes) {
        val leftWhiteIdx = whiteNotes.indexOfLast { it < midi }
        if (leftWhiteIdx < 0) continue
        val centerX = (leftWhiteIdx + 1) * whiteW
        val bx = centerX - blackW / 2f
        val isPressed = pressedNotes.contains(midi)

        // Drop shadow
        drawRoundRect(
          color = Color(0x66000000),
          topLeft = Offset(bx + 1.5f, 2f),
          size = Size(blackW, blackH + 2f),
          cornerRadius = CornerRadius(2.5f, 2.5f),
        )
        // Black key body
        drawRoundRect(
          brush = Brush.verticalGradient(
            if (isPressed) {
              listOf(PsrColors.BlackKeyPressed, Color(0xFF1F2329))
            } else {
              listOf(PsrColors.BlackKeyTop, PsrColors.BlackKeyBody)
            }
          ),
          topLeft = Offset(bx, 2f),
          size = Size(blackW, blackH),
          cornerRadius = CornerRadius(2.5f, 2.5f),
        )
        // Top bevel highlight
        drawRect(
          color = if (isPressed) PsrColors.LedBlue.copy(alpha = 0.45f) else Color(0x28FFFFFF),
          topLeft = Offset(bx + 2f, 4f),
          size = Size(blackW - 4f, blackH - 12f),
        )
      }
    }
  }
}

private fun isBlackKey(midi: Int): Boolean {
  return when (((midi % 12) + 12) % 12) {
    1, 3, 6, 8, 10 -> true
    else -> false
  }
}

private fun findHitMidiNote(
  x: Float,
  y: Float,
  whiteNotes: List<Int>,
  blackNotes: List<Int>,
  whiteW: Float,
  blackW: Float,
  blackH: Float,
): Int? {
  if (y <= blackH) {
    for (midi in blackNotes) {
      val leftWhiteIdx = whiteNotes.indexOfLast { it < midi }
      if (leftWhiteIdx < 0) continue
      val centerX = (leftWhiteIdx + 1) * whiteW
      val bx = centerX - blackW / 2f
      if (x in bx..(bx + blackW)) {
        return midi
      }
    }
  }
  val whiteIdx = (x / whiteW).toInt().coerceIn(0, (whiteNotes.size - 1).coerceAtLeast(0))
  return whiteNotes.getOrNull(whiteIdx)
}
