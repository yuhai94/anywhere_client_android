package com.yuhai94.awcli.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yuhai94.awcli.AppContainer
import com.yuhai94.awcli.data.InstanceSummary
import com.yuhai94.awcli.ssh.TunnelState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class InstancesUiState(
    val instances: List<InstanceSummary> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)

class InstancesViewModel : ViewModel() {
    private val repository = AppContainer.repository
    private val appLog = AppContainer.appLog
    private val appForeground = AppContainer.appForeground
    private val configFlow = AppContainer.configStore.configFlow
    private val tunnelState = AppContainer.sshTunnelManager.state
    private var pollingJob: Job? = null
    private val _uiState = MutableStateFlow(InstancesUiState())
    val uiState: StateFlow<InstancesUiState> = _uiState

    init {
        viewModelScope.launch {
            combine(appForeground, configFlow, tunnelState) { foreground, config, tunnel ->
                foreground && config != null && tunnel is TunnelState.Connected
            }.collect { shouldPoll ->
                if (shouldPoll) {
                    startPolling()
                } else {
                    stopPolling()
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            loadInstances(true)
        }
    }

    private fun startPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch {
            loadInstances(false)
            while (true) {
                delay(5000)
                try {
                    loadInstances(false)
                } catch (e: Exception) {
                    appLog.add("轮询加载实例失败: ${e.message}")
                }
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private suspend fun loadInstances(manual: Boolean) {
        _uiState.value = _uiState.value.copy(
            isLoading = !manual,
            isRefreshing = manual,
            errorMessage = null
        )
        val result = repository.listInstances()
        if (result.isSuccess) {
            _uiState.value = _uiState.value.copy(
                instances = result.getOrDefault(emptyList()),
                isLoading = false,
                isRefreshing = false
            )
        } else {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isRefreshing = false,
                errorMessage = result.exceptionOrNull()?.message
            )
        }
    }
}
