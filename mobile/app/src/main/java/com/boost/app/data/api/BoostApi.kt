package com.boost.app.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.*

// ── Request models ────────────────────────────────────────────────

data class AnalyzeRequest(
    @SerializedName("device_id")    val deviceId: String,
    @SerializedName("image_b64")    val imageB64: String,
    @SerializedName("product_name") val productName: String? = null,
    @SerializedName("niche")        val niche: String,
    @SerializedName("vibe")         val vibe: String,
    @SerializedName("platform")     val platform: String = "TikTok",
)

data class GenerateImagesRequest(
    @SerializedName("device_id")        val deviceId: String,
    @SerializedName("project_id")       val projectId: String,
    @SerializedName("image_b64")        val imageB64: String,
    @SerializedName("product_context")  val productContext: String,
)

data class GenerateVideoRequest(
    @SerializedName("device_id")   val deviceId: String,
    @SerializedName("project_id")  val projectId: String,
    @SerializedName("video_b64")   val videoB64: String? = null,
    @SerializedName("image_b64")   val imageB64: String? = null,
    @SerializedName("hook_text")   val hookText: String,
    @SerializedName("caption")     val caption: String,
    @SerializedName("cta_text")    val ctaText: String = "Link in bio 👇",
)

data class VerifySubscriptionRequest(
    @SerializedName("device_id")      val deviceId: String,
    @SerializedName("purchase_token") val purchaseToken: String,
    @SerializedName("product_id")     val productId: String = "boost_pro_monthly",
)

// ── Response models ───────────────────────────────────────────────

data class DeviceResponse(
    @SerializedName("device_id")            val deviceId: String,
    @SerializedName("is_pro")               val isPro: Boolean,
    @SerializedName("subscription_expiry")  val subscriptionExpiry: String?,
)

data class QuotaItem(
    @SerializedName("gen_type")  val genType: String,
    @SerializedName("used")      val used: Int,
    @SerializedName("limit")     val limit: Int,
    @SerializedName("remaining") val remaining: Int,
)

data class QuotaResponse(
    @SerializedName("is_pro")    val isPro: Boolean,
    @SerializedName("date_utc")  val dateUtc: String,
    @SerializedName("quotas")    val quotas: List<QuotaItem>,
    @SerializedName("resets_at") val resetsAt: String,
)

data class StrategyCard(
    @SerializedName("angle")             val angle: String,
    @SerializedName("audience")          val audience: String,
    @SerializedName("content_potential") val contentPotential: String,
    @SerializedName("tags")              val tags: List<String>,
)

data class CaptionItem(
    @SerializedName("hook")    val hook: String,
    @SerializedName("caption") val caption: String,
)

data class HashtagPack(
    @SerializedName("viral") val viral: List<String>,
    @SerializedName("niche") val niche: List<String>,
    @SerializedName("brand") val brand: List<String>,
)

data class AnalyzeResponse(
    @SerializedName("project_id") val projectId: String,
    @SerializedName("strategy")   val strategy: StrategyCard,
    @SerializedName("captions")   val captions: List<CaptionItem>,
    @SerializedName("hashtags")   val hashtags: HashtagPack,
)

data class ImageResult(
    @SerializedName("label") val label: String,
    @SerializedName("url")   val url: String,
)

data class GenerateImagesResponse(
    @SerializedName("project_id") val projectId: String,
    @SerializedName("images")     val images: List<ImageResult>,
)

data class VideoResult(
    @SerializedName("label") val label: String,
    @SerializedName("url")   val url: String,
)

data class GenerateVideoResponse(
    @SerializedName("project_id") val projectId: String,
    @SerializedName("videos")     val videos: List<VideoResult>,
)

data class ProjectSummary(
    @SerializedName("id")            val id: String,
    @SerializedName("product_name")  val productName: String?,
    @SerializedName("niche")         val niche: String?,
    @SerializedName("platform")      val platform: String?,
    @SerializedName("image_count")   val imageCount: Int,
    @SerializedName("video_count")   val videoCount: Int,
    @SerializedName("caption_count") val captionCount: Int,
    @SerializedName("created_at")    val createdAt: String,
)

data class ProjectDetail(
    @SerializedName("id")           val id: String,
    @SerializedName("product_name") val productName: String?,
    @SerializedName("niche")        val niche: String?,
    @SerializedName("vibe")         val vibe: String?,
    @SerializedName("platform")     val platform: String?,
    @SerializedName("strategy")     val strategy: Map<String, Any>?,
    @SerializedName("captions")     val captions: List<Map<String, String>>?,
    @SerializedName("image_urls")   val imageUrls: List<Map<String, String>>?,
    @SerializedName("video_urls")   val videoUrls: List<Map<String, String>>?,
    @SerializedName("hashtags")     val hashtags: Map<String, List<String>>?,
    @SerializedName("created_at")   val createdAt: String,
)

data class TrendItem(
    @SerializedName("id")          val id: String,
    @SerializedName("emoji")       val emoji: String,
    @SerializedName("category")    val category: String,
    @SerializedName("trend")       val trend: String,
    @SerializedName("heat")        val heat: Int,
    @SerializedName("growth")      val growth: String,
    @SerializedName("is_pro_only") val isProOnly: Boolean,
    @SerializedName("rank")        val rank: Int,
)

data class ApiError(
    @SerializedName("detail") val detail: String,
)

// ── Retrofit API Interface ─────────────────────────────────────────

interface BoostApi {
    @GET("health")
    suspend fun health(): Map<String, String>

    @POST("device/register")
    suspend fun registerDevice(@Query("device_id") deviceId: String): DeviceResponse

    @GET("device/{device_id}")
    suspend fun getDevice(@Path("device_id") deviceId: String): DeviceResponse

    @GET("quota/{device_id}")
    suspend fun getQuota(@Path("device_id") deviceId: String): QuotaResponse

    @POST("analyze")
    suspend fun analyze(@Body request: AnalyzeRequest): AnalyzeResponse

    @POST("generate-images")
    suspend fun generateImages(@Body request: GenerateImagesRequest): GenerateImagesResponse

    @POST("generate-video")
    suspend fun generateVideo(@Body request: GenerateVideoRequest): GenerateVideoResponse

    @GET("projects/{device_id}")
    suspend fun listProjects(@Path("device_id") deviceId: String): List<ProjectSummary>

    @GET("projects/{device_id}/{project_id}")
    suspend fun getProject(
        @Path("device_id") deviceId: String,
        @Path("project_id") projectId: String,
    ): ProjectDetail

    @GET("trending")
    suspend fun getTrending(@Query("device_id") deviceId: String? = null): List<TrendItem>

    @POST("verify-subscription")
    suspend fun verifySubscription(@Body request: VerifySubscriptionRequest): DeviceResponse
}
