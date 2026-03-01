package com.yuhai94.awcli

import android.content.Context
import com.google.gson.Gson
import com.yuhai94.awcli.data.ConfigStore
import com.yuhai94.awcli.data.V2RayRepository
import com.yuhai94.awcli.ssh.SshTunnelManager
import com.yuhai94.awcli.util.AppLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object AppContainer {
    private lateinit var appContext: Context
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val appLog = AppLog()
    val appForeground = MutableStateFlow(true)

    val gson: Gson by lazy { Gson() }

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    lateinit var configStore: ConfigStore
        private set
    lateinit var sshTunnelManager: SshTunnelManager
        private set
    lateinit var repository: V2RayRepository
        private set

    fun initialize(context: Context) {
        appContext = context.applicationContext
        configStore = ConfigStore(appContext)
        sshTunnelManager = SshTunnelManager(appScope, appLog)
        repository = V2RayRepository(configStore, sshTunnelManager, appLog)
    }
}
