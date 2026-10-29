package com.hikins.tunelfai.lembrete

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.hikins.tunelfai.MainActivity
import com.hikins.tunelfai.data.Configuracoes
import com.hikins.tunelfai.ui.Formatador
import java.util.Calendar

/**
 * Lembretes de fechamento: 15 min antes do fim de cada intervalo (06:45 ... 14:45)
 * e às 15:35 para o intervalo das 15:00. Agenda sempre só o próximo alarme.
 */
object Lembretes {

    // Canal sem vibração própria: a vibração é feita pelo app, com a força/duração configuradas.
    private const val CANAL = "lembretes_fechamento_v2"
    private const val ACAO = "com.hikins.tunelfai.LEMBRETE"

    /** (hora, minuto, intervalo) de cada lembrete do dia. */
    val horarios: List<Triple<Int, Int, String>> =
        (6..14).map { Triple(it, 45, "%02d:00".format(it)) } + Triple(15, 35, "15:00")

    /** Próximo lembrete estritamente depois de [agora]. */
    fun proximo(agora: Calendar): Pair<Calendar, String> {
        for (dia in 0..1) {
            for ((h, m, intv) in horarios) {
                val c = (agora.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_YEAR, dia)
                    set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                if (c.after(agora)) return c to intv
            }
        }
        error("sem horário")   // inalcançável: sempre há um horário no dia seguinte
    }

    private fun pendente(context: Context, intervalo: String?): PendingIntent {
        val i = Intent(context, LembreteReceiver::class.java).setAction(ACAO)
        if (intervalo != null) i.putExtra("intervalo", intervalo)
        return PendingIntent.getBroadcast(
            context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Agenda (ou cancela) conforme a configuração. */
    fun atualizar(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        if (!Configuracoes(context).lembretes) {
            am.cancel(pendente(context, null))
            return
        }
        val (quando, intervalo) = proximo(Calendar.getInstance())
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, quando.timeInMillis, pendente(context, intervalo))
    }

    fun notificar(context: Context, intervalo: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val nm = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26 && nm.getNotificationChannel(CANAL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CANAL, "Lembretes de fechamento", NotificationManager.IMPORTANCE_HIGH)
                    .apply { enableVibration(false) }
            )
        }
        val abrir = PendingIntent.getActivity(
            context, 1, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Fechamento do túnel")
            .setContentText("Faça o fechamento dos veículos de ${Formatador.faixa(intervalo)}.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(abrir)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(intervalo.hashCode(), n)

        val cfg = Configuracoes(context)
        if (cfg.vibrar) vibrar(context, cfg.vibracaoForca, cfg.vibracaoSegundos)
    }

    /** Vibra por [segundos] com força 0 = fraca, 1 = média, 2 = forte. */
    @Suppress("DEPRECATION")
    fun vibrar(context: Context, forca: Int, segundos: Int) {
        val v = context.getSystemService(Vibrator::class.java) ?: return
        if (!v.hasVibrator()) return
        val ms = segundos.coerceIn(1, 10) * 1000L
        if (Build.VERSION.SDK_INT >= 26) {
            val amplitude = when (forca) { 0 -> 70; 2 -> 255; else -> 160 }
            v.vibrate(VibrationEffect.createOneShot(ms, amplitude))
        } else {
            v.vibrate(ms)
        }
    }
}

/** Recebe o alarme (mostra a notificação e agenda o próximo) e o boot do aparelho. */
class LembreteReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val intervalo = intent.getStringExtra("intervalo")
        if (intervalo != null && Configuracoes(context).lembretes) {
            Lembretes.notificar(context, intervalo)
        }
        Lembretes.atualizar(context)
    }
}
