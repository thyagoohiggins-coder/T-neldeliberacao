package com.hikins.tunelfai.model

/** Status possíveis de um veículo no túnel. */
enum class Status(val texto: String) {
    LIBERADO("Liberado"),
    NEGADO("Negado")
}

/** Motivo da negação (só se aplica quando NEGADO). */
enum class Motivo(val texto: String) {
    DEMERITO_ABERTO("Demérito em aberto"),
    REPARO_INADEQUADO("Reparo Inadequado")
}

/**
 * Um veículo registrado no Túnel de liberação.
 * @param ordem sequência global de adição, usada para ordenar dentro do intervalo.
 */
data class Registro(
    val vin: String,
    val status: Status,
    val motivo: Motivo?,      // null quando LIBERADO
    val intervalo: String,    // ex.: "13:00"
    val ordem: Long
)
