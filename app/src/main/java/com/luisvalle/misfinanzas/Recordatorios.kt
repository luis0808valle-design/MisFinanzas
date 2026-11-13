package com.luisvalle.misfinanzas

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.luisvalle.misfinanzas.data.AppDatabase
import com.luisvalle.misfinanzas.data.Programado
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

fun formatoDinero(valor: Double): String =
    Apariencia.moneda + " " + String.format(Locale.forLanguageTag("es-DO"), "%,.2f", valor)

object Recordatorios {
    private const val CANAL = "recordatorios"

    fun crearCanal(ctx: Context) {
        val canal = NotificationChannel(
            CANAL,
            "Recordatorios de pagos y cobros",
            NotificationManager.IMPORTANCE_HIGH
        )
        ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    private fun momento(p: Programado): Long =
        LocalDate.parse(p.fecha)
            .minusDays(p.diasAntes.toLong())
            .atTime(p.hora, p.minuto)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    private fun pendiente(ctx: Context, p: Programado): PendingIntent {
        val intent = Intent(ctx, AlarmaReceiver::class.java).putExtra("id", p.id)
        return PendingIntent.getBroadcast(
            ctx,
            p.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun programar(ctx: Context, p: Programado) {
        val alarmas = ctx.getSystemService(AlarmManager::class.java)
        alarmas.cancel(pendiente(ctx, p))
        if (!p.activo) return
        val cuando = momento(p)
        if (cuando <= System.currentTimeMillis()) return
        alarmas.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cuando, pendiente(ctx, p))
    }

    fun cancelar(ctx: Context, p: Programado) {
        ctx.getSystemService(AlarmManager::class.java).cancel(pendiente(ctx, p))
    }

    fun reprogramarTodo(ctx: Context) {
        AppDatabase.obtener(ctx).movimientoDao().programadosActivosSync()
            .forEach { programar(ctx, it) }
    }

    fun mostrar(ctx: Context, p: Programado) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val abrir = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val titulo = (if (p.esGasto) "Pagar: " else "Cobrar: ") + p.nombre
        val texto = p.mensaje.ifBlank { formatoDinero(p.monto) + " · " + p.fecha }

        val notificacion = NotificationCompat.Builder(ctx, CANAL)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setContentIntent(abrir)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        NotificationManagerCompat.from(ctx).notify(p.id.toInt(), notificacion)
    }
}

class AlarmaReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Apariencia.cargar(context); val id = intent.getLongExtra("id", -1L)
        if (id < 0) return
        val pendiente = goAsync()
        Thread {
            try {
                val p = AppDatabase.obtener(context).movimientoDao().programadoPorIdSync(id)
                if (p != null && p.activo) Recordatorios.mostrar(context, p)
            } finally {
                pendiente.finish()
            }
        }.start()
    }
}

class ArranqueReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendiente = goAsync()
        Thread {
            try {
                Recordatorios.reprogramarTodo(context)
            } finally {
                pendiente.finish()
            }
        }.start()
    }
}