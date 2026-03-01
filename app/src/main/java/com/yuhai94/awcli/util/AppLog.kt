package com.yuhai94.awcli.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

data class LogEntry(
    val time: String,
    val message: String
)

class AppLog {
    private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs
    private val pendingLogs = ConcurrentLinkedQueue<LogEntry>()

    fun add(message: String) {
        val entry = LogEntry(
            time = formatter.format(Date()),
            message = message
        )
        pendingLogs.offer(entry)
        flushPendingLogs()
    }

    private fun flushPendingLogs() {
        if (pendingLogs.isEmpty()) return
        val currentLogs = _logs.value.toMutableList()
        while (pendingLogs.isNotEmpty()) {
            val entry = pendingLogs.poll()
            if (entry != null) {
                currentLogs.add(entry)
            }
        }
        while (currentLogs.size > 300) {
            currentLogs.removeAt(0)
        }
        _logs.value = currentLogs
    }

    fun clear() {
        pendingLogs.clear()
        _logs.value = emptyList()
    }
}
