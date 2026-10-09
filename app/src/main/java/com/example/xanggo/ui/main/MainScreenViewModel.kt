package com.example.xanggo.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xanggo.data.FuelRepository
import com.example.xanggo.data.model.VietnamFuelData
import com.example.xanggo.data.model.WorldFuelSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
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

    private val _currentTab = MutableStateFlow(0) // 0: Giá xăng VN, 1: Thế giới
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Loading)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun selectTab(tabIndex: Int) {
        _currentTab.value = tabIndex
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                fuelRepository.getVietnamFuelPrices().collect { vnData ->
                    fuelRepository.getWorldFuelSummary().collect { worldData ->
                        _uiState.value = MainUiState.Success(
                            vietnamData = vnData,
                            worldData = worldData
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = MainUiState.Error(e.message ?: "Lỗi tải dữ liệu")
            }
        }
    }
}
