package com.agon.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import com.agon.app.ui.theme.EmberOrange
import com.agon.app.ui.theme.EmberOrangeDark
import com.agon.app.ui.theme.EmberRed
import com.agon.app.ui.theme.EmberRedDark
import com.agon.app.ui.theme.GoldTrophy
import com.agon.app.ui.theme.SuccessGreen
import kotlin.math.sin
import kotlin.random.Random

private data class ConfettiParticle(
    val xFrac: Float,
    val yFrac: Float,
    val size: Float,
    val color: Color,
    val speed: Float,
    val sway: Float,
    val phase: Float,
    val rotation: Float,
    val spin: Float
)

@Composable
fun ConfettiOverlay(modifier: Modifier = Modifier, particleCount: Int = 90) {
    val palette = listOf(
        EmberRed, EmberRedDark, EmberOrange, EmberOrangeDark,
        GoldTrophy, SuccessGreen, Color(0xFF4DA6FF), Color(0xFFB388FF)
    )
    val particles = remember {
        val rnd = Random(42)
        List(particleCount) {
            ConfettiParticle(
                xFrac = rnd.nextFloat(),
                yFrac = rnd.nextFloat(),
                size = 10f + rnd.nextFloat() * 16f,
                color = palette[rnd.nextInt(palette.size)],
                speed = 0.25f + rnd.nextFloat() * 0.55f,
                sway = 20f + rnd.nextFloat() * 60f,
                phase = rnd.nextFloat() * 6.28f,
                rotation = rnd.nextFloat() * 360f,
                spin = 90f + rnd.nextFloat() * 270f
            )
        }
    }
    val infinite = rememberInfiniteTransition(label = "confetti")
    val progress by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fall"
    )
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        particles.forEach { p ->
            val fall = (p.yFrac + progress * p.speed) % 1f
            val y = fall * h
            val x = (p.xFrac * w + sin(p.phase + progress * 6.28f) * p.sway).toFloat()
            val rot = p.rotation + progress * p.spin
            rotate(rot, pivot = androidx.compose.ui.geometry.Offset(x, y)) {
                drawRect(
                    color = p.color,
                    topLeft = androidx.compose.ui.geometry.Offset(x - p.size / 2f, y - p.size / 4f),
                    size = androidx.compose.ui.geometry.Size(p.size, p.size / 2f)
                )
            }
            // little circle twin for variety
            drawCircle(
                color = p.color.copy(alpha = 0.85f),
                radius = p.size / 5f,
                center = androidx.compose.ui.geometry.Offset(
                    (x + p.sway * 0.4f).toFloat() % w,
                    (y + 26f) % h
                )
            )
        }
    }
}
