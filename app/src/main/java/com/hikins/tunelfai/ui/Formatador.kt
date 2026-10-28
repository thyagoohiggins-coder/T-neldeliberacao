package com.hikins.tunelfai.ui

import com.hikins.tunelfai.model.Intervalos
import com.hikins.tunelfai.model.Registro
import com.hikins.tunelfai.model.Status

/** Estatística de um intervalo (ou do total). Produção/Aprovado null = ainda sem veículos. */
data class Estatistica(val meta: Int, val prod: Int?, val aprov: Int?) {
    val dif: Int? get() = prod?.let { it - meta }   // Produção - Meta: negativo se não bateu a meta
}

/** Geração das mensagens de WhatsApp no formato do "FECHAMENTO TÚNEL". */
object Formatador {

    private const val CABECALHO = "_FECHAMENTO TÚNEL_"

    /** "06:00" -> "06:00 ÀS 07:00". */
    fun faixa(intervalo: String): String {
        val hora = intervalo.substringBefore(':').toIntOrNull() ?: return intervalo
        return "%s ÀS %02d:00".format(intervalo, hora + 1)
    }

    /**
     * Produção = veículos com status (Aprovado ou Reparo); Aprovado = só os aprovados.
     * Veículo sem status (Fazendo Inspeção 360) não conta. Intervalo sem veículos sai em branco.
     */
    fun estatistica(intervalo: String, registros: List<Registro>): Estatistica {
        val doIntervalo = registros.filter { it.intervalo == intervalo }
        val meta = Intervalos.metaDe(intervalo)
        if (doIntervalo.isEmpty()) return Estatistica(meta, null, null)
        return Estatistica(
            meta,
            doIntervalo.count { it.contabiliza },
            doIntervalo.count { it.status == Status.APROVADO }
        )
    }

    /** TOTAL: Meta fixa (80), soma dos intervalos e Diferença = Produção - Meta. */
    fun estatisticaTotal(registros: List<Registro>): Estatistica {
        val porIntervalo = Intervalos.lista.map { estatistica(it, registros) }
        val algum = porIntervalo.any { it.prod != null }
        return Estatistica(
            Intervalos.META_TOTAL,
            if (algum) porIntervalo.sumOf { it.prod ?: 0 } else null,
            if (algum) porIntervalo.sumOf { it.aprov ?: 0 } else null
        )
    }

    private fun bloco(titulo: String, e: Estatistica): String =
        "*$titulo:*\n" +
            "*META:* ${e.meta}\n" +
            "*PRODUÇÃO:* ${e.prod ?: ""}\n" +
            "*APROVADO:* ${e.aprov ?: ""}\n" +
            "*DIFERENÇA:* ${e.dif ?: ""}"

    /** Mensagem de um único intervalo, com o cabeçalho do fechamento. */
    fun intervalo(intervalo: String, registros: List<Registro>): String =
        CABECALHO + "\n\n" + bloco(faixa(intervalo), estatistica(intervalo, registros))

    /** Fechamento completo: todos os intervalos (06:00 a 15:00) e o TOTAL. */
    fun total(registros: List<Registro>): String {
        val blocos = Intervalos.lista.map { bloco(faixa(it), estatistica(it, registros)) }
        val total = bloco("TOTAL", estatisticaTotal(registros))
        return CABECALHO + "\n\n" + (blocos + total).joinToString("\n\n")
    }
}
