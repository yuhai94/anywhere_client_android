package com.yuhai94.awcli.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yuhai94.awcli.AppContainer
import com.yuhai94.awcli.data.RegionInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class CreateInstanceUiState(
    val regions: List<RegionInfo> = emptyList(),
    val selectedRegion: String? = null,
    val isSubmitting: Boolean = false,
    val isLoadingRegions: Boolean = false,
    val errorMessage: String? = null
)

class CreateInstanceViewModel : ViewModel() {
    private val repository = AppContainer.repository
    private val appLog = AppContainer.appLog
    private val _uiState = MutableStateFlow(CreateInstanceUiState())
    val uiState: StateFlow<CreateInstanceUiState> = _uiState

    fun selectRegion(region: String) {
        _uiState.value = _uiState.value.copy(selectedRegion = region, errorMessage = null)
    }

    fun loadRegions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingRegions = true, errorMessage = null)
            val result = repository.getRegions()
            if (result.isSuccess) {
                val regions = result.getOrNull() ?: emptyList()
                _uiState.value = _uiState.value.copy(
                    regions = regions,
                    isLoadingRegions = false
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoadingRegions = false,
                    errorMessage = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun submit(onSuccess: () -> Unit) {
        val region = _uiState.value.selectedRegion ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, errorMessage = null)
            val result = repository.createInstance(region)
            if (result.isSuccess) {
                val response = result.getOrNull()
                if (response?.status != null || response?.uuid != null) {
                    appLog.add("创建实例请求已提交：$region")
                    _uiState.value = _uiState.value.copy(isSubmitting = false)
                    onSuccess()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        errorMessage = response?.error ?: "创建失败"
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    errorMessage = result.exceptionOrNull()?.message
                )
            }
        }
    }
}
