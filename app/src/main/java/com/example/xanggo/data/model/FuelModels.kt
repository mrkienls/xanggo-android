package com.example.xanggo.data.model

data class FuelItem(
    val id: String,
    val name: String,
    val zone1Price: Int,
    val zone2Price: Int,
    val delta: Int = 0, // Mức tăng (+), giảm (-) so với kỳ trước
    val unit: String = "VND/lít",
    val badgeColorHex: String = "#0D9488",
    val isFeatured: Boolean = false,
    val description: String = ""
) {
    fun getPrice(zone: Int): Int = if (zone == 2) zone2Price else zone1Price
}

data class VietnamFuelData(
    val updatedAt: String,
    val announcementTitle: String,
    val source: String,
    val items: List<FuelItem>
)

data class CountryFuel(
    val code: String,
    val name: String,
    val flag: String,
    val region: String, // "ASEAN" hoặc "Global"
    val priceUsd: Double,
    val priceVnd: Int,
    val dailyIncomeUsd: Double,
    val affordabilityPercent: Double, // % thu nhập bình quân ngày để mua 1L xăng
    val isCurrent: Boolean = false,
    val note: String = ""
)

data class WorldFuelSummary(
    val updatedAt: String,
    val usdToVndRate: Double,
    val totalCountries: Int,
    val vnRank: Int,
    val countries: List<CountryFuel>
)
