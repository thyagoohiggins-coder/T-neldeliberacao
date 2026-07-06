package com.hikins.tunelfai.data

import android.content.Context
import com.hikins.tunelfai.model.Motivo
import com.hikins.tunelfai.model.Registro
import com.hikins.tunelfai.model.Status
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Entrada de produção/aprovados digitada pelo usuário para um intervalo. */
data class MetaInput(val prod: String = "", val aprov: String = "")

/** Estado completo persistido do app. */
data class EstadoTunel(
    val registros: List<Registro> = emptyList(),
    val metas: Map<String, MetaInput> = emptyMap(),
    val seq: Long = 0
)

/** Lê e grava o estado em um arquivo JSON no armazenamento interno do app. */
class Repositorio(context: Context) {

    private val arquivo = File(context.filesDir, "tunel_dados.json")

    fun carregar(): EstadoTunel {
        if (!arquivo.exists()) return EstadoTunel()
        return try {
            val raiz = JSONObject(arquivo.readText())

            val registros = mutableListOf<Registro>()
            val arr = raiz.optJSONArray("registros") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val status = Status.valueOf(o.getString("status"))
                val motivoStr = o.optString("motivo", "")
                registros += Registro(
                    vin = o.getString("vin"),
                    status = status,
                    motivo = if (motivoStr.isBlank()) null else Motivo.valueOf(motivoStr),
                    intervalo = o.getString("intervalo"),
                    ordem = o.getLong("ordem")
                )
            }

            val metas = mutableMapOf<String, MetaInput>()
            val metasObj = raiz.optJSONObject("metas") ?: JSONObject()
            for (chave in metasObj.keys()) {
                val m = metasObj.getJSONObject(chave)
                metas[chave] = MetaInput(m.optString("prod", ""), m.optString("aprov", ""))
            }

            EstadoTunel(registros, metas, raiz.optLong("seq", 0))
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
                    .put("status", r.status.name)
                    .put("motivo", r.motivo?.name ?: "")
                    .put("intervalo", r.intervalo)
                    .put("ordem", r.ordem)
            )
        }
        raiz.put("registros", arr)

        val metasObj = JSONObject()
        estado.metas.forEach { (chave, m) ->
            metasObj.put(chave, JSONObject().put("prod", m.prod).put("aprov", m.aprov))
        }
        raiz.put("metas", metasObj)
        raiz.put("seq", estado.seq)

        arquivo.writeText(raiz.toString())
    }
}
