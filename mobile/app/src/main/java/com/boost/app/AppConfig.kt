package com.boost.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

// ── App Config ────────────────────────────────────────────────────
object AppConfig {
    // Aura lesson: Platform-aware URL
    // Debug → VPS IP (HTTP allowed via usesCleartextTraffic)
    // Release → same VPS IP or future HTTPS domain
    val BASE_URL: String = if (BuildConfig.DEBUG)
        "http://43.134.107.159:8004"
    else
        "http://43.134.107.159:8004"

    val API_URL = "$BASE_URL/boost"

    // Free tier limits — mirror backend
    const val FREE_ANALYSIS_LIMIT = 3
    const val FREE_IMAGE_LIMIT    = 1
    const val FREE_VIDEO_LIMIT    = 1
}

// ── Hilt Application ──────────────────────────────────────────────
@HiltAndroidApp
class BoostApp : Application()
