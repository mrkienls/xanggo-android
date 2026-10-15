package com.example.xanggo.ui.main

import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.xanggo.data.DefaultFuelRepository
import com.example.xanggo.i18n.AppLang
import com.example.xanggo.i18n.LanguagePrefs
import com.example.xanggo.i18n.LocalAppLanguage
import com.example.xanggo.i18n.tr
import com.example.xanggo.theme.*
import com.example.xanggo.ui.components.GasPumpVector
import com.example.xanggo.ui.components.GlobeVector
import com.example.xanggo.ui.components.LocationPinVector
import com.example.xanggo.ui.stations.GasStationScreen
import com.example.xanggo.ui.vietnam.VietnamFuelScreen
import com.example.xanggo.ui.world.WorldCompareScreen

@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val viewModel: MainScreenViewModel = viewModel {
        MainScreenViewModel(DefaultFuelRepository(context.cacheDir))
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    var appLang by remember { mutableStateOf(LanguagePrefs.getSavedLang(context)) }

    val toggleLang = {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        val next = if (appLang == AppLang.VI) AppLang.EN else AppLang.VI
        appLang = next
        LanguagePrefs.saveLang(context, next)
    }

    val triggerRefresh = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.refreshData()
        val msg = if (appLang == AppLang.EN) "Refreshing latest fuel prices…" else "Đang kiểm tra bảng giá xăng mới nhất…"
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    CompositionLocalProvider(LocalAppLanguage provides appLang) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = DarkBackground,
            bottomBar = {
                Column(modifier = Modifier.background(DarkBackground)) {
                    com.example.xanggo.ui.ads.AdMobBanner()

                    NavigationBar(
                        containerColor = DarkSurface,
                        contentColor = TextPrimaryDark,
                        tonalElevation = 8.dp
                    ) {
                        NavigationBarItem(
                            selected = currentTab == 0,
                            onClick = {
                                if (currentTab != 0) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.selectTab(0)
                            },
                            icon = {
                                Icon(
                                    imageVector = GasPumpVector,
                                    contentDescription = tr("Giá Xăng", "Fuel Prices"),
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = tr("Giá Xăng", "Fuel Prices"),
                                    fontSize = 11.sp,
                                    fontWeight = if (currentTab == 0) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PetrolTeal,
                                selectedTextColor = PetrolTeal,
                                indicatorColor = PetrolTeal.copy(alpha = 0.2f),
                                unselectedIconColor = TextSecondaryDark,
                                unselectedTextColor = TextSecondaryDark
                            )
                        )

                        NavigationBarItem(
                            selected = currentTab == 1,
                            onClick = {
                                if (currentTab != 1) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.selectTab(1)
                            },
                            icon = {
                                Icon(
                                    imageVector = LocationPinVector,
                                    contentDescription = tr("Trạm Xăng", "Stations"),
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = tr("Trạm Xăng", "Stations"),
                                    fontSize = 11.sp,
                                    fontWeight = if (currentTab == 1) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PetrolTeal,
                                selectedTextColor = PetrolTeal,
                                indicatorColor = PetrolTeal.copy(alpha = 0.2f),
                                unselectedIconColor = TextSecondaryDark,
                                unselectedTextColor = TextSecondaryDark
                            )
                        )

                        NavigationBarItem(
                            selected = currentTab == 2,
                            onClick = {
                                if (currentTab != 2) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.selectTab(2)
                            },
                            icon = {
                                Icon(
                                    imageVector = GlobeVector,
                                    contentDescription = tr("Thế Giới", "World"),
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = tr("Thế Giới", "World"),
                                    fontSize = 11.sp,
                                    fontWeight = if (currentTab == 2) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PetrolTeal,
                                selectedTextColor = PetrolTeal,
                                indicatorColor = PetrolTeal.copy(alpha = 0.2f),
                                unselectedIconColor = TextSecondaryDark,
                                unselectedTextColor = TextSecondaryDark
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (val state = uiState) {
                    is MainUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = PetrolTeal)
                        }
                    }
                    is MainUiState.Success -> {
                        Crossfade(
                            targetState = currentTab,
                            animationSpec = tween(durationMillis = 220),
                            label = "MainTabCrossfade"
                        ) { tab ->
                            when (tab) {
                                0 -> VietnamFuelScreen(
                                    data = state.vietnamData,
                                    worldData = state.worldData,
                                    onNavigateToStations = { viewModel.selectTab(1) },
                                    onNavigateToWorld = { viewModel.selectTab(2) },
                                    onToggleLanguage = toggleLang,
                                    onRefresh = triggerRefresh
                                )
                                1 -> GasStationScreen()
                                else -> WorldCompareScreen(
                                    data = state.worldData,
                                    onToggleLanguage = toggleLang,
                                    onRefresh = triggerRefresh
                                )
                            }
                        }
                    }
                    is MainUiState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tr("Lỗi tải dữ liệu: ${state.message}", "Error loading data: ${state.message}"),
                                color = PriceRed,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
