package com.clarity.ai.data.repository

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.Manifest
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.clarity.ai.model.FileInfo
import com.clarity.ai.model.FileAnalysis
import com.clarity.ai.model.StorageInsights
import com.clarity.ai.network.NetworkModule
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class FileRepository(private val context: Context) {

    private val contentResolver: ContentResolver = context.contentResolver
    private val apiService = NetworkModule.apiService

    private fun hasFilePermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            ).all { permission ->
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    suspend fun scanAllAccessibleFiles(): List<FileInfo> = withContext(Dispatchers.IO) {
        val allFiles = mutableListOf<FileInfo>()

        if (hasFilePermissions()) {
            // Scan real device files
            allFiles.addAll(scanImages())
            allFiles.addAll(scanVideos())
            allFiles.addAll(scanAudio())
            allFiles.addAll(scanDocuments())
        }

        // Only add sample files if no real files found OR backend not connected
        if (allFiles.isEmpty()) {
            val isBackendConnected = testBackendConnection()
            if (!isBackendConnected) {
                allFiles.addAll(generateSampleFiles())
            }
            // If backend *is* connected, do nothing (no samples)
        }

        return@withContext allFiles.distinctBy { it.name }
    }

    private suspend fun scanImages(): List<FileInfo> {
        return scanMediaStore(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.DATE_MODIFIED,
                MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.DATA
            )
        )
    }

    private suspend fun scanVideos(): List<FileInfo> {
        return scanMediaStore(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATE_MODIFIED,
                MediaStore.Video.Media.MIME_TYPE,
                MediaStore.Video.Media.DATA
            )
        )
    }

    private suspend fun scanAudio(): List<FileInfo> {
        return scanMediaStore(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.DATE_MODIFIED,
                MediaStore.Audio.Media.MIME_TYPE,
                MediaStore.Audio.Media.DATA
            )
        )
    }

    private suspend fun scanDocuments(): List<FileInfo> {
        return scanMediaStore(
            MediaStore.Files.getContentUri("external"),
            arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.DATE_MODIFIED,
                MediaStore.Files.FileColumns.MIME_TYPE,
                MediaStore.Files.FileColumns.DATA
            ),
            selection = "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'application/pdf' OR " +
                    "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'application/msword' OR " +
                    "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' OR " +
                    "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'text/%'"
        )
    }

    private suspend fun scanMediaStore(
        uri: android.net.Uri,
        projection: Array<String>,
        selection: String? = null
    ): List<FileInfo> {
        val files = mutableListOf<FileInfo>()

        try {
            contentResolver.query(
                uri,
                projection,
                selection,
                null,
                "${projection[3]} DESC" // Increased limit to show more files
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(0)
                    val name = cursor.getString(1) ?: "Unknown"
                    val size = cursor.getLong(2)
                    val lastModified = cursor.getLong(3) * 1000
                    val mimeType = cursor.getString(4) ?: "unknown"
                    val path = cursor.getString(5) ?: ""

                    if (size > 0 && name.isNotEmpty()) {
                        files.add(
                            FileInfo(
                                id = id,
                                name = name,
                                size = size,
                                lastModified = lastModified,
                                mimeType = mimeType,
                                path = path
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return files
    }

    private fun generateSampleFiles(): List<FileInfo> {
        val sampleFiles = mutableListOf<FileInfo>()
        val currentTime = System.currentTimeMillis()

        // Note: Sample files only shown when no real files found AND backend disconnected
        sampleFiles.add(FileInfo(
            id = 9001L,
            name = "[DEMO] sample_document.pdf",
            size = 1_200_000L,
            lastModified = currentTime - (24 * 60 * 60 * 1000),
            mimeType = "application/pdf",
            path = "/sample/sample_document.pdf"
        ))

        sampleFiles.add(FileInfo(
            id = 9002L,
            name = "[DEMO] sample_image.jpg",
            size = 2_500_000L,
            lastModified = currentTime - (12 * 60 * 60 * 1000),
            mimeType = "image/jpeg",
            path = "/sample/sample_image.jpg"
        ))

        sampleFiles.add(FileInfo(
            id = 9003L,
            name = "[DEMO] sample_video.mp4",
            size = 15_000_000L,
            lastModified = currentTime - (48 * 60 * 60 * 1000),
            mimeType = "video/mp4",
            path = "/sample/sample_video.mp4"
        ))

        return sampleFiles
    }

    // REAL FILE UPLOAD AND ANALYSIS
    suspend fun performAIAnalysis(fileInfo: FileInfo): FileAnalysis {
        return withContext(Dispatchers.IO) {
            try {
                val isConnected = testBackendConnection()
                android.util.Log.d("FileRepository", "Backend connected: $isConnected")
                if (!isConnected) {
                    android.util.Log.d("FileRepository", "Backend unavailable, using local analysis")
                    return@withContext createLocalAnalysis(fileInfo, backendAvailable = false)
                }
                if (fileInfo.path.contains("/sample/")) {
                    android.util.Log.d("FileRepository", "Sample file path, skipping upload: " + fileInfo.path)
                    return@withContext createLocalAnalysis(fileInfo, backendAvailable = true, isSample = true)
                }
                val file = File(fileInfo.path)
                android.util.Log.d("FileRepository", "File exists: ${file.exists()}, canRead: ${file.canRead()}, path: ${file.absolutePath}")
                if (!file.exists() || !file.canRead()) {
                    return@withContext createLocalAnalysis(fileInfo, backendAvailable = true)
                }
                android.util.Log.d("FileRepository", "Attempting backend upload for: ${file.name}")
                uploadAndAnalyzeFile(file, fileInfo)
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("FileRepository", "Upload failed: ${e.message}")
                createLocalAnalysis(fileInfo, backendAvailable = false)
            }
        }
    }


    private suspend fun uploadAndAnalyzeFile(file: File, fileInfo: FileInfo): FileAnalysis {
        return try {
            android.util.Log.d("FileRepository", "Uploading to backend: ${file.name}, type: ${fileInfo.mimeType}")

            when {
                fileInfo.mimeType.startsWith("image/") -> analyzeImageWithBackend(file, fileInfo)
                fileInfo.mimeType == "application/pdf" -> analyzeDocumentWithBackend(file, fileInfo)
                fileInfo.mimeType.contains("word") -> analyzeDocumentWithBackend(file, fileInfo)
                fileInfo.mimeType.startsWith("text/") -> analyzeDocumentWithBackend(file, fileInfo)
                else -> createLocalAnalysis(fileInfo, backendAvailable = true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("FileRepository", "Analysis failed: ${e.message}")
            createLocalAnalysis(fileInfo, backendAvailable = true, uploadFailed = true)
        }
    }

    private suspend fun analyzeImageWithBackend(file: File, fileInfo: FileInfo): FileAnalysis {
        return try {
            android.util.Log.d("FileRepository", "POST /analyze/image: ${file.name}")

            val requestFile = file.asRequestBody(fileInfo.mimeType.toMediaTypeOrNull())
            val multipartBody = MultipartBody.Part.createFormData("file", file.name, requestFile)

            val response = apiService.analyzeImage(multipartBody)

            android.util.Log.d("FileRepository", "Response code: ${response.code()}")

            if (response.isSuccessful && response.body() != null) {
                val result = response.body()!!
                android.util.Log.d("FileRepository", "Success! Hash: ${result.image_hash}")

                FileAnalysis(
                    fileInfo = fileInfo,
                    isDuplicate = false,
                    relevanceScore = result.relevance_score ?: 0.7f,
                    recommendedAction = result.recommendations?.joinToString("\n") ?: "Image analyzed successfully",
                    contentSummary = buildString {
                        append("Backend AI Analysis\n\n")
                        append("Image Hash: ${result.image_hash}\n")
                        append("Size: ${result.file_size_formatted ?: formatFileSize(result.file_size_bytes.toLong())}\n")
                        append("Analyzed: ${result.date_analyzed.substringBefore("T")}\n")
                        result.mobile_summary?.let { append("\n$it") }
                    }
                )
            } else {
                android.util.Log.e("FileRepository", "Upload failed: ${response.code()} - ${response.message()}")
                createLocalAnalysis(fileInfo, backendAvailable = true, uploadFailed = true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("FileRepository", "Upload exception: ${e.message}")
            createLocalAnalysis(fileInfo, backendAvailable = true, uploadFailed = true)
        }
    }

    private suspend fun analyzeDocumentWithBackend(file: File, fileInfo: FileInfo): FileAnalysis {
        return try {
            android.util.Log.d("FileRepository", "POST /analyze/document: ${file.name}")

            val requestFile = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
            val multipartBody = MultipartBody.Part.createFormData("file", file.name, requestFile)

            val response = apiService.analyzeDocument(multipartBody)

            android.util.Log.d("FileRepository", "Response code: ${response.code()}")

            if (response.isSuccessful && response.body() != null) {
                val result = response.body()!!
                android.util.Log.d("FileRepository", "Success! Summary: ${result.summary?.take(50)}...")

                FileAnalysis(
                    fileInfo = fileInfo,
                    isDuplicate = false,
                    relevanceScore = result.relevance_score ?: 0.5f,
                    recommendedAction = result.recommendations?.joinToString("\n") ?: "Document analyzed successfully",
                    contentSummary = buildString {
                        append("Backend AI Analysis\n\n")
                        append("DistilBART Summary:\n")
                        result.summary?.let { append("\"$it\"\n\n") }
                        result.page_count?.let { append("Pages: $it\n") }
                        append("Size: ${result.file_size_formatted ?: formatFileSize(result.file_size_bytes.toLong())}\n")
                        result.content_hash?.let { append("Hash: ${it.take(16)}...\n") }
                        append("Analyzed: ${result.date_analyzed.substringBefore("T")}")
                    }
                )
            } else {
                android.util.Log.e("FileRepository", "Upload failed: ${response.code()} - ${response.message()}")
                createLocalAnalysis(fileInfo, backendAvailable = true, uploadFailed = true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("FileRepository", "Upload exception: ${e.message}")
            createLocalAnalysis(fileInfo, backendAvailable = true, uploadFailed = true)
        }
    }

    private fun createLocalAnalysis(
        fileInfo: FileInfo,
        backendAvailable: Boolean = false,
        isSample: Boolean = false,
        uploadFailed: Boolean = false
    ): FileAnalysis {
        val statusMessage = when {
            uploadFailed -> "Upload failed - using local analysis"
            !backendAvailable -> "Backend unavailable - local analysis"
            isSample -> "Sample file - local analysis"
            else -> "Local analysis"
        }

        val recommendation = when {
            fileInfo.mimeType.startsWith("image/") -> {
                if (fileInfo.size > 5_000_000L) "Large image - Consider compression"
                else "Standard image file"
            }
            fileInfo.mimeType == "application/pdf" -> {
                if (fileInfo.size > 10_000_000L) "Large PDF - Consider compression"
                else "PDF document"
            }
            else -> "Standard file"
        }

        val relevanceScore = when {
            fileInfo.size > 100_000_000L -> 0.3f
            (System.currentTimeMillis() - fileInfo.lastModified) < (7 * 24 * 60 * 60 * 1000) -> 0.9f
            (System.currentTimeMillis() - fileInfo.lastModified) > (365 * 24 * 60 * 60 * 1000) -> 0.2f
            else -> 0.7f
        }

        return FileAnalysis(
            fileInfo = fileInfo,
            isDuplicate = false,
            relevanceScore = relevanceScore,
            recommendedAction = recommendation,
            contentSummary = "$statusMessage\n\nFile: ${fileInfo.name}\nSize: ${formatFileSize(fileInfo.size)}\n$recommendation"
        )
    }

    suspend fun generateStorageInsights(files: List<FileInfo>): StorageInsights {
        return withContext(Dispatchers.IO) {
            try {
                val isConnected = testBackendConnection()

                if (isConnected) {
                    getBackendInsights(files)
                } else {
                    generateLocalStorageInsights(files)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                generateLocalStorageInsights(files)
            }
        }
    }

    private suspend fun getBackendInsights(files: List<FileInfo>): StorageInsights {
        try {
            val response = apiService.getMobileInsights()
            if (response.isSuccessful && response.body() != null) {
                val insights = response.body()!!
                val realFiles = files.filter { !it.path.contains("/sample/") }
                val sampleFiles = files.filter { it.path.contains("/sample/") }

                return StorageInsights(
                    totalFiles = insights.storage_overview.total_files_analyzed,
                    totalSize = (insights.storage_overview.storage_used_mb * 1024 * 1024).toLong(),
                    duplicateFiles = insights.storage_overview.potential_savings_mb.toInt(),
                    duplicateSize = (insights.storage_overview.potential_savings_mb * 1024 * 1024).toLong(),
                    largestFiles = files.sortedByDescending { it.size }.take(5),
                    oldestFiles = files.sortedBy { it.lastModified }.take(5),
                    recommendations = buildList {
                        add("Connected to AI backend")
                        add("Optimization Score: ${insights.storage_overview.optimization_score}/10")
                        if (realFiles.isNotEmpty()) {
                            add("Analyzing ${realFiles.size} files from device")
                        }
                        if (sampleFiles.isNotEmpty()) {
                            add("${sampleFiles.size} demo files shown")
                        }
                        addAll(insights.top_recommendations)
                    }
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return generateLocalStorageInsights(files)
    }

    private fun generateLocalStorageInsights(files: List<FileInfo>): StorageInsights {
        val realFiles = files.filter { !it.path.contains("/sample/") }
        val sampleFiles = files.filter { it.path.contains("/sample/") }
        val totalSize = files.sumOf { it.size }
        val largeFiles = files.filter { it.size > 50_000_000L }
        val oldFiles = files.filter {
            (System.currentTimeMillis() - it.lastModified) > (365 * 24 * 60 * 60 * 1000L)
        }

        val recommendations = mutableListOf<String>()

        if (realFiles.isNotEmpty()) {
            recommendations.add("Analyzed ${realFiles.size} files from device")
        } else {
            recommendations.add("No files found on device")
        }

        if (sampleFiles.isNotEmpty()) {
            recommendations.add("${sampleFiles.size} demo files shown")
        }

        if (largeFiles.isNotEmpty()) {
            recommendations.add("Found ${largeFiles.size} large files (>50MB)")
        }
        if (oldFiles.isNotEmpty()) {
            recommendations.add("${oldFiles.size} files over 1 year old")
        }

        if (realFiles.isEmpty()) {
            recommendations.add("Grant permissions or upload files to scan")
        } else {
            recommendations.add("Connect to backend for AI analysis")
        }

        return StorageInsights(
            totalFiles = files.size,
            totalSize = totalSize,
            duplicateFiles = 0,
            duplicateSize = 0L,
            largestFiles = files.sortedByDescending { it.size }.take(5),
            oldestFiles = files.sortedBy { it.lastModified }.take(5),
            recommendations = recommendations
        )
    }

    suspend fun testBackendConnection(): Boolean {
        return try {
            val response = apiService.getDashboard()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    private fun formatFileSize(bytes: Long): String {
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
}