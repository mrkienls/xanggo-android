package com.example.xanggo.ui.stations

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.xanggo.data.NearbyStation
import com.example.xanggo.data.NearbyStationRepository
import com.example.xanggo.i18n.AppLang
import com.example.xanggo.i18n.LocalAppLanguage
import com.example.xanggo.i18n.tr
import kotlinx.coroutines.launch
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xanggo.theme.*
import com.example.xanggo.ui.components.LocationPinVector
import androidx.compose.ui.text.style.TextOverflow
import java.text.Normalizer
import java.util.regex.Pattern

/** Trạng thái tải danh sách cây xăng thật. */
private sealed interface NearbyState {
    object NeedPermission : NearbyState
    object Loading : NearbyState
    data class Loaded(val stations: List<NearbyStation>) : NearbyState
    data class Failed(val locationIssue: Boolean) : NearbyState
}

private data class BrandStyle(val label: String, val color: Color, val emoji: String)

private fun brandStyle(key: String, isEn: Boolean = false): BrandStyle = when (key) {
    "PETROLIMEX" -> BrandStyle("Petrolimex", Color(0xFFF97316), "🟠")
    "PVOIL" -> BrandStyle("PVOIL", Color(0xFF38BDF8), "🔵")
    "SAIGON_PETRO" -> BrandStyle("Saigon Petro", Color(0xFF10B981), "🟢")
    "IDEMITSU" -> BrandStyle("Idemitsu Q8", Color(0xFFEF4444), "🔴")
    else -> BrandStyle(if (isEn) "Gas Station" else "Cây xăng", Color(0xFF94A3B8), "⛽")
}

private fun formatDistance(meters: Int, isEn: Boolean = false): String =
    if (meters < 1000) "$meters m" else {
        val formatted = String.format(java.util.Locale.US, "%.1f km", meters / 1000.0)
        if (isEn) formatted else formatted.replace('.', ',')
    }

private fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

@Composable
fun GasStationScreen() {
    val context = LocalContext.current
    val lang = LocalAppLanguage.current
    val isEn = lang == AppLang.EN
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var selectedBrandFilter by remember { mutableStateOf("ALL") } // "ALL", "PETROLIMEX", "PVOIL", "SAIGON_PETRO", "IDEMITSU"

    var nearbyState by remember {
        mutableStateOf<NearbyState>(if (hasLocationPermission(context)) NearbyState.Loading else NearbyState.NeedPermission)
    }

    fun load(force: Boolean = false) {
        nearbyState = NearbyState.Loading
        scope.launch {
            nearbyState = try {
                NearbyState.Loaded(NearbyStationRepository.loadNearby(context, force))
            } catch (e: NearbyStationRepository.LocationUnavailableException) {
                NearbyState.Failed(locationIssue = true)
            } catch (e: Exception) {
                NearbyState.Failed(locationIssue = false)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.any { it }) load() else nearbyState = NearbyState.NeedPermission
    }

    val requestPermission = {
        permissionLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }

    LaunchedEffect(Unit) {
        if (hasLocationPermission(context)) load()
    }

    val allStations = (nearbyState as? NearbyState.Loaded)?.stations.orEmpty()
    val nearest = allStations.firstOrNull()

    val filteredStations = remember(allStations, searchQuery, selectedBrandFilter) {
        val query = removeVietnameseAccents(searchQuery)
        allStations.filter { station ->
            val matchBrand = selectedBrandFilter == "ALL" || station.brandKey == selectedBrandFilter
            val matchQuery = query.isEmpty() ||
                removeVietnameseAccents(station.name).contains(query) ||
                removeVietnameseAccents(brandStyle(station.brandKey, isEn).label).contains(query) ||
                removeVietnameseAccents(station.address).contains(query)
            matchBrand && matchQuery
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
        }

        // 1. Header: Biểu tượng Vị trí & Trạng thái GPS
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        PetrolTeal.copy(alpha = 0.25f),
                                        PetrolTeal.copy(alpha = 0.08f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = LocationPinVector,
                            contentDescription = null,
                            tint = PetrolTeal,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = tr("Tìm Trạm ", "Find Gas "),
                                color = TextPrimaryDark,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = tr("Xăng", "Stations"),
                                color = PetrolTeal,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                        Text(
                            text = tr("Trạm chính hãng gần bạn", "Official stations near you"),
                            color = TextSecondaryDark,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // 2. Ô tìm kiếm trạm xăng nhanh
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = tr("🔍 Quận, đường, tên cây xăng…", "🔍 District, street, station name…"),
                        color = TextSecondaryDark,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Text("✕", color = TextSecondaryDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    focusedBorderColor = PetrolTeal,
                    unfocusedBorderColor = DarkSurfaceVariant,
                    focusedTextColor = TextPrimaryDark,
                    unfocusedTextColor = TextPrimaryDark,
                    cursorColor = PetrolTeal
                )
            )
        }

        // 3. THẺ HERO RADAR LỚN
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, PetrolTeal.copy(alpha = 0.7f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            Color(0xFF14B8A6),
                                            Color(0xFF0D9488),
                                            Color(0xFF042F2E)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = LocationPinVector,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tr("Trạm gần bạn", "Stations Near You"),
                                color = TextPrimaryDark,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = tr("Chỉ đường 1 chạm", "1-tap GPS navigation"),
                                color = TextSecondaryDark,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Surface(
                            color = PetrolTeal,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.clickable { openMapsSearchDirect(context, "cây xăng gần đây") }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "🗺️", fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = tr("Bản đồ ›", "Map ›"),
                                    color = Color.Black,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Lưới 4 thương hiệu lớn tại Việt Nam dạng 2x2 Grid
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BrandOptionCard(
                                name = "Petrolimex",
                                subTag = tr("Phổ biến · QR", "Popular · QR Pay"),
                                logoEmoji = "🟠",
                                accentColor = Color(0xFFF97316),
                                isSelected = selectedBrandFilter == "PETROLIMEX",
                                onClick = {
                                    selectedBrandFilter = if (selectedBrandFilter == "PETROLIMEX") "ALL" else "PETROLIMEX"
                                },
                                modifier = Modifier.weight(1f)
                            )
                            BrandOptionCard(
                                name = "PVOIL",
                                subTag = "PVOIL Easy · QR",
                                logoEmoji = "🔵",
                                accentColor = Color(0xFF38BDF8),
                                isSelected = selectedBrandFilter == "PVOIL",
                                onClick = {
                                    selectedBrandFilter = if (selectedBrandFilter == "PVOIL") "ALL" else "PVOIL"
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BrandOptionCard(
                                name = "Saigon Petro",
                                subTag = tr("Miền Nam", "Southern VN"),
                                logoEmoji = "🟢",
                                accentColor = Color(0xFF10B981),
                                isSelected = selectedBrandFilter == "SAIGON_PETRO",
                                onClick = {
                                    selectedBrandFilter = if (selectedBrandFilter == "SAIGON_PETRO") "ALL" else "SAIGON_PETRO"
                                },
                                modifier = Modifier.weight(1f)
                            )
                            BrandOptionCard(
                                name = "Idemitsu Q8",
                                subTag = tr("Chuẩn Nhật · Thẻ", "Japanese · Card"),
                                logoEmoji = "🔴",
                                accentColor = Color(0xFFEF4444),
                                isSelected = selectedBrandFilter == "IDEMITSU",
                                onClick = {
                                    selectedBrandFilter = if (selectedBrandFilter == "IDEMITSU") "ALL" else "IDEMITSU"
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // KHỐI MINI RADAR PREVIEW & CHỈ ĐƯỜNG TRỰC TIẾP
                    Surface(
                        color = DarkBackground,
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant.copy(alpha = 0.8f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (nearest != null) openDirections(context, nearest.lat, nearest.lon)
                                else openMapsSearchDirect(context, "cây xăng gần đây nhất")
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 85.dp, height = 62.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0F172A)),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val w = size.width
                                    val h = size.height
                                    drawLine(
                                        color = Color(0xFF1E293B),
                                        start = Offset(0f, h * 0.4f),
                                        end = Offset(w, h * 0.5f),
                                        strokeWidth = 3f
                                    )
                                    drawLine(
                                        color = Color(0xFF1E293B),
                                        start = Offset(w * 0.35f, 0f),
                                        end = Offset(w * 0.4f, h),
                                        strokeWidth = 3f
                                    )
                                    drawLine(
                                        color = Color(0xFF1E293B),
                                        start = Offset(w * 0.7f, 0f),
                                        end = Offset(w * 0.65f, h),
                                        strokeWidth = 2f
                                    )

                                    drawCircle(
                                        color = PetrolTeal.copy(alpha = 0.25f),
                                        radius = 20f,
                                        center = Offset(w * 0.48f, h * 0.52f)
                                    )
                                    drawCircle(
                                        color = Color(0xFF38BDF8),
                                        radius = 7f,
                                        center = Offset(w * 0.48f, h * 0.52f)
                                    )
                                    drawCircle(
                                        color = Color.White,
                                        radius = 3f,
                                        center = Offset(w * 0.48f, h * 0.52f)
                                    )

                                    drawCircle(color = PetrolTeal, radius = 5f, center = Offset(w * 0.22f, h * 0.3f))
                                    drawCircle(color = PetrolTeal, radius = 5f, center = Offset(w * 0.78f, h * 0.35f))
                                    drawCircle(color = PetrolTeal, radius = 5f, center = Offset(w * 0.82f, h * 0.72f))
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = LocationPinVector,
                                        contentDescription = null,
                                        tint = PetrolTeal,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = nearest?.let { "${it.name} · ${formatDistance(it.distanceMeters, isEn)}" }
                                            ?: tr("Cây xăng gần nhất", "Nearest gas station"),
                                        color = TextPrimaryDark,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "🧭", fontSize = 11.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = tr("Mở Google Maps", "Open Google Maps"),
                                        color = PetrolTeal,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Text(
                                text = "›",
                                color = PetrolTeal,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 4. DANH SÁCH TRẠM XĂNG THẬT QUANH BẠN (OpenStreetMap)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "📍", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (nearbyState is NearbyState.Loaded)
                            tr("Gần bạn (${filteredStations.size})", "Nearby (${filteredStations.size})")
                        else
                            tr("Gần bạn", "Nearby"),
                        color = TextPrimaryDark,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (nearbyState is NearbyState.Loaded) {
                    Text(
                        text = tr("↻ Làm mới", "↻ Refresh"),
                        color = PetrolTeal,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.clickable { load(force = true) }
                    )
                }
            }
        }

        when (val s = nearbyState) {
            NearbyState.NeedPermission -> item {
                NearbyInfoCard(
                    emoji = "📍",
                    title = tr("Bật vị trí để xem cây xăng thật quanh bạn", "Enable location to see nearby gas stations"),
                    message = tr(
                        "Vị trí chỉ dùng để tìm trạm quanh bạn, XăngGo không lưu lại.",
                        "Your location is only used to find nearby stations and is never stored."
                    ),
                    actionLabel = tr("Cho phép vị trí", "Allow Location"),
                    onAction = requestPermission
                )
            }

            NearbyState.Loading -> item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            color = PetrolTeal,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            tr("Đang tìm cây xăng quanh bạn…", "Finding gas stations near you…"),
                            color = TextSecondaryDark,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            is NearbyState.Failed -> item {
                NearbyInfoCard(
                    emoji = if (s.locationIssue) "📡" else "⚠️",
                    title = if (s.locationIssue)
                        tr("Chưa lấy được vị trí", "Location unavailable")
                    else
                        tr("Không tải được danh sách", "Could not load stations"),
                    message = if (s.locationIssue)
                        tr("Hãy bật Định vị (GPS) trên máy rồi thử lại.", "Please enable GPS on your device and try again.")
                    else
                        tr("Kiểm tra kết nối mạng rồi thử lại, hoặc mở Google Maps.", "Check your internet connection and retry, or open Google Maps."),
                    actionLabel = tr("Thử lại", "Retry"),
                    onAction = { load(force = true) },
                    secondaryLabel = tr("Mở Google Maps", "Open Google Maps"),
                    onSecondary = { openMapsSearchDirect(context, "cây xăng gần đây") }
                )
            }

            is NearbyState.Loaded -> {
                if (filteredStations.isEmpty()) {
                    item {
                        NearbyInfoCard(
                            emoji = "🔍",
                            title = if (s.stations.isEmpty())
                                tr("Chưa có dữ liệu cây xăng trong 5 km", "No station data within 5 km")
                            else
                                tr("Không có trạm khớp bộ lọc", "No stations match your filter"),
                            message = tr("Bạn có thể tìm trực tiếp trên Google Maps.", "You can search directly on Google Maps."),
                            actionLabel = tr("Mở Google Maps", "Open Google Maps"),
                            onAction = {
                                val brand = if (selectedBrandFilter == "ALL") "" else brandStyle(selectedBrandFilter, isEn).label + " "
                                openMapsSearchDirect(context, "cây xăng ${brand}gần đây")
                            }
                        )
                    }
                } else {
                    items(filteredStations, key = { it.id }) { station ->
                        StationDetailCard(
                            station = station,
                            isEn = isEn,
                            onDirectClick = { openDirections(context, station.lat, station.lon) },
                            onShareClick = { shareStationLocation(context, station, isEn) }
                        )
                    }
                    item {
                        Text(
                            text = tr("Dữ liệu trạm © OpenStreetMap contributors", "Station data © OpenStreetMap contributors"),
                            color = TextSecondaryDark.copy(alpha = 0.6f),
                            fontSize = 9.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // 5. Thẻ Mẹo Đổ Xăng Tiết Kiệm & An Toàn
        item {
            Surface(
                color = DarkSurface,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "💡", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tr("Mẹo đổ xăng thông minh từ XăngGo", "Smart Refueling Tips by XăngGo"),
                            color = EnergyAmber,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = tr(
                            "• Luôn ưu tiên trạm xăng chính hãng (Petrolimex, PVOIL, Saigon Petro) để đảm bảo chuẩn chất lượng và đo đúng dung tích.\n" +
                                "• Theo dõi đồng hồ cột bơm về 0 trước khi bơm và dừng khi vòi tự ngắt, không cố nhồi thêm.",
                            "• Always prioritize official stations (Petrolimex, PVOIL, Saigon Petro) for guaranteed fuel quality and accurate metering.\n" +
                                "• Check that the pump meter resets to 0 before fueling and stop at the first auto-click."
                        ),
                        color = TextSecondaryDark,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(90.dp))
        }
    }
}

@Composable
fun BrandOptionCard(
    name: String,
    subTag: String,
    logoEmoji: String,
    accentColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isSelected) accentColor.copy(alpha = 0.2f) else DarkBackground,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) accentColor else DarkSurfaceVariant.copy(alpha = 0.6f)
        ),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = logoEmoji, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = name,
                color = if (isSelected) accentColor else TextPrimaryDark,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = subTag,
                    color = TextSecondaryDark,
                    fontSize = 9.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun StationDetailCard(
    station: NearbyStation,
    isEn: Boolean,
    onDirectClick: () -> Unit,
    onShareClick: () -> Unit
) {
    val style = brandStyle(station.brandKey, isEn)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onDirectClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = style.emoji, fontSize = 20.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = station.name,
                    color = TextPrimaryDark,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val subtitle = listOfNotNull(
                    station.address.ifBlank { null },
                    if (station.openingHours == "24/7") (if (isEn) "Open 24/7" else "Mở 24/7") else null
                ).joinToString(" · ").ifBlank { style.label }
                Text(
                    text = subtitle,
                    color = TextSecondaryDark,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatDistance(station.distanceMeters, isEn),
                    color = PetrolTeal,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = DarkBackground,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant),
                        modifier = Modifier.clickable { onShareClick() }
                    ) {
                        Text(
                            text = "📤",
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                        )
                    }
                    Surface(color = PetrolTeal, shape = RoundedCornerShape(8.dp)) {
                        Text(
                            text = if (isEn) "🧭 Go" else "🧭 Đi",
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NearbyInfoCard(
    emoji: String,
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null
) {
    Surface(
        color = DarkSurface,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, PetrolTeal.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = emoji, fontSize = 18.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    color = TextPrimaryDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = message, color = TextSecondaryDark, fontSize = 12.sp, lineHeight = 17.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = PetrolTeal, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(actionLabel, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                }
                if (secondaryLabel != null && onSecondary != null) {
                    OutlinedButton(
                        onClick = onSecondary,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PetrolTeal.copy(alpha = 0.5f))
                    ) {
                        Text(secondaryLabel, color = PetrolTeal, fontSize = 13.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}

/** Chia sẻ vị trí cây xăng qua Zalo, Messenger, SMS... */
private fun shareStationLocation(context: Context, station: NearbyStation, isEn: Boolean) {
    val mapsUrl = "https://www.google.com/maps/dir/?api=1&destination=${station.lat},${station.lon}"
    val dist = formatDistance(station.distanceMeters, isEn)
    val text = if (isEn) {
        "⛽ ${station.name} ($dist away)\n📍 ${station.address.ifBlank { "Google Maps GPS" }}\n🧭 Directions: $mapsUrl\n— Shared via XăngGo"
    } else {
        "⛽ ${station.name} (cách $dist)\n📍 ${station.address.ifBlank { "Định vị Google Maps" }}\n🧭 Chỉ đường: $mapsUrl\n— Chia sẻ từ ứng dụng XăngGo"
    }
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(
        Intent.createChooser(sendIntent, if (isEn) "Share gas station" else "Chia sẻ vị trí cây xăng")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}

/** Mở Google Maps chỉ đường thẳng tới toạ độ cây xăng (fallback sang web nếu không có app Maps). */
fun openDirections(context: Context, lat: Double, lon: Double) {
    val navIntent = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$lat,$lon")).apply {
        setPackage("com.google.android.apps.maps")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    try {
        context.startActivity(navIntent)
    } catch (e: Exception) {
        try {
            val web = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lon")
            context.startActivity(Intent(Intent.ACTION_VIEW, web).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
        } catch (_: Exception) {}
    }
}

fun openMapsSearchDirect(context: Context, query: String) {
    try {
        val uri = Uri.parse("geo:0,0?q=${Uri.encode(query)}")
        val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        if (mapIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(mapIntent)
        } else {
            val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(query)}")
            val browserIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(browserIntent)
        }
    } catch (e: Exception) {
        try {
            val webUri = Uri.parse("https://www.google.com/maps/search/${Uri.encode(query)}")
            val browserIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(browserIntent)
        } catch (_: Exception) {}
    }
}

fun removeVietnameseAccents(str: String): String {
    return try {
        val nfd = Normalizer.normalize(str, Normalizer.Form.NFD)
        val pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
        pattern.matcher(nfd).replaceAll("").replace("đ", "d").replace("Đ", "D").lowercase().trim()
    } catch (e: Exception) {
        str.lowercase().trim()
    }
}
