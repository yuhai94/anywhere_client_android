package com.yuhai94.awcli.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateInstanceScreen(
    onBack: () -> Unit,
    onCreated: () -> Unit
) {
    val viewModel: CreateInstanceViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // 加载 regions 列表
    LaunchedEffect(Unit) {
        viewModel.loadRegions()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "创建实例",
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "选择Region", style = MaterialTheme.typography.titleMedium)
            if (uiState.isLoadingRegions) {
                // 显示加载状态
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "加载中...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.regions) { regionInfo ->
                        val selected = uiState.selectedRegion == regionInfo.region
                        androidx.compose.material3.OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { viewModel.selectRegion(regionInfo.region) },
                            colors = androidx.compose.material3.CardDefaults.cardColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                            ),
                            border = androidx.compose.material3.CardDefaults.outlinedCardBorder(
                                enabled = true
                            )
                        ) {
                            Text(
                                text = regionInfo.name,
                                modifier = Modifier.padding(16.dp),
                                style = if (selected) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error
                )
            }
            Button(
                onClick = { viewModel.submit(onCreated) },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.selectedRegion != null && !uiState.isSubmitting
            ) {
                Text(if (uiState.isSubmitting) "创建中..." else "确认创建")
            }
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("返回")
            }
        }
    }
}
