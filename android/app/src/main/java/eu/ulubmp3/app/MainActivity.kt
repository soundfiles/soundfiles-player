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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import eu.ulubmp3.app.data.Category
import eu.ulubmp3.app.data.Track
import eu.ulubmp3.app.ui.HomeUiState
import eu.ulubmp3.app.ui.HomeViewModel
import eu.ulubmp3.app.ui.UlubTheme
import kotlinx.coroutines.delay

private val BrandBlue = Color(0xFF079CFF)
private val BrandBlueDark = Color(0xFF0087E3)
private val BrandOrange = Color(0xFFC46A1A)
private val SoftBorder = Color(0xFFDCE5EC)
private val SoftPlayer = Color(0xFFF7F9FB)
private val MetaGray = Color(0xFF626B73)

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
    val liked = remember { mutableStateMapOf<Int, Boolean>() }
    val audio = remember { AudioController() }
    var tab by remember { mutableStateOf(AppTab.HOME) }
    var dark by remember { mutableStateOf(false) }

    DisposableEffect(Unit) { onDispose { audio.release() } }

    UlubTheme(darkTheme = dark) {
        Scaffold(
            containerColor = if (dark) MaterialTheme.colorScheme.background else Color(0xFFF0F4FA),
            bottomBar = {
                Column {
                    audio.current?.let { MiniPlayer(it, audio) }
                    SmartphoneBottomBar(
                        tab = tab,
                        onCategories = { tab = AppTab.CATEGORIES },
                        onHome = { tab = AppTab.HOME; vm.selectCategory(null) },
                        onPlaylist = { tab = AppTab.PLAYLIST }
                    )
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.TopCenter
            ) {
                Surface(
                    modifier = Modifier.fillMaxHeight().widthIn(max = 430.dp).fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = if (dark) 0.dp else 1.dp
                ) {
                    when (tab) {
                        AppTab.HOME -> HomeContent(
                            state = state,
                            dark = dark,
                            onToggleTheme = { dark = !dark },
                            onQuery = vm::setQuery,
                            onSearch = { if (state.query.isNotBlank()) vm.setQuery(state.query) },
                            onPlay = audio::play,
                            onAdd = { if (playlist.none { p -> p.id == it.id }) playlist.add(it) },
                            onDownload = { downloadTrack(context, it) },
                            onShare = { shareTrack(context, it) },
                            isLiked = { liked[it.id] == true },
                            onLike = { liked[it.id] = !(liked[it.id] ?: false) }
                        )
                        AppTab.CATEGORIES -> CategoriesContent(
                            categories = state.categories,
                            selected = state.selectedCategory,
                            onSelect = {
                                vm.selectCategory(it)
                                tab = AppTab.HOME
                            }
                        )
                        AppTab.PLAYLIST -> PlaylistContent(
                            playlist = playlist,
                            onPlay = audio::play,
                            onRemove = { playlist.remove(it) },
                            onDownload = { downloadTrack(context, it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    dark: Boolean,
    onToggleTheme: () -> Unit,
    onQuery: (String) -> Unit,
    onSearch: () -> Unit,
    onPlay: (Track) -> Unit,
    onAdd: (Track) -> Unit,
    onDownload: (Track) -> Unit,
    onShare: (Track) -> Unit,
    isLiked: (Track) -> Boolean,
    onLike: (Track) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 14.dp)
    ) {
        item { SmartphoneHeader(dark, onToggleTheme) }
        item { SmartphoneSearch(state.query, onQuery, onSearch) }

        if (state.query.isBlank() && state.selectedCategory == null && state.top.isNotEmpty()) {
            item {
                Column(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                    Text(
                        "Top 15 Dziś",
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.top.take(15), key = { "top-${it.id}" }) { track ->
                            Surface(
                                modifier = Modifier.width(176.dp).clickable { onPlay(track) },
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, SoftBorder),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        Modifier.size(34.dp).clip(CircleShape).background(BrandBlue),
                                        contentAlignment = Alignment.Center
                                    ) { Icon(Icons.Default.PlayArrow, null, tint = Color.White) }
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        track.title,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        color = BrandBlue,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (state.selectedCategory != null || state.query.isNotBlank()) {
            item {
                Text(
                    state.selectedCategory?.name ?: "Wyniki wyszukiwania",
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (state.loading && state.latest.isEmpty()) {
            item { Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = BrandBlue) } }
        }
        state.error?.let { message ->
            item { Text(message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }
        }
        items(state.latest, key = { it.id }) { track ->
            SmartphoneTrackCard(
                track = track,
                liked = isLiked(track),
                onPlay = onPlay,
                onAdd = onAdd,
                onDownload = onDownload,
                onShare = onShare,
                onLike = onLike
            )
        }
    }
}

@Composable
private fun SmartphoneHeader(dark: Boolean, onToggleTheme: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.GraphicEq, null, tint = BrandBlue, modifier = Modifier.size(30.dp))
                Text("ulub", color = BrandBlue, fontSize = 25.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
            }
            Text(
                "Twoja ulubiona muzyka w jednym miejscu",
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(onClick = onToggleTheme, modifier = Modifier.size(34.dp)) {
                Icon(
                    if (dark) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Motyw",
                    tint = if (dark) Color(0xFFFFC94A) else Color(0xFFE3A700),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
    HorizontalDivider(color = SoftBorder)
}

@Composable
private fun SmartphoneSearch(query: String, onQuery: (String) -> Unit, onSearch: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            modifier = Modifier.weight(1f).height(52.dp),
            placeholder = { Text("Wpisz tytuł utworu lub wykonawcę", fontSize = 12.sp) },
            singleLine = true,
            shape = RoundedCornerShape(topStart = 7.dp, bottomStart = 7.dp),
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = { onQuery("") }) { Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp)) }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrandBlue,
                unfocusedBorderColor = BrandBlue
            )
        )
        Button(
            onClick = onSearch,
            modifier = Modifier.height(52.dp),
            shape = RoundedCornerShape(topEnd = 7.dp, bottomEnd = 7.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
            contentPadding = PaddingValues(horizontal = 14.dp)
        ) {
            Icon(Icons.Default.Search, null, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(4.dp))
            Text("Szukaj", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SmartphoneTrackCard(
    track: Track,
    liked: Boolean,
    onPlay: (Track) -> Unit,
    onAdd: (Track) -> Unit,
    onDownload: (Track) -> Unit,
    onShare: (Track) -> Unit,
    onLike: (Track) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 9.dp),
        shape = RoundedCornerShape(7.dp),
        border = BorderStroke(1.dp, SoftBorder),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    track.title,
                    modifier = Modifier.weight(1f),
                    color = BrandBlue,
                    fontSize = 15.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                TextButton(
                    onClick = { onAdd(track) },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(Icons.Default.Add, null, tint = BrandOrange, modifier = Modifier.size(15.dp))
                    Text("PlayLista", color = BrandOrange, fontSize = 11.sp)
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth().height(92.dp).background(SoftPlayer),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.width(88.dp).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        onClick = { onPlay(track) },
                        enabled = !track.audioUrl.isNullOrBlank(),
                        modifier = Modifier.size(58.dp),
                        shape = CircleShape,
                        color = BrandBlue,
                        shadowElevation = 2.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.PlayArrow, "Odtwórz", tint = Color.White, modifier = Modifier.size(33.dp))
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Text("00:00  /  00:00", fontSize = 9.sp, color = MetaGray)
                }

                Waveform(
                    modifier = Modifier.weight(1f).fillMaxHeight().padding(horizontal = 8.dp, vertical = 14.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Folder, null, tint = MetaGray, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(track.category.ifBlank { "Muzyka" }, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MetaGray)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.Schedule, null, tint = MetaGray, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(3.dp))
                Text(track.date, fontSize = 10.sp, color = MetaGray)
            }

            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = { onShare(track) },
                    modifier = Modifier.weight(1f).height(42.dp),
                    shape = RoundedCornerShape(5.dp),
                    border = BorderStroke(1.dp, SoftBorder),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.Share, null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Udostępnij", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
                }

                OutlinedButton(
                    onClick = { onLike(track) },
                    modifier = Modifier.width(54.dp).height(42.dp),
                    shape = RoundedCornerShape(5.dp),
                    border = BorderStroke(1.dp, if (liked) Color(0xFFFFA6A6) else SoftBorder),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        Icons.Default.ThumbUp,
                        null,
                        tint = if (liked) Color(0xFFFF5B64) else Color(0xFF9AA2A9),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(if (liked) "1" else "0", fontSize = 10.sp, color = if (liked) Color(0xFFFF5B64) else MetaGray)
                }

                Button(
                    onClick = { onDownload(track) },
                    enabled = !track.downloadUrl.isNullOrBlank(),
                    modifier = Modifier.weight(1f).height(42.dp),
                    shape = RoundedCornerShape(5.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Pobierz MP3", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun Waveform(modifier: Modifier = Modifier) {
    val bars = remember {
        listOf(0.38f,0.72f,0.46f,0.88f,0.60f,0.31f,0.69f,0.91f,0.56f,0.80f,0.43f,0.72f,0.93f,0.51f,0.66f,0.84f,0.39f,0.73f,0.47f,0.90f,0.61f,0.34f,0.78f,0.56f,0.87f,0.45f,0.75f,0.95f,0.53f,0.68f,0.82f,0.42f,0.71f,0.50f,0.89f)
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        bars.forEach { factor ->
            Box(
                Modifier
                    .width(3.dp)
                    .fillMaxHeight(factor)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF777777))
            )
        }
    }
}

@Composable
private fun SmartphoneBottomBar(
    tab: AppTab,
    onCategories: () -> Unit,
    onHome: () -> Unit,
    onPlaylist: () -> Unit
) {
    Surface(shadowElevation = 12.dp, color = MaterialTheme.colorScheme.surface) {
        Box(Modifier.fillMaxWidth().height(72.dp)) {
            Row(
                Modifier.fillMaxWidth().align(Alignment.BottomCenter).height(60.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomSideItem(
                    modifier = Modifier.weight(1f),
                    selected = tab == AppTab.CATEGORIES,
                    icon = Icons.Default.List,
                    label = "KATEGORIE",
                    onClick = onCategories
                )
                Spacer(Modifier.weight(1f))
                BottomSideItem(
                    modifier = Modifier.weight(1f),
                    selected = tab == AppTab.PLAYLIST,
                    icon = Icons.Default.PlaylistPlay,
                    label = "PLAYLISTA",
                    onClick = onPlaylist
                )
            }

            Surface(
                onClick = onHome,
                modifier = Modifier.size(68.dp).align(Alignment.TopCenter),
                shape = CircleShape,
                color = BrandBlue,
                border = BorderStroke(4.dp, Color.White),
                shadowElevation = 8.dp
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.Home, null, tint = Color.White, modifier = Modifier.size(25.dp))
                    Text("GŁÓWNA", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun BottomSideItem(
    modifier: Modifier,
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxHeight().clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = if (selected) BrandBlue else Color(0xFF333333), modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (selected) BrandBlue else Color(0xFF333333))
    }
}

@Composable
private fun CategoriesContent(
    categories: List<Category>,
    selected: Category?,
    onSelect: (Category) -> Unit
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp)) {
        item {
            Text("KATEGORIE MUZYCZNE", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(vertical = 10.dp))
        }
        items(categories, key = { it.id }) { category ->
            Surface(
                onClick = { onSelect(category) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, SoftBorder),
                color = if (selected?.id == category.id) BrandBlue.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Folder, null, tint = BrandBlue)
                    Spacer(Modifier.width(10.dp))
                    Text(category.name, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.ChevronRight, null, tint = MetaGray)
                }
            }
        }
    }
}

@Composable
private fun PlaylistContent(
    playlist: List<Track>,
    onPlay: (Track) -> Unit,
    onRemove: (Track) -> Unit,
    onDownload: (Track) -> Unit
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp)) {
        item { Text("Moja PlayLista", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(vertical = 10.dp)) }
        if (playlist.isEmpty()) item { Text("Playlista jest pusta.", modifier = Modifier.padding(vertical = 20.dp), color = MetaGray) }
        items(playlist, key = { it.id }) { track ->
            Surface(
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                shape = RoundedCornerShape(7.dp),
                border = BorderStroke(1.dp, SoftBorder)
            ) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(onClick = { onPlay(track) }, shape = CircleShape, color = BrandBlue, modifier = Modifier.size(38.dp)) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.PlayArrow, null, tint = Color.White) }
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(track.title, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis, color = BrandBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
    Surface(shadowElevation = 6.dp, color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = BrandBlue, modifier = Modifier.size(38.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.MusicNote, null, tint = Color.White) }
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandBlue)
                Text("${formatTime(audio.position)} / ${formatTime(audio.durationMs)}", fontSize = 9.sp, color = MetaGray)
            }
            IconButton(onClick = audio::toggle) { Icon(if (audio.playing) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = BrandBlue) }
            IconButton(onClick = audio::stop) { Icon(Icons.Default.Close, null) }
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
