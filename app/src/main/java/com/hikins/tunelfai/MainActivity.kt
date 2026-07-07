package com.hikins.tunelfai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hikins.tunelfai.ui.TunelApp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val pedirNotificacao =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            pedirNotificacao.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF64B5F6))) {
                AppRoot()
            }
        }
    }
}

@Composable
private fun AppRoot() {
    var mostrarSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(2000)          // pop-up de abertura dura 2 segundos
        mostrarSplash = false
    }
    Surface(modifier = Modifier.fillMaxSize()) {
        if (mostrarSplash) Splash() else TunelApp()
    }
}

@Composable
private fun Splash() {
    val infiniteTransition = rememberInfiniteTransition(label = "splash")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    val scale1 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale1"
    )
    val scale2 by infiniteTransition.animateFloat(
        initialValue = 1.5f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale2"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D47A1)),
        contentAlignment = Alignment.Center
    ) {
        // Wallpaper animado tecnológico com pontos em movimento
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val radius = size.width / 3
            val numPoints = 6

            // Pontos orbitais
            for (i in 0 until numPoints) {
                val angle = Math.toRadians(i * 60.0 + rotation)
                val x = centerX + radius * kotlin.math.cos(angle).toFloat()
                val y = centerY + radius * kotlin.math.sin(angle).toFloat()
                drawCircle(
                    color = Color(0xFF64B5F6).copy(alpha = 0.6f),
                    radius = 8f,
                    center = androidx.compose.ui.geometry.Offset(x, y)
                )
            }

            // Centro
            drawCircle(
                color = Color(0xFF64B5F6),
                radius = 6f * scale1,
                center = androidx.compose.ui.geometry.Offset(centerX, centerY)
            )

            // Anel externo
            drawCircle(
                color = Color(0xFF64B5F6).copy(alpha = 0.3f),
                radius = radius * scale2,
                center = androidx.compose.ui.geometry.Offset(centerX, centerY),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Hora a Hora\nTúnel de liberação FAI",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Criado por Hikins",
                color = Color(0xFFBBDEFB),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 24.dp)
            )
        }
    }
}
