package com.yuhai94.awcli.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yuhai94.awcli.AppContainer
import com.yuhai94.awcli.data.AppConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val sshIp: String = "",
    val sshPort: String = "",
    val sshUser: String = "",
    val sshPassword: String = "",
    val serviceIp: String = "",
    val servicePort: String = "",
    val isChecking: Boolean = false,
    val errorMessage: String? = null
)

class SettingsViewModel : ViewModel() {
    private val configStore = AppContainer.configStore
    private val repository = AppContainer.repository
    private val appLog = AppContainer.appLog
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        val config = configStore.configFlow.value
        if (config != null) {
            _uiState.value = _uiState.value.copy(
                sshIp = config.sshIp,
                sshPort = config.sshPort.toString(),
                sshUser = config.sshUser,
                sshPassword = config.sshPassword,
                serviceIp = config.serviceIp,
                servicePort = config.servicePort.toString()
            )
        }
    }

    fun updateField(
        sshIp: String? = null,
        sshPort: String? = null,
        sshUser: String? = null,
        sshPassword: String? = null,
        serviceIp: String? = null,
        servicePort: String? = null
    ) {
        _uiState.value = _uiState.value.copy(
            sshIp = sshIp ?: _uiState.value.sshIp,
            sshPort = sshPort ?: _uiState.value.sshPort,
            sshUser = sshUser ?: _uiState.value.sshUser,
            sshPassword = sshPassword ?: _uiState.value.sshPassword,
            serviceIp = serviceIp ?: _uiState.value.serviceIp,
            servicePort = servicePort ?: _uiState.value.servicePort,
            errorMessage = null
        )
    }

    fun saveConfig() {
        val config = buildConfig() ?: return
        configStore.save(config)
        appLog.add("配置已保存")
    }

    fun exportJson(): String {
        val config = buildConfig() ?: configStore.configFlow.value
        return configStore.exportJson(config)
    }

    fun importJson(json: String): Boolean {
        val config = configStore.importJson(json) ?: run {
            _uiState.value = _uiState.value.copy(errorMessage = "JSON解析失败")
            return false
        }
        if (!config.isComplete()) {
            _uiState.value = _uiState.value.copy(errorMessage = "JSON配置不完整")
            return false
        }
        _uiState.value = _uiState.value.copy(
            sshIp = config.sshIp,
            sshPort = config.sshPort.toString(),
            sshUser = config.sshUser,
            sshPassword = config.sshPassword,
            serviceIp = config.serviceIp,
            servicePort = config.servicePort.toString(),
            errorMessage = null
        )
        configStore.save(config)
        appLog.add("配置已导入")
        return true
    }

    fun checkConfig() {
        val config = buildConfig() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isChecking = true, errorMessage = null)
            configStore.save(config)
            appLog.add("开始检查配置")
            val result = repository.listInstances()
            if (result.isSuccess) {
                appLog.add("配置检查成功")
                _uiState.value = _uiState.value.copy(isChecking = false)
            } else {
                val message = result.exceptionOrNull()?.message ?: "检查失败"
                appLog.add("配置检查失败：$message")
                _uiState.value = _uiState.value.copy(isChecking = false, errorMessage = message)
            }
        }
    }

    private fun buildConfig(): AppConfig? {
        val sshIp = _uiState.value.sshIp.trim()
        val sshPort = _uiState.value.sshPort.toIntOrNull()
        val sshUser = _uiState.value.sshUser.trim()
        val sshPassword = _uiState.value.sshPassword
        val serviceIp = _uiState.value.serviceIp.trim()
        val servicePort = _uiState.value.servicePort.toIntOrNull()

        when {
            sshIp.isEmpty() -> {
                _uiState.value = _uiState.value.copy(errorMessage = "SSH IP不能为空")
                return null
            }
            !isValidIpAddress(sshIp) -> {
                _uiState.value = _uiState.value.copy(errorMessage = "SSH IP格式不正确")
                return null
            }
            sshPort == null || sshPort <= 0 || sshPort > 65535 -> {
                _uiState.value = _uiState.value.copy(errorMessage = "SSH端口必须在1-65535之间")
                return null
            }
            sshUser.isEmpty() -> {
                _uiState.value = _uiState.value.copy(errorMessage = "SSH用户名不能为空")
                return null
            }
            sshPassword.isEmpty() -> {
                _uiState.value = _uiState.value.copy(errorMessage = "SSH密码不能为空")
                return null
            }
            serviceIp.isEmpty() -> {
                _uiState.value = _uiState.value.copy(errorMessage = "服务IP不能为空")
                return null
            }
            !isValidIpAddress(serviceIp) -> {
                _uiState.value = _uiState.value.copy(errorMessage = "服务IP格式不正确")
                return null
            }
            servicePort == null || servicePort <= 0 || servicePort > 65535 -> {
                _uiState.value = _uiState.value.copy(errorMessage = "服务端口必须在1-65535之间")
                return null
            }
        }

        val config = AppConfig(
            sshIp = sshIp,
            sshPort = sshPort,
            sshUser = sshUser,
            sshPassword = sshPassword,
            serviceIp = serviceIp,
            servicePort = servicePort
        )
        return config
    }

    private fun isValidIpAddress(ip: String): Boolean {
        val ipRegex = Regex("^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$")
        return ipRegex.matches(ip)
    }
}
