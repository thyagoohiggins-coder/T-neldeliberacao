package com.hikins.tunelfai.data

import android.content.Context

/** Preferências do usuário (tela de Configurações). */
class Configuracoes(context: Context) {

    private val prefs = context.getSharedPreferences("config", Context.MODE_PRIVATE)

    /** Faltando 15 min para o próximo intervalo, o carro já vai para o intervalo seguinte. */
    var adiantarIntervalo: Boolean
        get() = prefs.getBoolean("adiantar_intervalo", false)
        set(v) = prefs.edit().putBoolean("adiantar_intervalo", v).apply()

    /** Lembrete de fechamento 15 min antes de cada intervalo. */
    var lembretes: Boolean
        get() = prefs.getBoolean("lembretes", false)
        set(v) = prefs.edit().putBoolean("lembretes", v).apply()
}
