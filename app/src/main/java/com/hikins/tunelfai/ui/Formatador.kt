package com.hikins.tunelfai.ui

import com.hikins.tunelfai.model.Intervalos
import com.hikins.tunelfai.model.Registro
import com.hikins.tunelfai.model.Status

/** Estatística de um intervalo (ou do total). Produção/Aprovado null = ainda sem veículos. */
data class Estatistica(val meta: Int, val prod: Int?, val aprov: Int?) {
    val dif: Int? get() = aprov?.let { it - meta }   // Aprovado - Meta: negativo = faltam aprovados
}

/** Geração das mensagens de WhatsApp no formato do "FECHAMENTO TÚNEL". */
object Formatador {

    private const val CABECALHO = "_FECHAMENTO TÚNEL_"

    /** Fim do turno: o último intervalo vai das 15:00 às 15:48. */
    private const val FIM_ULTIMO = "15:48"

    /** "06:00" -> "06:00 ÀS 07:00"; "15:00" -> "15:00 ÀS 15:48". */
    fun faixa(intervalo: String): String {
        val hora = intervalo.substringBefore(':').toIntOrNull() ?: return intervalo
        if (intervalo == Intervalos.lista.last()) return "$intervalo ÀS $FIM_ULTIMO"
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

    /** TOTAL: Meta = soma das metas dos intervalos, soma dos intervalos e Diferença = Aprovado - Meta. */
    fun estatisticaTotal(registros: List<Registro>): Estatistica {
        val porIntervalo = Intervalos.lista.map { estatistica(it, registros) }
        val algum = porIntervalo.any { it.prod != null }
        return Estatistica(
            Intervalos.metaTotal,
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

    /** Mensagem de veículo negado (Reparo), enviada junto com as fotos do demérito. */
    fun negado(r: Registro): String =
        "*Veículo negado túnel*\n\n" +
            "*Vin:* *${r.vin}*\n" +
            "*Modelo:* *${r.modelo.orEmpty()}*\n" +
            "*NC:* *${r.reparo.orEmpty()}*\n" +
            "*Responsável:* *${r.responsavel.orEmpty()}*"

    /**
     * Veículo Bloqueado/Separado: Modelo e Responsável sempre; Motivo e Observação só se preenchidos.
     */
    private fun bloqueioOuSeparacao(titulo: String, r: Registro): String {
        // Só negrito (sem itálico) e dois espaços depois dos dois pontos.
        val linhas = mutableListOf(
            "*$titulo*",
            "",
            "*Vin:*  ${r.vin}",
            "*Modelo:*  ${r.modelo.orEmpty()}",
            "*Responsável:*  ${r.responsavel.orEmpty()}"
        )
        r.motivo?.takeIf { it.isNotBlank() }?.let { linhas += "*Motivo:*  $it" }
        r.observacao?.takeIf { it.isNotBlank() }?.let { linhas += "*Observação:*  $it" }
        return linhas.joinToString("\n")
    }

    fun bloqueado(r: Registro): String = bloqueioOuSeparacao("Veiculo Bloqueado", r)

    fun separado(r: Registro): String = bloqueioOuSeparacao("Veiculo Separado", r)

    /** Mensagem individual do veículo conforme o status (null se o status não tem envio). */
    fun mensagemDe(r: Registro): String? = when (r.status) {
        Status.REPARO -> negado(r)
        Status.BLOQUEADO -> bloqueado(r)
        Status.SEPARADO -> separado(r)
        else -> null
    }

    /** Título da mensagem de um intervalo específico (negrito + itálico). */
    private const val CABECALHO_INTERVALO = "*_H/H LIBERAÇÃO TUNEL_*"

    /** Mensagem de um único intervalo, seguida do TOTAL do dia. */
    fun intervalo(intervalo: String, registros: List<Registro>): String =
        CABECALHO_INTERVALO + "\n\n" +
            bloco(faixa(intervalo), estatistica(intervalo, registros)) + "\n\n" +
            bloco("TOTAL", estatisticaTotal(registros))

    /** Fechamento completo: todos os intervalos (06:00 a 15:00) e o TOTAL. */
    fun total(registros: List<Registro>): String {
        val blocos = Intervalos.lista.map { bloco(faixa(it), estatistica(it, registros)) }
        val total = bloco("TOTAL", estatisticaTotal(registros))
        return CABECALHO + "\n\n" + (blocos + total).joinToString("\n\n")
    }
}
