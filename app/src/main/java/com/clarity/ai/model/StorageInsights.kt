package com.clarity.ai.model

data class StorageInsights(
    val totalFiles: Int,
    val totalSize: Long,
    val duplicateFiles: Int,
    val duplicateSize: Long,
    val largestFiles: List<FileInfo>,
    val oldestFiles: List<FileInfo>,
    val recommendations: List<String>
)