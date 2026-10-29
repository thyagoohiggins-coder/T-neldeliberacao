package com.hikins.tunelfai.data

import android.content.Context
import org.json.JSONArray

/** Preferências do usuário (tela de Configurações). */
class Configuracoes(context: Context) {

    private val prefs = context.getSharedPreferences("config", Context.MODE_PRIVATE)

    /** Faltando 15 min para o próximo intervalo, o carro já vai para o intervalo seguinte. */
    var adiantarIntervalo: Boolean
        get() = prefs.getBoolean("adiantar_intervalo", false)
        set(v) = prefs.edit().putBoolean("adiantar_intervalo", v).apply()

    /** Seleciona o modelo automaticamente pelo prefixo do chassi (digitado ou lido no código). */
    var autoModelo: Boolean
        get() = prefs.getBoolean("auto_modelo", false)
        set(v) = prefs.edit().putBoolean("auto_modelo", v).apply()

    /** Mostra a logo da marca (Chery/Changan) junto do VIN. */
    var mostrarLogos: Boolean
        get() = prefs.getBoolean("mostrar_logos", false)
        set(v) = prefs.edit().putBoolean("mostrar_logos", v).apply()

    /** Lembrete de fechamento 15 min antes de cada intervalo. */
    var lembretes: Boolean
        get() = prefs.getBoolean("lembretes", false)
        set(v) = prefs.edit().putBoolean("lembretes", v).apply()

    /** Vibrar o celular junto com o lembrete de fechamento. */
    var vibrar: Boolean
        get() = prefs.getBoolean("vibrar", false)
        set(v) = prefs.edit().putBoolean("vibrar", v).apply()

    /** Força da vibração: 0 = fraca, 1 = média, 2 = forte. */
    var vibracaoForca: Int
        get() = prefs.getInt("vibracao_forca", 1)
        set(v) = prefs.edit().putInt("vibracao_forca", v).apply()

    /** Duração da vibração em segundos (2, 3 ou 4). */
    var vibracaoSegundos: Int
        get() = prefs.getInt("vibracao_segundos", 3)
        set(v) = prefs.edit().putInt("vibracao_segundos", v).apply()

    /** Metas editadas por intervalo ("06:00" -> 8). Vazio = metas padrão. */
    var metas: Map<String, Int>
        get() = try {
            val o = org.json.JSONObject(prefs.getString("metas", "{}") ?: "{}")
            o.keys().asSequence().associateWith { o.getInt(it) }
        } catch (e: Exception) {
            emptyMap()
        }
        set(v) = prefs.edit().putString("metas", org.json.JSONObject(v as Map<*, *>).toString()).apply()

    /** Às 15:35, mostrar no app o aviso para enviar o fechamento do túnel. */
    var avisoFechamento: Boolean
        get() = prefs.getBoolean("aviso_fechamento", true)
        set(v) = prefs.edit().putBoolean("aviso_fechamento", v).apply()

    /** Dia (yyyy-MM-dd) em que o aviso de fechamento já foi mostrado. */
    var avisoFechamentoDia: String
        get() = prefs.getString("aviso_fechamento_dia", "") ?: ""
        set(v) = prefs.edit().putString("aviso_fechamento_dia", v).apply()

    /** Banco de reparos pré-definidos, sugeridos ao escrever o reparo de um veículo. */
    var reparos: List<String>
        get() {
            val bruto = prefs.getString("reparos", null) ?: return REPAROS_PADRAO
            return try {
                val arr = JSONArray(bruto)
                (0 until arr.length()).map { arr.getString(it) }
            } catch (e: Exception) {
                REPAROS_PADRAO
            }
        }
        set(v) = prefs.edit().putString("reparos", JSONArray(v).toString()).apply()

    companion object {
        val REPAROS_PADRAO = listOf("Demérito em aberto", "Reparo Inadequado")

        /** Sugestões do banco para o texto digitado (contém, sem diferenciar maiúsculas). */
        fun sugerir(banco: List<String>, texto: String, limite: Int = 8): List<String> {
            val t = texto.trim()
            return banco
                .filter { t.isEmpty() || (it.contains(t, ignoreCase = true) && !it.equals(t, ignoreCase = true)) }
                .take(limite)
        }
    }
}
