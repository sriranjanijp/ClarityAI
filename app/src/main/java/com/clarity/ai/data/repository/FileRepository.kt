package com.clarity.ai.data.repository

import android.content.ContentResolver
import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.clarity.ai.model.FileInfo
import com.clarity.ai.model.FileAnalysis
import com.clarity.ai.model.StorageInsights
import java.util.Random

class FileRepository(private val context: Context) {

    private val contentResolver: ContentResolver = context.contentResolver
    private val random = Random()

    suspend fun scanAllAccessibleFiles(): List<FileInfo> = withContext(Dispatchers.IO) {
        val allFiles = mutableListOf<FileInfo>()

        // Scan media files (images, videos, audio)
        allFiles.addAll(scanMediaStore())

        // Add sample files for emulator testing
        allFiles.addAll(generateSampleFiles())

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
                null,
                null,
                MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC"
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val file = FileInfo(
                        id = cursor.getLong(0),
                        name = cursor.getString(1) ?: "Unknown",
                        size = cursor.getLong(2),
                        lastModified = cursor.getLong(3),
                        mimeType = cursor.getString(4) ?: "unknown",
                        path = cursor.getString(5) ?: ""
                    )
                    files.add(file)
                }
            }
        } catch (e: Exception) {
            // In case of permission issues, we'll rely on sample data
            e.printStackTrace()
        }

        return files
    }

    // Generate sample files for emulator testing
    private fun generateSampleFiles(): List<FileInfo> {
        val sampleFiles = mutableListOf<FileInfo>()
        val currentTime = System.currentTimeMillis()

        // Sample images
        sampleFiles.add(FileInfo(
            id = 1001L,
            name = "vacation_photo_1.jpg",
            size = 2_500_000L, // 2.5 MB
            lastModified = currentTime - (24 * 60 * 60 * 1000), // 1 day ago
            mimeType = "image/jpeg",
            path = "/storage/emulated/0/DCIM/Camera/vacation_photo_1.jpg"
        ))

        sampleFiles.add(FileInfo(
            id = 1002L,
            name = "vacation_photo_2.jpg",
            size = 2_450_000L, // Similar size - potential duplicate
            lastModified = currentTime - (24 * 60 * 60 * 1000), // 1 day ago
            mimeType = "image/jpeg",
            path = "/storage/emulated/0/DCIM/Camera/vacation_photo_2.jpg"
        ))

        // Sample videos
        sampleFiles.add(FileInfo(
            id = 1003L,
            name = "birthday_video.mp4",
            size = 25_000_000L, // 25 MB
            lastModified = currentTime - (7 * 24 * 60 * 60 * 1000), // 1 week ago
            mimeType = "video/mp4",
            path = "/storage/emulated/0/DCIM/Camera/birthday_video.mp4"
        ))

        // Sample documents
        sampleFiles.add(FileInfo(
            id = 1004L,
            name = "important_document.pdf",
            size = 1_200_000L, // 1.2 MB
            lastModified = currentTime - (30 * 24 * 60 * 60 * 1000), // 1 month ago
            mimeType = "application/pdf",
            path = "/storage/emulated/0/Download/important_document.pdf"
        ))

        sampleFiles.add(FileInfo(
            id = 1005L,
            name = "old_document.pdf",
            size = 850_000L, // 850 KB
            lastModified = currentTime - (365 * 24 * 60 * 60 * 1000), // 1 year ago
            mimeType = "application/pdf",
            path = "/storage/emulated/0/Download/old_document.pdf"
        ))

        // Sample music
        sampleFiles.add(FileInfo(
            id = 1006L,
            name = "favorite_song.mp3",
            size = 4_500_000L, // 4.5 MB
            lastModified = currentTime - (10 * 24 * 60 * 60 * 1000), // 10 days ago
            mimeType = "audio/mpeg",
            path = "/storage/emulated/0/Music/favorite_song.mp3"
        ))

        // Large file sample
        sampleFiles.add(FileInfo(
            id = 1007L,
            name = "large_video_file.mov",
            size = 150_000_000L, // 150 MB
            lastModified = currentTime - (90 * 24 * 60 * 60 * 1000), // 3 months ago
            mimeType = "video/quicktime",
            path = "/storage/emulated/0/Movies/large_video_file.mov"
        ))

        // Duplicate sample
        sampleFiles.add(FileInfo(
            id = 1008L,
            name = "vacation_photo_1_copy.jpg",
            size = 2_500_000L, // Same size as vacation_photo_1.jpg
            lastModified = currentTime - (23 * 60 * 60 * 1000), // 23 hours ago
            mimeType = "image/jpeg",
            path = "/storage/emulated/0/Download/vacation_photo_1_copy.jpg"
        ))

        return sampleFiles
    }

    suspend fun performAIAnalysis(fileInfo: FileInfo): FileAnalysis {
        return withContext(Dispatchers.IO) {
            // Simulate AI analysis with realistic results
            FileAnalysis(
                fileInfo = fileInfo,
                isDuplicate = checkForDuplicates(fileInfo),
                relevanceScore = calculateRelevanceScore(fileInfo),
                recommendedAction = generateRecommendation(fileInfo),
                contentSummary = generateContentSummary(fileInfo)
            )
        }
    }

    private fun checkForDuplicates(fileInfo: FileInfo): Boolean {
        // Simple duplicate detection based on filename similarity and size
        return fileInfo.name.contains("copy") ||
                fileInfo.name.contains("duplicate") ||
                (fileInfo.name.contains("vacation_photo") && fileInfo.size > 2_400_000L)
    }

    private fun calculateRelevanceScore(fileInfo: FileInfo): Float {
        val daysSinceModified = (System.currentTimeMillis() - fileInfo.lastModified) / (24 * 60 * 60 * 1000)

        return when {
            daysSinceModified <= 7 -> 0.9f + random.nextFloat() * 0.1f // Recent files: 0.9-1.0
            daysSinceModified <= 30 -> 0.7f + random.nextFloat() * 0.2f // Month old: 0.7-0.9
            daysSinceModified <= 90 -> 0.4f + random.nextFloat() * 0.3f // 3 months: 0.4-0.7
            else -> 0.1f + random.nextFloat() * 0.3f // Very old: 0.1-0.4
        }
    }

    private fun generateRecommendation(fileInfo: FileInfo): String {
        val daysSinceModified = (System.currentTimeMillis() - fileInfo.lastModified) / (24 * 60 * 60 * 1000)
        val sizeMB = fileInfo.size / (1024 * 1024)

        return when {
            checkForDuplicates(fileInfo) -> "🔄 Potential duplicate - Consider removing"
            sizeMB > 100 && daysSinceModified > 90 -> "🗑️ Large old file - Safe to delete"
            daysSinceModified > 365 -> "📁 Very old file - Archive or delete"
            daysSinceModified <= 7 -> "✅ Recently used - Keep"
            fileInfo.mimeType.startsWith("image/") -> "🖼️ Photo file - Review for duplicates"
            fileInfo.mimeType.startsWith("video/") -> "🎥 Video file - Large storage impact"
            else -> "📄 Regular file - Monitor usage"
        }
    }

    private fun generateContentSummary(fileInfo: FileInfo): String {
        return when {
            fileInfo.mimeType.startsWith("image/") -> "📸 Image file: ${formatFileSize(fileInfo.size)}, likely contains visual content"
            fileInfo.mimeType.startsWith("video/") -> "🎬 Video file: ${formatFileSize(fileInfo.size)}, may contain important memories"
            fileInfo.mimeType.startsWith("audio/") -> "🎵 Audio file: ${formatFileSize(fileInfo.size)}, music or recording"
            fileInfo.mimeType == "application/pdf" -> "📋 PDF document: ${formatFileSize(fileInfo.size)}, text-based content"
            else -> "📄 File: ${formatFileSize(fileInfo.size)}, ${fileInfo.mimeType}"
        }
    }

    suspend fun generateStorageInsights(files: List<FileInfo>): StorageInsights {
        return withContext(Dispatchers.IO) {
            val duplicates = files.filter { checkForDuplicates(it) }

            StorageInsights(
                totalFiles = files.size,
                totalSize = files.sumOf { it.size },
                duplicateFiles = duplicates.size,
                duplicateSize = duplicates.sumOf { it.size },
                largestFiles = files.sortedByDescending { it.size }.take(5),
                oldestFiles = files.sortedBy { it.lastModified }.take(5),
                recommendations = generateRecommendations(files)
            )
        }
    }

    private fun generateRecommendations(files: List<FileInfo>): List<String> {
        val recommendations = mutableListOf<String>()

        val largeFiles = files.filter { it.size > 50_000_000L } // > 50MB
        val oldFiles = files.filter {
            (System.currentTimeMillis() - it.lastModified) > (365 * 24 * 60 * 60 * 1000L)
        }
        val duplicates = files.filter { checkForDuplicates(it) }

        if (largeFiles.isNotEmpty()) {
            recommendations.add("🎯 Found ${largeFiles.size} large files (>50MB) - Review for cleanup")
        }

        if (oldFiles.isNotEmpty()) {
            recommendations.add("📅 ${oldFiles.size} files are over 1 year old - Consider archiving")
        }

        if (duplicates.isNotEmpty()) {
            recommendations.add("🔄 ${duplicates.size} potential duplicates detected - Could free ${formatFileSize(duplicates.sumOf { it.size })}")
        }

        val totalSizeGB = files.sumOf { it.size } / (1024.0 * 1024.0 * 1024.0)
        if (totalSizeGB > 5) {
            recommendations.add("💾 Total storage: ${String.format("%.1f GB", totalSizeGB)} - Monitor usage regularly")
        }

        return recommendations
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