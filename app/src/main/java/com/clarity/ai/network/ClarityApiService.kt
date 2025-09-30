package com.clarity.ai.network

import retrofit2.Response
import retrofit2.http.*
import okhttp3.MultipartBody

// Document Analysis Response - matches your backend exactly
data class DocumentAnalysisResponse(
    val filename: String,
    val file_type: String,
    val file_size_bytes: Int,
    val page_count: Int?,
    val content_hash: String?,
    val summary: String?,
    val date_analyzed: String,
    val mobile_summary: String?,
    val file_size_formatted: String?,
    val recommendations: List<String>?,
    val relevance_score: Float?
)

// Image Analysis Response - matches your backend exactly
data class ImageAnalysisResponse(
    val filename: String,
    val image_hash: String,
    val file_size_bytes: Int,
    val date_analyzed: String,
    val mobile_summary: String?,
    val file_size_formatted: String?,
    val recommendations: List<String>?,
    val relevance_score: Float?
)

// Dashboard Response - matches your backend exactly
data class DashboardResponse(
    val summary_stats: SummaryStats,
    val storage_breakdown: List<StorageBreakdown>,
    val total_documents: Int,
    val total_images: Int,
    val storage_used: Long
)

data class SummaryStats(
    val total_files_analyzed: Int,
    val reclaimable_storage_mb: Float
)

data class StorageBreakdown(
    val category_name: String,
    val file_count: Int,
    val total_size_mb: Float
)

// Cleanup Suggestions Response - matches your backend exactly
data class CleanupSuggestionsResponse(
    val suggestions: List<CleanupSuggestion>
)

data class CleanupSuggestion(
    val type: String,
    val description: String,
    val files_affected: Int,
    val storage_saved: String
)

// Mobile Insights Response - matches your backend exactly
data class MobileInsightsResponse(
    val storage_overview: StorageOverview,
    val top_recommendations: List<String>,
    val file_breakdown: FileBreakdown,
    val recent_analysis: RecentAnalysis?
)

data class StorageOverview(
    val total_files_analyzed: Int,
    val storage_used_mb: Float,
    val potential_savings_mb: Float,
    val optimization_score: Float
)

data class FileBreakdown(
    val images: Int,
    val documents: Int,
    val videos: Int,
    val other: Int
)

data class RecentAnalysis(
    val documents: List<RecentDocument>,
    val images: List<RecentImage>
)

data class RecentDocument(
    val filename: String,
    val file_type: String,
    val file_size_bytes: Int,
    val page_count: Int?,
    val content_hash: String?,
    val summary: String?,
    val date_analyzed: String
)

data class RecentImage(
    val filename: String,
    val image_hash: String,
    val file_size_bytes: Int,
    val date_analyzed: String
)

// Rename Suggestion
data class RenameSuggestion(
    val original_filename: String,
    val suggested_filename: String,
    val reason: String
)

// Document from /documents endpoint
data class DocumentRecord(
    val filename: String,
    val file_type: String,
    val file_size_bytes: Int,
    val page_count: Int?,
    val content_hash: String?,
    val summary: String?,
    val date_analyzed: String,
    val mobile_summary: String?,
    val file_size_formatted: String?,
    val recommendations: List<String>?,
    val relevance_score: Float?
)

// Image from /images endpoint
data class ImageRecord(
    val filename: String,
    val image_hash: String,
    val file_size_bytes: Int,
    val date_analyzed: String,
    val mobile_summary: String?,
    val file_size_formatted: String?,
    val recommendations: List<String>?,
    val relevance_score: Float?
)

interface ClarityApiService {

    @GET("/dashboard")
    suspend fun getDashboard(): Response<DashboardResponse>

    @Multipart
    @POST("/analyze/document")
    suspend fun analyzeDocument(
        @Part file: MultipartBody.Part
    ): Response<DocumentAnalysisResponse>

    @Multipart
    @POST("/analyze/image")
    suspend fun analyzeImage(
        @Part file: MultipartBody.Part
    ): Response<ImageAnalysisResponse>

    @GET("/suggestions")
    suspend fun getCleanupSuggestions(): Response<CleanupSuggestionsResponse>

    @GET("/rename/suggestions")
    suspend fun getRenameSuggestions(): Response<List<RenameSuggestion>>

    @GET("/documents")
    suspend fun getAllDocuments(): Response<List<DocumentRecord>>

    @GET("/images")
    suspend fun getAllImages(): Response<List<ImageRecord>>

    @GET("/mobile/insights")
    suspend fun getMobileInsights(): Response<MobileInsightsResponse>
}