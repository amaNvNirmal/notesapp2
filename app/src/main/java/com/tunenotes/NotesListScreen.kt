package com.tunenotes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlin.math.abs

private val Tints = listOf(
    Color(0xFFB69CFF), Color(0xFF5EEAD4), Color(0xFFFF9EC4),
    Color(0xFFFFC46B), Color(0xFF7DB8FF), Color(0xFFA3E635)
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NotesListScreen(
    vm: NotesViewModel,
    onOpen: (String) -> Unit,
    onNew: () -> Unit
) {
    val all by vm.notes.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    val haptics = LocalHapticFeedback.current

    val shown = remember(all, query) {
        val q = query.trim()
        all.filter {
            q.isEmpty() ||
                it.title.contains(q, ignoreCase = true) ||
                it.body.contains(q, ignoreCase = true) ||
                it.musicTitle.contains(q, ignoreCase = true)
        }.sortedWith(compareByDescending<Note> { it.pinned }.thenByDescending { it.updatedAt })
    }

    Box(Modifier.fillMaxSize()) {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
            verticalItemSpacing = 12.dp,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(span = StaggeredGridItemSpan.FullLine) {
                Header(count = all.size, query = query, onQuery = { query = it })
            }
            if (shown.isEmpty()) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    EmptyState(searching = query.isNotBlank())
                }
            }
            items(shown, key = { it.id }) { note ->
                NoteCard(
                    note = note,
                    onClick = { onOpen(note.id) },
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        vm.togglePin(note.id)
                    }
                )
            }
        }

        ExtendedFloatingActionButton(
            onClick = onNew,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(20.dp),
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("New note", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun Header(count: Int, query: String, onQuery: (String) -> Unit) {
    Column(
        Modifier
            .statusBarsPadding()
            .padding(top = 24.dp, bottom = 16.dp)
    ) {
        Text(
            "Notes",
            fontSize = 38.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-1).sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            if (count == 1) "1 note" else "$count notes",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(18.dp))
        SearchField(query, onQuery)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SearchField(query: String, onQuery: (String) -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = query,
                onValueChange = onQuery,
                singleLine = true,
                textStyle = TextStyle(fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text(
                                "Search notes and songs",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        inner()
                    }
                }
            )
            if (query.isNotEmpty()) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Clear search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .combinedClickable(onClick = { onQuery("") })
                )
            }
        }
    }
}

@Composable
private fun EmptyState(searching: Boolean) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top =
