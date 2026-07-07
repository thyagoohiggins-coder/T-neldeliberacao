package com.hikins.tunelfai.data

import android.content.Context
import com.hikins.tunelfai.model.Configuracao

/** Persiste as [Configuracao] do app em SharedPreferences. */
class ConfigRepo(context: Context) {

    private val prefs = context.getSharedPreferences("config_tunel", Context.MODE_PRIVATE)

    fun carregar(): Configuracao = Configuracao(
        exigirFoto = prefs.getBoolean("exigirFoto", false),
        motivoAtivado = prefs.getBoolean("motivoAtivado", true),
        motivoLivre = prefs.getBoolean("motivoLivre", false),
        vibracaoAtivada = prefs.getBoolean("vibracaoAtivada", false),
        vibracaoSegundos = prefs.getInt("vibracaoSegundos", 3),
        destacarNegrito = prefs.getBoolean("destacarNegrito", true),
        retornoAuto = prefs.getBoolean("retornoAuto", false),
        vinPrefixoVB = prefs.getBoolean("vinPrefixoVB", false)
    )

    fun salvar(c: Configuracao) {
        prefs.edit()
            .putBoolean("exigirFoto", c.exigirFoto)
            .putBoolean("motivoAtivado", c.motivoAtivado)
            .putBoolean("motivoLivre", c.motivoLivre)
            .putBoolean("vibracaoAtivada", c.vibracaoAtivada)
            .putInt("vibracaoSegundos", c.vibracaoSegundos)
            .putBoolean("destacarNegrito", c.destacarNegrito)
            .putBoolean("retornoAuto", c.retornoAuto)
            .putBoolean("vinPrefixoVB", c.vinPrefixoVB)
            .apply()
    }
}
