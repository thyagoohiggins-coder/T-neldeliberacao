package com.hikins.tunelfai.notif

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.hikins.tunelfai.model.Intervalos
import java.util.Calendar

/**
 * Agenda o lembrete "Fazer Relatório" para disparar 10 minutos antes de cada
 * intervalo começar (05:50, 06:50, ..., 14:50), com vibração forte.
 */
object Lembretes {

    const val EXTRA_SEGUNDOS = "segundos"
    private const val REQ = 4321
    private const val ANTECEDENCIA_MIN = 10

    private fun pendingIntent(context: Context, segundos: Int): PendingIntent {
        val i = Intent(context, LembreteReceiver::class.java).apply {
            putExtra(EXTRA_SEGUNDOS, segundos)
        }
        return PendingIntent.getBroadcast(
            context, REQ, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Próximo horário (ms) 10 min antes de um intervalo, posterior a [refMs]. */
    fun proximoDisparo(refMs: Long = System.currentTimeMillis()): Long {
        var melhor = Long.MAX_VALUE
        // Avalia hoje e amanhã para garantir que sempre há um próximo alvo.
        for (diaOffset in 0..1) {
            for (intv in Intervalos.lista) {
                val partes = intv.split(":")
                val c = Calendar.getInstance().apply {
                    timeInMillis = refMs
                    add(Calendar.DAY_OF_YEAR, diaOffset)
                    set(Calendar.HOUR_OF_DAY, partes[0].toInt())
                    set(Calendar.MINUTE, partes[1].toInt())
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    add(Calendar.MINUTE, -ANTECEDENCIA_MIN)
                }
                if (c.timeInMillis > refMs && c.timeInMillis < melhor) melhor = c.timeInMillis
            }
        }
        return melhor
    }

    fun agendar(context: Context, segundos: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val quando = proximoDisparo()
        val pi = pendingIntent(context, segundos)
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, quando, pi)
        } catch (e: SecurityException) {
            am.set(AlarmManager.RTC_WAKEUP, quando, pi)
        }
    }

    fun cancelar(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pendingIntent(context, 0))
    }
}
