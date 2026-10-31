package com.tunenotes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object YouTube {

    data class Meta(val title: String, val author: String)

    // Handles youtu.be/ID, watch?v=ID (also with extra params), embed/, shorts/, live/,
    // m.youtube.com and music.youtube.com links.
    private val idRegex = Regex(
        """(?:youtu\.be/|youtube(?:-nocookie)?\.com/(?:watch\?(?:[^#\s]*&)?v=|embed/|shorts/|live/|v/))([A-Za-z0-9_-]{11})"""
    )

    fun extractId(text: String): String? =
        idRegex.find(text.trim())?.groupValues?.getOrNull(1)

    fun watchUrl(id: String) = "https://www.youtube.com/watch?v=$id"

    /** Title + channel via YouTube's public oEmbed endpoint. Returns null when offline. */
    suspend fun fetchMeta(url: String): Meta? = withContext(Dispatchers.IO) {
        val id = extractId(url) ?: return@withContext null
        try {
            val endpoint = "https://www.youtube.com/oembed?format=json&url=" +
                URLEncoder.encode(watchUrl(id), "UTF-8")
            val conn = URL(endpoint).openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.inputStream.bufferedReader().use { reader ->
                val json = JSONObject(reader.readText())
                Meta(json.optString("title"), json.optString("author_name"))
            }
        } catch (e: Exception) {
            null
        }
    }
}
