package eu.ulubmp3.app

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import eu.ulubmp3.app.data.Category
import eu.ulubmp3.app.data.Track
import eu.ulubmp3.app.ui.HomeViewModel
import eu.ulubmp3.app.ui.UlubBlue
import eu.ulubmp3.app.ui.UlubTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { UlubMp3App() }
    }
}

private enum class AppTab { HOME, CATEGORIES, PLAYLIST }

@Composable
private fun UlubMp3App() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val vm: HomeViewModel = viewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val playlist = remember { mutableStateListOf<Track>() }
    val audio = remember { AudioController() }
    var tab by remember { mutableStateOf(AppTab.HOME) }
    var dark by remember { mutableStateOf(false) }

    DisposableEffect(Unit) { onDispose { audio.release() } }

    UlubTheme(darkTheme = dark) {
        Scaffold(
            topBar = {
                AppHeader(
                    dark = dark,
                    onTheme = { dark = !dark },
                    onHome = { tab = AppTab.HOME; vm.selectCategory(null) }
                )
            },
            bottomBar = {
                Column {
                    audio.current?.let { MiniPlayer(it, audio) }
                    NavigationBar {
                        NavigationBarItem(
                            selected = tab == AppTab.CATEGORIES,
                            onClick = { tab = AppTab.CATEGORIES },
                            icon = { Icon(Icons.Default.List, null) },
                            label = { Text("Kategorie") }
                        )
                        NavigationBarItem(
                            selected = tab == AppTab.HOME,
                            onClick = { tab = AppTab.HOME; vm.selectCategory(null) },
                            icon = { Icon(Icons.Default.Home, null) },
                            label = { Text("Główna") }
                        )
                        NavigationBarItem(
                            selected = tab == AppTab.PLAYLIST,
                            onClick = { tab = AppTab.PLAYLIST },
                            icon = { Icon(Icons.Default.PlaylistPlay, null) },
                            label = { Text("Playlista") }
                        )
                    }
                }
            }
        ) { padding ->
            when (tab) {
                AppTab.HOME -> HomeContent(
                    modifier = Modifier.padding(padding),
                    state = state,
                    onQuery = vm::setQuery,
                    onPlay = audio::play,
                    onAdd = { if (playlist.none { p -> p.id == it.id }) playlist.add(it) },
                    onDownload = { downloadTrack(context, it) },
                    onShare = { shareTrack(context, it) }
                )
                AppTab.CATEGORIES -> CategoriesContent(
                    modifier = Modifier.padding(padding),
                    categories = state.categories,
                    selected = state.selectedCategory,
                    onSelect = {
                        vm.selectCategory(it)
                        tab = AppTab.HOME
                    }
                )
                AppTab.PLAYLIST -> PlaylistContent(
                    modifier = Modifier.padding(padding),
                    playlist = playlist,
                    onPlay = audio::play,
                    onRemove = { playlist.remove(it) },
                    onDownload = { downloadTrack(context, it) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppHeader(dark: Boolean, onTheme: () -> Unit, onHome: () -> Unit) {
    TopAppBar(
        title = {
            Column {
                Text("ULUBMP3.EU", color = UlubBlue, fontWeight = FontWeight.Black)
                Text("Muzyka zawsze pod ręką", fontSize = 11.sp)
            }
        },
        actions = {
            IconButton(onClick = onTheme) {
                Icon(if (dark) Icons.Default.LightMode else Icons.Default.DarkMode, "Motyw")
            }
            IconButton(onClick = onHome) { Icon(Icons.Default.Home, "Główna") }
        }
    )
}

@Composable
private fun HomeContent(
    modifier: Modifier,
    state: eu.ulubmp3.app.ui.HomeUiState,
    onQuery: (String) -> Unit,
    onPlay: (Track) -> Unit,
    onAdd: (Track) -> Unit,
    onDownload: (Track) -> Unit,
    onShare: (Track) -> Unit
) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 12.dp)) {
        item {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQuery,
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                placeholder = { Text("Szukaj utworu lub wykonawcy...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (state.query.isNotBlank()) {
                        IconButton(onClick = { onQuery("") }) { Icon(Icons.Default.Close, null) }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp)
            )
        }

        if (state.query.isBlank() && state.selectedCategory == null && state.top.isNotEmpty()) {
            item {
                Text("Top 15 Dziś", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(12.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.top, key = { it.id }) { track ->
                        ElevatedCard(onClick = { onPlay(track) }, modifier = Modifier.width(210.dp)) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MusicNote, null, tint = UlubBlue)
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(track.title, maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold, color = UlubBlue)
                                    Text(track.category, fontSize = 11.sp)
                                }
                                Icon(Icons.Default.PlayArrow, null)
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                state.selectedCategory?.name ?: if (state.query.isBlank()) "Najnowsze utwory" else "Wyniki wyszukiwania",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(12.dp)
            )
        }

        if (state.loading && state.latest.isEmpty()) {
            item { Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        }
        state.error?.let { message ->
            item { Text(message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp)) }
        }
        items(state.latest, key = { it.id }) { track ->
            TrackCard(track, onPlay, onAdd, onDownload, onShare)
        }
    }
}

@Composable
private fun TrackCard(
    track: Track,
    onPlay: (Track) -> Unit,
    onAdd: (Track) -> Unit,
    onDownload: (Track) -> Unit,
    onShare: (Track) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column {
            Column(Modifier.padding(12.dp)) {
                Text(track.title, color = UlubBlue, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(track.category, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                val meta = listOfNotNull(track.date, track.size, track.bitrate).filter { it.isNotBlank() }.joinToString(" • ")
                if (meta.isNotBlank()) Text(meta, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(
                Modifier.fillMaxWidth().background(UlubBlue.copy(alpha = 0.08f)).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledIconButton(onClick = { onPlay(track) }, enabled = !track.audioUrl.isNullOrBlank()) {
                    Icon(Icons.Default.PlayArrow, "Odtwórz")
                }
                Spacer(Modifier.width(8.dp))
                Text(if (track.audioUrl.isNullOrBlank()) "Brak audio" else "Odtwórz", modifier = Modifier.weight(1f))
                TextButton(onClick = { onAdd(track) }) { Text("PlayLista") }
            }
            Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = { onShare(track) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Share, null); Spacer(Modifier.width(4.dp)); Text("Udostępnij", fontSize = 11.sp)
                }
                Button(onClick = { onDownload(track) }, enabled = !track.downloadUrl.isNullOrBlank(), modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Download, null); Spacer(Modifier.width(4.dp)); Text("Pobierz MP3", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun CategoriesContent(
    modifier: Modifier,
    categories: List<Category>,
    selected: Category?,
    onSelect: (Category) -> Unit
) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp)) {
        item { Text("Kategorie muzyczne", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(vertical = 8.dp)) }
        items(categories, key = { it.id }) { category ->
            ListItem(
                headlineContent = { Text(category.name, fontWeight = if (selected?.id == category.id) FontWeight.Bold else FontWeight.Normal) },
                leadingContent = { Icon(Icons.Default.Folder, null, tint = UlubBlue) },
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = { onSelect(category) }, modifier = Modifier.fillMaxWidth()) { Text("Pokaż ${category.name}") }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun PlaylistContent(
    modifier: Modifier,
    playlist: List<Track>,
    onPlay: (Track) -> Unit,
    onRemove: (Track) -> Unit,
    onDownload: (Track) -> Unit
) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp)) {
        item { Text("Moja PlayLista", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(vertical = 8.dp)) }
        if (playlist.isEmpty()) item { Text("Playlista jest pusta.", modifier = Modifier.padding(16.dp)) }
        items(playlist, key = { it.id }) { track ->
            Surface(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(track.title, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    IconButton(onClick = { onPlay(track) }) { Icon(Icons.Default.PlayArrow, "Odtwórz") }
                    IconButton(onClick = { onDownload(track) }) { Icon(Icons.Default.Download, "Pobierz") }
                    IconButton(onClick = { onRemove(track) }) { Icon(Icons.Default.Delete, "Usuń") }
                }
            }
        }
    }
}

@Stable
private class AudioController {
    var current by mutableStateOf<Track?>(null); private set
    var playing by mutableStateOf(false); private set
    var position by mutableIntStateOf(0); private set
    var durationMs by mutableIntStateOf(0); private set
    private var mediaPlayer: MediaPlayer? = null

    fun play(track: Track) {
        val url = track.audioUrl ?: return
        if (current?.id == track.id && mediaPlayer != null) {
            toggle()
            return
        }
        releasePlayer()
        current = track
        position = 0
        durationMs = 0
        mediaPlayer = MediaPlayer().also { player ->
            player.setDataSource(url)
            player.setOnPreparedListener { prepared ->
                durationMs = prepared.duration.coerceAtLeast(0)
                prepared.start()
                playing = true
            }
            player.setOnCompletionListener {
                playing = false
                position = durationMs
            }
            player.setOnErrorListener { _, _, _ -> playing = false; true }
            player.prepareAsync()
        }
    }

    fun toggle() {
        mediaPlayer?.let { p ->
            if (p.isPlaying) { p.pause(); playing = false } else { p.start(); playing = true }
        }
    }

    fun updatePosition() { position = runCatching { mediaPlayer?.currentPosition ?: 0 }.getOrDefault(0) }
    fun seekTo(ms: Int) { runCatching { mediaPlayer?.seekTo(ms) } }
    fun stop() { releasePlayer(); current = null; playing = false; position = 0; durationMs = 0 }
    fun release() = stop()
    private fun releasePlayer() { runCatching { mediaPlayer?.stop() }; runCatching { mediaPlayer?.release() }; mediaPlayer = null }
}

@Composable
private fun MiniPlayer(track: Track, audio: AudioController) {
    LaunchedEffect(track.id, audio.playing) {
        while (true) { audio.updatePosition(); delay(500) }
    }
    Surface(shadowElevation = 6.dp) {
        Column {
            if (audio.durationMs > 0) {
                Slider(
                    value = audio.position.toFloat().coerceIn(0f, audio.durationMs.toFloat()),
                    onValueChange = { audio.seekTo(it.toInt()) },
                    valueRange = 0f..audio.durationMs.toFloat(),
                    modifier = Modifier.fillMaxWidth().height(24.dp)
                )
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MusicNote, null, tint = UlubBlue)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("${formatTime(audio.position)} / ${formatTime(audio.durationMs)}", fontSize = 10.sp)
                }
                IconButton(onClick = audio::toggle) { Icon(if (audio.playing) Icons.Default.Pause else Icons.Default.PlayArrow, null) }
                IconButton(onClick = audio::stop) { Icon(Icons.Default.Close, null) }
            }
        }
    }
}

private fun formatTime(ms: Int): String {
    val sec = (ms / 1000).coerceAtLeast(0)
    return "%02d:%02d".format(sec / 60, sec % 60)
}

private fun downloadTrack(context: Context, track: Track) {
    val url = track.downloadUrl ?: run {
        Toast.makeText(context, "Brak linku do pobrania.", Toast.LENGTH_SHORT).show()
        return
    }
    runCatching {
        val fileName = track.title.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(120) + ".mp3"
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(track.title)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
        Toast.makeText(context, "Rozpoczęto pobieranie.", Toast.LENGTH_SHORT).show()
    }.onFailure {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}

private fun shareTrack(context: Context, track: Track) {
    val url = track.pageUrl ?: track.downloadUrl ?: "https://ulubmp3.eu"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "${track.title}\n$url")
    }
    context.startActivity(Intent.createChooser(intent, "Udostępnij"))
}
