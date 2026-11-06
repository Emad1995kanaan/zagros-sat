package com.kanaani.ibo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.kanaani.ibo.BuildConfig
import com.kanaani.ibo.data.Channel
import com.kanaani.ibo.data.Movie
import com.kanaani.ibo.data.Show
import com.kanaani.ibo.data.Store
import com.kanaani.ibo.data.Xtream
import kotlinx.coroutines.launch

// ───────────────────────── Favoris ─────────────────────────

@Composable
fun FavoritesScreen(nav: Navigator, x: Xtream) {
    var tab by rememberSaveable { mutableStateOf("live") }
    val list = Store.favorites.filter { it.kind == tab }
    val first = remember { FocusRequester() }
    InitialFocus(first)
    Backdrop {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Header(Str.favorites)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("live" to Str.live, "movie" to Str.movies, "series" to Str.series).forEachIndexed { i, (k, label) ->
                    val on = tab == k
                    Tile({ tab = k }, (if (i == 0) Modifier.focusRequester(first) else Modifier).height(38.dp), RoundedCornerShape(19.dp),
                        colors = if (on) C.live else listOf(C.card), focusColors = C.live, focusScale = 1.04f) {
                        Box(Modifier.padding(horizontal = 20.dp).height(38.dp), contentAlignment = Alignment.Center) {
                            Label(label, 14.sp, weight = if (on) FontWeight.Bold else FontWeight.Medium)
                        }
                    }
                }
            }
            if (list.isEmpty()) { Message(Str.empty); return@Column }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(if (tab == "live") 210.dp else 118.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(4.dp),
            ) {
                itemsIndexed(list, key = { _, f -> f.kind + f.id }) { i, f ->
                    when (tab) {
                        "live" -> Tile({
                            val channels = list.mapIndexed { n, c -> Channel(c.id, c.name, c.image, n + 1, null) }
                            nav.go(Screen.Play(f.name, x.liveUrl(f.id), live = true, channels = channels, index = i))
                        }, Modifier.fillMaxWidth().height(56.dp), RoundedCornerShape(12.dp), colors = listOf(C.card), focusColors = C.live, focusScale = 1.03f) {
                            Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(f.image, null, Modifier.size(36.dp).clip(RoundedCornerShape(6.dp)), contentScale = ContentScale.Fit)
                                Spacer(Modifier.width(10.dp))
                                Label(f.name, 14.sp, weight = FontWeight.SemiBold)
                            }
                        }
                        "movie" -> PosterCard(Poster(f.id, f.name, f.image, null), Modifier) {
                            nav.go(Screen.MovieDetail(Movie(f.id, f.name, f.image, null, f.ext.ifEmpty { "mp4" }, null)))
                        }
                        else -> PosterCard(Poster(f.id, f.name, f.image, null), Modifier) {
                            nav.go(Screen.ShowDetail(Show(f.id, f.name, f.image, null, null)))
                        }
                    }
                }
            }
        }
    }
}

// ───────────────────────── Compte ─────────────────────────

@Composable
fun AccountScreen(x: Xtream) {
    LaunchedEffect(x) { AccountCache.load(x) }
    val a = AccountCache.value
    Backdrop {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 14.dp)) {
            Header(Str.account)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    Modifier.width(460.dp).clip(RoundedCornerShape(20.dp)).background(C.panel).padding(26.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Logo(40.dp)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Label(x.username, 20.sp, weight = FontWeight.Bold)
                            Label(com.kanaani.ibo.Brand.name, 12.sp, C.text2)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    if (a == null) { Box(Modifier.height(120.dp)) { Loading() }; return@Column }
                    val active = a.status?.lowercase() == "active"
                    InfoRow(Str.status, if (active) Str.active else (a.status ?: "—"), if (active) Color(0xFF4ADE80) else C.error)
                    InfoRow(Str.expires, a.expiresAt?.let { date(it) } ?: Str.unlimited)
                    a.expiresAt?.let { exp ->
                        val days = ((exp - System.currentTimeMillis()).coerceAtLeast(0) + 86_399_999L) / 86_400_000L
                        InfoRow(Str.daysLeft, "$days")
                    }
                    InfoRow(Str.connections, "${a.activeConnections ?: 0} / ${a.maxConnections ?: "—"}")
                    a.createdAt?.let { InfoRow(Str.created, date(it)) }
                    InfoRow(Str.trial, if (a.trial) Str.yes else Str.no)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(k: String, v: String, color: Color = C.text) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Label(k, 14.sp, C.text2, modifier = Modifier.weight(1f))
        Label(v, 14.sp, color, FontWeight.Bold)
    }
}

// ───────────────────────── Réglages ─────────────────────────

private data class SettingItem(val label: String, val icon: String, val colors: List<Color> = C.small, val onClick: () -> Unit)

@Composable
fun SettingsScreen(nav: Navigator, x: Xtream) {
    var dialog by remember { mutableStateOf<String?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(toast) { if (toast != null) { kotlinx.coroutines.delay(2_500); toast = null } }
    val first = remember { FocusRequester() }
    InitialFocus(first)
    val items = listOf(
        SettingItem(Str.language, Icons.Globe) { dialog = "lang" },
        SettingItem(Str.homeTheme, Icons.Grid) { dialog = "theme" },
        SettingItem(Str.account, Icons.Account) { nav.go(Screen.Account) },
        SettingItem(Str.orderLive, Icons.Live) { nav.go(Screen.Reorder("live")) },
        SettingItem(Str.orderMovies, Icons.Movies) { nav.go(Screen.Reorder("movie")) },
        SettingItem(Str.orderSeries, Icons.Series) { nav.go(Screen.Reorder("series")) },
        SettingItem(Str.refresh, Icons.Refresh) { x.clearCache(); AccountCache.value = null; toast = Str.refreshed },
        SettingItem(Str.checkUpdates, Icons.Refresh) { scope.launch { com.kanaani.ibo.data.Updater.check(manual = true) } },
        SettingItem(Str.about, Icons.Info) { dialog = "about" },
        SettingItem(Str.logout, Icons.Logout, C.danger) { dialog = "logout" },
    )
    Backdrop {
        Column(Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 18.dp)) {
            Header(Str.settings)
            Spacer(Modifier.height(20.dp))
            items.chunked(4).forEachIndexed { r, row ->
            if (r > 0) Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth().height(112.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEachIndexed { i, item ->
                    Tile(item.onClick, (if (r == 0 && i == 0) Modifier.focusRequester(first) else Modifier).weight(1f).height(112.dp),
                        RoundedCornerShape(18.dp), colors = item.colors, focusColors = C.live) {
                        Column(Modifier.fillMaxSize().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(item.icon, 42.dp, Color.White, stroke = 1.6f)
                            Spacer(Modifier.height(12.dp))
                            Label(item.label, 15.sp, weight = FontWeight.Bold, lines = 2)
                        }
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
            }
            Spacer(Modifier.weight(1f))
            toast?.let { Label(it, 14.sp, Color(0xFFFF6B70), FontWeight.SemiBold) }
        }
    }
    when (dialog) {
        "theme" -> ChoiceDialog(Str.homeTheme, listOf(Str.themeBillboard, Str.themeQueen).mapIndexed { i, n ->
            (if (Store.homeTheme == i) "✓  $n" else n) to { Store.chooseHomeTheme(i); dialog = null }
        }) { dialog = null }
        "lang" -> ChoiceDialog(Str.language, Lang.entries.map { l -> l.label to { Store.chooseLanguage(l.code); dialog = null } }) { dialog = null }
        "logout" -> ChoiceDialog(Str.logoutAsk, listOf(
            Str.logout to { dialog = null; AccountCache.value = null; com.kanaani.ibo.player.Playback.stop(); Store.logout() },
            Str.cancel to { dialog = null },
        )) { dialog = null }
        "about" -> ChoiceDialog("${com.kanaani.ibo.Brand.name} · ${Str.version} ${BuildConfig.VERSION_NAME}", listOf(Str.ok to { dialog = null })) { dialog = null }
    }
}
