package com.example.xtreamtvapp.data

import android.content.Context
import org.json.JSONObject
import java.io.File

/**
 * Persists user credentials and settings to a JSON file in app-private storage
 * so they survive app restarts. Used for Xtream URL, username, and password.
 */
object UserSettings {

    private const val FILENAME = "user_settings.json"
    private const val KEY_BASE_URL = "base_url"
    private const val KEY_USERNAME = "username"
    private const val KEY_PASSWORD = "password"

    data class Credentials(
        val baseUrl: String,
        val username: String,
        val password: String
    ) {
        fun isValid(): Boolean =
            baseUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()
    }

    private fun file(context: Context): File =
        File(context.filesDir, FILENAME)

    /**
     * Load saved credentials from the JSON file. Returns null if file doesn't exist or is invalid.
     */
    fun loadCredentials(context: Context): Credentials? {
        return try {
            val f = file(context)
            if (!f.exists() || f.length() == 0L) return null
            val json = JSONObject(f.readText())
            val baseUrl = json.optString(KEY_BASE_URL, "").trim()
            val username = json.optString(KEY_USERNAME, "").trim()
            val password = json.optString(KEY_PASSWORD, "").trim()
            Credentials(baseUrl, username, password).takeIf { it.isValid() }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Save credentials to the JSON file. Overwrites any existing settings.
     */
    fun saveCredentials(context: Context, baseUrl: String, username: String, password: String) {
        try {
            val json = JSONObject().apply {
                put(KEY_BASE_URL, baseUrl.trim())
                put(KEY_USERNAME, username.trim())
                put(KEY_PASSWORD, password.trim())
            }
            file(context).writeText(json.toString())
        } catch (e: Exception) {
            // ignore; optional persistence
        }
    }

    /**
     * Clear saved credentials (e.g. for logout). Optional for later use.
     */
    fun clearCredentials(context: Context) {
        try {
            file(context).delete()
        } catch (_: Exception) { }
    }
}
