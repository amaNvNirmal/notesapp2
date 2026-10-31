package com.tunenotes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EditorScreen(vm: NotesViewModel, id: String, onBack: () -> Unit) {
    val initial = remember(id) { vm.get(id) }
    if (initial == null) {
        LaunchedEffect(id) { onBack() }
        return
    }

    val notes by vm.notes.collectAsState()
    val note = notes.firstOrNull { it.id == id } ?: initial

    var title by remember(id) { mutableStateOf(initial.title) }
    var body by remember(id) { mutableStateOf(initial.body) }
    var confirmDelete by remember { mutableStateOf(false) }
    val titleFocus = remember { FocusRequester() }
    val colors = MaterialTheme.colorScheme

    BackHandler(onBack = onBack)

    LaunchedEffect(id) {
        if (initial.title.isEmpty() && initial.body.isEmpty() && initial.musicUrl.isEmpty()) {
            titleFocus.requestFocus()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Box(Modifier.weight(1f))
            IconButton(onClick = { vm.togglePin(id) }) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = if (note.pinned) "Unpin" else "Pin",
                    tint = if (note.pinned) colors.primary else colors.onSurfaceVariant.copy(alpha = 0.45f)
                )
            }
            IconButton(onClick = { confirmDelete = true }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = colors.onSurfaceVariant
                )
            }
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // Title
            BasicTextField(
                value = title,
                onValueChange = {
                    title = it
                    vm.update(id) { n -> n.copy(title = it) }
                },
                textStyle = TextStyle(
                    fontSize = 30.sp,
                    lineHeight = 38.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onBackground
                ),
                cursorBrush = SolidColor(colors.primary),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(titleFocus),
                decorationBox = { inner ->
                    Box {
                        if (title.isEmpty()) {
                            Text(
                                "Title",
                                fontSize = 30.sp,
                                lineHeight = 38.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurfaceVariant.copy(alpha = 0.45f)
                            )
                        }
                        inner()
                    }
                }
            )

            Spacer(Modifier.height(16.dp))

            // Music section, attached to this note
            MusicSection(
                note = note,
                onUrl = { url -> vm.setMusic(id, url) },
                onClear = { vm.clearMusic(id) }
            )

            Spacer(Modifier.height(20.dp))

            // Notes body, below the music
            BasicTextField(
                value = body,
                onValueChange = {
                    body = it
                    vm.update(id) { n -> n.copy(body = it) }
                },
                textStyle = TextStyle(
                    fontSize = 17.sp,
                    lineHeight = 26.sp,
                    color = colors.onBackground
                ),
                cursorBrush = SolidColor(colors.primary),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 280.dp),
                decorationBox = { inner ->
                    Box {
                        if (body.isEmpty()) {
                            Text(
                                "Start writing…",
                                fontSize = 17.sp,
                                lineHeight = 26.sp,
                                color = colors.onSurfaceVariant.copy(alpha = 0.45f)
                            )
                        }
                        inner()
                    }
                }
            )

            Spacer(Modifier.height(96.dp))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this note?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.delete(id)
                    onBack()
                }) { Text("Delete", color = colors.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            }
        )
    }
}
