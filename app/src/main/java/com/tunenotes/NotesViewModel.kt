package com.tunenotes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NotesViewModel(app: Application) : AndroidViewModel(app) {

    private val store = NoteStore(app)
    private val _notes = MutableStateFlow(store.load())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    private var saveJob: Job? = null

    fun get(id: String): Note? = _notes.value.firstOrNull { it.id == id }

    fun create(): String {
        val note = Note()
        _notes.update { listOf(note) + it }
        return note.id
    }

    fun update(id: String, change: (Note) -> Note) {
        _notes.update { list ->
            list.map { if (it.id == id) change(it).copy(updatedAt = System.currentTimeMillis()) else it }
        }
        persist()
    }

    fun togglePin(id: String) = update(id) { it.copy(pinned = !it.pinned) }

    fun setMusic(id: String, url: String) {
        update(id) { it.copy(musicUrl = url, musicTitle = "", musicAuthor = "") }
        viewModelScope.launch {
            YouTube.fetchMeta(url)?.let { meta ->
                update(id) { n ->
                    if (n.musicUrl == url) n.copy(musicTitle = meta.title, musicAuthor = meta.author) else n
                }
            }
        }
    }

    fun clearMusic(id: String) =
        update(id) { it.copy(musicUrl = "", musicTitle = "", musicAuthor = "") }

    fun delete(id: String) {
        _notes.update { list -> list.filterNot { it.id == id } }
        persist()
    }

    fun discardIfEmpty(id: String) {
        val note = get(id) ?: return
        if (note.isBlank) delete(id)
    }

    /** Debounced background save while typing. */
    private fun persist() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch(Dispatchers.IO) {
            delay(350)
            store.save(_notes.value)
        }
    }

    /** Immediate save, called when the app goes to the background. */
    fun flush() {
        saveJob?.cancel()
        store.save(_notes.value)
    }
}
