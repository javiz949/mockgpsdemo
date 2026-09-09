package com.example.mockgpsdemo

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock

class MockLocationService : Service() {

    private lateinit var locationManager: LocationManager
    private val providers = listOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER
    )
    private val handler = Handler(Looper.getMainLooper())
    private var running = false

    private val pushRunnable = object : Runnable {
        override fun run() {
            if (running) {
                pushMockLocation()
                handler.postDelayed(this, 1000L)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForegroundNotification()
        setupProviders()
        running = true
        pushMockLocation()
        handler.postDelayed(pushRunnable, 1000L)
        return START_STICKY
    }

    private fun setupProviders() {
        for (p in providers) {
            try {
                locationManager.removeTestProvider(p)
            } catch (_: Exception) {
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val props = ProviderProperties.Builder()
                        .setAccuracy(ProviderProperties.ACCURACY_FINE)
                        .setPowerUsage(ProviderProperties.POWER_USAGE_LOW)
                        .build()
                    locationManager.addTestProvider(p, props)
                } else {
                    @Suppress("DEPRECATION")
                    locationManager.addTestProvider(
                        p, false, false, false, false,
                        false, true, true,
                        Criteria.POWER_LOW, Criteria.ACCURACY_FINE
                    )
                }
                locationManager.setTestProviderEnabled(p, true)
            } catch (_: Exception) {
            }
        }
    }

    private fun pushMockLocation() {
        for (p in providers) {
            try {
                val loc = Location(p).apply {
                    latitude = NY_LAT
                    longitude = NY_LON
                    accuracy = 5f
                    altitude = 10.0
                    bearing = 0f
                    speed = 0f
                    time = System.currentTimeMillis()
                    elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                }
                locationManager.setTestProviderLocation(p, loc)
            } catch (_: Exception) {
            }
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

    override fun onDestroy() {
        super.onDestroy()
        running = false
        handler.removeCallbacks(pushRunnable)
        for (p in providers) {
            try {
                locationManager.setTestProviderEnabled(p, false)
                locationManager.removeTestProvider(p)
            } catch (_: Exception) {
            }
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
