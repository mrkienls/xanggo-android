package com.example.xanggo.data

import com.example.xanggo.data.model.CountryFuel
import com.example.xanggo.data.model.FuelItem
import com.example.xanggo.data.model.PriceHistoryPoint
import com.example.xanggo.data.model.VietnamFuelData
import com.example.xanggo.data.model.WorldFuelSummary
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

interface FuelRepository {
    fun getVietnamFuelPrices(): Flow<VietnamFuelData>
    fun getWorldFuelSummary(): Flow<WorldFuelSummary>
}

/**
 * Hệ thống lấy giá xăng dầu tự động chống lỗi 4 tầng (4-Layer Failover Architecture):
 * - Tầng 1 (0.01s): Bộ nhớ đệm (Memory Cache) hoặc dữ liệu Fallback chuẩn kỳ điều hành gần nhất.
 * - Tầng 2 (GitHub Raw CDN): Đọc trực tiếp file JSON được GitHub Actions tự động cào & commit mỗi ngày.
 * - Tầng 3 (Firebase Cloud Firestore): Lắng nghe realtime push; tự động so sánh timestamp để luôn giữ bản mới nhất.
 * - Tầng 4 (Direct Live Web Scraper): Nếu cả CDN lẫn Firestore đều lỗi mạng, app tự bóc tách trực tiếp từ cổng dữ liệu web.
 */
class DefaultFuelRepository : FuelRepository {

    companion object {
        private const val GITHUB_VN_URL =
            "https://raw.githubusercontent.com/mrkienls/xanggo-android/main/backend/output/vietnam_fuel.json"
        private const val GITHUB_WORLD_URL =
            "https://raw.githubusercontent.com/mrkienls/xanggo-android/main/backend/output/world_fuel.json"
        private const val LIVE_SOURCE_1_URL = "https://vietnambiz.vn/gia-xang-dau-hom-nay.html"
        private const val LIVE_SOURCE_2_URL = "https://webgia.com/gia-xang-dau/petrolimex/"

        @Volatile
        private var cachedVnData: VietnamFuelData? = null

        @Volatile
        private var cachedVnEpoch: Long = 0L

        @Volatile
        private var cachedWorldData: WorldFuelSummary? = null

        @Volatile
        private var cachedWorldEpoch: Long = 0L
    }

    private val db = try {
        Firebase.firestore
    } catch (e: Exception) {
        null
    }

    override fun getVietnamFuelPrices(): Flow<VietnamFuelData> = callbackFlow {
        val defaultData = getFallbackVietnamData()
        // Tầng 1: Phát ngay lập tức trong 0.01s
        trySend(cachedVnData ?: defaultData)

        // Tầng 2 & Tầng 4: Tải từ GitHub Raw CDN (nếu lỗi thì tự cào trực tiếp từ nguồn web)
        launch(Dispatchers.IO) {
            val cdnResult = fetchVietnamFromGitHubCdn(defaultData)
            if (cdnResult != null) {
                val (data, epoch) = cdnResult
                if (epoch >= cachedVnEpoch) {
                    cachedVnEpoch = epoch
                    cachedVnData = data
                    trySend(data)
                }
            } else if (cachedVnData == null) {
                // Tầng 4: Tự cào trực tiếp trên thiết bị nếu CDN không phản hồi
                val scraped = scrapeVietnamDirectFallback(defaultData)
                if (scraped != null) {
                    cachedVnData = scraped
                    trySend(scraped)
                }
            }
        }

        // Tầng 3: Lắng nghe cập nhật Realtime từ Cloud Firestore
        if (db != null) {
            val listener = db.collection("vietnam_fuel").document("current")
                .addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null && snapshot.exists()) {
                        try {
                            val rawEpoch = (snapshot.get("updated_epoch") as? Number)?.toLong() ?: 0L
                            val rawUpdatedAt = snapshot.getString("updated_at") ?: defaultData.updatedAt
                            val docEpoch = if (rawEpoch > 0L) rawEpoch else parseEpochFromTimestamp(rawUpdatedAt)

                            // Không cho phép bản cũ trên Firestore ghi đè bản mới hơn từ GitHub CDN
                            if (docEpoch > 0L && cachedVnEpoch > 0L && docEpoch < cachedVnEpoch) {
                                return@addSnapshotListener
                            }

                            val updatedAt = formatCleanTimestamp(rawUpdatedAt)
                            val title = snapshot.getString("announcement_title") ?: defaultData.announcementTitle
                            val source = snapshot.getString("source") ?: defaultData.source
                            val rawItems = snapshot.get("items") as? List<Map<String, Any>> ?: emptyList()
                            val rawHistory = snapshot.get("history") as? List<Map<String, Any>> ?: emptyList()

                            if (rawItems.isNotEmpty()) {
                                val parsedItems = rawItems.map { map ->
                                    FuelItem(
                                        id = (map["id"] as? String ?: "").replace(",", ""),
                                        name = (map["name"] as? String ?: "").replace(",", "."),
                                        zone1Price = (map["zone_1"] as? Number)?.toInt() ?: 0,
                                        zone2Price = (map["zone_2"] as? Number)?.toInt() ?: 0,
                                        delta = (map["delta"] as? Number)?.toInt() ?: 0,
                                        unit = map["unit"] as? String ?: "VND/lít",
                                        badgeColorHex = map["badge_color"] as? String ?: "#0D9488",
                                        isFeatured = map["is_featured"] as? Boolean ?: false,
                                        description = map["description"] as? String ?: ""
                                    )
                                }

                                val parsedHistory = if (rawHistory.isNotEmpty()) {
                                    rawHistory.map { map ->
                                        PriceHistoryPoint(
                                            date = map["date"] as? String ?: "",
                                            fullDate = map["full_date"] as? String ?: "",
                                            ron95Price = (map["ron95_price"] as? Number)?.toInt() ?: 0,
                                            e5Price = (map["e5_price"] as? Number)?.toInt() ?: 0,
                                            dieselPrice = (map["diesel_price"] as? Number)?.toInt() ?: 0,
                                            deltaRon95 = (map["delta_ron95"] as? Number)?.toInt() ?: 0
                                        )
                                    }
                                } else {
                                    defaultData.history
                                }

                                val data = VietnamFuelData(updatedAt, title, source, parsedItems, parsedHistory)
                                if (docEpoch >= cachedVnEpoch) {
                                    cachedVnEpoch = docEpoch
                                    cachedVnData = data
                                }
                                trySend(data)
                            }
                        } catch (_: Exception) {
                            // Giữ nguyên dữ liệu hiện tại nếu lỗi format
                        }
                    }
                }
            awaitClose { listener.remove() }
        } else {
            awaitClose { }
        }
    }

    override fun getWorldFuelSummary(): Flow<WorldFuelSummary> = callbackFlow {
        val defaultData = getFallbackWorldData()
        trySend(cachedWorldData ?: defaultData)

        // Tầng 2: Tải dữ liệu 170 quốc gia từ GitHub Raw CDN
        launch(Dispatchers.IO) {
            val cdnResult = fetchWorldFromGitHubCdn(defaultData)
            if (cdnResult != null) {
                val (data, epoch) = cdnResult
                if (epoch >= cachedWorldEpoch) {
                    cachedWorldEpoch = epoch
                    cachedWorldData = data
                    trySend(data)
                }
            }
        }

        // Tầng 3: Lắng nghe từ Cloud Firestore
        if (db != null) {
            val listener = db.collection("world_fuel").document("summary")
                .addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null && snapshot.exists()) {
                        try {
                            val rawEpoch = (snapshot.get("updated_epoch") as? Number)?.toLong() ?: 0L
                            val rawUpdatedAt = snapshot.getString("updated_at") ?: defaultData.updatedAt
                            val docEpoch = if (rawEpoch > 0L) rawEpoch else parseEpochFromTimestamp(rawUpdatedAt)

                            if (docEpoch > 0L && cachedWorldEpoch > 0L && docEpoch < cachedWorldEpoch) {
                                return@addSnapshotListener
                            }

                            val updatedAt = formatCleanTimestamp(rawUpdatedAt)
                            val rate = (snapshot.get("usd_to_vnd_rate") as? Number)?.toDouble() ?: defaultData.usdToVndRate
                            val total = (snapshot.get("total_countries") as? Number)?.toInt() ?: defaultData.totalCountries
                            val vnRank = (snapshot.get("vn_rank") as? Number)?.toInt() ?: defaultData.vnRank
                            val rawCountries = snapshot.get("countries") as? List<Map<String, Any>> ?: emptyList()

                            if (rawCountries.isNotEmpty()) {
                                val parsedCountries = rawCountries.map { map ->
                                    CountryFuel(
                                        code = map["code"] as? String ?: "",
                                        name = map["name"] as? String ?: "",
                                        flag = map["flag"] as? String ?: "🏳️",
                                        region = map["region"] as? String ?: "Global",
                                        priceUsd = (map["price_usd"] as? Number)?.toDouble() ?: 0.0,
                                        priceVnd = (map["price_vnd"] as? Number)?.toInt() ?: 0,
                                        dailyIncomeUsd = (map["daily_income_usd"] as? Number)?.toDouble() ?: 10.0,
                                        affordabilityPercent = (map["affordability_percent"] as? Number)?.toDouble() ?: 0.0,
                                        isCurrent = map["is_current"] as? Boolean ?: false,
                                        note = map["note"] as? String ?: ""
                                    )
                                }
                                val data = WorldFuelSummary(updatedAt, rate, total, vnRank, parsedCountries)
                                if (docEpoch >= cachedWorldEpoch) {
                                    cachedWorldEpoch = docEpoch
                                    cachedWorldData = data
                                }
                                trySend(data)
                            }
                        } catch (_: Exception) {
                            // Giữ dữ liệu hiện tại
                        }
                    }
                }
            awaitClose { listener.remove() }
        } else {
            awaitClose { }
        }
    }

    // -------------------------------------------------------------------------
    // TẦNG 2: GITHUB RAW CDN PARSER
    // -------------------------------------------------------------------------
    private suspend fun fetchVietnamFromGitHubCdn(defaultData: VietnamFuelData): Pair<VietnamFuelData, Long>? =
        withContext(Dispatchers.IO) {
            try {
                val jsonStr = httpGet("$GITHUB_VN_URL?t=${System.currentTimeMillis() / 60000}") ?: return@withContext null
                val root = JSONObject(jsonStr)
                val rawUpdatedAt = root.optString("updated_at", defaultData.updatedAt)
                val epoch = root.optLong("updated_epoch", parseEpochFromTimestamp(rawUpdatedAt))
                val updatedAt = formatCleanTimestamp(rawUpdatedAt)
                val title = root.optString("announcement_title", defaultData.announcementTitle)
                val source = root.optString("source", defaultData.source)

                val itemsArr = root.optJSONArray("items") ?: JSONArray()
                if (itemsArr.length() == 0) return@withContext null

                val items = mutableListOf<FuelItem>()
                for (i in 0 until itemsArr.length()) {
                    val obj = itemsArr.getJSONObject(i)
                    items.add(
                        FuelItem(
                            id = obj.optString("id", "").replace(",", ""),
                            name = obj.optString("name", "").replace(",", "."),
                            zone1Price = obj.optInt("zone_1", 0),
                            zone2Price = obj.optInt("zone_2", 0),
                            delta = obj.optInt("delta", 0),
                            unit = obj.optString("unit", "VND/lít"),
                            badgeColorHex = obj.optString("badge_color", "#0D9488"),
                            isFeatured = obj.optBoolean("is_featured", false),
                            description = obj.optString("description", "")
                        )
                    )
                }

                val histArr = root.optJSONArray("history")
                val history = if (histArr != null && histArr.length() > 0) {
                    val list = mutableListOf<PriceHistoryPoint>()
                    for (i in 0 until histArr.length()) {
                        val h = histArr.getJSONObject(i)
                        list.add(
                            PriceHistoryPoint(
                                date = h.optString("date", ""),
                                fullDate = h.optString("full_date", ""),
                                ron95Price = h.optInt("ron95_price", 0),
                                e5Price = h.optInt("e5_price", 0),
                                dieselPrice = h.optInt("diesel_price", 0),
                                deltaRon95 = h.optInt("delta_ron95", 0)
                            )
                        )
                    }
                    list
                } else {
                    defaultData.history
                }

                Pair(VietnamFuelData(updatedAt, title, source, items, history), epoch)
            } catch (_: Exception) {
                null
            }
        }

    private suspend fun fetchWorldFromGitHubCdn(defaultData: WorldFuelSummary): Pair<WorldFuelSummary, Long>? =
        withContext(Dispatchers.IO) {
            try {
                val jsonStr = httpGet("$GITHUB_WORLD_URL?t=${System.currentTimeMillis() / 60000}") ?: return@withContext null
                val root = JSONObject(jsonStr)
                val rawUpdatedAt = root.optString("updated_at", defaultData.updatedAt)
                val epoch = root.optLong("updated_epoch", parseEpochFromTimestamp(rawUpdatedAt))
                val updatedAt = formatCleanTimestamp(rawUpdatedAt)
                val rate = root.optDouble("usd_to_vnd_rate", defaultData.usdToVndRate)
                val total = root.optInt("total_countries", defaultData.totalCountries)
                val vnRank = root.optInt("vn_rank", defaultData.vnRank)

                val arr = root.optJSONArray("countries") ?: JSONArray()
                if (arr.length() == 0) return@withContext null

                val countries = mutableListOf<CountryFuel>()
                for (i in 0 until arr.length()) {
                    val c = arr.getJSONObject(i)
                    countries.add(
                        CountryFuel(
                            code = c.optString("code", ""),
                            name = c.optString("name", ""),
                            flag = c.optString("flag", "🏳️"),
                            region = c.optString("region", "Global"),
                            priceUsd = c.optDouble("price_usd", 0.0),
                            priceVnd = c.optInt("price_vnd", 0),
                            dailyIncomeUsd = c.optDouble("daily_income_usd", 10.0),
                            affordabilityPercent = c.optDouble("affordability_percent", 0.0),
                            isCurrent = c.optBoolean("is_current", false),
                            note = c.optString("note", "")
                        )
                    )
                }
                Pair(WorldFuelSummary(updatedAt, rate, total, vnRank, countries), epoch)
            } catch (_: Exception) {
                null
            }
        }

    // -------------------------------------------------------------------------
    // TẦNG 4: DIRECT LIVE WEB SCRAPER FALLBACK TRÊN THIẾT BỊ ANDROID
    // -------------------------------------------------------------------------
    private suspend fun scrapeVietnamDirectFallback(defaultData: VietnamFuelData): VietnamFuelData? =
        withContext(Dispatchers.IO) {
            try {
                // Thử Nguồn 1 (VietnamBiz) hoặc Nguồn 2 (WebGia Petrolimex) bằng Regex nhẹ
                val html = httpGet(LIVE_SOURCE_1_URL) ?: httpGet(LIVE_SOURCE_2_URL) ?: return@withContext null
                val trRegex = Regex("<tr[^>]*>(.*?)</tr>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
                val tdRegex = Regex("<t[dh][^>]*>(.*?)</t[dh]>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
                val tagStrip = Regex("<[^>]+>")

                val items = mutableListOf<FuelItem>()
                for (rowMatch in trRegex.findAll(html)) {
                    val rowHtml = rowMatch.groupValues[1]
                    val cols = tdRegex.findAll(rowHtml)
                        .map { tagStrip.replace(it.groupValues[1], "").replace("&nbsp;", " ").trim() }
                        .toList()

                    if (cols.size >= 3) {
                        // Kiểm tra cấu trúc 4 cột (STT, Mặt hàng, Giá V1, Biến động) hoặc 3 cột (Sản phẩm, V1, V2)
                        val isFourCol = cols.size >= 4 && cols[2].any { it.isDigit() }
                        val rawName = if (isFourCol) cols[1] else cols[0]
                        val priceCol1 = if (isFourCol) cols[2] else cols[1]
                        val zone1 = priceCol1.filter { it.isDigit() }.toIntOrNull() ?: continue
                        if (zone1 !in 10000..60000) continue

                        val zone2 = if (!isFourCol) {
                            cols[2].filter { it.isDigit() }.toIntOrNull() ?: ((zone1 * 102 + 50) / 100 / 10 * 10)
                        } else {
                            Math.round(zone1 * 1.02 / 10.0).toInt() * 10
                        }

                        val delta = if (isFourCol) {
                            val rawDelta = cols[3].replace("đ", "").replace(".", "").replace(",", "").trim()
                            rawDelta.toIntOrNull() ?: 0
                        } else {
                            0
                        }

                        val cleanName = rawName.replace(",", ".")
                        val id = cleanName.lowercase()
                            .replace("xăng", "").replace("dầu", "").trim()
                            .replace(" ", "_").replace("-", "_").replace(".", "")

                        val badgeColor = when {
                            "95-v" in cleanName.lowercase() -> "#D97706"
                            "95" in cleanName.lowercase() -> "#F97316"
                            "e5" in cleanName.lowercase() || "92" in cleanName.lowercase() -> "#16A34A"
                            else -> "#475569"
                        }

                        items.add(
                            FuelItem(
                                id = id,
                                name = cleanName,
                                zone1Price = zone1,
                                zone2Price = zone2,
                                delta = delta,
                                badgeColorHex = badgeColor,
                                isFeatured = "95-iii" in cleanName.lowercase() || "mức 3" in cleanName.lowercase()
                            )
                        )
                    }
                }

                if (items.size >= 3) {
                    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
                    }
                    val nowStr = sdf.format(Date())
                    VietnamFuelData(
                        updatedAt = nowStr,
                        announcementTitle = "Bảng giá xăng dầu ngày ${nowStr.substringBefore(" ")}",
                        source = defaultData.source,
                        items = items,
                        history = defaultData.history
                    )
                } else null
            } catch (_: Exception) {
                null
            }
        }

    private fun httpGet(urlStr: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                )
            }
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else null
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    /** Chuyển đổi chuỗi ISO (`2026-10-15T20:19:50...`) thành dạng gọn đẹp `15/10/2026 20:19`. */
    private fun formatCleanTimestamp(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.contains("T") && trimmed.length >= 16) {
            // Ví dụ: "2026-10-15T20:19:50.561985+07:00"
            val datePart = trimmed.substring(0, 10).split("-")
            val timePart = trimmed.substring(11, 16)
            if (datePart.size == 3) {
                return "${datePart[2]}/${datePart[1]}/${datePart[0]} $timePart"
            }
        }
        return trimmed
    }

    /** Chuyển chuỗi thời gian sang Unix epoch (giây) để so sánh bản nào mới hơn. */
    private fun parseEpochFromTimestamp(raw: String): Long {
        val trimmed = raw.trim()
        return try {
            if (trimmed.contains("T") && trimmed.length >= 16) {
                val clean = trimmed.substring(0, 16).replace("T", " ")
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
                }
                (sdf.parse(clean)?.time ?: 0L) / 1000L
            } else if (trimmed.contains("/")) {
                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
                }
                (sdf.parse(trimmed)?.time ?: 0L) / 1000L
            } else 0L
        } catch (_: Exception) {
            0L
        }
    }

    private fun getFallbackVietnamData(): VietnamFuelData {
        return VietnamFuelData(
            updatedAt = "15/10/2026 15:00",
            announcementTitle = "Bảng giá xăng dầu ngày 15/10/2026",
            source = "Kỳ điều hành Liên Bộ Công Thương - Tài chính (Petrolimex)",
            items = listOf(
                FuelItem("e10_ron_95_iii", "Xăng E10 RON 95-III", 28250, 28820, 1070, badgeColorHex = "#F97316", isFeatured = true),
                FuelItem("e5_ron_92_ii", "Xăng E5 RON 92-II", 27700, 28250, 1140, badgeColorHex = "#16A34A", isFeatured = false),
                FuelItem("do_005s_ii", "Dầu DO 0.05S-II", 29120, 29700, -590, badgeColorHex = "#475569", isFeatured = false),
                FuelItem("do_0001s_v", "Dầu DO 0.001S-V", 30520, 31130, -590, badgeColorHex = "#334155", isFeatured = false)
            ),
            history = listOf(
                PriceHistoryPoint("08/10", "08/10/2026", 28250, 27700, 29120, 1070)
            )
        )
    }

    private fun getFallbackWorldData(): WorldFuelSummary {
        return WorldFuelSummary(
            updatedAt = "Tuần này",
            usdToVndRate = 25890.0,
            totalCountries = 170,
            vnRank = 30,
            countries = listOf(
                CountryFuel("VE", "Venezuela", "🇻🇪", "Global", 0.035, 905, 6.0, 0.58, note = "Trợ cấp 0đ"),
                CountryFuel("MY", "Malaysia", "🇲🇾", "ASEAN", 0.44, 11382, 33.0, 1.33, note = "Trợ giá xăng"),
                CountryFuel("ID", "Indonesia", "🇮🇩", "ASEAN", 0.79, 20436, 13.5, 5.85),
                CountryFuel("US", "Hoa Kỳ", "🇺🇸", "Global", 0.95, 24575, 215.0, 0.44),
                CountryFuel("VN", "Việt Nam", "🇻🇳", "ASEAN", 1.09, 28250, 12.8, 8.52, isCurrent = true, note = "Kỳ điều hành hôm nay"),
                CountryFuel("PH", "Philippines", "🇵🇭", "ASEAN", 1.10, 28455, 11.2, 9.82),
                CountryFuel("TH", "Thái Lan", "🇹🇭", "ASEAN", 1.15, 29748, 20.5, 5.61),
                CountryFuel("KH", "Campuchia", "🇰🇭", "ASEAN", 1.22, 31559, 5.8, 21.03),
                CountryFuel("LA", "Lào", "🇱🇦", "ASEAN", 1.35, 34922, 5.2, 25.96, note = "Nhập khẩu 100%"),
                CountryFuel("SG", "Singapore", "🇸🇬", "ASEAN", 2.15, 55617, 230.0, 0.93, note = "Thuế môi trường cao"),
                CountryFuel("HK", "Hong Kong", "🇭🇰", "Global", 3.25, 84072, 145.0, 2.24, note = "Đắt nhất thế giới")
            )
        )
    }
}
