package mg.sitraka.fihirana

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Garde l'appli active pendant les téléchargements audio (notification de progression). */
class DownloadService : Service() {
    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    private fun channel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(NotificationChannel("audio", "Rakitra audio", NotificationManager.IMPORTANCE_LOW))
        }
    }

    private fun build(): Notification {
        val done = Dl.finished + Dl.failed
        return NotificationCompat.Builder(this, "audio")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Mampidina rakitra audio")
            .setContentText("$done/${Dl.total} · ${Dl.current}")
            .setProgress(if (Dl.total > 0) Dl.total else 1, done, Dl.total == 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        channel()
        try {
            startForeground(4242, build())
        } catch (e: Exception) {
            stopSelf()
            return START_NOT_STICKY
        }
        job?.cancel()
        job = CoroutineScope(Dispatchers.Main).launch {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            while (Dl.running) {
                nm.notify(4242, build())
                delay(1000)
            }
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        job?.cancel()
        super.onDestroy()
    }
}
