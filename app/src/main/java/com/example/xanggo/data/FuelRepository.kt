package com.example.xanggo.data

import com.example.xanggo.data.model.CountryFuel
import com.example.xanggo.data.model.FuelItem
import com.example.xanggo.data.model.VietnamFuelData
import com.example.xanggo.data.model.WorldFuelSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface FuelRepository {
    fun getVietnamFuelPrices(): Flow<VietnamFuelData>
    fun getWorldFuelSummary(): Flow<WorldFuelSummary>
}

class DefaultFuelRepository : FuelRepository {

    override fun getVietnamFuelPrices(): Flow<VietnamFuelData> = flow {
        // Đồng bộ 100% với dữ liệu crawler thực tế
        emit(
            VietnamFuelData(
                updatedAt = "08/10/2026 15:00",
                announcementTitle = "Kỳ điều hành ngày 08/10/2026",
                source = "Kỳ điều hành Liên Bộ Công Thương - Tài chính (Petrolimex)",
                items = listOf(
                    FuelItem(
                        id = "e10_ron_95_iii",
                        name = "Xăng E10 RON 95-III",
                        zone1Price = 28250,
                        zone2Price = 28820,
                        delta = 1070,
                        badgeColorHex = "#F97316",
                        isFeatured = true,
                        description = "Nhiên liệu phổ biến nhất cho xe máy & ô tô"
                    ),
                    FuelItem(
                        id = "e5_ron_92_ii",
                        name = "Xăng E5 RON 92-II",
                        zone1Price = 27700,
                        zone2Price = 28250,
                        delta = 1140,
                        badgeColorHex = "#16A34A",
                        isFeatured = false,
                        description = "Xăng sinh học bảo vệ môi trường"
                    ),
                    FuelItem(
                        id = "do_005s_ii",
                        name = "Dầu DO 0.05S-II",
                        zone1Price = 29120,
                        zone2Price = 29700,
                        delta = -590,
                        badgeColorHex = "#64748B",
                        isFeatured = false,
                        description = "Dầu Diesel tiêu chuẩn"
                    ),
                    FuelItem(
                        id = "do_0001s_v",
                        name = "Dầu DO 0.001S-V",
                        zone1Price = 30520,
                        zone2Price = 31130,
                        delta = -590,
                        badgeColorHex = "#334155",
                        isFeatured = false,
                        description = "Dầu Diesel cao cấp Euro 5"
                    )
                )
            )
        )
    }

    override fun getWorldFuelSummary(): Flow<WorldFuelSummary> = flow {
        emit(
            WorldFuelSummary(
                updatedAt = "Tuần này",
                usdToVndRate = 25868.0,
                totalCountries = 14,
                vnRank = 5,
                countries = listOf(
                    CountryFuel(
                        code = "VE",
                        name = "Venezuela",
                        flag = "🇻🇪",
                        region = "Global",
                        priceUsd = 0.035,
                        priceVnd = 905,
                        dailyIncomeUsd = 6.0,
                        affordabilityPercent = 0.58,
                        note = "Trợ cấp dầu mỏ gần như 0đ"
                    ),
                    CountryFuel(
                        code = "MY",
                        name = "Malaysia",
                        flag = "🇲🇾",
                        region = "ASEAN",
                        priceUsd = 0.44,
                        priceVnd = 11382,
                        dailyIncomeUsd = 33.0,
                        affordabilityPercent = 1.33,
                        note = "Chính phủ trợ giá xăng"
                    ),
                    CountryFuel(
                        code = "ID",
                        name = "Indonesia",
                        flag = "🇮🇩",
                        region = "ASEAN",
                        priceUsd = 0.79,
                        priceVnd = 20436,
                        dailyIncomeUsd = 13.5,
                        affordabilityPercent = 5.85
                    ),
                    CountryFuel(
                        code = "US",
                        name = "Hoa Kỳ",
                        flag = "🇺🇸",
                        region = "Global",
                        priceUsd = 0.95,
                        priceVnd = 24575,
                        dailyIncomeUsd = 215.0,
                        affordabilityPercent = 0.44
                    ),
                    CountryFuel(
                        code = "VN",
                        name = "Việt Nam",
                        flag = "🇻🇳",
                        region = "ASEAN",
                        priceUsd = 1.09,
                        priceVnd = 28250,
                        dailyIncomeUsd = 12.8,
                        affordabilityPercent = 8.52,
                        isCurrent = true,
                        note = "Kỳ điều hành ngày 08/10"
                    ),
                    CountryFuel(
                        code = "PH",
                        name = "Philippines",
                        flag = "🇵🇭",
                        region = "ASEAN",
                        priceUsd = 1.10,
                        priceVnd = 28455,
                        dailyIncomeUsd = 11.2,
                        affordabilityPercent = 9.82
                    ),
                    CountryFuel(
                        code = "TH",
                        name = "Thái Lan",
                        flag = "🇹🇭",
                        region = "ASEAN",
                        priceUsd = 1.15,
                        priceVnd = 29748,
                        dailyIncomeUsd = 20.5,
                        affordabilityPercent = 5.61
                    ),
                    CountryFuel(
                        code = "KH",
                        name = "Campuchia",
                        flag = "🇰🇭",
                        region = "ASEAN",
                        priceUsd = 1.22,
                        priceVnd = 31559,
                        dailyIncomeUsd = 5.8,
                        affordabilityPercent = 21.03
                    ),
                    CountryFuel(
                        code = "LA",
                        name = "Lào",
                        flag = "🇱🇦",
                        region = "ASEAN",
                        priceUsd = 1.35,
                        priceVnd = 34922,
                        dailyIncomeUsd = 5.2,
                        affordabilityPercent = 25.96,
                        note = "Nhập khẩu 100%"
                    ),
                    CountryFuel(
                        code = "SG",
                        name = "Singapore",
                        flag = "🇸🇬",
                        region = "ASEAN",
                        priceUsd = 2.15,
                        priceVnd = 55617,
                        dailyIncomeUsd = 230.0,
                        affordabilityPercent = 0.93,
                        note = "Thuế bảo vệ môi trường cao"
                    ),
                    CountryFuel(
                        code = "HK",
                        name = "Hong Kong",
                        flag = "🇭🇰",
                        region = "Global",
                        priceUsd = 3.25,
                        priceVnd = 84072,
                        dailyIncomeUsd = 145.0,
                        affordabilityPercent = 2.24,
                        note = "Giá xăng đắt nhất thế giới"
                    )
                )
            )
        )
    }
}
