package com.hikins.tunelfai.model

/** Intervalos de hora e metas fixas do Túnel de liberação FAI. */
object Intervalos {

    /** Ordem crescente dos intervalos disponíveis. */
    val lista: List<String> = listOf(
        "06:00", "07:00", "08:00", "09:00", "10:00",
        "11:00", "12:00", "13:00", "14:00", "15:00"
    )

    /** Meta esperada de veículos processados em cada intervalo (soma = 80). */
    private val metas: Map<String, Int> = mapOf(
        "06:00" to 8,
        "07:00" to 10,
        "08:00" to 8,
        "09:00" to 10,
        "10:00" to 10,
        "11:00" to 6,
        "12:00" to 2,
        "13:00" to 10,
        "14:00" to 8,
        "15:00" to 8
    )

    /** Meta total do dia (fixa) usada no resumo geral. */
    const val META_TOTAL: Int = 80

    fun metaDe(intervalo: String): Int = metas[intervalo] ?: 0

    /**
     * Intervalo correspondente à hora informada (ex.: 13 -> "13:00").
     * Antes do primeiro intervalo usa o primeiro; depois do último usa o último.
     */
    fun paraHora(hora: Int): String {
        val alvo = "%02d:00".format(hora)
        return when {
            lista.contains(alvo) -> alvo
            hora < 6 -> lista.first()
            else -> lista.last()
        }
    }
}
