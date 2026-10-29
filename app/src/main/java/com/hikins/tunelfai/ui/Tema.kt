package com.hikins.tunelfai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Paleta do app (tema escuro). */
object Cores {
    val Fundo = Color(0xFF0E1116)
    val Superficie = Color(0xFF161B22)
    val SuperficieAlta = Color(0xFF1F2630)
    val Borda = Color(0xFF2A323D)
    val Texto = Color(0xFFE6EDF3)
    val TextoSuave = Color(0xFF8B96A5)
    val Azul = Color(0xFF4C8DFF)
    val AzulEscuro = Color(0xFF1A2F55)
    val Verde = Color(0xFF2FBF71)
    val Vermelho = Color(0xFFFF5C5C)
    val Ambar = Color(0xFFFFB547)
    val Cinza = Color(0xFF8B96A5)
    val WhatsApp = Color(0xFF1FAF5A)
}

/** Cor da bolinha que representa a cor do carro. */
fun corDoCarro(cor: String?): Color = when (cor) {
    "Cinza" -> Color(0xFF9AA0A6)
    "Branco" -> Color(0xFFF5F5F5)
    "Preto" -> Color(0xFF1B1B1B)
    "Azul" -> Color(0xFF2F6BFF)
    else -> Color.Transparent
}

/** Diferença (Aprovado - Meta): vermelho se negativa (faltam aprovados), verde se bateu/passou a meta. */
fun corDiferenca(dif: Int?): Color = when {
    dif == null -> Cores.TextoSuave
    dif < 0 -> Cores.Vermelho
    else -> Cores.Verde
}

@Composable
fun TemaTunel(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Cores.Azul,
            onPrimary = Color.White,
            primaryContainer = Cores.AzulEscuro,
            onPrimaryContainer = Color(0xFFCFE0FF),
            secondary = Cores.Verde,
            onSecondary = Color.White,
            secondaryContainer = Color(0xFF173A2A),
            onSecondaryContainer = Color(0xFFBFF0D5),
            background = Cores.Fundo,
            onBackground = Cores.Texto,
            surface = Cores.Fundo,
            onSurface = Cores.Texto,
            surfaceVariant = Cores.SuperficieAlta,
            onSurfaceVariant = Cores.TextoSuave,
            surfaceContainerLowest = Cores.Fundo,
            surfaceContainerLow = Cores.Superficie,
            surfaceContainer = Cores.Superficie,
            surfaceContainerHigh = Cores.SuperficieAlta,
            surfaceContainerHighest = Cores.SuperficieAlta,
            outline = Cores.Borda,
            outlineVariant = Cores.Borda,
            error = Cores.Vermelho
        ),
        shapes = Shapes(
            extraSmall = RoundedCornerShape(8.dp),
            small = RoundedCornerShape(10.dp),
            medium = RoundedCornerShape(14.dp),
            large = RoundedCornerShape(20.dp),
            extraLarge = RoundedCornerShape(28.dp)
        ),
        content = content
    )
}

/** Barra de progresso arredondada e animada (0..1). */
@Composable
fun BarraProgresso(fracao: Float, cor: Color, modifier: Modifier = Modifier, altura: Dp = 8.dp) {
    val animada by animateFloatAsState(fracao.coerceIn(0f, 1f), label = "progresso")
    Box(
        modifier
            .fillMaxWidth()
            .height(altura)
            .clip(RoundedCornerShape(50))
            .background(Cores.Borda)
    ) {
        Box(
            Modifier
                .fillMaxWidth(animada)
                .height(altura)
                .clip(RoundedCornerShape(50))
                .background(cor)
        )
    }
}
