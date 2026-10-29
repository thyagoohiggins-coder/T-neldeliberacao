package com.hikins.tunelfai.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Size
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as TamanhoCompose
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Leitor de código de barras em tela cheia: só aceita o código cujo centro estiver
 * dentro do retângulo destacado (ignora o resto que aparece na câmera).
 */
@Composable
fun ScannerCodigo(onLido: (String) -> Unit, onFechar: () -> Unit) {
    val context = LocalContext.current
    var temPermissao by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val pedir = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        temPermissao = ok
        if (!ok) {
            Toast.makeText(context, "Sem permissão de câmera não dá para ler o código.", Toast.LENGTH_LONG).show()
            onFechar()
        }
    }
    LaunchedEffect(Unit) { if (!temPermissao) pedir.launch(Manifest.permission.CAMERA) }

    Dialog(onDismissRequest = onFechar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (temPermissao) CameraComRecorte(onLido)
            IconButton(
                onClick = onFechar,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .clip(CircleShape)
                    .background(Color(0x99000000))
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Fechar leitor", tint = Color.White)
            }
        }
    }
}

/** Retângulo de leitura em pixels da tela: largura, altura da tela e left, top, right, bottom. */
private class Recorte(val vw: Float, val vh: Float, val l: Float, val t: Float, val r: Float, val b: Float)

@Composable
private fun CameraComRecorte(onLido: (String) -> Unit) {
    val context = LocalContext.current
    val dono = LocalLifecycleOwner.current
    val densidade = LocalDensity.current
    val recorte = remember { AtomicReference<Recorte?>(null) }
    val jaLido = remember { AtomicBoolean(false) }
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val vw = with(densidade) { maxWidth.toPx() }
        val vh = with(densidade) { maxHeight.toPx() }
        // Retângulo do tamanho de um código de barras de chassi (largo e baixo).
        val largura = vw * 0.72f
        val altura = with(densidade) { 84.dp.toPx() }
        val l = (vw - largura) / 2f
        val t = (vh - altura) / 2f
        recorte.set(Recorte(vw, vh, l, t, l + largura, t + altura))

        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        // Escurece tudo e deixa só o retângulo de leitura aberto.
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        ) {
            drawRect(Color(0xB3000000))
            drawRoundRect(
                Color.Transparent, topLeft = Offset(l, t), size = TamanhoCompose(largura, altura),
                cornerRadius = CornerRadius(24f, 24f), blendMode = BlendMode.Clear
            )
            drawRoundRect(
                Cores.Azul, topLeft = Offset(l, t), size = TamanhoCompose(largura, altura),
                cornerRadius = CornerRadius(24f, 24f), style = Stroke(width = 6f)
            )
            drawLine(Cores.Vermelho.copy(alpha = 0.8f), Offset(l + 24f, t + altura / 2f),
                Offset(l + largura - 24f, t + altura / 2f), strokeWidth = 3f)
        }

        Column(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = with(densidade) { t.toDp() } - 72.dp)
        ) {
            Text(
                "Encaixe o código de barras do chassi no retângulo",
                color = Color.White, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )
        }
    }

    DisposableEffect(Unit) {
        val executor = Executors.newSingleThreadExecutor()
        val leitor = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_CODE_128, Barcode.FORMAT_CODE_39, Barcode.FORMAT_CODE_93,
                    Barcode.FORMAT_DATA_MATRIX, Barcode.FORMAT_QR_CODE
                )
                .build()
        )
        val principal = Handler(Looper.getMainLooper())
        val futuro = ProcessCameraProvider.getInstance(context)
        futuro.addListener({
            val provedor = futuro.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val analise = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            ResolutionStrategy(
                                Size(1280, 720),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                            )
                        )
                        .build()
                )
                .build()
            analise.setAnalyzer(executor) { proxy ->
                analisar(proxy, leitor, recorte.get()) { valor ->
                    if (jaLido.compareAndSet(false, true)) principal.post { onLido(valor) }
                }
            }
            runCatching {
                provedor.unbindAll()
                provedor.bindToLifecycle(dono, CameraSelector.DEFAULT_BACK_CAMERA, preview, analise)
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            runCatching { futuro.get().unbindAll() }
            executor.shutdown()
            leitor.close()
        }
    }
}

@OptIn(ExperimentalGetImage::class)
private fun analisar(
    proxy: ImageProxy,
    leitor: com.google.mlkit.vision.barcode.BarcodeScanner,
    recorte: Recorte?,
    onAchou: (String) -> Unit
) {
    val media = proxy.image
    if (media == null || recorte == null) {
        proxy.close()
        return
    }
    val rotacao = proxy.imageInfo.rotationDegrees
    val (iw, ih) = if (rotacao == 90 || rotacao == 270) proxy.height to proxy.width else proxy.width to proxy.height
    // PreviewView em FILL_CENTER: escala para cobrir a tela e corta as sobras; converte o retângulo da tela para a imagem.
    val escala = maxOf(recorte.vw / iw, recorte.vh / ih)
    val dx = (iw - recorte.vw / escala) / 2f
    val dy = (ih - recorte.vh / escala) / 2f
    val l = dx + recorte.l / escala
    val t = dy + recorte.t / escala
    val r = dx + recorte.r / escala
    val b = dy + recorte.b / escala

    leitor.process(InputImage.fromMediaImage(media, rotacao))
        .addOnSuccessListener { codigos ->
            val dentro = codigos.filter { c ->
                val caixa = c.boundingBox
                !c.rawValue.isNullOrBlank() && caixa != null &&
                    caixa.exactCenterX() in l..r && caixa.exactCenterY() in t..b
            }
            // Prefere o que tem cara de chassi (17 caracteres).
            val alvo = dentro.firstOrNull { it.rawValue!!.trim().length == 17 } ?: dentro.firstOrNull()
            alvo?.rawValue?.let(onAchou)
        }
        .addOnCompleteListener { proxy.close() }
}
