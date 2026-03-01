package com.yuhai94.awcli.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogsClick: () -> Unit
) {
    val viewModel: SettingsViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showImportDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var jsonBuffer by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "设置",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .shadow(2.dp)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("SSH 配置", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.sshIp,
                    onValueChange = { viewModel.updateField(sshIp = it) },
                    label = { Text("ssh_ip") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = uiState.sshPort,
                    onValueChange = { viewModel.updateField(sshPort = it) },
                    label = { Text("ssh_port") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.sshUser,
                    onValueChange = { viewModel.updateField(sshUser = it) },
                    label = { Text("ssh_user") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = uiState.sshPassword,
                    onValueChange = { viewModel.updateField(sshPassword = it) },
                    label = { Text("ssh_password") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
            }
            Text("服务配置", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.serviceIp,
                    onValueChange = { viewModel.updateField(serviceIp = it) },
                    label = { Text("service_ip") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = uiState.servicePort,
                    onValueChange = { viewModel.updateField(servicePort = it) },
                    label = { Text("service_port") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.saveConfig() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("保存")
                }
                OutlinedButton(
                    onClick = { viewModel.checkConfig() },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isChecking
                ) {
                    Text(if (uiState.isChecking) "检查中..." else "检查")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        jsonBuffer = ""
                        showImportDialog = true
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("导入JSON")
                }
                OutlinedButton(
                    onClick = {
                        jsonBuffer = viewModel.exportJson()
                        showExportDialog = true
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("导出JSON")
                }
            }
            OutlinedButton(
                onClick = onLogsClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("查看日志")
            }

            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error
                )
            }

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("返回")
            }
        }
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.importJson(jsonBuffer)
                        showImportDialog = false
                    }
                ) {
                    Text("导入")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("取消")
                }
            },
            title = { Text("导入JSON") },
            text = {
                OutlinedTextField(
                    value = jsonBuffer,
                    onValueChange = { jsonBuffer = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    label = { Text("配置 JSON") }
                )
            }
        )
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            confirmButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("关闭")
                }
            },
            title = { Text("导出JSON") },
            text = {
                OutlinedTextField(
                    value = jsonBuffer,
                    onValueChange = { },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    label = { Text("配置 JSON") },
                    readOnly = true
                )
            }
        )
    }
}
