package com.hikins.tunelfai.notif

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat

/**
 * Disparo do lembrete: vibra forte por N segundos, mostra a notificação
 * "Fazer Relatório" e reagenda o próximo intervalo.
 */
class LembreteReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val segundos = intent.getIntExtra(Lembretes.EXTRA_SEGUNDOS, 3).coerceIn(1, 60)
        vibrarForte(context, segundos)
        notificar(context)
        // Reagenda para o próximo intervalo do dia.
        Lembretes.agendar(context, segundos)
    }

    private fun vibrarForte(context: Context, segundos: Int) {
        val vibrador = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        val ms = segundos * 1000L
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Amplitude máxima (255) = vibração forte.
            vibrador.vibrate(VibrationEffect.createOneShot(ms, 255))
        } else {
            @Suppress("DEPRECATION")
            vibrador.vibrate(ms)
        }
    }

    private fun notificar(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val canalId = "lembrete_relatorio"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                canalId,
                "Lembretes de relatório",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { enableVibration(true) }
            nm.createNotificationChannel(canal)
        }
        val n = NotificationCompat.Builder(context, canalId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Fazer Relatório")
            .setContentText("Um novo intervalo começa em 10 minutos.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .build()
        nm.notify(981, n)
    }
}
