package com.hikins.tunelfai.model

/** Status possíveis de um veículo no túnel. */
enum class Status(val texto: String) {
    LIBERADO("Liberado"),
    NEGADO("Negado")
}

/** Motivo padrão da negação (só se aplica quando NEGADO e sem motivo livre). */
enum class Motivo(val texto: String) {
    DEMERITO_ABERTO("Demérito em aberto"),
    REPARO_INADEQUADO("Reparo Inadequado")
}

/**
 * Um veículo registrado no Túnel de liberação.
 * @param motivo motivo padrão (null quando LIBERADO ou quando há motivo livre)
 * @param motivoTexto motivo escrito à mão pelo usuário (null quando não usado)
 * @param fotos URIs de até 3 fotos anexadas ao veículo
 * @param ordem sequência global de adição, usada para ordenar dentro do intervalo.
 */
data class Registro(
    val vin: String,
    val status: Status,
    val motivo: Motivo?,          // null quando LIBERADO ou motivo livre
    val motivoTexto: String? = null,
    val intervalo: String,        // ex.: "13:00"
    val ordem: Long,
    val fotos: List<String> = emptyList()
) {
    /** Texto do motivo a exibir/enviar: prioriza o motivo livre. */
    fun motivoExibido(): String? {
        val livre = motivoTexto?.trim()
        if (!livre.isNullOrBlank()) return livre
        return motivo?.texto
    }
}
