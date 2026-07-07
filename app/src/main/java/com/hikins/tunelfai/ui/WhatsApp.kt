package com.hikins.tunelfai.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Abre o WhatsApp diretamente com a mensagem pronta para escolher o contato/grupo.
 * Tenta WhatsApp comum, depois WhatsApp Business e, por fim, o link wa.me.
 *
 * Quando [fotos] não está vazio, envia as imagens junto com o texto usando
 * ACTION_SEND (1 foto) ou ACTION_SEND_MULTIPLE (várias).
 */
fun enviarWhatsApp(context: Context, texto: String, fotos: List<String> = emptyList()) {
    if (texto.isBlank() && fotos.isEmpty()) {
        Toast.makeText(context, "Nada para enviar neste intervalo.", Toast.LENGTH_SHORT).show()
        return
    }

    val uris = fotos.mapNotNull { runCatching { Uri.parse(it) }.getOrNull() }
    val pacotes = listOf("com.whatsapp", "com.whatsapp.w4b")

    for (pacote in pacotes) {
        val i = criarIntent(texto, uris).apply { setPackage(pacote) }
        if (i.resolveActivity(context.packageManager) != null) {
            context.startActivity(i)
            return
        }
    }

    // Fallback sem fotos: abre o WhatsApp pela web se o app não estiver instalado.
    try {
        if (uris.isNotEmpty()) {
            val i = criarIntent(texto, uris)
            context.startActivity(Intent.createChooser(i, "Enviar"))
            return
        }
        val url = "https://wa.me/?text=" + Uri.encode(texto)
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: Exception) {
        Toast.makeText(context, "WhatsApp não encontrado.", Toast.LENGTH_LONG).show()
    }
}

private fun criarIntent(texto: String, uris: List<Uri>): Intent {
    if (uris.isEmpty()) {
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, texto)
        }
    }
    val base = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).apply { putExtra(Intent.EXTRA_STREAM, uris.first()) }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        }
    }
    return base.apply {
        type = "image/*"
        putExtra(Intent.EXTRA_TEXT, texto)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData = ClipData.newUri(null, "fotos", uris.first()).also { cd ->
            uris.drop(1).forEach { cd.addItem(ClipData.Item(it)) }
        }
    }
}
