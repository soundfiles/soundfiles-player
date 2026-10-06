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
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import eu.ulubmp3.app.data.Track
import eu.ulubmp3.app.ui.HomeUiState
import eu.ulubmp3.app.ui.HomeViewModel
import eu.ulubmp3.app.ui.UlubBlue
import eu.ulubmp3.app.ui.UlubTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { UlubMp3App() }
    }
}

private enum class Screen { HOME, PLAYLIST }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UlubMp3App() {
    val context = LocalContext.current
    val vm: HomeViewModel = viewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val playlist = remember { mutableStateListOf<Track>() }
    val player = remember { AppAudioPlayer() }
    var screen by remember { mutableStateOf(Screen.HOME) }
    var dark by remember { mutableStateOf(false) }

    DisposableEffect(Unit) { onDispose { player.release() } }

    UlubTheme(darkTheme = dark) {
        ModalNavigationDrawer(
            drawerState = drawer,
            drawerContent = {
                ModalDrawerSheet(modifier = Modifier.width(310.dp)) {
                    Text("KATEGORIE MUZYCZNE", fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(18.dp))
                    HorizontalDivider()
                    NavigationDrawerItem(
                        label = { Text("Wszystkie / Główna") },
                        selected = state.selectedCategory == null,
                        icon = { Icon(Icons.Default.Home, null) },
                        onClick = {
                            screen = Screen.HOME
                            vm.selectCategory(null)
                            scope.close(drawer)
                        },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    state.categories.forEach { category ->
                        NavigationDrawerItem(
                            label = { Text(category.name) },
                            selected = state.selectedCategory?.id == category.id,
                            icon = { Icon(Icons.Default.Folder, null) },
                            onClick = {
                                screen = Screen.HOME
                                vm.selectCategory(category)
                                scope.close(drawer)
                            },
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }
        ) {
            Scaffold(
                bottomBar = {
                    Column {
                        player.current?.let { MiniPlayer(it, player) }
                        BottomBar(
                            screen = screen,
                            onCategories = { scope.open(drawer) },
                            onHome = { screen = Screen.HOME; vm.selectCategory(null) },
                            onPlaylist = { screen = Screen.PLAYLIST }
                        )
                    }
                }
            ) { padding ->
                when (screen) {
                    Screen.HOME -> HomeScreen(
                        modifier = Modifier.padding(padding),
                        state = state,
                        dark = dark,
                        onToggleTheme = { dark = !dark },
                        onQuery = vm::setQuery,
                        onCategories = { scope.open(drawer) },
                        onPlay = player::play,
                        onAdd = { track ->
                            if (playlist.none { it.id == track.id }) playlist.add(track)
                        },
                        onDownload = { downloadTrack(context, it) },
                        onShare = { shareTrack(context, it) }
                    )
                    Screen.PLAYLIST -> PlaylistScreen(
                        modifier = Modifier.padding(padding),
                        playlist = playlist,
                        onPlay = player::play,
                        onRemove = { playlist.remove(it) },
                        onDownload = { downloadTrack(context, it) }
                    )
                }
            }
        }
    }
}

private fun CoroutineScope.open(drawer: DrawerState) = launch { drawer.open() }
private fun CoroutineScope.close(drawer: DrawerState) = launch { drawer.close() }

@Composable
private fun HomeScreen(
    modifier: Modifier,
    state: HomeUiState,
    dark: Boolean,
    onToggleTheme: () -> Unit,
    onQuery: (String) -> Unit,
    onCategories: () -> Unit,
    onPlay: (Track) -> Unit,
    onAdd: (Track) -> Unit,
    onDownload: (Track) -> Unit,
    onShare: (Track) -> Unit
) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 14.dp)) {
        item { Header(dark, onToggleTheme) }
        item { SearchField(state.query, onQuery) }

        if (state.query.isBlank() && state.selectedCategory == null && state.top.isNotEmpty()) {
            item {
                Text("Top 15 Dziś", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(14.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.top, key = { it.id }) { track -> TopCard(track, onPlay) }
                }
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    state.selectedCategory?.name ?: if (state.query.isBlank()) "Najnowsze utwory" else "Wyniki wyszukiwania",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onCategories) {
                    Icon(Icons.Default.List, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Kategorie")
                }
            }
        }

        if (state.loading && state.latest.isEmpty()) {
            item { Box(Modifier.fillMaxWidth().padding(42.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        }
        state.error?.let { message ->
            item { Text(message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(14.dp)) }
        }
        items(state.latest, key = { it.id }) { track ->
            TrackCard(track, onPlay, onAdd, onDownload, onShare)
        }
    }
}

@Composable
private fun Header(dark: Boolean, onToggleTheme: () -> Unit) {
    Surface(shadowElevation = 2.dp) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("ULUBMP3.EU", color = UlubBlue, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text("Muzyka zawsze pod ręką", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onToggleTheme) {
                Icon(if (dark) Icons.Default.LightMode else Icons.Default.DarkMode, "MotyÉw")
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQuery: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQuery,
        leadingIcon = { Icon(Icons.Default.Search, null) },
        trailingIcon = {
            if (query.isNotBlank()) IconButton(onClick = { onQuery("") }) { Icon(Icons.Default.Close, null) }
        },
        placeholder = { Text("Szukaji utworu lub wykonawcy...") },
        singleLine = true,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth().padding(12.dp)
    )
}

@Composable
private fun TopCard(track: Track, onPlay: (Track) -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.width(210.dp).clickable { onPlay(track) }
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Cover(track, 54.dp)
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, color = UlubBlue, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(track.category, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.PlayArrow, null, tint = UlubBlue)
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
        shape = RoundedCornerShape(7.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Column {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Cover(track, 66.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(track.title, color = UlubBlue, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    Text(track.category, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Text(listOfNotNull(track.date, track.size, track.bitrate).filter { it.isNOtBlank() }.joinToString(" • "), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(
                Modifier.fillMaxWidth().background(UlubBlue.copy(alpha = 0.07f)).padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledIconButton(onClick = { onPlay(track) }, enabled = !track.audioUrl.isNullOrBlank()) {
                    Icon(Icons.Default.PlayArrow, "Odtwórz")
                }
                Spacer(Modifier.width(10.dp))
                Text(if (track.audioUrl.isNullOrBlank()) "Brak pliku audio" else "Odtwórz utwór", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                TextButton(onClick = { onAdd(track) }) { Icon(Icons.Default.Add, null); Text("PlayLista") }
            }
            Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = { onShare(track) }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Share, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Udostępnij", fontSize = 10.sp) }
                Button(onClick = { onDownload(track) }, enabled = !track.downloadUrl.isNullOrBlank(), modifier = Modifier.weight(1f)) { Icon(Icons.Default.Download, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Pobierz MP3", fontSize = 10.sp) }
            }
        }
    }
}

@Composable
private fun Cover(track: Track, size: androidx.compose.ui.unit.Dp) {
    if (!track.imageUrl.isNullOrBlank()) {
        AsyncImage(
            model = track.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(size).clip(RoundedCornerShape(6.dp))
        )
    } else {
        Box(
            Modifier.size(size).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Default.MusicNote, null, tint = UlubBlue) }
    }
}

@Composable
private fun BottomBar(screen: Screen, onCategories: () -> Unit, onHome: () -> Unit, onPlaylist: () -> Unit) {
    NavigationBar {
        NavigationBarItem(selected = false, onClick = onCategories, icon = { Icon(Icons.Default.List, null) }, label = { Text("Kategorie") })
        NavigationBarItem(selected = screen == Screen.HOME, onClick = onHome, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Główna") })
        NavigationBarItem(selected = screen == Screen.PLAYLIST, onClick = onPlaylist, icon = { Icon(Icons.Default.PlaylistPlay, null) }, label = { Text("Playlista") })
    }
}

@Composable
private fun PlaylistScreen(
    modifier: Modifier,
    playlist: List<Track>,
    onPlay: (Track) -> Unit,
    onRemove: (Track) -> Unit,
    onDownload: (Track) -> Unit
) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp)) {
        item { Text("Moja PlayLista", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(vertical = 12.dp)) }
        if (playlist.isEmpty()) item { Text("Playlista jest pusta.", modifier = Modifier.padding(16.dp)) }
        items(playlist, key = { it.id }) { track ->
            Surface(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), shape = RoundedCornerShape(6.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Cover(track, 48.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(track.title, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { onPlay(track) }) { Icon(Icons.Default.PlayArrow, "Odtwórz") }
                    IconButton(onClick = { onDownload(track) }) { Icon(Icons.Default.Download, "Pobierz") }
                    IconButton(onClick = { onRemove(track) }) { Icon(Icons.Default.Delete, "Usuń") }
                }
            }
        }
    }
}

@Stable
private class AppAudioPlayer {
    var current by mutableStateOf<Track?>(null); private set
    var playing by mutableStateOf(false); private set
    var position by mutableIntStateOf(0); private set
    var duration by mutableIntStateOf(0); private set
    private var mediaPlayer: MediaPlayer? = null

    fun play(track: Track) {
        val url = track.audioUrl ?: return
        if (current?.id == track.id && mediaPlayer != null) {
            toggle()
            return
        }
        releaseMedia()
        current = track
        position = 0
        duration = 0
        mediaPlayer = MediaPlayer().apply {
            setDataSource(url)
            setOnPreparedListener { p -> duration = p.duration.coerceAtLeast(0); p.start(); playing = true }
            setOnCompletionListener { playing = false; position = duration }
            setOnErrorListener { _, _, _ -> playing = false; true }
            prepareAsync()
        }
    }

    fun toggle() {
        mediaPlayer?.let { p -> if (p.isPlaying) { p.pause(); playing = false } else { p.start(); playing = true } }
    }

    fun updatePosition() { runCatching { position = mediaPlayer?.currentPosition ?: 0 } }
    fun seekTo(ms: Int) { runCatching { mediaPlayer?.seekTo(ms) } }
    fun stop() { releaseMedia(); current = null; playing = false; position = 0; duration = 0 }
    fun release() = stop()
    private fun releaseMedia() { runCatching { mediaPlayer?.stop() }; runCatching { mediaPlayer?.release() }; mediaPlayer = null }
}

@Composable
private fun MiniPlayer(track: Track, player: AppAudioPlayer) {
    LaunchedEffect(track.id, player.playing) {
        while (true) { player.updatePosition(); delay(500) }
    }
    Surface(shadowElevation = 8.dp) {
        Column {
            if (player.duration > 0) {
                Slider(
                    value = player.position.toFloat().coerceIn(0f, player.duration.toFloat()),
                    onValueChange = { player.seekTo(it.toInt()) },
                    valueRange = 0f..player.duration.toFloat(),
                    modifier = Modifier.fillMaxWidth().height(22.dp)
                )
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Cover(track, 40.dp)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("${time(player.position)} / ${time(player.duration)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = player::toggle) { Icon(if (player.playing) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = UlubBlue) }
                IconButton(onClick = player::stop) { Icon(Icons.Default.Close, null) }
            }
        }
    }
}

private fun time(ms: Int): String {
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
