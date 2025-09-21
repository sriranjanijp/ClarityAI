package com.clarity.ai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clarity.ai.viewmodel.FileAnalysisViewModel
import com.clarity.ai.model.ScanState
import com.clarity.ai.model.FileInfo
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: FileAnalysisViewModel) {
    val scanState by viewModel.scanState.collectAsState()
    val analyzedFiles by viewModel.analyzedFiles.collectAsState()
    val storageInsights by viewModel.storageInsights.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "🤖 Clarity AI",
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Intelligent File Analysis & Storage Optimization",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Scan Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.startFileScan() },
                modifier = Modifier.weight(1f),
                enabled = scanState != ScanState.SCANNING
            ) {
                Text(
                    text = when (scanState) {
                        ScanState.SCANNING -> "Scanning..."
                        ScanState.COMPLETED -> "Scan Again"
                        ScanState.ERROR -> "Retry Scan"
                        else -> "🔍 Start AI Scan"
                    }
                )
            }

            if (scanState == ScanState.COMPLETED) {
                OutlinedButton(
                    onClick = { viewModel.resetScan() },
                    modifier = Modifier.weight(0.5f)
                ) {
                    Text("Reset")
                }
            }
        }

        // Progress Bar
        if (scanState == ScanState.SCANNING) {
            LinearProgressIndicator(
                progress = scanProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
            Text(
                text = "Analyzing files... ${(scanProgress * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Storage Insights Card
        storageInsights?.let { insights ->
            StorageInsightsCard(insights = insights)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Error State
        if (scanState == ScanState.ERROR) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "❌ Scan Error",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "Unable to complete file scan. Please check permissions and try again.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // File List
        if (analyzedFiles.isNotEmpty()) {
            Text(
                text = "📁 Analyzed Files (${analyzedFiles.size})",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(analyzedFiles) { fileInfo ->
                    FileInfoCard(
                        fileInfo = fileInfo,
                        onAnalyzeClick = { viewModel.analyzeSpecificFile(fileInfo) }
                    )
                }
            }
        } else if (scanState == ScanState.IDLE) {
            // Welcome message
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🚀",
                        style = MaterialTheme.typography.displayMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Ready to Optimize Your Storage",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap 'Start AI Scan' to analyze your files and get intelligent storage recommendations.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun StorageInsightsCard(insights: com.clarity.ai.model.StorageInsights) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "📊 Storage Insights",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InsightItem(
                    label = "Total Files",
                    value = insights.totalFiles.toString(),
                    icon = "📄"
                )
                InsightItem(
                    label = "Total Size",
                    value = formatFileSize(insights.totalSize),
                    icon = "💾"
                )
                InsightItem(
                    label = "Duplicates",
                    value = insights.duplicateFiles.toString(),
                    icon = "🔄"
                )
            }

            if (insights.recommendations.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "💡 AI Recommendations:",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                insights.recommendations.forEach { recommendation ->
                    Text(
                        text = "• $recommendation",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun InsightItem(label: String, value: String, icon: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun FileInfoCard(
    fileInfo: FileInfo,
    onAnalyzeClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = getFileIcon(fileInfo.mimeType) + " " + fileInfo.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${formatFileSize(fileInfo.size)} • ${fileInfo.mimeType}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Modified: ${formatDate(fileInfo.lastModified)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledTonalButton(
                    onClick = onAnalyzeClick,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text("Analyze")
                }
            }
        }
    }
}

fun getFileIcon(mimeType: String): String {
    return when {
        mimeType.startsWith("image/") -> "🖼️"
        mimeType.startsWith("video/") -> "🎥"
        mimeType.startsWith("audio/") -> "🎵"
        mimeType == "application/pdf" -> "📋"
        mimeType.startsWith("text/") -> "📝"
        mimeType.startsWith("application/") -> "📄"
        else -> "📁"
    }
}

fun formatFileSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0

    return when {
        gb >= 1 -> String.format("%.1f GB", gb)
        mb >= 1 -> String.format("%.1f MB", mb)
        kb >= 1 -> String.format("%.1f KB", kb)
        else -> "$bytes B"
    }
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
