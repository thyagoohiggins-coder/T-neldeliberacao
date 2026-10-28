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

    /** Minutos finais do intervalo em que o carro já vai para o intervalo seguinte (se ativado). */
    const val MINUTOS_ADIANTAR: Int = 15

    fun metaDe(intervalo: String): Int = metas[intervalo] ?: 0

    /**
     * Intervalo pela hora do celular: 06:59 ainda é "06:00".
     * Com [adiantar], faltando 15 min ou menos para o próximo, já cai no seguinte.
     * Fora do turno, fica no primeiro/último intervalo.
     */
    fun atual(hora: Int, minuto: Int, adiantar: Boolean): String {
        var h = hora
        if (adiantar && minuto >= 60 - MINUTOS_ADIANTAR) h += 1
        h = h.coerceIn(6, 15)
        return "%02d:00".format(h)
    }
}
