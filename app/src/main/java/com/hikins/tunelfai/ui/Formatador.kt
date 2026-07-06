package com.hikins.tunelfai.ui

import com.hikins.tunelfai.data.MetaInput
import com.hikins.tunelfai.model.Intervalos
import com.hikins.tunelfai.model.Registro
import com.hikins.tunelfai.model.Status

/** Geração das mensagens de WhatsApp (texto puro, sem emojis). */
object Formatador {

    private fun paraInt(s: String): Int = s.trim().toIntOrNull() ?: 0

    /**
     * Mensagem de um intervalo: lista numerada dos veículos na ordem de adição.
     * Liberado sai sem "Motivo". Se [incluirMeta], acrescenta a linha de estatística.
     */
    fun intervalo(
        intervalo: String,
        registros: List<Registro>,
        incluirMeta: Boolean,
        meta: MetaInput
    ): String {
        val sb = StringBuilder()
        registros.forEachIndexed { i, r ->
            sb.append(i + 1).append(". ")
            sb.append("VIN: ").append(r.vin)
            sb.append(" Status: ").append(r.status.texto)
            if (r.status == Status.NEGADO && r.motivo != null) {
                sb.append(" Motivo: ").append(r.motivo.texto)
            }
            sb.append(" Intervalo: ").append(intervalo)
            sb.append('\n')
        }
        if (incluirMeta) {
            val meta_ = Intervalos.metaDe(intervalo)
            val prod = paraInt(meta.prod)
            val aprov = paraInt(meta.aprov)
            sb.append("Intervalo: ").append(intervalo)
                .append(" Meta: ").append(meta_)
                .append(" Prod: ").append(prod)
                .append(" Aprov: ").append(aprov)
                .append(" Dif: ").append(prod - meta_)   // Produção - Meta: negativo se não bateu a meta
        }
        return sb.toString().trimEnd('\n')
    }

    /**
     * Resumo total de todos os intervalos.
     * TTL é fixo (80). Prod = total de VINs registrados; Aprov = VINs Liberados.
     * Dif = saldo acumulado: soma de (Produção digitada - Meta) de cada intervalo
     * presente no app. Pode terminar positivo ou negativo.
     */
    fun total(todos: List<Registro>, metas: Map<String, MetaInput>): String {
        val prod = todos.size
        val aprov = todos.count { it.status == Status.LIBERADO }
        val difAcumulada = todos.map { it.intervalo }.distinct().sumOf { intv ->
            val prodDigitado = paraInt(metas[intv]?.prod ?: "")
            prodDigitado - Intervalos.metaDe(intv)
        }
        return "TTL: ${Intervalos.META_TOTAL} Prod: $prod Aprov: $aprov Dif: $difAcumulada"
    }
}
