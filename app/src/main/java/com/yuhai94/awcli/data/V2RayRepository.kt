package com.yuhai94.awcli.data

import android.util.Log
import com.google.gson.reflect.TypeToken
import com.yuhai94.awcli.AppContainer
import com.yuhai94.awcli.ssh.SshTunnelManager
import com.yuhai94.awcli.util.AppLog
import java.lang.reflect.ParameterizedType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class V2RayRepository(
    private val configStore: ConfigStore,
    private val sshTunnelManager: SshTunnelManager,
    private val appLog: AppLog
) {
    companion object {
        private const val TAG = "V2RayRepository"
    }
    private val gson = AppContainer.gson
    private val client = AppContainer.okHttpClient
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun listInstances(): Result<List<InstanceSummary>> {
        Log.d(TAG, "Listing instances")
        return requestWithReconnect { baseUrl ->
            val request = Request.Builder()
                .url("$baseUrl/api/v2ray/instances")
                .get()
                .build()
            val type = object : TypeToken<List<InstanceSummary>>() {}.type
            executeRequest(request, type)
        }
    }

    suspend fun createInstance(region: String): Result<CreateInstanceResponse> {
        Log.d(TAG, "Creating instance in region: $region")
        val requestBody = gson.toJson(CreateInstanceRequest(region)).toRequestBody(jsonMediaType)
        return requestWithReconnect { baseUrl ->
            val request = Request.Builder()
                .url("$baseUrl/api/v2ray/instances")
                .post(requestBody)
                .build()
            executeRequest(request, CreateInstanceResponse::class.java)
        }
    }

    suspend fun getInstance(uuid: String): Result<InstanceDetail> {
        Log.d(TAG, "Getting instance details for uuid: $uuid")
        return requestWithReconnect { baseUrl ->
            val request = Request.Builder()
                .url("$baseUrl/api/v2ray/instances/$uuid")
                .get()
                .build()
            executeRequest(request, InstanceDetail::class.java)
        }
    }

    suspend fun deleteInstance(uuid: String): Result<DeleteInstanceResponse> {
        Log.d(TAG, "Deleting instance with uuid: $uuid")
        return requestWithReconnect { baseUrl ->
            val request = Request.Builder()
                .url("$baseUrl/api/v2ray/instances/$uuid")
                .delete()
                .build()
            executeRequest(request, DeleteInstanceResponse::class.java)
        }
    }

    suspend fun getRegions(): Result<List<RegionInfo>> {
        Log.d(TAG, "Getting regions list")
        return requestWithReconnect { baseUrl ->
            val request = Request.Builder()
                .url("$baseUrl/api/v2ray/regions")
                .get()
                .build()
            val type = object : TypeToken<List<RegionInfo>>() {}.type
            executeRequest(request, type)
        }
    }

    private suspend fun <T> requestWithReconnect(block: suspend (String) -> Result<T>): Result<T> {
        val config = configStore.configFlow.value
        if (config == null) {
            Log.e(TAG, "No config available")
            return Result.failure(IllegalStateException("缺少配置"))
        }
        val baseUrl = buildBaseUrl(config) ?: run {
            Log.e(TAG, "SSH tunnel not ready")
            return Result.failure(IllegalStateException("SSH隧道未就绪"))
        }
        Log.d(TAG, "Making request to: $baseUrl")
        val first = block(baseUrl)
        if (first.isSuccess) {
            Log.d(TAG, "Request successful")
            return first
        }
        val cause = first.exceptionOrNull()
        if (cause is IOException) {
            appLog.add("检测到连接异常，准备重连SSH隧道")
            Log.w(TAG, "Connection exception detected, reconnecting SSH tunnel", cause)
            sshTunnelManager.forceReconnect()
            delay(1000)
            val retryBaseUrl = buildBaseUrl(config) ?: return first
            Log.d(TAG, "Retrying request after reconnection")
            return block(retryBaseUrl)
        }
        Log.e(TAG, "Request failed", cause)
        return first
    }

    private suspend fun buildBaseUrl(config: AppConfig): String? {
        val port = sshTunnelManager.ensureConnected(config)
        if (port == null) {
            Log.e(TAG, "Failed to ensure SSH connection")
            return null
        }
        val baseUrl = "http://127.0.0.1:$port"
        Log.d(TAG, "Base URL built: $baseUrl")
        return baseUrl
    }

    private suspend fun <T> executeRequest(request: Request, type: java.lang.reflect.Type): Result<T> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Executing HTTP request: ${request.method} ${request.url}")
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        val apiError = runCatching { gson.fromJson(body, ApiError::class.java) }.getOrNull()
                        val errorMsg = apiError?.error ?: "请求失败"
                        Log.e(TAG, "HTTP error: ${response.code} - $errorMsg")
                        return@withContext Result.failure(IOException(errorMsg))
                    }
                    val result = gson.fromJson<T>(body, type)
                    Log.d(TAG, "HTTP response parsed successfully")
                    // 确保当 API 返回 null 时，返回一个空列表
                    if (result == null) {
                        @Suppress("UNCHECKED_CAST")
                        return@withContext Result.success(emptyList<Any>() as T)
                    }
                    Result.success(result)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Request execution failed", e)
                Result.failure(e)
            }
        }
    }
}
