package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.random.Random

private data class ConfettiParticle(
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val size: Float,
    val color: Color,
    val shape: ParticleShape,
    val rotationSpeed: Float,
    var rotation: Float = 0f
)

private enum class ParticleShape {
    RECTANGLE, CIRCLE, STAR
}

@Composable
fun ConfettiEffect(
    modifier: Modifier = Modifier,
    particleCount: Int = 90,
    durationMs: Int = 3200
) {
    val progress = remember { Animatable(0f) }

    val particles = remember {
        val colors = listOf(
            Color(0xFFFFD700), // Gold
            Color(0xFF22C55E), // Emerald Green
            Color(0xFF3B82F6), // Bright Blue
            Color(0xFFEF4444), // Coral Red
            Color(0xFFA855F7), // Purple
            Color(0xFFF97316), // Orange
            Color(0xFF06B6D4)  // Cyan
        )
        val rnd = Random(System.currentTimeMillis())
        List(particleCount) {
            val startX = 0.5f + (rnd.nextFloat() - 0.5f) * 0.35f
            val startY = 0.35f + (rnd.nextFloat() - 0.5f) * 0.2f
            val angle = rnd.nextFloat() * 2f * Math.PI.toFloat()
            val speed = rnd.nextFloat() * 700f + 350f
            ConfettiParticle(
                x = startX,
                y = startY,
                vx = kotlin.math.cos(angle) * speed,
                vy = kotlin.math.sin(angle) * speed - 200f,
                size = rnd.nextFloat() * 14f + 8f,
                color = colors[rnd.nextInt(colors.size)],
                shape = ParticleShape.entries[rnd.nextInt(ParticleShape.entries.size)],
                rotationSpeed = (rnd.nextFloat() - 0.5f) * 600f
            )
        }
    }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = durationMs, easing = LinearEasing)
        )
    }

    if (progress.value < 1f) {
        val t = progress.value
        val alpha = (1f - (t * 1.15f)).coerceIn(0f, 1f)
        val gravity = 900f

        Canvas(modifier = modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height

            particles.forEach { p ->
                val currentX = (p.x * canvasW) + (p.vx * t) + (kotlin.math.sin(t * 8f + p.size) * 35f)
                val currentY = (p.y * canvasH) + (p.vy * t) + (0.5f * gravity * t * t)
                val currentRotation = p.rotation + p.rotationSpeed * t

                if (currentY in -50f..(canvasH + 50f) && currentX in -50f..(canvasW + 50f)) {
                    val pColor = p.color.copy(alpha = alpha)
                    rotate(degrees = currentRotation, pivot = Offset(currentX, currentY)) {
                        when (p.shape) {
                            ParticleShape.RECTANGLE -> {
                                drawRect(
                                    color = pColor,
                                    topLeft = Offset(currentX - p.size / 2, currentY - p.size / 4),
                                    size = Size(p.size, p.size / 2)
                                )
                            }
                            ParticleShape.CIRCLE -> {
                                drawCircle(
                                    color = pColor,
                                    radius = p.size / 2,
                                    center = Offset(currentX, currentY)
                                )
                            }
                            ParticleShape.STAR -> {
                                // Draw bright star/diamond
                                drawRect(
                                    color = pColor,
                                    topLeft = Offset(currentX - p.size / 2.5f, currentY - p.size / 2.5f),
                                    size = Size(p.size * 0.8f, p.size * 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
