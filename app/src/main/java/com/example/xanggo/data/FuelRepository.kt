package com.example.xanggo.data

import com.example.xanggo.data.model.CountryFuel
import com.example.xanggo.data.model.FuelItem
import com.example.xanggo.data.model.PriceHistoryPoint
import com.example.xanggo.data.model.VietnamFuelData
import com.example.xanggo.data.model.WorldFuelSummary
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

interface FuelRepository {
    fun getVietnamFuelPrices(): Flow<VietnamFuelData>
    fun getWorldFuelSummary(): Flow<WorldFuelSummary>
}

class DefaultFuelRepository : FuelRepository {

    private val db = try {
        Firebase.firestore
    } catch (e: Exception) {
        null
    }

    override fun getVietnamFuelPrices(): Flow<VietnamFuelData> = callbackFlow {
        // 1. Luôn phát dữ liệu offline cache trước để màn hình load tức thì trong 0.05s
        val defaultData = getFallbackVietnamData()
        trySend(defaultData)

        // 2. Lắng nghe cập nhật realtime từ Cloud Firestore
        if (db != null) {
            val listener = db.collection("vietnam_fuel").document("current")
                .addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null && snapshot.exists()) {
                        try {
                            val updatedAt = snapshot.getString("updated_at") ?: defaultData.updatedAt
                            val title = snapshot.getString("announcement_title") ?: defaultData.announcementTitle
                            val source = snapshot.getString("source") ?: defaultData.source
                            val rawItems = snapshot.get("items") as? List<Map<String, Any>> ?: emptyList()
                            val rawHistory = snapshot.get("history") as? List<Map<String, Any>> ?: emptyList()

                            if (rawItems.isNotEmpty()) {
                                val parsedItems = rawItems.map { map ->
                                    FuelItem(
                                        id = map["id"] as? String ?: "",
                                        name = map["name"] as? String ?: "",
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

                                trySend(VietnamFuelData(updatedAt, title, source, parsedItems, parsedHistory))
                            }
                        } catch (e: Exception) {
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
        trySend(defaultData)

        if (db != null) {
            val listener = db.collection("world_fuel").document("summary")
                .addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null && snapshot.exists()) {
                        try {
                            val updatedAt = snapshot.getString("updated_at") ?: defaultData.updatedAt
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
                                trySend(WorldFuelSummary(updatedAt, rate, total, vnRank, parsedCountries))
                            }
                        } catch (e: Exception) {
                            // Giữ dữ liệu hiện tại
                        }
                    }
                }
            awaitClose { listener.remove() }
        } else {
            awaitClose { }
        }
    }

    private fun getFallbackVietnamData(): VietnamFuelData {
        return VietnamFuelData(
            updatedAt = "08/10/2026 15:00",
            announcementTitle = "Kỳ điều hành ngày 08/10/2026",
            source = "Kỳ điều hành Liên Bộ Công Thương - Tài chính (Petrolimex)",
            items = listOf(
                FuelItem("e10_ron_95_iii", "Xăng E10 RON 95-III", 28250, 28820, 1070, badgeColorHex = "#F97316", isFeatured = true),
                FuelItem("e5_ron_92_ii", "Xăng E5 RON 92-II", 27700, 28250, 1140, badgeColorHex = "#16A34A", isFeatured = false),
                FuelItem("do_005s_ii", "Dầu DO 0.05S-II", 29120, 29700, -590, badgeColorHex = "#64748B", isFeatured = false),
                FuelItem("do_0001s_v", "Dầu DO 0.001S-V", 30520, 31130, -590, badgeColorHex = "#334155", isFeatured = false)
            ),
            // Chỉ giữ kỳ hiện tại; lịch sử thật do crawler tích luỹ dần theo từng kỳ điều hành
            history = listOf(
                PriceHistoryPoint("08/10", "08/10/2026", 28250, 27700, 29120, 1070)
            )
        )
    }

    private fun getFallbackWorldData(): WorldFuelSummary {
        return WorldFuelSummary(
            updatedAt = "Tuần này",
            usdToVndRate = 25863.0,
            totalCountries = 170,
            vnRank = 30,
            countries = listOf(
                CountryFuel("VE", "Venezuela", "🇻🇪", "Global", 0.035, 905, 6.0, 0.58, note = "Trợ cấp 0đ"),
                CountryFuel("MY", "Malaysia", "🇲🇾", "ASEAN", 0.44, 11382, 33.0, 1.33, note = "Trợ giá xăng"),
                CountryFuel("ID", "Indonesia", "🇮🇩", "ASEAN", 0.79, 20436, 13.5, 5.85),
                CountryFuel("US", "Hoa Kỳ", "🇺🇸", "Global", 0.95, 24575, 215.0, 0.44),
                CountryFuel("VN", "Việt Nam", "🇻🇳", "ASEAN", 1.09, 28250, 12.8, 8.52, isCurrent = true, note = "Kỳ điều hành ngày 08/10"),
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
