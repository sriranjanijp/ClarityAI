package com.clarity.ai.model

data class FileInfo(
    val id: Long,
    val name: String,
    val size: Long,
    val lastModified: Long,
    val mimeType: String,
    val path: String,
    val isAnalyzed: Boolean = false
)