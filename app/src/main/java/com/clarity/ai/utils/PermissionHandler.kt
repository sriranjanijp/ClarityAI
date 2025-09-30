package com.clarity.ai.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionHandler(
    onPermissionsGranted: () -> Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO
        )
    } else {
        listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    val multiplePermissionsState = rememberMultiplePermissionsState(permissions)

    LaunchedEffect(multiplePermissionsState.allPermissionsGranted) {
        if (multiplePermissionsState.allPermissionsGranted) {
            onPermissionsGranted()
        }
    }

    when {
        multiplePermissionsState.allPermissionsGranted -> {
            content()
        }
        multiplePermissionsState.shouldShowRationale -> {
            PermissionRationaleDialog(
                onRequestPermissions = { multiplePermissionsState.launchMultiplePermissionRequest() },
                onDismiss = { /* Continue with sample data */ }
            )
            content() // Show content with sample data
        }
        else -> {
            PermissionRequestScreen(
                onRequestPermissions = { multiplePermissionsState.launchMultiplePermissionRequest() }
            )
            content() // Show content with sample data
        }
    }
}

@Composable
fun PermissionRationaleDialog(
    onRequestPermissions: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("File Access Permission") },
        text = {
            Text(
                "Clarity AI needs access to your files to analyze them with AI. " +
                        "Without permission, we'll use sample data for demonstration.",
                textAlign = TextAlign.Start
            )
        },
        confirmButton = {
            TextButton(onClick = onRequestPermissions) {
                Text("Grant Permission")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Use Sample Data")
            }
        }
    )
}

@Composable
fun PermissionRequestScreen(
    onRequestPermissions: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "📁",
                style = MaterialTheme.typography.displayMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "File Access Needed",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "To analyze your actual files with AI, Clarity needs access to your media files. You can also continue with sample data.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRequestPermissions,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Grant File Access")
            }
        }
    }
}