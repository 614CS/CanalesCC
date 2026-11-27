package com.example.canalescc

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(channel: Channel, onBack: () -> Unit) {
    val context = LocalContext.current
    val player = remember(channel) { ExoPlayer.Builder(context).build() }
    var ccOn by remember { mutableStateOf(true) }
    var hasCc by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onTracksChanged(tracks: Tracks) {
                hasCc = tracks.containsType(C.TRACK_TYPE_TEXT)
            }
            override fun onPlayerError(e: PlaybackException) {
                error = e.errorCodeName
            }
        }
        player.addListener(listener)
        player.setMediaItem(MediaItem.fromUri(channel.url))
        player.prepare()
        player.playWhenReady = true
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    // Activa/desactiva los subtítulos (CC embebidos CEA-608/708 o WebVTT del HLS)
    LaunchedEffect(ccOn, player) {
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setSelectUndeterminedTextLanguage(true)
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !ccOn)
            .build()
    }

    BackHandler(onBack = onBack)

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = true
                    setShowSubtitleButton(true)
                }
            }
        )
        Row(
            Modifier.align(Alignment.TopStart).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { ccOn = !ccOn }) {
                Text(if (ccOn) "CC: activados" else "CC: apagados")
            }
            Spacer(Modifier.width(12.dp))
            Text(
                error?.let { "Error: $it" }
                    ?: if (hasCc) "CC detectados" else "Sin CC detectados (aún)"
            )
        }
    }
}
