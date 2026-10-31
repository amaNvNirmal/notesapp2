package com.tunenotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

class MainActivity : ComponentActivity() {

    private val vm: NotesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TuneNotesTheme { Root(vm) }
        }
    }

    override fun onStop() {
        vm.flush()
        super.onStop()
    }
}

@Composable
private fun Root(vm: NotesViewModel) {
    var openId by rememberSaveable { mutableStateOf<String?>(null) }
    val primary = MaterialTheme.colorScheme.primary

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(primary.copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(size.width * 0.15f, 0f),
                        radius = size.width * 1.1f
                    )
                )
            }
    ) {
        AnimatedContent(
            targetState = openId,
            transitionSpec = {
                if (targetState != null) {
                    (fadeIn(tween(200)) + slideInHorizontally(tween(260)) { it / 10 }) togetherWith
                        fadeOut(tween(120))
                } else {
                    fadeIn(tween(200)) togetherWith
                        (fadeOut(tween(140)) + slideOutHorizontally(tween(220)) { it / 10 })
                }
            },
            label = "screen"
        ) { id ->
            if (id == null) {
                NotesListScreen(
                    vm = vm,
                    onOpen = { openId = it },
                    onNew = { openId = vm.create() }
                )
            } else {
                EditorScreen(
                    vm = vm,
                    id = id,
                    onBack = {
                        vm.discardIfEmpty(id)
                        openId = null
                    }
                )
            }
        }
    }
}
