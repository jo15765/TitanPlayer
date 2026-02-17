package com.example.xtreamtvapp.data

import android.util.Log
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

object XtreamRepository {

    private const val TAG = "XtreamRepo"

    /**
     * Normalize base URL: ensure it has scheme and no path, then return base for API.
     * e.g. "server.com:8080" -> "http://server.com:8080"
     *      "http://server.com:8080/" -> "http://server.com:8080"
     */
    fun normalizeBaseUrl(input: String): String {
        var s = input.trim()
        if (s.isEmpty()) return ""
        if (!s.startsWith("http://") && !s.startsWith("https://")) {
            s = "http://$s"
        }
        return try {
            val url = URL(s)
            "${url.protocol}://${url.host}${if (url.port in 1..65535) ":${url.port}" else ""}"
        } catch (e: Exception) {
            Log.e(TAG, "Invalid URL: $input", e)
            ""
        }
    }

    /** Exact request URL (no trailing slash): e.g. https://example.com:8080/player_api.php */
    private fun playerApiUrl(normalizedRoot: String): String {
        val base = normalizedRoot.trimEnd('/')
        return "$base/player_api.php"
    }

    private val okHttp = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private fun createApi(): XtreamApi {
        val gson = GsonBuilder()
            .registerTypeAdapter(EpgResponse::class.java, EpgResponseDeserializer())
            .registerTypeAdapter(LiveCategory::class.java, LiveCategoryDeserializer())
            .create()
        val retrofit = Retrofit.Builder()
            .baseUrl("http://127.0.0.1/") // required placeholder; each call uses @Url with full URL
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        return retrofit.create(XtreamApi::class.java)
    }

    /**
     * Verify login. Returns true if credentials are valid.
     */
    suspend fun login(baseUrl: String, username: String, password: String): Result<Unit> {
        if (baseUrl.isBlank() || username.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("URL, username and password are required"))
        }
        val normalized = normalizeBaseUrl(baseUrl)
        if (normalized.isEmpty()) {
            return Result.failure(IllegalArgumentException("Invalid server URL"))
        }
        return try {
            val api = createApi()
            val url = playerApiUrl(normalized)
            val response = api.getServerInfo(url, username, password)
            if (!response.isSuccessful) {
                return Result.failure(Exception("HTTP ${response.code()}"))
            }
            val body = response.body()
            if (body?.userInfo?.auth == 1) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(body?.userInfo?.message ?: "Authentication failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Login failed", e)
            Result.failure(e)
        }
    }

    /**
     * Fetch list of live streams.
     */
    suspend fun getLiveStreams(
        baseUrl: String,
        username: String,
        password: String
    ): Result<List<LiveStream>> {
        val normalized = normalizeBaseUrl(baseUrl)
        if (normalized.isEmpty()) {
            return Result.failure(IllegalArgumentException("Invalid server URL"))
        }
        return try {
            val api = createApi()
            val url = playerApiUrl(normalized)
            val response = api.getLiveStreams(url, username, password)
            if (!response.isSuccessful) {
                return Result.failure(Exception("HTTP ${response.code()}"))
            }
            val list = response.body() ?: emptyList()
            Result.success(list.sortedBy { it.name.lowercase() })
        } catch (e: Exception) {
            Log.e(TAG, "getLiveStreams failed", e)
            Result.failure(e)
        }
    }

    /**
     * Fetch only live categories (lightweight, for country discovery).
     */
    suspend fun getLiveCategories(
        baseUrl: String,
        username: String,
        password: String
    ): Result<List<LiveCategory>> {
        val normalized = normalizeBaseUrl(baseUrl)
        if (normalized.isEmpty()) {
            return Result.failure(IllegalArgumentException("Invalid server URL"))
        }
        return try {
            val api = createApi()
            val url = playerApiUrl(normalized)
            val response = api.getLiveCategories(url, username, password)
            if (!response.isSuccessful) {
                return Result.failure(Exception("HTTP ${response.code()}"))
            }
            val list = response.body()?.sortedBy { it.categoryName.lowercase() } ?: emptyList()
            Result.success(list)
        } catch (e: Exception) {
            Log.e(TAG, "getLiveCategories failed", e)
            Result.failure(e)
        }
    }

    /**
     * Fetch live categories, then return streams grouped by category.
     * Channels without a category go under "Other".
     */
    suspend fun getLiveStreamsWithCategories(
        baseUrl: String,
        username: String,
        password: String
    ): Result<Pair<List<LiveCategory>, List<LiveStream>>> {
        val normalized = normalizeBaseUrl(baseUrl)
        if (normalized.isEmpty()) {
            return Result.failure(IllegalArgumentException("Invalid server URL"))
        }
        return try {
            val api = createApi()
            val url = playerApiUrl(normalized)
            val streamsResponse = api.getLiveStreams(url, username, password)
            val categoriesResponse = api.getLiveCategories(url, username, password)
            if (!streamsResponse.isSuccessful) {
                return Result.failure(Exception("Streams: HTTP ${streamsResponse.code()}"))
            }
            val streams = streamsResponse.body()?.sortedBy { it.name.lowercase() } ?: emptyList()
            val categories = if (categoriesResponse.isSuccessful) {
                categoriesResponse.body()?.sortedBy { it.categoryName.lowercase() } ?: emptyList()
            } else {
                emptyList()
            }
            Result.success(Pair(categories, streams))
        } catch (e: Exception) {
            Log.e(TAG, "getLiveStreamsWithCategories failed", e)
            Result.failure(e)
        }
    }

    /**
     * Fetch short EPG for one stream. Returns list of programs; use [currentProgramAt] to get "now".
     * Runs on IO – do not call from main thread.
     */
    suspend fun getShortEpg(
        baseUrl: String,
        username: String,
        password: String,
        streamId: Int
    ): Result<List<EpgEntry>> {
        val normalized = normalizeBaseUrl(baseUrl)
        if (normalized.isEmpty()) return Result.failure(IllegalArgumentException("Invalid server URL"))
        return try {
            val api = createApi()
            val url = playerApiUrl(normalized)
            val response = api.getShortEpg(
                url = url,
                username = username,
                password = password,
                streamId = streamId.toString(),
                limit = 50
            )
            if (!response.isSuccessful) return Result.failure(Exception("EPG HTTP ${response.code()}"))
            val list = response.body()?.epgListings?.filter { it.start != null || it.startTimestamp != null } ?: emptyList()
            if (list.isNotEmpty()) Log.d(TAG, "getShortEpg streamId=$streamId got ${list.size} entries")
            else Log.w(TAG, "getShortEpg streamId=$streamId empty or no start times")
            Result.success(list)
        } catch (e: Exception) {
            Log.e(TAG, "getShortEpg failed for streamId=$streamId", e)
            Result.failure(e)
        }
    }
}

/** Start and end time in millis for an EPG entry, or null if unparseable. Handles unix seconds or "yyyy-MM-dd HH:mm:ss". */
fun EpgEntry.toMillisRange(): Pair<Long, Long>? {
    val start = startTimestamp?.toLongOrNull()?.let { sec -> if (sec < 1e12) sec * 1000 else sec }
        ?: parseEpgTimeToMillis(start)
    val end = stopTimestamp?.toLongOrNull()?.let { sec -> if (sec < 1e12) sec * 1000 else sec }
        ?: parseEpgTimeToMillis(end)
    if (start == null || end == null) return null
    return start to end
}

/**
 * Find the program that is currently on at [nowMillis] (UTC or server time).
 * Prefers start_timestamp/stop_timestamp; falls back to parsing start/end strings.
 */
fun currentProgramAt(entries: List<EpgEntry>, nowMillis: Long): EpgEntry? {
    for (e in entries) {
        val start = e.startTimestamp?.toLongOrNull()?.let { it * 1000 }
            ?: parseEpgTimeToMillis(e.start)
        val end = e.stopTimestamp?.toLongOrNull()?.let { it * 1000 }
            ?: parseEpgTimeToMillis(e.end)
        if (start != null && end != null && nowMillis in start..end) return e
    }
    return null
}

private fun parseEpgTimeToMillis(s: String?): Long? {
    if (s.isNullOrBlank()) return null
    return try {
        if (s.all { it.isDigit() }) {
            s.toLong().let { if (it < 1e12) it * 1000 else it }
        } else {
            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }.parse(s)?.time
        }
    } catch (_: Exception) {
        null
    }
}

/** Format EPG entry start–end as "HH:mm – HH:mm" for display. */
fun EpgEntry.formatTimeRange(): String {
    val start = startTimestamp?.toLongOrNull()?.let { it * 1000 } ?: parseEpgTimeToMillis(start)
    val end = stopTimestamp?.toLongOrNull()?.let { it * 1000 } ?: parseEpgTimeToMillis(end)
    if (start == null || end == null) return ""
    val fmt = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    return "${fmt.format(java.util.Date(start))} – ${fmt.format(java.util.Date(end))}"
}

/** Decode EPG title; some servers send Base64. */
fun EpgEntry.displayTitle(): String {
    val t = title ?: return ""
    if (!t.matches(Regex("^[A-Za-z0-9+/]+=*$"))) return t
    return try {
        val decoded = android.util.Base64.decode(t, android.util.Base64.DEFAULT)
        String(decoded, StandardCharsets.UTF_8)
    } catch (_: Exception) {
        t
    }
}
