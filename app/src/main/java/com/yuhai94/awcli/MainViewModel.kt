package com.yuhai94.awcli

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yuhai94.awcli.data.AppConfig
import com.yuhai94.awcli.ssh.TunnelState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

data class MainUiState(
    val config: AppConfig?,
    val tunnelState: TunnelState
) {
    val isConfigReady: Boolean = config != null
    val isConnected: Boolean = tunnelState is TunnelState.Connected
    val isConnecting: Boolean = tunnelState is TunnelState.Connecting
}

class MainViewModel : ViewModel() {
    private val configStore = AppContainer.configStore
    private val tunnelManager = AppContainer.sshTunnelManager
    private val _uiState = MutableStateFlow(
        MainUiState(
            config = configStore.configFlow.value,
            tunnelState = tunnelManager.state.value
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState

    init {
        viewModelScope.launch {
            combine(
                configStore.configFlow,
                tunnelManager.state
            ) { config, tunnelState ->
                MainUiState(config = config, tunnelState = tunnelState)
            }.distinctUntilChanged().collect { state ->
                _uiState.value = state
                if (state.config != null) {
                    val currentTunnelState = tunnelManager.state.value
                    if (currentTunnelState !is TunnelState.Connected && currentTunnelState !is TunnelState.Connecting) {
                        tunnelManager.start(state.config)
                    }
                } else {
                    tunnelManager.stop()
                }
            }
        }
    }
}
