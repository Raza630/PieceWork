package com.example.workman

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.workman.components.MapLocationPicker
import com.example.workman.ui.theme.BgColor
import com.example.workman.ui.theme.PrimaryBlue
import com.example.workman.ui.theme.TextDark
import com.example.workman.ui.theme.TextMuted
import com.google.android.gms.location.LocationServices
import java.util.Locale

/**
 * Full-screen map picker activity.
 * Returns: latitude, longitude, locationName via Intent result.
 *
 * Usage:
 *   val intent = Intent(this, MapPickerActivity::class.java)
 *   intent.putExtra("initial_lat", 28.6139)
 *   intent.putExtra("initial_lng", 77.2090)
 *   mapPickerLauncher.launch(intent)
 *
 * Result:
 *   data.getDoubleExtra("latitude", 0.0)
 *   data.getDoubleExtra("longitude", 0.0)
 *   data.getStringExtra("locationName")
 */
class MapPickerActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val initialLat = intent.getDoubleExtra("initial_lat", 20.5937)
        val initialLng = intent.getDoubleExtra("initial_lng", 78.9629)
        val initialZoom = if (initialLat != 20.5937) 15.0 else 5.0

        setContent {
            var selectedLat by remember { mutableDoubleStateOf(initialLat) }
            var selectedLng by remember { mutableDoubleStateOf(initialLng) }
            // Drives programmatic map recenter/zoom (boss GPS, My Location, pre-set location)
            var recenterTarget by remember {
                mutableStateOf<Pair<Double, Double>?>(
                    if (initialLat != 20.5937 || initialLng != 78.9629) initialLat to initialLng else null
                )
            }
            var addressText by remember { mutableStateOf("Move the map to select location") }
            val context = LocalContext.current

            // Geocode on location change
            LaunchedEffect(selectedLat, selectedLng) {
                if (selectedLat != 0.0 && selectedLng != 0.0) {
                    try {
                        @Suppress("DEPRECATION")
                        val geocoder = Geocoder(context, Locale.getDefault())
                        val addresses = geocoder.getFromLocation(selectedLat, selectedLng, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val addr = addresses[0]
                            addressText = buildString {
                                addr.subLocality?.let { append("$it, ") }
                                addr.locality?.let { append("$it, ") }
                                addr.adminArea?.let { append(it) }
                            }.trimEnd(',', ' ')
                            if (addressText.isEmpty()) {
                                addressText = addr.getAddressLine(0) ?: "Unknown location"
                            }
                        }
                    } catch (_: Exception) {
                        addressText = "Lat: %.4f, Lng: %.4f".format(selectedLat, selectedLng)
                    }
                }
            }

            // Permission + auto-center on user location
            val fusedLocationClient = remember {
                LocationServices.getFusedLocationProviderClient(context)
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                if (permissions.values.any { it }) {
                    centerOnUserLocation(fusedLocationClient) { lat, lng ->
                        selectedLat = lat
                        selectedLng = lng
                        recenterTarget = lat to lng
                    }
                }
            }

            // Auto-center if initial is default (no pre-set location)
            LaunchedEffect(Unit) {
                if (initialLat == 20.5937 && initialLng == 78.9629) {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        centerOnUserLocation(fusedLocationClient) { lat, lng ->
                            selectedLat = lat
                            selectedLng = lng
                            recenterTarget = lat to lng
                        }
                    } else {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                }
            }

            MaterialTheme {
                Scaffold(
                    containerColor = BgColor,
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    "Pick Job Location",
                                    color = TextDark,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.Default.ArrowBack, "Back", tint = TextDark)
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                        )
                    }
                ) { padding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
                        // Map
                        MapLocationPicker(
                            modifier = Modifier.fillMaxSize(),
                            initialLatitude = selectedLat,
                            initialLongitude = selectedLng,
                            initialZoom = initialZoom,
                            recenterTo = recenterTarget,
                            onLocationSelected = { lat, lng ->
                                selectedLat = lat
                                selectedLng = lng
                            },
                            onMyLocationClick = {
                                if (ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    centerOnUserLocation(fusedLocationClient) { lat, lng ->
                                        selectedLat = lat
                                        selectedLng = lng
                                        recenterTarget = lat to lng
                                    }
                                } else {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            }
                        )

                        // Bottom card with address + confirm button
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(16.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White,
                            shadowElevation = 8.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = addressText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextDark,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "%.5f, %.5f".format(selectedLat, selectedLng),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                                Spacer(Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        val resultIntent = Intent().apply {
                                            putExtra("latitude", selectedLat)
                                            putExtra("longitude", selectedLng)
                                            putExtra("locationName", addressText)
                                        }
                                        setResult(RESULT_OK, resultIntent)
                                        finish()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                                ) {
                                    Text(
                                        "Confirm Location",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Suppress("MissingPermission")
    private fun centerOnUserLocation(
        fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient,
        onLocation: (Double, Double) -> Unit
    ) {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                onLocation(location.latitude, location.longitude)
            }
        }
    }
}

