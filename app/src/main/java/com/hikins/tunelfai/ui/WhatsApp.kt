package com.hikins.tunelfai.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.io.File

/**
 * Abre o WhatsApp diretamente com a mensagem pronta para escolher o contato/grupo.
 * Tenta WhatsApp comum, depois WhatsApp Business e, por fim, o link wa.me.
 */
fun enviarWhatsApp(context: Context, texto: String) {
    if (texto.isBlank()) {
        Toast.makeText(context, "Nada para enviar neste intervalo.", Toast.LENGTH_SHORT).show()
        return
    }

    val pacotes = listOf("com.whatsapp", "com.whatsapp.w4b")
    for (pacote in pacotes) {
        val i = Intent(Intent.ACTION_SEND).apply {
            setPackage(pacote)
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, texto)
        }
        if (i.resolveActivity(context.packageManager) != null) {
            context.startActivity(i)
            return
        }
    }

    // Fallback: abre o WhatsApp pela web se o app não estiver instalado.
    try {
        val url = "https://wa.me/?text=" + Uri.encode(texto)
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: Exception) {
        Toast.makeText(context, "WhatsApp não encontrado.", Toast.LENGTH_LONG).show()
    }
}

/**
 * Envia texto + fotos para o WhatsApp (o texto vai como legenda).
 * Sem fotos, cai no envio só de texto.
 */
fun enviarWhatsAppComFotos(context: Context, texto: String, caminhos: List<String>) {
    val uris = caminhos.map { File(it) }.filter { it.exists() }.map { Fotos.uriDe(context, it) }
    if (uris.isEmpty()) {
        enviarWhatsApp(context, texto)
        return
    }
    for (pacote in listOf("com.whatsapp", "com.whatsapp.w4b")) {
        val i = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris[0])
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        }.apply {
            setPackage(pacote)
            type = "image/*"
            putExtra(Intent.EXTRA_TEXT, texto)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        if (i.resolveActivity(context.packageManager) != null) {
            context.startActivity(i)
            return
        }
    }
    Toast.makeText(context, "WhatsApp não encontrado.", Toast.LENGTH_LONG).show()
}
