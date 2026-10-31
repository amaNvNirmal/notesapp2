package com.tunenotes

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlin.math.max

/** The music block of a note: a paste field when empty, a player once a link is set. */
@Composable
fun MusicSection(note: Note, onUrl: (String) -> Unit, onClear: () -> Unit) {
    val videoId = remember(note.musicUrl) { YouTube.extractId(note.musicUrl) }
    if (videoId == null) {
        LinkInput(onUrl)
    } else {
        PlayerCard(note, videoId, onClear)
    }
}

@Composable
private fun LinkInput(onUrl: (String) -> Unit) {
    val clipboard = LocalClipboardManager.current
    val colors = MaterialTheme.colorScheme
    var text by remember { mutableStateOf("") }
    var invalid by remember { mutableStateOf(false) }

    fun submit(value: String) {
        if (YouTube.extractId(value) != null) {
            onUrl(value.trim())
        } else {
            invalid = value.isNotBlank()
        }
    }

    Column {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.surfaceVariant,
            border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier.padding(start = 16.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🎵", fontSize = 18.sp)
                Spacer(Modifier.width(10.dp))
                BasicTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        invalid = false
                        if (YouTube.extractId(it) != null) submit(it)
                    },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 15.sp, color = colors.onSurface),
                    cursorBrush = SolidColor(colors.primary),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (text.isEmpty()) {
                                Text(
                                    "Paste a YouTube link",
                                    fontSize = 15.sp,
                                    color = colors.onSurfaceVariant
                                )
                            }
                            inner()
                        }
                    }
                )
                TextButton(onClick = {
                    clipboard.getText()?.text?.let {
                        text = it
                        submit(it)
                    }
                }) {
                    Text("Paste", fontWeight = FontWeight.SemiBold)
                }
            }
        }
        if (invalid) {
            Text(
                "That doesn't look like a YouTube link",
                fontSize = 12.sp,
                color = colors.error,
                modifier = Modifier.padding(start = 16.dp, top = 6.dp)
            )
        }
    }
}

@Composable
private fun PlayerCard(note: Note, videoId: String, onClear: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val colors = MaterialTheme.colorScheme

    var player by remember(videoId) { mutableStateOf<YouTubePlayer?>(null) }
    var playing by remember(videoId) { mutableStateOf(false) }
    var position by remember(videoId) { mutableFloatStateOf(0f) }
    var duration by remember(videoId) { mutableFloatStateOf(0f) }
    var scrubbing by remember(videoId) { mutableStateOf(false) }
    var scrubTo by remember(videoId) { mutableFloatStateOf(0f) }
    var failed by remember(videoId) { mutableStateOf(false) }
    var compact by rememberSaveable { mutableStateOf(false) }

    val playerView = remember(videoId) {
        YouTubePlayerView(context).apply {
            enableAutomaticInitialization = false
            enableBackgroundPlayback(true)
            val options = IFramePlayerOptions.Builder().controls(0).build()
            initialize(object : AbstractYouTubePlayerListener() {
                override fun onReady(youTubePlayer: YouTubePlayer) {
                    player = youTubePlayer
                    youTubePlayer.cueVideo(videoId, 0f)
                }

                override fun onStateChange(
                    youTubePlayer: YouTubePlayer,
                    state: PlayerConstants.PlayerState
                ) {
                    playing = state == PlayerConstants.PlayerState.PLAYING ||
                        state == PlayerConstants.PlayerState.BUFFERING
                    if (state == PlayerConstants.PlayerState.ENDED) position = 0f
                }

                override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                    position = second
                }

                override fun onVideoDuration(youTubePlayer: YouTubePlayer, duration2: Float) {
                    duration = duration2
                }

                override fun onError(
                    youTubePlayer: YouTubePlayer,
                    error: PlayerConstants.PlayerError
                ) {
                    failed = true
                }
            }, true, options)
        }
    }

    DisposableEffect(playerView) {
        lifecycleOwner.lifecycle.addObserver(playerView)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(playerView)
            playerView.release()
        }
    }

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = colors.surfaceVariant,
        border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            // Video surface (YouTube's embedded player; controls are replaced by the ones below)
            Box(
                Modifier
                    .then(if (compact) Modifier.width(200.dp) else Modifier.fillMaxWidth())
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black)
            ) {
                AndroidView(factory = { playerView }, modifier = Modifier.fillMaxSize())
            }

            // Title row
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        note.musicTitle.ifBlank { "YouTube audio" },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = colors.onSurface
                    )
                    Text(
                        note.musicAuthor.ifBlank { "YouTube" },
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = colors.onSurfaceVariant
                    )
                }
                IconButton(onClick = { compact = !compact }) {
                    Icon(
                        if (compact) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                        contentDescription = if (compact) "Enlarge video" else "Shrink video",
                        tint = colors.onSurfaceVariant
                    )
                }
                IconButton(onClick = onClear) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove music",
                        tint = colors.onSurfaceVariant
                    )
                }
            }

            if (failed) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "This video can't be played inside other apps.",
                        fontSize = 13.sp,
                        color = colors.error,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp)
                    )
                    TextButton(onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(YouTube.watchUrl(videoId)))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }) { Text("Open in YouTube") }
                }
            } else {
                // Seek bar
                val shown = if (scrubbing) scrubTo else position
                Slider(
                    value = shown.coerceIn(0f, max(duration, 1f)),
                    onValueChange = {
                        scrubbing = true
                        scrubTo = it
                    },
                    onValueChangeFinished = {
                        player?.seekTo(scrubTo)
                        position = scrubTo
                        scrubbing = false
                    },
                    valueRange = 0f..max(duration, 1f),
                    colors = SliderDefaults.colors(
                        thumbColor = colors.primary,
                        activeTrackColor = colors.primary,
                        inactiveTrackColor = colors.outline.copy(alpha = 0.4f)
                    )
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatTime(shown), fontSize = 12.sp, color = colors.onSurfaceVariant)
                    Text(formatTime(duration), fontSize = 12.sp, color = colors.onSurfaceVariant)
                }

                // Transport
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        player?.seekTo((position - 10f).coerceAtLeast(0f))
                    }) { Text("−10s", color = colors.onSurfaceVariant) }

                    Spacer(Modifier.width(12.dp))

                    Box(
                        Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(colors.primary, colors.secondary))
                            )
                            .clickable {
                                if (playing) player?.pause() else player?.play()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        PlayPauseIcon(playing = playing, tint = Color(0xFF0B0B10))
                    }

                    Spacer(Modifier.width(12.dp))

                    TextButton(onClick = {
                        val target = position + 10f
                        player?.seekTo(if (duration > 0f) target.coerceAtMost(duration) else target)
                    }) { Text("+10s", color = colors.onSurfaceVariant) }
                }
            }
        }
    }
}

@Composable
private fun PlayPauseIcon(playing: Boolean, tint: Color, modifier: Modifier = Modifier.size(26.dp)) {
    Canvas(modifier) {
        if (playing) {
            val barW = size.width * 0.26f
            val gap = size.width * 0.14f
            val barH = size.height * 0.74f
            val top = (size.height - barH) / 2f
            val left = (size.width - (2 * barW + gap)) / 2f
            drawRoundRect(tint, Offset(left, top), Size(barW, barH), CornerRadius(barW / 3f))
            drawRoundRect(tint, Offset(left + barW + gap, top), Size(barW, barH), CornerRadius(barW / 3f))
        } else {
            val p = Path().apply {
                moveTo(size.width * 0.30f, size.height * 0.16f)
                lineTo(size.width * 0.86f, size.height * 0.50f)
                lineTo(size.width * 0.30f, size.height * 0.84f)
                close()
            }
            drawPath(p, tint)
        }
    }
}

private fun formatTime(seconds: Float): String {
    val total = seconds.toInt().coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}
