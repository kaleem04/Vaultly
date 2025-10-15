package com.dapp.vaultly.ui.screens


import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen() {
    // Animation state for the drawing effect
    val animatedProgress = remember { Animatable(0f) }

    // Animate the path drawing from 0% to 100% when the screen is first composed
    LaunchedEffect(Unit) {
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1500, delayMillis = 200)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background), // Use theme background color
        contentAlignment = Alignment.Center
    ) {
        VaultlyAnimatedLogo(progress = animatedProgress.value)
    }
}

@Composable
fun VaultlyAnimatedLogo(progress: Float) {
    val logoColor = MaterialTheme.colorScheme.primary
    val logoSize = 120.dp

    Box(contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .size(logoSize)
        ) {
            val strokeWidth = 12f
            val path = Path().apply {
                // Defines the "V" shape
                moveTo(0f, 0f) // Top-left
                lineTo(size.width / 2f, size.height) // Bottom-center
                lineTo(size.width, 0f) // Top-right
            }

            // A dashed path effect that reveals the path as 'progress' goes from 0 to 1
            val pathEffect = PathEffect.dashPathEffect(
                intervals = floatArrayOf(path.getBounds().width, path.getBounds().width),
                phase = path.getBounds().width * (1f - progress)
            )

            drawPath(
                path = path,
                color = logoColor,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round // Rounded line ends for a softer look
                ),

            )
        }

        // The "Vaultly" text fades in after the logo is mostly drawn
        if (progress > 0.7f) {
            Text(
                text = "VAULTLY",
                color = logoColor.copy(alpha = (progress - 0.7f) / 0.3f), // Fade-in effect
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                modifier = Modifier.padding(top = logoSize + 48.dp) // Position below the logo
            )
        }
    }
}

@Preview(showBackground = true, name = "Splash Screen Preview")
@Composable
fun SplashScreenPreview() {
    // A custom preview to test the animation states
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(200) // initial delay
        progress.animateTo(1f, animationSpec = tween(1500))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black), // Dark background for preview
        contentAlignment = Alignment.Center
    ) {
        VaultlyAnimatedLogo(progress = progress.value)
    }
}

