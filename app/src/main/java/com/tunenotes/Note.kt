package com.tunenotes

import org.json.JSONObject
import java.util.UUID

data class Note(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val body: String = "",
    val musicUrl: String = "",
    val musicTitle: String = "",
    val musicAuthor: String = "",
    val pinned: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isBlank: Boolean
        get() = title.isBlank() && body.isBlank() && musicUrl.isBlank()

    val thumbUrl: String?
        get() = YouTube.extractId(musicUrl)?.let { "https://i.ytimg.com/vi/$it/mqdefault.jpg" }
}

fun Note.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("title", title)
    put("body", body)
    put("musicUrl", musicUrl)
    put("musicTitle", musicTitle)
    put("musicAuthor", musicAuthor)
    put("pinned", pinned)
    put("updatedAt", updatedAt)
}

fun JSONObject.toNote(): Note = Note(
    id = optString("id", UUID.randomUUID().toString()),
    title = optString("title"),
    body = optString("body"),
    musicUrl = optString("musicUrl"),
    musicTitle = optString("musicTitle"),
    musicAuthor = optString("musicAuthor"),
    pinned = optBoolean("pinned"),
    updatedAt = optLong("updatedAt", System.currentTimeMillis())
)
