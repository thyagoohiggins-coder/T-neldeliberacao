package com.hikins.tunelfai.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import java.io.File

/** Fotos dos deméritos: ficam no armazenamento interno do app (files/fotos). */
object Fotos {

    private fun pasta(context: Context): File =
        File(context.filesDir, "fotos").apply { mkdirs() }

    fun novoArquivo(context: Context): File =
        File(pasta(context), "NC_${System.currentTimeMillis()}.jpg")

    fun uriDe(context: Context, arquivo: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", arquivo)

    /** Copia uma imagem da galeria para a pasta do app. Retorna o caminho ou null. */
    fun copiarDaGaleria(context: Context, uri: Uri): String? = try {
        val destino = novoArquivo(context)
        context.contentResolver.openInputStream(uri)?.use { entrada ->
            destino.outputStream().use { entrada.copyTo(it) }
        }
        if (destino.length() > 0) destino.absolutePath else { destino.delete(); null }
    } catch (e: Exception) {
        null
    }

    fun apagar(caminhos: List<String>) {
        caminhos.forEach { runCatching { File(it).delete() } }
    }

    /** Miniatura leve (~[lado] px) para mostrar na tela. */
    fun miniatura(caminho: String, lado: Int = 240): ImageBitmap? = try {
        val opcoes = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(caminho, opcoes)
        var amostra = 1
        while (opcoes.outWidth / (amostra * 2) >= lado && opcoes.outHeight / (amostra * 2) >= lado) amostra *= 2
        BitmapFactory.decodeFile(caminho, BitmapFactory.Options().apply { inSampleSize = amostra })
            ?.asImageBitmap()
    } catch (e: Exception) {
        null
    }
}
