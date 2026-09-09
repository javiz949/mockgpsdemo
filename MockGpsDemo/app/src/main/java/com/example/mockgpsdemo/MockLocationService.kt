package com.example.mockgpsdemo

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class MockLocationService : Service() {

    private lateinit var fused: FusedLocationProviderClient
    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private val pushIntervalMs = 250L

    private val pushRunnable = object : Runnable {
        override fun run() {
            if (running) {
                pushMockLocation()
                handler.postDelayed(this, pushIntervalMs)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        fused = LocationServices.getFusedLocationProviderClient(this)
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForegroundNotification()
        running = true
        // Inyecta directo en el motor Fused (la capa que lee el detector),
        // eliminando la carrera con la cache de la ubicacion real.
        fused.setMockMode(true)
            .addOnSuccessListener {
                pushMockLocation()
                handler.postDelayed(pushRunnable, pushIntervalMs)
            }
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun pushMockLocation() {
        val loc = Location(LocationManager.GPS_PROVIDER).apply {
            latitude = NY_LAT
            longitude = NY_LON
            accuracy = 5f
            altitude = 10.0
            bearing = 0f
            speed = 0f
            time = System.currentTimeMillis()
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                bearingAccuracyDegrees = 0.1f
                verticalAccuracyMeters = 0.1f
                speedAccuracyMetersPerSecond = 0.01f
            }
        }
        try {
            fused.setMockLocation(loc)
        } catch (_: Exception) {
        }
    }

    private fun startForegroundNotification() {
        val channelId = "mock_gps"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            val ch = NotificationChannel(
                channelId,
                "Simulación GPS",
                NotificationManager.IMPORTANCE_LOW
            )
            nm.createNotificationChannel(ch)
        }
        val notif: Notification = Notification.Builder(this, channelId)
            .setContentTitle("Simulando Nueva York")
            .setContentText("Ubicación falsa activa (40.7128, -74.0060)")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(1, notif)
        }
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()
        running = false
        handler.removeCallbacks(pushRunnable)
        // Devuelve el Fused a modo normal: restaura la ubicacion real.
        try {
            fused.setMockMode(false)
        } catch (_: Exception) {
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_STOP = "com.example.mockgpsdemo.STOP"
        const val NY_LAT = 40.7128
        const val NY_LON = -74.0060

        fun start(ctx: Context) {
            val i = Intent(ctx, MockLocationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(i)
            } else {
                ctx.startService(i)
            }
        }

        fun stop(ctx: Context) {
            val i = Intent(ctx, MockLocationService::class.java)
            i.action = ACTION_STOP
            ctx.startService(i)
        }
    }
}
