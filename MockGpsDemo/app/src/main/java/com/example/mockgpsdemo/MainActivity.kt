package com.example.mockgpsdemo

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Looper
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
    private val REQ = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fused = LocationServices.getFusedLocationProviderClient(this)
        txtResult = findViewById(R.id.txtResult)
        findViewById<Button>(R.id.btnCheck).setOnClickListener {
            checkPermissionAndGetLocation()
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
