package com.hikins.tunelfai.data

import android.content.Context
import com.hikins.tunelfai.model.Registro
import com.hikins.tunelfai.model.Status
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Estado completo persistido do app. */
data class EstadoTunel(
    val registros: List<Registro> = emptyList(),
    val seq: Long = 0
)

/** Lê e grava o estado em um arquivo JSON no armazenamento interno do app. */
class Repositorio(context: Context) {

    private val arquivo = File(context.filesDir, "tunel_dados.json")

    private fun JSONObject.textoOuNull(chave: String): String? =
        optString(chave, "").takeIf { it.isNotBlank() }

    fun carregar(): EstadoTunel {
        if (!arquivo.exists()) return EstadoTunel()
        return try {
            val raiz = JSONObject(arquivo.readText())

            val registros = mutableListOf<Registro>()
            val arr = raiz.optJSONArray("registros") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                var reparo = o.textoOuNull("reparo")
                // Compatibilidade com dados antigos: LIBERADO -> Aprovado, NEGADO -> Reparo.
                val status = when (val s = o.optString("status", "")) {
                    "LIBERADO" -> Status.APROVADO
                    "NEGADO" -> {
                        if (reparo == null) reparo = when (o.optString("motivo", "")) {
                            "DEMERITO_ABERTO" -> "Demérito em aberto"
                            "REPARO_INADEQUADO" -> "Reparo Inadequado"
                            else -> "Reparo"
                        }
                        Status.REPARO
                    }
                    else -> Status.entries.firstOrNull { it.name == s }
                }
                registros += Registro(
                    vin = o.getString("vin"),
                    modelo = o.textoOuNull("modelo"),
                    cor = o.textoOuNull("cor"),
                    status = status,
                    reparo = reparo,
                    intervalo = o.getString("intervalo"),
                    ordem = o.getLong("ordem"),
                    responsavel = o.textoOuNull("responsavel"),
                    fotos = o.optJSONArray("fotos")?.let { f -> (0 until f.length()).map { f.getString(it) } }
                        ?: emptyList()
                )
            }

            EstadoTunel(registros, raiz.optLong("seq", 0))
        } catch (e: Exception) {
            EstadoTunel()
        }
    }

    fun salvar(estado: EstadoTunel) {
        val raiz = JSONObject()

        val arr = JSONArray()
        estado.registros.forEach { r ->
            arr.put(
                JSONObject()
                    .put("vin", r.vin)
                    .put("modelo", r.modelo ?: "")
                    .put("cor", r.cor ?: "")
                    .put("status", r.status?.name ?: "")
                    .put("reparo", r.reparo ?: "")
                    .put("intervalo", r.intervalo)
                    .put("ordem", r.ordem)
                    .put("responsavel", r.responsavel ?: "")
                    .put("fotos", JSONArray(r.fotos))
            )
        }
        raiz.put("registros", arr)
        raiz.put("seq", estado.seq)

        arquivo.writeText(raiz.toString())
    }
}
