package com.yuhai94.awcli.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yuhai94.awcli.data.InstanceSummary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    onCreateClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onInstanceClick: (String) -> Unit
) {
    val viewModel: InstancesViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember {
        SnackbarHostState()
    }
    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.isRefreshing,
        onRefresh = { viewModel.refresh() }
    )
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "实例列表",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                actions = {
                    IconButton(
                        onClick = onCreateClick,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Icon(
                            Icons.Filled.Add, 
                            contentDescription = "创建", 
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.width(24.dp).height(24.dp)
                        )
                    }
                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Icon(
                            Icons.Filled.Settings, 
                            contentDescription = "设置",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.width(24.dp).height(24.dp)
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .shadow(2.dp)
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) {
        padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pullRefresh(pullRefreshState)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (uiState.errorMessage != null) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(2.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.medium,
                            tonalElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Info,
                                    contentDescription = "错误",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.width(20.dp).height(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = uiState.errorMessage ?: "",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
                items(
                    items = uiState.instances,
                    key = { instance -> instance.uuid }
                ) {
                    instance ->
                    InstanceCard(
                        instance = instance,
                        onCopyDirect = {
                            val link = instance.directLink ?: "vmess://direct/${instance.uuid}"
                            clipboardManager.setText(AnnotatedString(link))
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "直连链接已复制到剪贴板",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        },
                        onCopyRelay = {
                            val link = instance.relayLink ?: "vmess://relay/${instance.uuid}"
                            clipboardManager.setText(AnnotatedString(link))
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "中转链接已复制到剪贴板",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        },
                        onClick = { onInstanceClick(instance.uuid) }
                    )
                }
                if (uiState.instances.isEmpty() && !uiState.isLoading) {
                    item {
                        EmptyState(
                            onCreateClick = onCreateClick
                        )
                    }
                }
                if (uiState.isLoading && uiState.instances.isEmpty()) {
                    item {
                        LoadingState()
                    }
                }
                if (uiState.isLoading && uiState.instances.isNotEmpty()) {
                    item {
                        LoadingMoreState()
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
            PullRefreshIndicator(
                refreshing = uiState.isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                backgroundColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                scale = true
            )
        }
    }


}

@Composable
private fun EmptyState(
    onCreateClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .width(120.dp).height(120.dp)
                .shadow(4.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Filled.Info,
                    contentDescription = "空状态",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(48.dp).height(48.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "暂无实例",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "点击下方按钮创建您的第一个实例",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onCreateClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = "创建",
                modifier = Modifier.width(18.dp).height(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("创建实例")
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .width(80.dp).height(80.dp)
                .shadow(2.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.width(32.dp).height(32.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "加载中...",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LoadingMoreState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 2.dp,
            modifier = Modifier.width(24.dp).height(24.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "加载更多实例...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InstanceCard(
    instance: InstanceSummary,
    onCopyDirect: () -> Unit,
    onCopyRelay: () -> Unit,
    onClick: () -> Unit
) {
    val elevation by animateDpAsState(
        targetValue = 4.dp,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
    )
    
    Card(
                modifier = Modifier
                    .fillMaxWidth(),
                onClick = onClick,
                elevation = CardDefaults.cardElevation(
                    defaultElevation = elevation
                ),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 第一行：区域名、状态、IP
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 区域名
                            Column(
                                horizontalAlignment = Alignment.Start
                            ) {
                                if (!instance.ec2RegionName.isNullOrEmpty()) {
                                    Text(
                                        text = instance.ec2RegionName,
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                } else {
                                    Text(
                                        text = instance.ec2Region ?: "-",
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                            
                            // 状态
                            Text(
                                text = when (instance.status) {
                                    "pending" -> "等待中"
                                    "creating" -> "创建中"
                                    "running" -> "运行中"
                                    "deleting" -> "删除中"
                                    "deleted" -> "已删除"
                                    "error" -> "异常"
                                    else -> instance.status ?: "-"
                                },
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.labelMedium,
                                color = when (instance.status) {
                                    "pending" -> MaterialTheme.colorScheme.onSurfaceVariant
                                    "creating" -> MaterialTheme.colorScheme.primary
                                    "running" -> androidx.compose.ui.graphics.Color(0xFF008000)
                                    "deleting" -> MaterialTheme.colorScheme.secondary
                                    "deleted" -> MaterialTheme.colorScheme.error
                                    "error" -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            
                            // IP
                            Text(
                                text = instance.ec2PublicIp ?: "-",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
                        // 第二行：两个按钮
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onCopyDirect,
                                modifier = Modifier.weight(1f),
                                shape = MaterialTheme.shapes.medium,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    Icons.Filled.CopyAll,
                                    contentDescription = "复制",
                                    modifier = Modifier.width(14.dp).height(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "vmess直连链接",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            OutlinedButton(
                                onClick = onCopyRelay,
                                modifier = Modifier.weight(1f),
                                shape = MaterialTheme.shapes.medium,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    Icons.Filled.CopyAll,
                                    contentDescription = "复制",
                                    modifier = Modifier.width(14.dp).height(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "vmess中转链接",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
}