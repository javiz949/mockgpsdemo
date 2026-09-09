package com.example.mockgpsdemo

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class MainActivity : Activity() {

    private lateinit var fused: FusedLocationProviderClient
    private lateinit var txtResult: TextView
    private lateinit var btnMock: Button
    private lateinit var locationManager: LocationManager
    private val REQ = 1001

    // Coordenadas de Nueva York (Times Square aprox.)
    private val NY_LAT = 40.7128
    private val NY_LON = -74.0060

    // Se inyecta en TODOS los proveedores para que el "fused location"
    // de Google Play Services devuelva Nueva York de forma consistente,
    // no solo la primera lectura.
    private val mockProviders = listOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER
    )
    private var mocking = false
    private val handler = Handler(Looper.getMainLooper())
    private val pushRunnable = object : Runnable {
        override fun run() {
            if (mocking) {
                pushMockLocation()
                // Re-inyecta cada segundo para que la ubicación falsa "pegue"
                handler.postDelayed(this, 1000L)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fused = LocationServices.getFusedLocationProviderClient(this)
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        txtResult = findViewById(R.id.txtResult)
        btnMock = findViewById(R.id.btnMockNY)

        findViewById<Button>(R.id.btnCheck).setOnClickListener {
            checkPermissionAndGetLocation()
        }
        btnMock.setOnClickListener {
            if (mocking) stopMocking() else startMockNewYork()
        }
    }

    private fun checkPermissionAndGetLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                REQ
            )
        } else {
            getLocation()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            getLocation()
        } else {
            txtResult.text = "Permiso de ubicación denegado."
        }
    }

    // ---------------- Inyección de ubicación falsa ----------------

    @SuppressLint("MissingPermission")
    private fun startMockNewYork() {
        try {
            for (provider in mockProviders) {
                // Limpia cualquier proveedor de prueba previo
                try {
                    locationManager.removeTestProvider(provider)
                } catch (_: Exception) {
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val props = ProviderProperties.Builder()
                        .setAccuracy(ProviderProperties.ACCURACY_FINE)
                        .setPowerUsage(ProviderProperties.POWER_USAGE_LOW)
                        .build()
                    locationManager.addTestProvider(provider, props)
                } else {
                    @Suppress("DEPRECATION")
                    locationManager.addTestProvider(
                        provider,
                        false, false, false, false,
                        false, true, true,
                        Criteria.POWER_LOW, Criteria.ACCURACY_FINE
                    )
                }
                locationManager.setTestProviderEnabled(provider, true)
            }

            mocking = true
            btnMock.text = "Detener simulación"
            pushMockLocation()
            handler.postDelayed(pushRunnable, 1000L)

            txtResult.text = buildString {
                appendLine("📍 Simulando Nueva York…")
                appendLine("Lat: $NY_LAT   Lon: $NY_LON")
                appendLine("--------------------------------")
                appendLine("Ahora abre Google Maps o presiona")
                append("\"Verificar mi ubicación\" para confirmar.")
            }
        } catch (e: SecurityException) {
            mocking = false
            btnMock.text = "Simular Nueva York"
            txtResult.text = buildString {
                appendLine("⚠️ Esta app NO está seleccionada como app de ubicación de prueba.")
                appendLine("--------------------------------")
                appendLine("Ve a: Ajustes > Opciones de desarrollador >")
                appendLine("\"Seleccionar app de ubicación de prueba\"")
                append("y elige \"Detector Mock GPS\". Luego reintenta.")
            }
        } catch (e: Exception) {
            mocking = false
            btnMock.text = "Simular Nueva York"
            txtResult.text = "Error al simular: ${e.message}"
        }
    }

    @SuppressLint("MissingPermission")
    private fun pushMockLocation() {
        for (provider in mockProviders) {
            try {
                val loc = Location(provider).apply {
                    latitude = NY_LAT
                    longitude = NY_LON
                    accuracy = 5f
                    altitude = 10.0
                    bearing = 0f
                    speed = 0f
                    time = System.currentTimeMillis()
                    elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                }
                locationManager.setTestProviderLocation(provider, loc)
            } catch (_: Exception) {
                // Si el provider fue removido por el sistema, se ignora
            }
        }
    }

    private fun stopMocking() {
        mocking = false
        handler.removeCallbacks(pushRunnable)
        for (provider in mockProviders) {
            try {
                locationManager.setTestProviderEnabled(provider, false)
                locationManager.removeTestProvider(provider)
            } catch (_: Exception) {
            }
        }
        btnMock.text = "Simular Nueva York"
        txtResult.text = "Simulación detenida. Ubicación real restaurada."
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mocking) {
            handler.removeCallbacks(pushRunnable)
            for (provider in mockProviders) {
                try {
                    locationManager.removeTestProvider(provider)
                } catch (_: Exception) {
                }
            }
        }
    }

    // ---------------- Lectura / detección de ubicación ----------------

    @SuppressLint("MissingPermission")
    private fun getLocation() {
        txtResult.text = "Obteniendo ubicación..."
        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMaxUpdates(1)
            .build()
        fused.requestLocationUpdates(req, object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                fused.removeLocationUpdates(this)
                val loc = result.lastLocation
                if (loc != null) show(loc)
                else txtResult.text = "No se pudo obtener ubicación. Activa el GPS del teléfono."
            }
        }, Looper.getMainLooper())
    }

    private fun show(loc: Location) {
        val mockOld = loc.isFromMockProvider
        val mockNew = if (Build.VERSION.SDK_INT >= 31) loc.isMock else "N/A (API < 31)"
        txtResult.text = buildString {
            appendLine("Latitud:  ${loc.latitude}")
            appendLine("Longitud: ${loc.longitude}")
            appendLine("Proveedor: ${loc.provider}")
            appendLine("Precisión: ${"%.1f".format(loc.accuracy)} m")
            appendLine("--------------------------------")
            appendLine("isFromMockProvider(): $mockOld")
            appendLine("isMock() (API 31+):   $mockNew")
            appendLine("--------------------------------")
            if (mockOld) {
                append("⚠️ UBICACIÓN SIMULADA DETECTADA")
            } else {
                append("✅ Ubicación genuina (o inyectada por debajo de la capa mock)")
            }
        }
    }
}
