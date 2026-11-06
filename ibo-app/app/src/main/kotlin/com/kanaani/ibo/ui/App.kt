package com.kanaani.ibo.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import com.kanaani.ibo.data.Store
import com.kanaani.ibo.player.Playback

/** Racine : connexion tant qu'aucun abonnement n'est enregistré, puis la pile d'écrans. */
@Composable
fun App(onExit: () -> Unit) {
    // Mise à jour : vérifiée à chaque ouverture ; la fenêtre passe au-dessus de tous les écrans.
    LaunchedEffect(Unit) { com.kanaani.ibo.data.Updater.check() }
    // Messages instantanés (ntfy) : abonnement tant que l'application est ouverte.
    LaunchedEffect(Unit) { com.kanaani.ibo.data.Messages.run() }
    // Panneau de contrôle : à l'ouverture puis toutes les 10 minutes.
    LaunchedEffect(Unit) { while (true) { com.kanaani.ibo.data.Panel.refresh(); kotlinx.coroutines.delay(10 * 60_000L) } }
    androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.fillMaxSize()) {
        Screens(onExit)
        UpdateDialog()
        NoticeDialog()
    }
}

@Composable
private fun Screens(onExit: () -> Unit) {
    val session = Store.session
    if (session == null) {
        LoginScreen()
        return
    }
    val nav = remember(session) { Navigator() }
    // Lecteur : si un flux échoue sur ce serveur, il essaie le même flux sur l'autre (principal / secours).
    Playback.alternatesFor = { u -> session.alternates(u) }
    val top = nav.top
    BackHandler { if (!nav.back()) onExit() }
    // Quitter le Direct ou un lecteur VOD vers un autre écran : la lecture s'arrête (sauf Direct ↔ plein écran).
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    LaunchedEffect(top) {
        keyboard?.hide() // le clavier ne reste jamais ouvert d'un écran à l'autre
        if (top !is Screen.Live && top !is Screen.Play) Playback.stop()
    }
    when (top) {
        Screen.Home -> HomeScreen(nav, session)
        is Screen.Live -> LiveScreen(nav, session, top)
        is Screen.Catalog -> CatalogScreen(nav, session, top)
        is Screen.MovieDetail -> MovieDetailScreen(nav, session, top)
        is Screen.ShowDetail -> ShowDetailScreen(nav, session, top)
        // Direct plein écran : dessiné par LiveVideoHost (même surface vidéo que l'aperçu).
        is Screen.Play -> if (top.live) androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black))
            else PlayerScreen(nav, session, top)
        Screen.Favorites -> FavoritesScreen(nav, session)
        is Screen.Reorder -> ReorderScreen(session, top)
        Screen.Account -> AccountScreen(session)
        Screen.Downloads -> DownloadsScreen(nav)
        Screen.Settings -> SettingsScreen(nav, session)
    }
    LiveVideoHost(nav, session, top)
    MessageTicker(top)
}
