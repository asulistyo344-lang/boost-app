package com.boost.app.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.boost.app.data.api.*
import com.boost.app.di.dataStore
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

// ── DataStore keys ─────────────────────────────────────────────────
private val KEY_DEVICE_ID = stringPreferencesKey("device_id")
private val KEY_IS_PRO    = booleanPreferencesKey("is_pro")

// ── Error parser (Aura/Nebula lesson — never generic messages) ─────
fun parseApiError(e: Exception): String = when (e) {
    is HttpException -> {
        try {
            val body = e.response()?.errorBody()?.string() ?: ""
            val err  = Gson().fromJson(body, ApiError::class.java)
            err.detail.ifBlank { "Server error ${e.code()}. Please retry." }
        } catch (_: Exception) {
            when (e.code()) {
                429  -> "Daily limit reached. Upgrade to Pro for unlimited."
                403  -> "This feature requires a Pro subscription."
                503  -> "AI service unavailable. Please retry shortly."
                else -> "Server error ${e.code()}. Please retry."
            }
        }
    }
    is IOException -> "Check your internet connection and try again."
    else           -> "Something went wrong. Please retry."
}

@Singleton
class BoostRepository @Inject constructor(
    private val api: BoostApi,
    @ApplicationContext private val context: Context,
) {

    // ── Device ID (generated once, persisted in DataStore) ─────────
    val deviceIdFlow: Flow<String?> = context.dataStore.data
        .map { it[KEY_DEVICE_ID] }

    val isProFlow: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_IS_PRO] ?: false }

    suspend fun getOrCreateDeviceId(): String {
        val prefs = context.dataStore.data.map { it[KEY_DEVICE_ID] }
        var id: String? = null
        context.dataStore.data.collect { id = it[KEY_DEVICE_ID] }

        if (id.isNullOrBlank()) {
            id = UUID.randomUUID().toString()
            context.dataStore.edit { it[KEY_DEVICE_ID] = id!! }
        }
        return id!!
    }

    suspend fun saveProStatus(isPro: Boolean) {
        context.dataStore.edit { it[KEY_IS_PRO] = isPro }
    }

    // ── API calls ─────────────────────────────────────────────────

    suspend fun registerDevice(deviceId: String): Result<DeviceResponse> =
        runCatching { api.registerDevice(deviceId) }

    suspend fun getQuota(deviceId: String): Result<QuotaResponse> =
        runCatching { api.getQuota(deviceId) }

    suspend fun analyze(request: AnalyzeRequest): Result<AnalyzeResponse> =
        runCatching { api.analyze(request) }

    suspend fun generateImages(request: GenerateImagesRequest): Result<GenerateImagesResponse> =
        runCatching { api.generateImages(request) }

    suspend fun generateVideo(request: GenerateVideoRequest): Result<GenerateVideoResponse> =
        runCatching { api.generateVideo(request) }

    suspend fun listProjects(deviceId: String): Result<List<ProjectSummary>> =
        runCatching { api.listProjects(deviceId) }

    suspend fun getProject(deviceId: String, projectId: String): Result<ProjectDetail> =
        runCatching { api.getProject(deviceId, projectId) }

    suspend fun getTrending(deviceId: String?): Result<List<TrendItem>> =
        runCatching { api.getTrending(deviceId) }

    suspend fun verifySubscription(request: VerifySubscriptionRequest): Result<DeviceResponse> =
        runCatching {
            val resp = api.verifySubscription(request)
            saveProStatus(resp.isPro)
            resp
        }

    // ── Image helpers ─────────────────────────────────────────────

    fun uriToBase64(uri: Uri, maxWidthPx: Int = 1024): String {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Cannot open image")

        val original = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        // Resize if needed (Aura lesson: compress before upload)
        val bitmap = if (original.width > maxWidthPx) {
            val ratio = maxWidthPx.toFloat() / original.width
            val newH  = (original.height * ratio).toInt()
            Bitmap.createScaledBitmap(original, maxWidthPx, newH, true)
        } else original

        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    fun videoUriToBase64(uri: Uri, maxSizeMb: Int = 50): String {
        val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
            ?: throw IOException("Cannot open video")

        val sizeMb = bytes.size / (1024 * 1024)
        if (sizeMb > maxSizeMb) throw IOException("Video too large. Max ${maxSizeMb}MB.")

        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}
