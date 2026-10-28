package com.hikins.tunelfai.ui

import com.hikins.tunelfai.data.MetaInput
import com.hikins.tunelfai.model.Intervalos

/** Geração das mensagens de WhatsApp no formato do "FECHAMENTO TÚNEL". */
object Formatador {

    private const val CABECALHO = "_FECHAMENTO TÚNEL_"

    private fun paraInt(s: String): Int? = s.trim().toIntOrNull()

    /** "06:00" -> "06:00 ÀS 07:00". */
    private fun faixa(intervalo: String): String {
        val hora = intervalo.substringBefore(':').toIntOrNull() ?: return intervalo
        return "%s ÀS %02d:00".format(intervalo, hora + 1)
    }

    private fun bloco(titulo: String, meta: Int, prod: Int?, aprov: Int?): String {
        val dif = prod?.let { it - meta }   // Produção - Meta: negativo se não bateu a meta
        return "*$titulo:*\n" +
            "*META:* $meta\n" +
            "*PRODUÇÃO:* ${prod ?: ""}\n" +
            "*APROVADO:* ${aprov ?: ""}\n" +
            "*DIFERENÇA:* ${dif ?: ""}"
    }

    private fun blocoIntervalo(intervalo: String, meta: MetaInput?): String =
        bloco(faixa(intervalo), Intervalos.metaDe(intervalo), paraInt(meta?.prod ?: ""), paraInt(meta?.aprov ?: ""))

    /** Mensagem de um único intervalo, com o cabeçalho do fechamento. */
    fun intervalo(intervalo: String, meta: MetaInput): String =
        CABECALHO + "\n\n" + blocoIntervalo(intervalo, meta)

    /**
     * Fechamento completo: todos os intervalos (06:00 a 15:00) e o TOTAL.
     * Produção/Aprovado são os valores digitados; campos vazios saem em branco.
     * TOTAL: Meta fixa (80), somas digitadas e Diferença = Produção - Meta.
     */
    fun total(metas: Map<String, MetaInput>): String {
        val blocos = Intervalos.lista.map { blocoIntervalo(it, metas[it]) }
        val prods = Intervalos.lista.mapNotNull { paraInt(metas[it]?.prod ?: "") }
        val aprovs = Intervalos.lista.mapNotNull { paraInt(metas[it]?.aprov ?: "") }
        val total = bloco(
            "TOTAL",
            Intervalos.META_TOTAL,
            if (prods.isEmpty()) null else prods.sum(),
            if (aprovs.isEmpty()) null else aprovs.sum()
        )
        return CABECALHO + "\n\n" + (blocos + total).joinToString("\n\n")
    }
}
