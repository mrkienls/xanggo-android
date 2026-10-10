package com.example.xanggo.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.xanggo.data.DefaultFuelRepository
import com.example.xanggo.theme.*
import com.example.xanggo.ui.vietnam.VietnamFuelScreen
import com.example.xanggo.ui.world.WorldCompareScreen

@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = viewModel { MainScreenViewModel(DefaultFuelRepository()) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        bottomBar = {
            Column(modifier = Modifier.background(DarkBackground)) {
                // 1. Google AdMob Banner thích ứng (Chân trang cố định)
                com.example.xanggo.ui.ads.AdMobBanner()

                // 2. Bottom Navigation Bar
                NavigationBar(
                    containerColor = DarkSurface,
                    contentColor = TextPrimaryDark,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { viewModel.selectTab(0) },
                        icon = { Text(text = "⛽", fontSize = 20.sp) },
                        label = {
                            Text(
                                text = "Giá Xăng",
                                fontSize = 12.sp,
                                fontWeight = if (currentTab == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PetrolTeal,
                            selectedTextColor = PetrolTeal,
                            indicatorColor = PetrolTeal.copy(alpha = 0.2f),
                            unselectedTextColor = TextSecondaryDark
                        )
                    )

                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { viewModel.selectTab(1) },
                        icon = { Text(text = "🌍", fontSize = 20.sp) },
                        label = {
                            Text(
                                text = "Thế Giới",
                                fontSize = 12.sp,
                                fontWeight = if (currentTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PetrolTeal,
                            selectedTextColor = PetrolTeal,
                            indicatorColor = PetrolTeal.copy(alpha = 0.2f),
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
                    if (currentTab == 0) {
                        VietnamFuelScreen(
                            data = state.vietnamData,
                            onNavigateToWorld = { viewModel.selectTab(1) }
                        )
                    } else {
                        WorldCompareScreen(
                            data = state.worldData
                        )
                    }
                }
                is MainUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Lỗi tải dữ liệu: ${state.message}",
                            color = PriceRed,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdMobAdaptiveBannerPlaceholder() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        color = DarkSurfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, TextSecondaryDark.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📢 [AdMob Adaptive Banner • 320x50]",
                color = TextSecondaryDark.copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
