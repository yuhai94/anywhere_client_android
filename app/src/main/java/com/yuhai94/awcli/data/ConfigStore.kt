package com.yuhai94.awcli.data

import android.content.Context
import android.util.Log
import com.yuhai94.awcli.AppContainer
import com.yuhai94.awcli.util.CryptoHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.core.content.edit

class ConfigStore(context: Context) {
    companion object {
        private const val TAG = "ConfigStore"
    }
    private val prefs = context.getSharedPreferences("awcli_config", Context.MODE_PRIVATE)
    private val gson = AppContainer.gson
    private val cryptoHelper = CryptoHelper(context)
    private val _configFlow = MutableStateFlow(loadInternal())
    val configFlow: StateFlow<AppConfig?> = _configFlow

    fun save(config: AppConfig) {
        try {
            Log.d(TAG, "Saving config: ${config.sshIp}:${config.sshPort}")
            val encryptedPassword = cryptoHelper.encrypt(config.sshPassword)
            prefs.edit {
                putString("ssh_ip", config.sshIp)
                    .putInt("ssh_port", config.sshPort)
                    .putString("ssh_user", config.sshUser)
                    .putString("ssh_password", encryptedPassword)
                    .putString("service_ip", config.serviceIp)
                    .putInt("service_port", config.servicePort)
            }
            _configFlow.value = config
            Log.d(TAG, "Config saved successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save config", e)
            _configFlow.value = null
        }
    }

    fun clear() {
        Log.d(TAG, "Clearing config")
        prefs.edit { clear() }
        _configFlow.value = null
        Log.d(TAG, "Config cleared successfully")
    }

    fun exportJson(config: AppConfig?): String {
        if (config == null) {
            Log.w(TAG, "Export config: config is null")
            return ""
        }
        val exportConfig = config.copy(sshPassword = "***")
        val json = gson.toJson(exportConfig)
        Log.d(TAG, "Export config (password masked)")
        return json
    }

    fun importJson(json: String): AppConfig? {
        Log.d(TAG, "Import config from JSON")
        return try {
            val config = gson.fromJson(json, AppConfig::class.java)
            Log.d(TAG, "Config imported successfully: ${config.sshIp}:${config.sshPort}")
            config
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import config from JSON", e)
            null
        }
    }

    private fun loadInternal(): AppConfig? {
        val sshIp = prefs.getString("ssh_ip", "") ?: ""
        val sshPort = prefs.getInt("ssh_port", 0)
        val sshUser = prefs.getString("ssh_user", "") ?: ""
        val encryptedPassword = prefs.getString("ssh_password", "") ?: ""
        val sshPassword = cryptoHelper.decrypt(encryptedPassword) ?: ""
        val serviceIp = prefs.getString("service_ip", "") ?: ""
        val servicePort = prefs.getInt("service_port", 0)
        val config = AppConfig(
            sshIp = sshIp,
            sshPort = sshPort,
            sshUser = sshUser,
            sshPassword = sshPassword,
            serviceIp = serviceIp,
            servicePort = servicePort
        )
        return if (config.isComplete()) config else null
    }
}
