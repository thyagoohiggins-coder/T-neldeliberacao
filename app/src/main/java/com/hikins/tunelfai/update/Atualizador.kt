package com.hikins.tunelfai.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Resultado da checagem de atualização. */
data class InfoAtualizacao(
    val temNova: Boolean,
    val versaoAtual: String,
    val versaoNova: String,
    val urlApk: String?
)

/**
 * Verifica as releases do GitHub e instala a nova versão do APK.
 * Repositório fixo do app.
 */
object Atualizador {

    private const val API_LATEST =
        "https://api.github.com/repos/thyagoohiggins-coder/t-neldeliberacao/releases/latest"

    fun versaoAtual(context: Context): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0"
    } catch (e: Exception) {
        "0"
    }

    /** Consulta a última release. Retorna null em caso de erro de rede. */
    suspend fun verificar(context: Context): InfoAtualizacao? = withContext(Dispatchers.IO) {
        try {
            val conn = (URL(API_LATEST).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "TunelFAI-App")
                connectTimeout = 15000
                readTimeout = 15000
            }
            if (conn.responseCode != 200) return@withContext null
            val corpo = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(corpo)

            val tag = json.optString("tag_name").removePrefix("v").trim()
            val assets = json.optJSONArray("assets")
            var urlApk: String? = null
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val a = assets.getJSONObject(i)
                    if (a.optString("name").endsWith(".apk", ignoreCase = true)) {
                        urlApk = a.optString("browser_download_url")
                        break
                    }
                }
            }
            val atual = versaoAtual(context)
            InfoAtualizacao(
                temNova = comparar(tag, atual) > 0 && urlApk != null,
                versaoAtual = atual,
                versaoNova = tag,
                urlApk = urlApk
            )
        } catch (e: Exception) {
            null
        }
    }

    /** Compara versões "0.2" vs "0.1". Retorna >0 se a for maior que b. */
    private fun comparar(a: String, b: String): Int {
        val pa = a.split(".").mapNotNull { it.trim().toIntOrNull() }
        val pb = b.split(".").mapNotNull { it.trim().toIntOrNull() }
        val n = maxOf(pa.size, pb.size)
        for (i in 0 until n) {
            val va = pa.getOrElse(i) { 0 }
            val vb = pb.getOrElse(i) { 0 }
            if (va != vb) return va - vb
        }
        return 0
    }

    /** Baixa o APK e dispara a instalação. Retorna false se falhar o download. */
    suspend fun baixarEInstalar(context: Context, urlApk: String): Boolean {
        val arquivo = withContext(Dispatchers.IO) {
            try {
                val destino = File(context.externalCacheDir, "tunelfai-update.apk")
                val conn = (URL(urlApk).openConnection() as HttpURLConnection).apply {
                    setRequestProperty("User-Agent", "TunelFAI-App")
                    connectTimeout = 20000
                    readTimeout = 60000
                    instanceFollowRedirects = true
                }
                conn.inputStream.use { input ->
                    destino.outputStream().use { output -> input.copyTo(output) }
                }
                destino
            } catch (e: Exception) {
                null
            }
        } ?: return false

        // A partir do Android 8 é preciso permissão para instalar de fontes desconhecidas.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            val i = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
            return true
        }

        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", arquivo
        )
        val install = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(install)
        return true
    }
}
