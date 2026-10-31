package com.tunenotes

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.foundation.layout.height
import kotlin.math.max

/**
 * YouTube only plays embedded videos for pages that identify themselves with a real
 * https origin (otherwise it answers with errors 152/153). We load our own tiny player
 * page with this app-specific origin.
 */
private const val PLAYER_ORIGIN = "https://com.tunenotes"

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

/** Receives events from the player page's JavaScript and forwards them on the main thread. */
class PlayerBridge(private val handler: (String, String) -> Unit) {
    private val main = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onEvent(type: String, value: String) {
        main.post { handler(type, value) }
    }
}

private fun playerHtml(videoId: String): String = """
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1">
<style>
  html, body { margin: 0; height: 100%; background: #000; overflow: hidden; }
  #p { position: absolute; top: 0; left: 0; width: 100%; height: 100%; }
</style>
</head>
<body>
<div id="p"></div>
<script src="https://www.youtube.com/iframe_api"></script>
<script>
  var player = null;
  function post(t, v) { try { Bridge.onEvent(t, String(v)); } catch (e) {} }
  function onYouTubeIframeAPIReady() {
    player = new YT.Player('p', {
      videoId: '__ID__',
      playerVars: {
        controls: 0, playsinline: 1, rel: 0, fs: 0, disablekb: 1,
        iv_load_policy: 3, origin: '__ORIGIN__', widget_referrer: '__ORIGIN__'
      },
      events: {
        onReady: function () { post('ready', player.getDuration()); },
        onStateChange: function (e) {
          post('state', e.data);
          if (e.data === 1 || e.data === 3) { post('duration', player.getDuration()); }
        },
        onError: function (e) { post('error', e.data); }
      }
    });
  }
  setInterval(function () {
    try {
      if (player && typeof player.getCurrentTime === 'function') {
        post('time', player.getCurrentTime());
      }
    } catch (e) {}
  }, 250);
</script>
</body>
</html>
""".trimIndent().replace("__ID__", videoId).replace("__ORIGIN__", PLAYER_ORIGIN)

@SuppressLint("SetJavaScriptEnabled")
private fun createPlayerWebView(context: Context, videoId: String, bridge: PlayerBridge): WebView =
    WebView(context).apply {
        setBackgroundColor(android.graphics.Color.BLACK)
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        webChromeClient = WebChromeClient()
        webViewClient = object : WebViewClient() {
            // Keep the page itself from ever navigating away (e.g. a tap on the YouTube logo).
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                request.isForMainFrame
        }
        addJavascriptInterface(bridge, "Bridge")
        loadDataWithBaseURL(PLAYER_ORIGIN, playerHtml(videoId), "text/html", "utf-8", null)
    }

@Composable
private fun PlayerCard(note: Note, videoId: String, onClear: () -> Unit) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme

    var playing by remember(videoId) { mutableStateOf(false) }
    var position by remember(videoId) { mutableFloatStateOf(0f) }
    var duration by remember(videoId) { mutableFloatStateOf(0f) }
    var scrubbing by remember(videoId) { mutableStateOf(false) }
    var scrubTo by remember(videoId) { mutableFloatStateOf(0f) }
    var errorCode by remember(videoId) { mutableIntStateOf(0) }
    var compact by rememberSaveable { mutableStateOf(false) }

    val webView = remember(videoId) {
        val bridge = PlayerBridge { type, value ->
            when (type) {
                "state" -> {
                    val s = value.toIntOrNull() ?: -2
                    playing = s == 1 || s == 3
                    if (s == 0) position = 0f
                }
                "time" -> value.toFloatOrNull()?.let { position = it }
                "duration", "ready" -> value.toFloatOrNull()?.let { if (it > 0f) duration = it }
                "error" -> errorCode = value.toIntOrNull() ?: -1
            }
        }
        createPlayerWebView(context, videoId, bridge)
    }

    DisposableEffect(webView) {
        onDispose {
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView.stopLoading()
            webView.destroy()
        }
    }

    fun run(js: String) {
        webView.evaluateJavascript(js, null)
    }

    val errorText: String? = when (errorCode) {
        0 -> null
        101, 150 -> "The video's owner doesn't allow it to play in other apps."
        100 -> "This video is private or has been removed."
        152, 153 -> "YouTube blocked the player (error $errorCode)."
        else -> "Couldn't play this video (error $errorCode)."
    }

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = colors.surfaceVariant,
        border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            // Video surface (YouTube's embedded player; its own controls are replaced by ours)
            Box(
                Modifier
                    .then(if (compact) Modifier.width(200.dp) else Modifier.fillMaxWidth())
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black)
            ) {
                AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize())
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

            if (errorText != null) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        errorText,
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
                        run("if (window.player && player.seekTo) { player.seekTo($scrubTo, true); }")
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
                        val target = (position - 10f).coerceAtLeast(0f)
                        run("if (window.player && player.seekTo) { player.seekTo($target, true); }")
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
                                if (playing) {
                                    run("if (window.player && player.pauseVideo) { player.pauseVideo(); }")
                                } else {
                                    run("if (window.player && player.playVideo) { player.playVideo(); }")
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        PlayPauseIcon(playing = playing, tint = Color(0xFF0B0B10))
                    }

                    Spacer(Modifier.width(12.dp))

                    TextButton(onClick = {
                        val raw = position + 10f
                        val target = if (duration > 0f) raw.coerceAtMost(duration) else raw
                        run("if (window.player && player.seekTo) { player.seekTo($target, true); }")
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
