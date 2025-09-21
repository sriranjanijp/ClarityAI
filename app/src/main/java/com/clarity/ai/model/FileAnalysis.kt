package com.clarity.ai.model

data class FileAnalysis(
    val fileInfo: FileInfo,
    val isDuplicate: Boolean,
    val relevanceScore: Float,
    val recommendedAction: String,
    val contentSummary: String,
    val analysisTimestamp: Long = System.currentTimeMillis()
)