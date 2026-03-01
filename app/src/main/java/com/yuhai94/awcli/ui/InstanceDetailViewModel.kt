package com.yuhai94.awcli.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yuhai94.awcli.AppContainer
import com.yuhai94.awcli.data.InstanceDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class InstanceDetailUiState(
    val instance: InstanceDetail? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class InstanceDetailViewModel : ViewModel() {
    private val repository = AppContainer.repository
    private val appLog = AppContainer.appLog
    private val _uiState = MutableStateFlow(InstanceDetailUiState())
    val uiState: StateFlow<InstanceDetailUiState> = _uiState

    fun load(uuid: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = repository.getInstance(uuid)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    instance = result.getOrNull(),
                    isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun delete(uuid: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = repository.deleteInstance(uuid)
            if (result.isSuccess) {
                appLog.add("删除实例请求已发送：$uuid")
                _uiState.value = _uiState.value.copy(isLoading = false)
                onSuccess()
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message
                )
            }
        }
    }
}
