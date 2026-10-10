package com.example.xanggo.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.Normalizer
import kotlin.coroutines.resume

/** Một cây xăng thật lấy từ OpenStreetMap. */
data class NearbyStation(
    val id: String,
    val name: String,
    val brandKey: String, // PETROLIMEX, PVOIL, SAIGON_PETRO, IDEMITSU, OTHER
    val address: String,
    val openingHours: String,
    val lat: Double,
    val lon: Double,
    val distanceMeters: Int
)

/**
 * Lấy vị trí người dùng (LocationManager có sẵn, không cần thêm thư viện) và truy vấn
 * cây xăng quanh đó qua Overpass API (OpenStreetMap, miễn phí, không cần API key).
 */
object NearbyStationRepository {

    private const val RADIUS_METERS = 5000
    private const val MAX_RESULTS = 20
    private const val CACHE_TTL_MS = 10 * 60 * 1000L
    private const val LOCATION_MAX_AGE_MS = 10 * 60 * 1000L

    private val OVERPASS_ENDPOINTS = listOf(
        "https://overpass-api.de/api/interpreter",
        "https://overpass.kumi.systems/api/interpreter"
    )

    // Cache trong bộ nhớ để chuyển tab qua lại không gọi lại API
    private var cachedAt = 0L
    private var cachedOrigin: Location? = null
    private var cachedStations: List<NearbyStation> = emptyList()

    class LocationUnavailableException : Exception("Không lấy được vị trí")

    suspend fun loadNearby(context: Context, forceRefresh: Boolean = false): List<NearbyStation> {
        val location = getCurrentLocation(context) ?: throw LocationUnavailableException()

        val cachedLoc = cachedOrigin
        if (!forceRefresh && cachedLoc != null &&
            System.currentTimeMillis() - cachedAt < CACHE_TTL_MS &&
            cachedLoc.distanceTo(location) < 300f
        ) {
            return cachedStations
        }

        val stations = fetchStations(location.latitude, location.longitude)
        cachedAt = System.currentTimeMillis()
        cachedOrigin = location
        cachedStations = stations
        return stations
    }

    @SuppressLint("MissingPermission")
    private suspend fun getCurrentLocation(context: Context): Location? {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
            .filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }

        val lastKnown = (providers + LocationManager.PASSIVE_PROVIDER)
            .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }

        if (lastKnown != null && System.currentTimeMillis() - lastKnown.time < LOCATION_MAX_AGE_MS) {
            return lastKnown
        }
        if (providers.isEmpty()) return lastKnown

        val fresh = withTimeoutOrNull(12_000L) { requestSingleFix(lm, providers.first()) }
        return fresh ?: lastKnown
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestSingleFix(lm: LocationManager, provider: String): Location? =
        suspendCancellableCoroutine { cont ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val signal = android.os.CancellationSignal()
                cont.invokeOnCancellation { signal.cancel() }
                try {
                    lm.getCurrentLocation(provider, signal, { it.run() }) { loc ->
                        if (cont.isActive) cont.resume(loc)
                    }
                } catch (e: Exception) {
                    if (cont.isActive) cont.resume(null)
                }
            } else {
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        if (cont.isActive) cont.resume(location)
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(p: String?, s: Int, b: android.os.Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }
                cont.invokeOnCancellation { lm.removeUpdates(listener) }
                try {
                    @Suppress("DEPRECATION")
                    lm.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                } catch (e: Exception) {
                    if (cont.isActive) cont.resume(null)
                }
            }
        }

    private suspend fun fetchStations(lat: Double, lon: Double): List<NearbyStation> =
        withContext(Dispatchers.IO) {
            val query = "[out:json][timeout:20];" +
                "nwr[\"amenity\"=\"fuel\"](around:$RADIUS_METERS,$lat,$lon);" +
                "out center tags 60;"
            val body = "data=" + URLEncoder.encode(query, "UTF-8")

            var lastError: Exception? = null
            for (endpoint in OVERPASS_ENDPOINTS) {
                try {
                    val json = httpPost(endpoint, body)
                    return@withContext parse(json, lat, lon)
                } catch (e: Exception) {
                    lastError = e
                }
            }
            throw lastError ?: Exception("Không tải được danh sách cây xăng")
        }

    private fun httpPost(endpoint: String, body: String): String {
        val conn = URL(endpoint).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 10_000
            conn.readTimeout = 25_000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.setRequestProperty("User-Agent", "XangGo-Android/1.0 (vn.xanggo.app)")
            conn.outputStream.use { it.write(body.toByteArray()) }
            if (conn.responseCode !in 200..299) throw Exception("HTTP ${conn.responseCode}")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun parse(json: String, originLat: Double, originLon: Double): List<NearbyStation> {
        val elements = JSONObject(json).optJSONArray("elements") ?: return emptyList()
        val result = ArrayList<NearbyStation>(elements.length())
        val dist = FloatArray(1)

        for (i in 0 until elements.length()) {
            val el = elements.getJSONObject(i)
            val center = el.optJSONObject("center")
            val lat = if (el.has("lat")) el.getDouble("lat") else center?.optDouble("lat") ?: continue
            val lon = if (el.has("lon")) el.getDouble("lon") else center?.optDouble("lon") ?: continue
            val tags = el.optJSONObject("tags") ?: JSONObject()

            val brandKey = detectBrand(
                listOf("brand", "name", "operator", "brand:en", "name:en").joinToString(" ") { tags.optString(it) }
            )
            val name = tags.optString("name").ifBlank { tags.optString("brand") }.ifBlank {
                when (brandKey) {
                    "PETROLIMEX" -> "Cây xăng Petrolimex"
                    "PVOIL" -> "Cây xăng PVOIL"
                    "SAIGON_PETRO" -> "Cây xăng Saigon Petro"
                    "IDEMITSU" -> "Cây xăng Idemitsu Q8"
                    else -> "Cây xăng"
                }
            }

            val street = listOf(tags.optString("addr:housenumber"), tags.optString("addr:street"))
                .filter { it.isNotBlank() }.joinToString(" ")
            val area = listOf("addr:subdistrict", "addr:district", "addr:city")
                .map { tags.optString(it) }.firstOrNull { it.isNotBlank() }.orEmpty()
            val address = listOf(street, area).filter { it.isNotBlank() }.joinToString(", ")

            Location.distanceBetween(originLat, originLon, lat, lon, dist)

            result += NearbyStation(
                id = "${el.optString("type")}/${el.optLong("id")}",
                name = name,
                brandKey = brandKey,
                address = address,
                openingHours = tags.optString("opening_hours"),
                lat = lat,
                lon = lon,
                distanceMeters = dist[0].toInt()
            )
        }
        return result.sortedBy { it.distanceMeters }.take(MAX_RESULTS)
    }

    private fun detectBrand(raw: String): String {
        val s = Normalizer.normalize(raw, Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .replace("đ", "d").replace("Đ", "D").lowercase()
        return when {
            "petrolimex" in s || "plx" in s -> "PETROLIMEX"
            "pvoil" in s || "pv oil" in s -> "PVOIL"
            "saigon petro" in s || "sai gon petro" in s || "saigonpetro" in s -> "SAIGON_PETRO"
            "idemitsu" in s || "q8" in s -> "IDEMITSU"
            else -> "OTHER"
        }
    }
}
