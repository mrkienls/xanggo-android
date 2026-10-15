package com.example.xanggo.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xanggo.data.FuelRepository
import com.example.xanggo.data.model.VietnamFuelData
import com.example.xanggo.data.model.WorldFuelSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

sealed interface MainUiState {
    object Loading : MainUiState
    data class Success(
        val vietnamData: VietnamFuelData,
        val worldData: WorldFuelSummary
    ) : MainUiState
    data class Error(val message: String) : MainUiState
}

class MainScreenViewModel(
    private val fuelRepository: FuelRepository
) : ViewModel() {

    private val _currentTab = MutableStateFlow(0) // 0: Giá xăng VN, 1: Trạm xăng, 2: Thế giới
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Loading)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun selectTab(tabIndex: Int) {
        _currentTab.value = tabIndex
    }

    fun refreshData() {
        fuelRepository.requestRefresh()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                fuelRepository.getVietnamFuelPrices(),
                fuelRepository.getWorldFuelSummary()
            ) { vnData, worldData ->
                MainUiState.Success(
                    vietnamData = vnData,
                    worldData = syncVietnamPrice(worldData, vnData)
                ) as MainUiState
            }
                .catch { e -> emit(MainUiState.Error(e.message ?: "Lỗi tải dữ liệu")) }
                .collect { _uiState.value = it }
        }
    }

    /** Đồng bộ giá VN trong dữ liệu thế giới với giá Petrolimex (Vùng 1) để 2 tab không lệch số. */
    private fun syncVietnamPrice(world: WorldFuelSummary, vn: VietnamFuelData): WorldFuelSummary {
        val vnPrice = (vn.items.firstOrNull { it.isFeatured } ?: vn.items.firstOrNull())?.zone1Price
            ?: return world
        if (vnPrice <= 0) return world
        return world.copy(
            countries = world.countries.map {
                if (it.isCurrent) it.copy(priceVnd = vnPrice) else it
            }
        )
    }
}
