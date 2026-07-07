package com.hikins.tunelfai.data

import android.content.Context
import com.hikins.tunelfai.model.Motivo
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

                val fotos = mutableListOf<String>()
                val fotosArr = o.optJSONArray("fotos") ?: JSONArray()
                for (j in 0 until fotosArr.length()) fotos += fotosArr.getString(j)

                registros += Registro(
                    vin = o.getString("vin"),
                    status = status,
                    motivo = if (motivoStr.isBlank()) null else Motivo.valueOf(motivoStr),
                    motivoTexto = o.optString("motivoTexto", "").ifBlank { null },
                    intervalo = o.getString("intervalo"),
                    ordem = o.getLong("ordem"),
                    fotos = fotos
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
            val fotosArr = JSONArray()
            r.fotos.forEach { fotosArr.put(it) }
            arr.put(
                JSONObject()
                    .put("vin", r.vin)
                    .put("status", r.status.name)
                    .put("motivo", r.motivo?.name ?: "")
                    .put("motivoTexto", r.motivoTexto ?: "")
                    .put("intervalo", r.intervalo)
                    .put("ordem", r.ordem)
                    .put("fotos", fotosArr)
            )
        }
        raiz.put("registros", arr)
        raiz.put("seq", estado.seq)

        arquivo.writeText(raiz.toString())
    }
}
