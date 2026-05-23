package com.example.workman.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Utility class for all location-related operations in WorkMan.
 *
 * Features:
 * - Haversine distance calculation between two coordinates
 * - Geohash encoding for efficient Firestore geo-queries
 * - Current location fetching and syncing to Firebase
 * - Distance-based filtering helpers
 */
object LocationHelper {

    private const val TAG = "LocationHelper"

    // Default search radius in kilometers
    const val DEFAULT_RADIUS_KM = 25.0
    const val MAX_RADIUS_KM = 100.0
    const val MIN_RADIUS_KM = 5.0

    // Earth's radius in km
    private const val EARTH_RADIUS_KM = 6371.0

    // Geohash base32 character set
    private const val BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz"

    // ── Distance Calculation ─────────────────────────────────────────────────

    /**
     * Calculate the distance between two points using the Haversine formula.
     * @return Distance in kilometers
     */
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_KM * c
    }

    /**
     * Check if a point is within a given radius from another point.
     */
    fun isWithinRadius(
        centerLat: Double, centerLon: Double,
        pointLat: Double, pointLon: Double,
        radiusKm: Double = DEFAULT_RADIUS_KM
    ): Boolean {
        return calculateDistance(centerLat, centerLon, pointLat, pointLon) <= radiusKm
    }

    /**
     * Format distance for display.
     */
    fun formatDistance(distanceKm: Double): String {
        return when {
            distanceKm < 1.0 -> "${(distanceKm * 1000).toInt()}m away"
            distanceKm < 10.0 -> String.format("%.1f km away", distanceKm)
            else -> "${distanceKm.toInt()} km away"
        }
    }

    // ── Geohash ──────────────────────────────────────────────────────────────

    /**
     * Encode latitude/longitude into a geohash string.
     * Precision of 9 characters gives ~4.77m accuracy – good enough for our use case.
     * We use precision 5 (~4.9km) for range queries and full precision for exact location.
     */
    fun encode(latitude: Double, longitude: Double, precision: Int = 9): String {
        var latRange = doubleArrayOf(-90.0, 90.0)
        var lonRange = doubleArrayOf(-180.0, 180.0)
        var isEven = true
        var bit = 0
        var ch = 0
        val geohash = StringBuilder()

        while (geohash.length < precision) {
            if (isEven) {
                val mid = (lonRange[0] + lonRange[1]) / 2
                if (longitude >= mid) {
                    ch = ch or (1 shl (4 - bit))
                    lonRange[0] = mid
                } else {
                    lonRange[1] = mid
                }
            } else {
                val mid = (latRange[0] + latRange[1]) / 2
                if (latitude >= mid) {
                    ch = ch or (1 shl (4 - bit))
                    latRange[0] = mid
                } else {
                    latRange[1] = mid
                }
            }
            isEven = !isEven
            if (bit < 4) {
                bit++
            } else {
                geohash.append(BASE32[ch])
                bit = 0
                ch = 0
            }
        }
        return geohash.toString()
    }

    /**
     * Get geohash neighbors for range queries.
     * Returns a list of geohash prefixes that cover the bounding box
     * around a point at the given radius.
     */
    fun getGeohashRange(
        latitude: Double,
        longitude: Double,
        radiusKm: Double
    ): Pair<String, String> {
        // Use precision based on radius
        val precision = when {
            radiusKm <= 5.0 -> 5
            radiusKm <= 20.0 -> 4
            radiusKm <= 80.0 -> 3
            else -> 2
        }

        val lat = radiusKm / EARTH_RADIUS_KM
        val lon = radiusKm / (EARTH_RADIUS_KM * cos(Math.toRadians(latitude)))

        val lowerLat = latitude - Math.toDegrees(lat)
        val lowerLon = longitude - Math.toDegrees(lon)
        val upperLat = latitude + Math.toDegrees(lat)
        val upperLon = longitude + Math.toDegrees(lon)

        val lower = encode(lowerLat, lowerLon, precision)
        val upper = encode(upperLat, upperLon, precision)

        return lower to upper
    }

    // ── Location Fetching ────────────────────────────────────────────────────

    /**
     * Data class to hold location result
     */
    data class LocationResult(
        val latitude: Double,
        val longitude: Double,
        val geohash: String,
        val locationName: String
    )

    /**
     * Get the current device location. Returns null if unavailable or no permission.
     */
    suspend fun getCurrentLocation(context: Context): LocationResult? {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Location permission not granted")
            return null
        }

        return try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            // Try lastLocation first (fast), then request a fresh one
            var location = fusedLocationClient.lastLocation.await()

            if (location == null) {
                // Request a fresh location
                val cancellationToken = CancellationTokenSource()
                location = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    cancellationToken.token
                ).await()
            }

            if (location != null) {
                val geohash = encode(location.latitude, location.longitude)
                val locationName = getLocationName(context, location.latitude, location.longitude)

                LocationResult(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    geohash = geohash,
                    locationName = locationName
                )
            } else {
                Log.w(TAG, "Location is null")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting location", e)
            null
        }
    }

    /**
     * Reverse geocode coordinates to a human-readable location name.
     */
    fun getLocationName(context: Context, latitude: Double, longitude: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val area = address.subLocality ?: address.locality ?: address.subAdminArea
                ?: "Unknown Area"
                val city = address.locality ?: ""
                if (city.isNotEmpty() && area != city) "$area, $city" else area
            } else {
                "Unknown Location"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Geocoding failed", e)
            "Unknown Location"
        }
    }

    /**
     * Sync the user's current location to Firebase (users collection).
     * Stores latitude, longitude, geohash, and readable location name.
     */
    suspend fun syncLocationToFirebase(context: Context): LocationResult? {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return null
        val locationResult = getCurrentLocation(context) ?: return null

        try {
            FirebaseFirestore.getInstance().collection("users").document(userId)
                .update(
                    mapOf(
                        "latitude" to locationResult.latitude,
                        "longitude" to locationResult.longitude,
                        "geohash" to locationResult.geohash,
                        "location" to locationResult.locationName
                    )
                ).await()
            Log.d(TAG, "Location synced for user $userId: ${locationResult.locationName}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync location to Firebase", e)
        }

        return locationResult
    }

    // ── Bounding Box Helpers ─────────────────────────────────────────────────

    /**
     * Calculate the bounding box around a point for a given radius.
     * Used for efficient Firestore queries with inequality filters on lat/lng.
     */
    data class BoundingBox(
        val minLat: Double,
        val maxLat: Double,
        val minLon: Double,
        val maxLon: Double
    )

    fun getBoundingBox(latitude: Double, longitude: Double, radiusKm: Double): BoundingBox {
        val latDelta = Math.toDegrees(radiusKm / EARTH_RADIUS_KM)
        val lonDelta = Math.toDegrees(radiusKm / (EARTH_RADIUS_KM * cos(Math.toRadians(latitude))))

        return BoundingBox(
            minLat = latitude - latDelta,
            maxLat = latitude + latDelta,
            minLon = longitude - lonDelta,
            maxLon = longitude + lonDelta
        )
    }
}

