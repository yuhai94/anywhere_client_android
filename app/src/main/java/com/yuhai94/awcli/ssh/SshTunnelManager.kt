package com.yuhai94.awcli.ssh

import android.util.Log
import com.yuhai94.awcli.data.AppConfig
import com.yuhai94.awcli.util.AppLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.apache.sshd.client.SshClient
import org.apache.sshd.client.session.ClientSession
import org.apache.sshd.common.util.net.SshdSocketAddress
import java.net.ServerSocket

sealed class TunnelState {
    data object Disconnected : TunnelState()
    data object Connecting : TunnelState()
    data class Connected(val localPort: Int) : TunnelState()
    data class Error(val message: String) : TunnelState()
}

class SshTunnelManager(
    private val scope: CoroutineScope,
    private val appLog: AppLog
) {
    companion object {
        private const val TAG = "SshTunnelManager"
        private const val KEEPALIVE_INTERVAL_MS = 30000L
        private const val MAX_RETRY_ATTEMPTS = 3
    }
    private var client: SshClient? = null
    private var session: ClientSession? = null
    private var activeLocalPort: Int? = null
    private var currentConfig: AppConfig? = null
    private var connectionJob: kotlinx.coroutines.Job? = null
    private var keepaliveJob: kotlinx.coroutines.Job? = null
    private val _state = MutableStateFlow<TunnelState>(TunnelState.Disconnected)
    val state: StateFlow<TunnelState> = _state
    private val stateLock = Any()

    fun start(config: AppConfig) {
        currentConfig = config
        connectionJob?.cancel()
        connectionJob = scope.launch {
            Log.d(TAG, "Starting SSH tunnel to ${config.sshIp}:${config.sshPort}")
            connectWithRetry(config)
        }
        startKeepalive()
    }

    fun stop() {
        Log.d(TAG, "Stopping SSH tunnel")
        connectionJob?.cancel()
        connectionJob = null
        stopKeepalive()
        scope.launch {
            closeSession()
            _state.value = TunnelState.Disconnected
        }
    }

    private fun startKeepalive() {
        stopKeepalive()
        keepaliveJob = scope.launch {
            while (scope.isActive) {
                delay(KEEPALIVE_INTERVAL_MS)
                checkAndReconnectIfNeeded()
            }
        }
    }

    private fun stopKeepalive() {
        keepaliveJob?.cancel()
        keepaliveJob = null
    }

    private suspend fun checkAndReconnectIfNeeded() {
        val currentState = synchronized(stateLock) { _state.value }
        if (currentState !is TunnelState.Connected) {
            return
        }
        val session = this.session
        if (session == null || !session.isOpen) {
            Log.w(TAG, "SSH session is not open, triggering reconnect")
            appLog.add("检测到SSH连接已断开，准备重连")
            val config = currentConfig ?: return
            connectWithRetry(config, true)
        }
    }

    suspend fun ensureConnected(config: AppConfig): Int? {
        if (_state.value is TunnelState.Connected && activeLocalPort != null) {
            Log.d(TAG, "Already connected on port $activeLocalPort")
            return activeLocalPort
        }
        currentConfig = config
        Log.d(TAG, "Ensuring connection to ${config.sshIp}:${config.sshPort}")
        connectWithRetry(config)
        return activeLocalPort
    }

    suspend fun forceReconnect() {
        val config = currentConfig ?: return
        Log.d(TAG, "Forcing reconnection to ${config.sshIp}:${config.sshPort}")
        connectWithRetry(config, true)
    }

    private suspend fun connectWithRetry(config: AppConfig, force: Boolean = false) {
        if (!force) {
            val existing = _state.value
            if (existing is TunnelState.Connected && activeLocalPort != null) {
                Log.d(TAG, "Already connected, skipping retry")
                return
            }
        }
        _state.value = TunnelState.Connecting
        appLog.add("开始建立SSH隧道")
        Log.d(TAG, "开始建立SSH隧道到 ${config.sshIp}:${config.sshPort}")
        var attempt = 0
        while (scope.isActive && attempt < MAX_RETRY_ATTEMPTS) {
            attempt += 1
            closeSession()
            val result = runCatching { connectOnce(config) }
            if (result.isSuccess) {
                Log.d(TAG, "SSH tunnel established successfully after $attempt attempts")
                return
            }
            val errorMessage = result.exceptionOrNull()?.message ?: "未知错误"
            appLog.add("SSH隧道连接失败，第${attempt}次：$errorMessage")
            Log.e(TAG, "SSH tunnel connection failed (attempt $attempt): $errorMessage", result.exceptionOrNull())
            _state.value = TunnelState.Error(errorMessage)
            if (attempt < MAX_RETRY_ATTEMPTS) {
                delay(2000)
            }
        }
        if (attempt >= MAX_RETRY_ATTEMPTS) {
            val finalError = "已达到最大重试次数 ($MAX_RETRY_ATTEMPTS)"
            appLog.add(finalError)
            _state.value = TunnelState.Error(finalError)
        }
    }

    private fun connectOnce(config: AppConfig) {
        Log.d(TAG, "Attempting SSH connection to ${config.sshUser}@${config.sshIp}:${config.sshPort}")
        closeSession()
        val client = SshClient.setUpDefaultClient()
        client.start()
        Log.d(TAG, "SSH client started")
        val session = client.connect(config.sshUser, config.sshIp, config.sshPort)
            .verify(15000)
            .session
        Log.d(TAG, "SSH session created")
        session.addPasswordIdentity(config.sshPassword)
        session.auth().verify(15000)
        Log.d(TAG, "SSH authentication successful")
        val localPort = allocatePort()
        val localAddress = SshdSocketAddress("127.0.0.1", localPort)
        val remoteAddress = SshdSocketAddress(config.serviceIp, config.servicePort)
        session.startLocalPortForwarding(localAddress, remoteAddress)
        Log.d(TAG, "Local port forwarding established: $localPort -> ${config.serviceIp}:${config.servicePort}")
        this.client = client
        this.session = session
        this.activeLocalPort = localPort
        _state.value = TunnelState.Connected(localPort)
        appLog.add("SSH隧道已建立，端口：$localPort")
    }

    private fun closeSession() {
        var sessionToClose: ClientSession? = null
        var clientToStop: SshClient? = null
        synchronized(this) {
            sessionToClose = session
            clientToStop = client
            session = null
            client = null
            activeLocalPort = null
        }
        try {
            sessionToClose?.close(false)
        } catch (e: Exception) {
            appLog.add("关闭SSH会话失败: ${e.message}")
        }
        try {
            clientToStop?.stop()
        } catch (e: Exception) {
            appLog.add("停止SSH客户端失败: ${e.message}")
        }
    }

    private fun allocatePort(): Int {
        ServerSocket(0).use { socket ->
            return socket.localPort
        }
    }
}
