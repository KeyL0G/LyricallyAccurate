package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.net.URI

var javaDriverFuerNotfall: org.openqa.selenium.WebDriver? = null

fun main() = application {

    val spotifyService = SpotifyService("66343fabfd474311af66d606bf8cf353", "540f82d94f3b4fd28249343495fcf11a")

    Window(
        onCloseRequest = {
            try {
                if (javaDriverFuerNotfall != null) {
                    javaDriverFuerNotfall?.quit()
                    println("🤖 Zombie-Browser erfolgreich beim Schließen der App abgewürgt!")
                }
            } catch (e: Exception) {
                // Ignorieren
            }
            exitApplication()
        },
        title = "Lyrically Accurate",
        state = androidx.compose.ui.window.rememberWindowState(width = 950.dp, height = 650.dp)
    ) {
        MaterialTheme(colorScheme = darkColorScheme()) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                app(spotifyService)
            }
        }
    }
}

@Composable
fun app(spotifyService: SpotifyService) {
    var zeigeLoginPopup by remember { mutableStateOf(false) }
    var eingegebeneUrl by remember { mutableStateOf("") }
    var loginFehler by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val eingeloggt = spotifyService.versucheAutomatischenLogin()
        isLoading = false
        if (!eingeloggt) {
            zeigeLoginPopup = true
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        // 🎯 HIER: Wir reichen den bereits eingeloggten Service an die UI weiter!
        scraperUI(spotifyService)
    }

    if (zeigeLoginPopup) {
        androidx.compose.ui.window.Dialog(onDismissRequest = {}) {
            Surface(
                modifier = Modifier.width(400.dp).wrapContentHeight().padding(16.dp),
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Spotify Verbindung erforderlich", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Klicke auf den Button, logge dich im Browser ein und füge die Localhost-URL hier ein:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Button(onClick = {
                        try {
                            val link = spotifyService.holeLoginLink()
                            Desktop.getDesktop().browse(URI(link))
                        } catch (e: Exception) {
                            println("Fehler beim Browser-Öffnen: ${e.message}")
                        }
                    }) {
                        Text("🔗 Im Browser einloggen")
                    }

                    OutlinedTextField(
                        value = eingegebeneUrl,
                        onValueChange = { eingegebeneUrl = it },
                        label = { Text("Localhost-URL hier rein") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (loginFehler) {
                        Text("❌ Fehler beim Verbinden. URL korrekt?", color = MaterialTheme.colorScheme.error)
                    }

                    Button(
                        onClick = {
                            val erfolg = spotifyService.verarbeiteAntwortUrl(eingegebeneUrl)
                            if (erfolg) {
                                val tokenAktiviert = spotifyService.versucheAutomatischenLogin()
                                if (tokenAktiviert) {
                                    loginFehler = false
                                    zeigeLoginPopup = false
                                } else {
                                    loginFehler = true
                                }
                            } else {
                                loginFehler = true
                            }
                        },
                        enabled = eingegebeneUrl.isNotBlank()
                    ) {
                        Text("Verbinden")
                    }
                }
            }
        }
    }
}

@Composable
fun scraperUI(spotifyService: SpotifyService) { // 🎯 HIER: Parameter hinzugefügt
    var artistInput by remember { mutableStateOf("") }
    var songInput by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("Bereit. Leer lassen zieht aktuellen Spotify-Song!") }
    var lyricsOutput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Row(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column(modifier = Modifier.weight(1.2f).padding(end = 8.dp)) {
            Text("Lyrics Detektiv", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = artistInput,
                onValueChange = { artistInput = it },
                label = { Text("Künstler (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = songInput,
                onValueChange = { songInput = it },
                label = { Text("Song (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    isLoading = true
                    statusText = "Browser wird gestartet..."
                    lyricsOutput = ""

                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            // 🎯 HIER: Die doppelten Zeilen (val clientId = ...) wurden gelöscht!
                            // Wir nutzen jetzt direkt das übergebene 'spotifyService' Objekt.

                            val artist: String
                            val song: String

                            if (artistInput.isNotBlank() && songInput.isNotBlank()) {
                                artist = artistInput.trim()
                                song = songInput.trim()
                                statusText = "Nutze UI-Eingabe: $artist - $song..."
                            } else {
                                statusText = "Frage aktuellen Spotify-Song ab..."
                                val aktuellerSong = spotifyService.getAktuellenSong()

                                if (aktuellerSong != null) {
                                    artist = aktuellerSong.first.trim()
                                    song = aktuellerSong.second.trim()
                                    statusText = "🎶 Spotify-Treffer: $artist - $song. Starte Suche..."
                                } else {
                                    statusText = "📭 Spotify läuft nicht oder ist pausiert."
                                    isLoading = false
                                    return@launch
                                }
                            }

                            // 3. Scraping-Prozess deiner Klasse triggern
                            val scraper = Scraper(
                                listOf("AQ.Ab8RN6LVUJMk6voVAOjkfP7KINliCHxX8hQ0SerkmCfJvv373A","AQ.Ab8RN6K2A8ibpzQxiJ-WQsu_yxaVMLZHnauFXRMblx7n1oAjuQ"),
                                1
                            )

                            statusText = "Suche Lyrics für: $artist - $song..."
                            val ergebnis = scraper.fetchJPopLyricsWithSelenium(artist, song)

                            lyricsOutput = ergebnis
                            statusText = "Suche erfolgreich beendet! 🎉"

                        } catch (e: Exception) {
                            statusText = "⚠️ Fehler: ${e.message}"
                            lyricsOutput = ""
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Prozess starten 🚀")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Text(statusText, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
            }
        }

        Column(modifier = Modifier.weight(1.8f).padding(start = 8.dp)) {
            Text("Gefundene Lyrics", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxSize(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
                    Text(
                        text = if (lyricsOutput.isBlank()) "Noch keine Lyrics geladen..." else lyricsOutput,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

//val scraper = Scraper(
//    listOf("AQ.Ab8RN6LVUJMk6voVAOjkfP7KINliCHxX8hQ0SerkmCfJvv373A","AQ.Ab8RN6K2A8ibpzQxiJ-WQsu_yxaVMLZHnauFXRMblx7n1oAjuQ"),
//    1
//)

//    println("Programm gestartet!")
//
//    // 1. Spotify Service mit deinen Secrets starten
//    val clientId = "66343fabfd474311af66d606bf8cf353"
//    val clientSecret = "540f82d94f3b4fd28249343495fcf11a"
//
//    val spotifyService = SpotifyService(clientId, clientSecret)
//
//    // Authentifizierung starten (Triggers den Konsolen-Input)
//    spotifyService.authentifizieren()
//
//    // 2. Aktuellen Song abfragen
//    val aktuellerSong = spotifyService.getAktuellenSong()
//
//    if (aktuellerSong != null) {
//        val (artist, song) = aktuellerSong
//        println("🎶 Spotify-Treffer! Starte Suche für: $artist - $song")
//    } else {
//        println("📭 Spotify läuft gerade nicht, ist pausiert oder der Song konnte nicht gelesen werden.")
//    }
//
//    println("Suche Test-Lyrics für: ${aktuellerSong!!.first.trim()} - ${aktuellerSong.second.trim()}")
//    scraper.fetchJPopLyricsWithSelenium(aktuellerSong.first, aktuellerSong.second)