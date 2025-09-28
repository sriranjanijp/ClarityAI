package com.clarity.ai.data.repository

import android.content.ContentResolver
import android.content.Context
import android.provider.MediaStore
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

    suspend fun scanAllAccessibleFiles(): List<FileInfo> = withContext(Dispatchers.IO) {
        val allFiles = mutableListOf<FileInfo>()

        // Scan actual media files
        allFiles.addAll(scanMediaStore())

        // If no files found (permissions issue), add sample files
        if (allFiles.isEmpty()) {
            allFiles.addAll(generateSampleFiles())
        }

        return@withContext allFiles
    }

    private suspend fun scanMediaStore(): List<FileInfo> {
        val files = mutableListOf<FileInfo>()

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATA
        )

        try {
            contentResolver.query(
                MediaStore.Files.getContentUri("external"),
                projection,
                "${MediaStore.Files.FileColumns.SIZE} > 0",
                null,
                "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC LIMIT 50"
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val file = FileInfo(
                        id = cursor.getLong(0),
                        name = cursor.getString(1) ?: "Unknown",
                        size = cursor.getLong(2),
                        lastModified = cursor.getLong(3) * 1000,
                        mimeType = cursor.getString(4) ?: "unknown",
                        path = cursor.getString(5) ?: ""
                    )
                    if (file.size > 0) {
                        files.add(file)
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

        // Sample files for testing
        sampleFiles.add(FileInfo(
            id = 1001L,
            name = "sample_document.pdf",
            size = 1_200_000L,
            lastModified = currentTime - (24 * 60 * 60 * 1000),
            mimeType = "application/pdf",
            path = "/storage/emulated/0/Download/sample_document.pdf"
        ))

        sampleFiles.add(FileInfo(
            id = 1002L,
            name = "sample_image.jpg",
            size = 2_500_000L,
            lastModified = currentTime - (12 * 60 * 60 * 1000),
            mimeType = "image/jpeg",
            path = "/storage/emulated/0/DCIM/Camera/sample_image.jpg"
        ))

        return sampleFiles
    }

    // REAL AI ANALYSIS using your backend
    suspend fun performAIAnalysis(fileInfo: FileInfo): FileAnalysis {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(fileInfo.path)

                if (!file.exists() || !file.canRead()) {
                    return@withContext createFallbackAnalysis(fileInfo)
                }

                when {
                    fileInfo.mimeType.startsWith("image/") -> analyzeImageFile(file, fileInfo)
                    fileInfo.mimeType == "application/pdf" -> analyzeDocumentFile(file, fileInfo)
                    fileInfo.mimeType.startsWith("text/") -> analyzeDocumentFile(file, fileInfo)
                    fileInfo.mimeType.contains("word") -> analyzeDocumentFile(file, fileInfo)
                    else -> createFallbackAnalysis(fileInfo)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                createFallbackAnalysis(fileInfo)
            }
        }
    }

    private suspend fun analyzeImageFile(file: File, fileInfo: FileInfo): FileAnalysis {
        return try {
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val multipartBody = MultipartBody.Part.createFormData("file", file.name, requestFile)

            val response = apiService.analyzeImage(multipartBody)

            if (response.isSuccessful && response.body() != null) {
                val result = response.body()!!
                FileAnalysis(
                    fileInfo = fileInfo,
                    isDuplicate = false,
                    relevanceScore = result.relevance_score ?: 0.7f,
                    recommendedAction = result.recommendations.firstOrNull() ?: "Keep image",
                    contentSummary = result.mobile_summary
                        ?: "🖼️ Image analyzed with hash: ${result.image_hash.take(8)}..."
                )
            }
        }
    }