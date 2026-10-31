package com.tunenotes

import android.content.Context
import org.json.JSONArray
import java.io.File

/** Tiny JSON-file store: no database setup, instant start-up, atomic writes. */
class NoteStore(context: Context) {

    private val file = File(context.filesDir, "notes.json")
    private val tmp = File(context.filesDir, "notes.json.tmp")

    fun load(): List<Note> {
        if (!file.exists()) return emptyList()
        return try {
            val arr = JSONArray(file.readText())
            List(arr.length()) { arr.getJSONObject(it).toNote() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun save(notes: List<Note>) {
        try {
            val arr = JSONArray()
            notes.forEach { arr.put(it.toJson()) }
            tmp.writeText(arr.toString())
            if (!tmp.renameTo(file)) {
                file.writeText(arr.toString())
                tmp.delete()
            }
        } catch (_: Exception) {
        }
    }
}
