package com.clarity.ai.network

import retrofit2.Response
import retrofit2.http.*
import okhttp3.MultipartBody

// Response models matching your backend
data class DocumentAnalysisResponse(
    val filename: String,
    val file_type: String,
    val file_size_bytes: Int,
    val page_count: Int?,
    val content_hash: String?,
    val extracted_text: String?,
    val summary: String?,
    val date_analyzed: String,
    val recommendations: List<String>,
    val mobile_summary: String?,
    val relevance_score: Float?
)

data class ImageAnalysisResponse(
    val filename: String,
    val image_hash: String,
    val file_size_bytes: Int,
    val date_analyzed: String,
    val recommendations: List<String>,
    val mobile_summary: String?,
    val relevance_score: Float?,
    val objects_detected: List<String>
)

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

data class CleanupSuggestion(
    val type: String,
    val description: String,
    val files_affected: Int,
    val storage_saved: String
)

data class CleanupSuggestionsResponse(
    val suggestions: List<CleanupSuggestion>
)

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
    val documents: List<Map<String, Any>>,
    val images: List<Map<String, Any>>
)

data class BulkAnalysisResponse(
    val total_files: Int,
    val processed: Int,
    val failed: Int,
    val results: List<AnalysisResult>
)

data class AnalysisResult(
    val file_name: String,
    val type: String,
    val analysis: Map<String, Any>?,
    val status: String,
    val error: String?
)

interface ClarityApiService {

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

    @GET("/dashboard")
    suspend fun getDashboard(): Response<DashboardResponse>

    @GET("/suggestions")
    suspend fun getCleanupSuggestions(): Response<CleanupSuggestionsResponse>

    @GET("/mobile/insights")
    suspend fun getMobileInsights(): Response<MobileInsightsResponse>

    @GET("/documents")
    suspend fun getAllDocuments(): Response<List<Map<String, Any>>>

    @GET("/images")
    suspend fun getAllImages(): Response<List<Map<String, Any>>>

    @Multipart
    @POST("/analyze/bulk")
    suspend fun bulkAnalyze(
        @Part files: List<MultipartBody.Part>
    ): Response<BulkAnalysisResponse>
}
