package com.clarity.ai.ui.screens

import android.app.Application // For Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign // For TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.clarity.ai.R
import com.clarity.ai.model.FileInfo
import com.clarity.ai.model.ScanState
import com.clarity.ai.model.StorageInsights
//import com.clarity.ai.ui.theme.ClarityAITheme // Assuming this is your theme
import com.clarity.ai.utils.PermissionHandler
// Assuming you will create and use this interface for previews
// import com.clarity.ai.viewmodel.MainScreenViewModelContract
import com.clarity.ai.viewmodel.FileAnalysisViewModel // Using concrete class for now as per original
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow // For potential interface
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: FileAnalysisViewModel) { // Original used FileAnalysisViewModel
    val scanState by viewModel.scanState.collectAsState()
    val analyzedFiles by viewModel.analyzedFiles.collectAsState()
    val storageInsights by viewModel.storageInsights.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()
    val hasPermissions by viewModel.hasFilePermissions.collectAsState()

    PermissionHandler(
        onPermissionsGranted = { viewModel.onPermissionsGranted() }
    ) {
        MainScreenContent(
            scanState = scanState,
            analyzedFiles = analyzedFiles,
            storageInsights = storageInsights,
            scanProgress = scanProgress,
            hasPermissions = hasPermissions,
            onStartScanClick = { viewModel.startFileScan() },
            onResetScanClick = { viewModel.resetScan() },
            onAnalyzeFileClick = { fileInfo -> viewModel.analyzeSpecificFile(fileInfo) }
        )
    }
}

@Composable
fun MainScreenContent(
    scanState: ScanState,
    analyzedFiles: List<FileInfo>,
    storageInsights: StorageInsights?,
    scanProgress: Float,
    hasPermissions: Boolean,
    onStartScanClick: () -> Unit,
    onResetScanClick: () -> Unit,
    onAnalyzeFileClick: (FileInfo) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 24.dp) // Increased vertical padding
    ) {
        ScreenHeader()
        Spacer(modifier = Modifier.height(24.dp)) // Increased space

        PermissionStatusIndicator(hasPermissions = hasPermissions)
        // Spacing now handled by PermissionStatusIndicator's internal spacer

        ScanControls(
            scanState = scanState,
            onStartScanClick = onStartScanClick,
            onResetScanClick = onResetScanClick
        )
        // Conditional spacing based on ScanProgressBar

        ScanProgressBar(scanState = scanState, scanProgress = scanProgress)
        // Conditional spacing based on StorageInsightsSection

        StorageInsightsSection(storageInsights = storageInsights)
        // Conditional spacing based on ScanErrorIndicator

        ScanErrorIndicator(scanState = scanState)

        if (storageInsights == null && scanState != ScanState.ERROR && scanState != ScanState.SCANNING) {
            Spacer(modifier = Modifier.height(16.dp))
        }

        AnalyzedFilesSection(
            scanState = scanState,
            analyzedFiles = analyzedFiles,
            hasPermissions = hasPermissions,
            onAnalyzeFileClick = onAnalyzeFileClick
        )
    }
}

@Composable
fun ScreenHeader() {
    Text(
        text = stringResource(R.string.app_name_styled),
        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 4.dp) // Slightly reduced
    )
    Text(
        text = stringResource(R.string.app_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
fun PermissionStatusIndicator(hasPermissions: Boolean) {
    PermissionStatusCard(hasPermissions = hasPermissions)
    Spacer(modifier = Modifier.height(20.dp)) // Standardized spacer
}

@Composable
fun ScanControls(
    scanState: ScanState,
    onStartScanClick: () -> Unit,
    onResetScanClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp), // Added vertical padding
        horizontalArrangement = Arrangement.spacedBy(12.dp) // Increased space
    ) {
        Button(
            onClick = onStartScanClick,
            modifier = Modifier.weight(1f),
            enabled = scanState != ScanState.SCANNING,
            contentPadding = PaddingValues(vertical = 12.dp) // Taller buttons
        ) {
            val buttonText = when (scanState) {
                ScanState.SCANNING -> stringResource(R.string.scanning_button)
                ScanState.COMPLETED -> stringResource(R.string.scan_again_button)
                ScanState.ERROR -> stringResource(R.string.retry_scan_button)
                else -> stringResource(R.string.start_ai_scan_button)
            }
            Text(text = buttonText)
        }

        if (scanState == ScanState.COMPLETED || scanState == ScanState.ERROR) { // Show Reset also on Error
            OutlinedButton(
                onClick = onResetScanClick,
                modifier = Modifier.weight(0.6f),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Text(stringResource(R.string.reset_button))
            }
        }
    }
}

@Composable
fun ScanProgressBar(scanState: ScanState, scanProgress: Float) {
    if (scanState == ScanState.SCANNING) {
        Column(modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)) {
            LinearProgressIndicator(
                progress = scanProgress, // Corrected from lambda
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp), // Slightly thicker
                trackColor = MaterialTheme.colorScheme.surfaceVariant // Custom track
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.analyzing_files_progress, (scanProgress * 100).toInt()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.End) // Align text
            )
        }
    }
}


@Composable
fun StorageInsightsSection(storageInsights: StorageInsights?) {
    storageInsights?.let { insights ->
        Spacer(modifier = Modifier.height(24.dp)) // Increased space
        StorageInsightsCard(insights = insights)
    }
}

@Composable
fun ScanErrorIndicator(scanState: ScanState) {
    if (scanState == ScanState.ERROR) {
        Spacer(modifier = Modifier.height(20.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.scan_error_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.scan_error_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
fun AnalyzedFilesSection(
    scanState: ScanState,
    analyzedFiles: List<FileInfo>,
    hasPermissions: Boolean,
    onAnalyzeFileClick: (FileInfo) -> Unit
) {
    if (analyzedFiles.isNotEmpty()) {
        Spacer(modifier = Modifier.height(24.dp)) // More space

        // Separate real files from sample files
        val realFiles = analyzedFiles.filter { !it.path.contains("/sample/") }
        val sampleFiles = analyzedFiles.filter { it.path.contains("/sample/") }

        // Show real files first if they exist
        if (realFiles.isNotEmpty()) {
            Text(
                text = stringResource(R.string.device_files_header, realFiles.size),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(realFiles, key = { it.id }) { fileInfo ->
                    FileInfoCard(
                        fileInfo = fileInfo,
                        onAnalyzeClick = { onAnalyzeFileClick(fileInfo) }
                    )
                }
            }
        }

        // Show sample files section if they exist
        if (sampleFiles.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.sample_files_header, sampleFiles.size),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = stringResource(R.string.sample_files_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sampleFiles, key = { it.id }) { fileInfo ->
                    SampleFileInfoCard(
                        fileInfo = fileInfo,
                        onAnalyzeClick = { onAnalyzeFileClick(fileInfo) }
                    )
                }
            }
        }
    } else if (scanState == ScanState.IDLE || (scanState == ScanState.COMPLETED && analyzedFiles.isEmpty())) {
        Spacer(modifier = Modifier.height(24.dp))
        WelcomeMessage(hasPermissions = hasPermissions)
    }
}

@Composable
fun WelcomeMessage(hasPermissions: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // Flatter
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp), // More padding
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🚀", // Consider R.string.welcome_icon
                style = MaterialTheme.typography.displayLarge, // Larger icon
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = stringResource(R.string.welcome_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Text(
                text = if (hasPermissions) {
                    stringResource(R.string.welcome_message_permissions_granted)
                } else {
                    stringResource(R.string.welcome_message_permissions_needed)
                },
                style = MaterialTheme.typography.bodyLarge, // Slightly larger
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center // Center align
            )
        }
    }
}

@Composable
fun PermissionStatusCard(hasPermissions: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (hasPermissions)
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f) // More subtle
            else
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(0.dp) // Flatter
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (hasPermissions) stringResource(R.string.permission_granted_icon) else stringResource(R.string.permission_info_icon),
                style = MaterialTheme.typography.titleLarge, // Larger icon
                modifier = Modifier.padding(end = 12.dp)
            )
            Text(
                text = if (hasPermissions) {
                    stringResource(R.string.permission_status_granted)
                } else {
                    stringResource(R.string.permission_status_sample_data)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (hasPermissions)
                    MaterialTheme.colorScheme.onSecondaryContainer
                else
                    MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
fun StorageInsightsCard(insights: StorageInsights) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.storage_insights_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp) // More space
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                InsightItem(
                    label = stringResource(R.string.insight_total_files),
                    value = insights.totalFiles.toString(),
                    icon = "📄" // Consider R.string.icon_total_files
                )
                InsightItem(
                    label = stringResource(R.string.insight_total_size),
                    value = formatFileSize(insights.totalSize),
                    icon = "💾" // Consider R.string.icon_total_size
                )
                InsightItem(
                    label = stringResource(R.string.insight_duplicates),
                    value =  formatFileSize(insights.duplicateSize), // Assuming duplicateFiles is part of StorageInsights
                    icon = "🔄" // Consider R.string.icon_duplicates
                )
            }

            if (insights.recommendations.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp)) // More space
                Text(
                    text = stringResource(R.string.ai_recommendations_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                insights.recommendations.forEach { recommendation ->
                    Row(modifier = Modifier.padding(bottom = 6.dp), verticalAlignment = Alignment.Top) {
                        Text("• ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            text = recommendation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface // Ensure text color is appropriate
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InsightItem(label: String, value: String, icon: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.displaySmall // Adjusted size
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge, // Larger value
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
    onAnalyzeClick: () -> Unit,
    isAnalyzing: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
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

                    // Device file indicator
                    Text(
                        text = "📱 Device file",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Show analysis status
                    if (fileInfo.isAnalyzed) {
                        Text(
                            text = "✅ Analyzed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else if (isAnalyzing) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Analyzing...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                FilledTonalButton(
                    onClick = onAnalyzeClick,
                    modifier = Modifier.padding(start = 8.dp),
                    enabled = !isAnalyzing && !fileInfo.isAnalyzed
                ) {
                    Text(if (fileInfo.isAnalyzed) "View" else "Analyze")
                }
            }
        }
    }
}

@Composable
fun SampleFileInfoCard(
    fileInfo: FileInfo,
    onAnalyzeClick: () -> Unit,
    isAnalyzing: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
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

                    // Sample file indicator
                    Text(
                        text = "ℹ️ Demo file",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )

                    // Show analysis status
                    if (fileInfo.isAnalyzed) {
                        Text(
                            text = "✅ Analyzed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else if (isAnalyzing) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Analyzing...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = onAnalyzeClick,
                    modifier = Modifier.padding(start = 8.dp),
                    enabled = !isAnalyzing && !fileInfo.isAnalyzed
                ) {
                    Text(if (fileInfo.isAnalyzed) "View" else "Demo")
                }
            }
        }
    }
}

// --- Utility Functions ---
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
    if (bytes < 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0

    return when {
        gb >= 1 -> String.format(Locale.getDefault(), "%.1f GB", gb)
        mb >= 1 -> String.format(Locale.getDefault(), "%.1f MB", mb)
        kb >= 1 -> String.format(Locale.getDefault(), "%.1f KB", kb)
        else -> "$bytes B"
    }
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}