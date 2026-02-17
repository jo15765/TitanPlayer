package com.example.xtreamtvapp.data

import com.google.gson.annotations.SerializedName

/**
 * Response from player_api.php?username=X&password=X (server info).
 */
data class XtreamServerInfo(
    @SerializedName("user_info") val userInfo: UserInfo? = null
)

data class UserInfo(
    @SerializedName("username") val username: String? = null,
    @SerializedName("password") val password: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("auth") val auth: Int? = null
)

/**
 * Live category from get_live_categories.
 * Many providers use "Country | Category" (e.g. "USA | Local ABC") so we can infer country.
 */
data class LiveCategory(
    @SerializedName("category_id") val categoryId: String = "",
    @SerializedName("category_name") val categoryName: String = ""
) {
    /** Prefix before " | " if present, else null; used as country/region for filtering. */
    fun countryPrefix(): String? {
        val name = categoryName.trim()
        if (name.contains(" | ")) {
            val prefix = name.substringBefore(" | ").trim()
            if (prefix.isNotEmpty()) return prefix
        }
        return null
    }
}

/**
 * Live stream entry from get_live_streams.
 */
data class LiveStream(
    @SerializedName("num") val num: Int = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("stream_type") val streamType: String = "live",
    @SerializedName("stream_id") val streamId: Int = 0,
    @SerializedName("stream_icon") val streamIcon: String? = null,
    @SerializedName("epg_channel_id") val epgChannelId: String? = null,
    @SerializedName("added") val added: String? = null,
    @SerializedName("category_id") val categoryId: String? = null,
    @SerializedName("category_ids") val categoryIds: List<Int>? = null
) {
    /**
     * Build playable URL: http://host:port/live/username/password/stream_id.ts
     */
    fun playUrl(baseUrl: String, username: String, password: String): String {
        val base = baseUrl.trimEnd('/')
        return "$base/live/$username/$password/$streamId.ts"
    }
}

/** EPG program entry (get_short_epg). Times can be "YYYY-MM-DD HH:MM:SS" or unix timestamp string. */
data class EpgEntry(
    @SerializedName("id") val id: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("start") val start: String? = null,
    @SerializedName("end") val end: String? = null,
    @SerializedName("start_timestamp") val startTimestamp: String? = null,
    @SerializedName("stop_timestamp") val stopTimestamp: String? = null,
    @SerializedName("description") val description: String? = null
)

/** Wrapper when server returns { "epg_listings": [...] }; some return raw array. */
data class EpgResponse(
    @SerializedName("epg_listings") val epgListings: List<EpgEntry>? = null
)
