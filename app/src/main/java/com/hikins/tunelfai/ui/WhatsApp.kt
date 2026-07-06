package com.hikins.tunelfai.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

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
