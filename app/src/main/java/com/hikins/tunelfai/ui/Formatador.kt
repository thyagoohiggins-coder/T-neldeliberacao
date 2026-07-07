package com.hikins.tunelfai.ui

import com.hikins.tunelfai.model.Configuracao
import com.hikins.tunelfai.model.Intervalos
import com.hikins.tunelfai.model.Registro
import com.hikins.tunelfai.model.Status

/**
 * Geração das mensagens de WhatsApp.
 *
 * Quando [Configuracao.destacarNegrito] está ligado, os prefixos (VIN, Status,
 * Motivo, Intervalo e os prefixos de estatística) saem envolvidos por "*", que
 * o WhatsApp renderiza em negrito automaticamente.
 *
 * Produção e Aprovados são calculados automaticamente a partir dos VINs
 * registrados no intervalo, e a Diferença sai de Prod - Meta.
 */
object Formatador {

    /** Envolve o prefixo com "*" para virar negrito no WhatsApp, se configurado. */
    private fun p(config: Configuracao, texto: String): String =
        if (config.destacarNegrito) "*$texto*" else texto

    private fun linhaVin(
        sb: StringBuilder,
        indice: Int,
        r: Registro,
        intervalo: String,
        config: Configuracao
    ) {
        sb.append(indice).append(". ")
        sb.append(p(config, "VIN")).append(": ").append(r.vin)
        sb.append(" ").append(p(config, "Status")).append(": ").append(r.status.texto)
        if (r.status == Status.NEGADO && config.motivoAtivado) {
            val m = r.motivoExibido()
            if (!m.isNullOrBlank()) {
                sb.append(" ").append(p(config, "Motivo")).append(": ").append(m)
            }
        }
        sb.append(" ").append(p(config, "Intervalo")).append(": ").append(intervalo)
        sb.append('\n')
    }

    /**
     * Mensagem de um intervalo: lista numerada dos veículos na ordem de adição.
     * Se [somenteNegado], só entram os VINs negados.
     * Se [incluirMeta], acrescenta a linha de estatística com contagem automática.
     */
    fun intervalo(
        intervalo: String,
        registros: List<Registro>,
        incluirMeta: Boolean,
        config: Configuracao,
        somenteNegado: Boolean = false
    ): String {
        val lista = if (somenteNegado) registros.filter { it.status == Status.NEGADO } else registros
        val sb = StringBuilder()
        lista.forEachIndexed { i, r -> linhaVin(sb, i + 1, r, intervalo, config) }

        if (incluirMeta && lista.isNotEmpty()) {
            val meta = Intervalos.metaDe(intervalo)
            val prod = registros.size
            val aprov = registros.count { it.status == Status.LIBERADO }
            sb.append("-> ")
                .append(p(config, "Intervalo")).append(": ").append(intervalo)
                .append(" ").append(p(config, "Meta")).append(": ").append(meta)
                .append(" ").append(p(config, "Prod")).append(": ").append(prod)
                .append(" ").append(p(config, "Aprov")).append(": ").append(aprov)
                .append(" ").append(p(config, "Dif")).append(": ").append(prod - meta)   // Produção - Meta
        }
        return sb.toString().trimEnd('\n')
    }

    /**
     * Todos os VINs de todos os intervalos numa única mensagem, agrupados por
     * intervalo em ordem decrescente (15:00 no topo), cada grupo com sua linha
     * de meta. Grupos separados por linha em branco.
     */
    fun todosAgrupados(todos: List<Registro>, config: Configuracao): String {
        val intervalos = todos.map { it.intervalo }.distinct().sortedDescending()
        return intervalos.joinToString("\n\n") { intv ->
            val doIntervalo = todos.filter { it.intervalo == intv }.sortedBy { it.ordem }
            intervalo(intv, doIntervalo, incluirMeta = true, config = config)
        }
    }

    /**
     * Somente os VINs negados de todos os intervalos, em ordem de adição.
     * Retorna string vazia se não houver nenhum negado.
     */
    fun negadosDeTodos(todos: List<Registro>, config: Configuracao): String {
        val negados = todos.filter { it.status == Status.NEGADO }.sortedBy { it.ordem }
        if (negados.isEmpty()) return ""
        val sb = StringBuilder()
        negados.forEachIndexed { i, r -> linhaVin(sb, i + 1, r, r.intervalo, config) }
        return sb.toString().trimEnd('\n')
    }

    /**
     * Resumo total de todos os intervalos.
     * TTL é fixo (80). Prod = total de VINs registrados; Aprov = VINs Liberados.
     * Dif = saldo acumulado: soma de (VINs do intervalo - Meta) de cada intervalo.
     */
    fun total(todos: List<Registro>, config: Configuracao): String {
        val prod = todos.size
        val aprov = todos.count { it.status == Status.LIBERADO }
        val difAcumulada = todos.map { it.intervalo }.distinct().sumOf { intv ->
            todos.count { it.intervalo == intv } - Intervalos.metaDe(intv)
        }
        return p(config, "TTL") + ": ${Intervalos.META_TOTAL} " +
            p(config, "Prod") + ": $prod " +
            p(config, "Aprov") + ": $aprov " +
            p(config, "Dif") + ": $difAcumulada"
    }
}
