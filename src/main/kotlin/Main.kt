package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.openqa.selenium.WebDriver

var javaDriverFuerNotfall: org.openqa.selenium.WebDriver? = null

fun main() = application {
    Window(
        onCloseRequest = {
            // 🌟 BEVOR das Fenster schließt, killen wir den Browser, falls er noch offen ist
            try {
                if (javaDriverFuerNotfall != null) {
                    javaDriverFuerNotfall?.quit()
                    println("🤖 Zombie-Browser erfolgreich beim Schließen der App abgewürgt!")
                }
            } catch (e: Exception) {
                // Falls er im Hintergrund eh schon zu war
            }

            // Jetzt erst beenden wir die Compose-Anwendung
            exitApplication()
        },
        title = "Lyrically Accurate",
        state = androidx.compose.ui.window.rememberWindowState(width = 950.dp, height = 650.dp)
    ) {
        MaterialTheme(colorScheme = darkColorScheme()) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                ScraperUI()
            }
        }
    }
}

@Composable
fun ScraperUI() {
    var artistInput by remember { mutableStateOf("") }
    var songInput by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("Bereit. Leer lassen zieht aktuellen Spotify-Song!") }
    var lyricsOutput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Row(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // --- LINKE SEITE: Controls ---
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
                    statusText = "Spotify-Status wird geprüft und Browser gestartet..."
                    lyricsOutput = ""

                    // Coroutine sorgt dafür, dass die UI während des Scrapings nicht einfriert
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            // 1. Spotify Service starten
                            val clientId = "66343fabfd474311af66d606bf8cf353"
                            val clientSecret = "540f82d94f3b4fd28249343495fcf11a"
                            val spotifyService = SpotifyService(clientId, clientSecret)

                            // Authentifizierung triggern
                            spotifyService.authentifizieren()

                            // 2. Song bestimmen: Entweder aus der UI oder als Fallback von Spotify
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
                                    statusText = "📭 Spotify läuft nicht und keine UI-Eingabe vorhanden."
                                    isLoading = false
                                    return@launch // Bricht die Coroutine sauber ab
                                }
                            }

                            // 3. Scraping-Prozess deiner Klasse triggern
                            // Wichtig: 'Scraper()' muss instanziiert werden (oder 'LyricsScraper()', je nachdem wie deine Klasse heißt)
                            val scraper = Scraper(
                                listOf("AQ.Ab8RN6LVUJMk6voVAOjkfP7KINliCHxX8hQ0SerkmCfJvv373A","AQ.Ab8RN6K2A8ibpzQxiJ-WQsu_yxaVMLZHnauFXRMblx7n1oAjuQ"),
                                1
                            )

                            statusText = "Suche Lyrics für: $artist - $song..."
                            val ergebnis = scraper.fetchJPopLyricsWithSelenium(artist, song)

                            // 4. UI mit den gefundenen Lyrics füttern
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

        // --- RECHTE SEITE: Die Lyrics ---
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