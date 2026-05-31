package com.example

import se.michaelthelin.spotify.SpotifyApi
import se.michaelthelin.spotify.model_objects.specification.Track
import java.io.File
import java.net.URI
import java.util.Scanner

class SpotifyService(clientId: String, clientSecret: String) {

    private val tokenFile = File("spotify_tokens.txt")
    private val spotifyApi: SpotifyApi = SpotifyApi.Builder()
        .setClientId(clientId)
        .setClientSecret(clientSecret)
        .setRedirectUri(URI.create("http://127.0.0.1:8080/callback"))
        .build()

    /**
     * Versucht zuerst, ein gespeichertes Token zu laden.
     * Falls keins existiert, wird der manuelle Link-Prozess gestartet.
     */
    fun authentifizieren() {
        if (tokenFile.exists()) {
            try {
                // Gespeichertes Refresh-Token aus der Datei lesen
                val savedRefreshToken = tokenFile.readText().trim()
                spotifyApi.refreshToken = savedRefreshToken

                // Ein frisches Access-Token von Spotify anfordern
                val clientCredentialsRequest = spotifyApi.authorizationCodeRefresh().build()
                val credentials = clientCredentialsRequest.execute()

                spotifyApi.accessToken = credentials.accessToken
                println("🔄 Automatisch via Refresh-Token eingeloggt!")
                return // Authentifizierung erfolgreich beendet!
            } catch (e: Exception) {
                println("⚠️ Automatischer Login fehlgeschlagen (${e.message}). Starte Setup neu...")
            }
        }

        // --- MANUELLER FLOW (Wird nur ausgeführt, wenn keine Datei da ist) ---
        val authorizationCodeUriRequest = spotifyApi.authorizationCodeUri()
            .scope("user-read-currently-playing")
            .show_dialog(true)
            .build()

        val authUri = authorizationCodeUriRequest.execute()

        println("🔗 Bitte öffne diesen Link EINMALIG in Firefox:")
        println(authUri)
        println("\nKopiere nach dem Akzeptieren die GESAMTE localhost-URL hier hinein:")

        val scanner = Scanner(System.`in`)
        val antwortUrl = scanner.nextLine()

        val code = antwortUrl.substringAfter("code=").substringBefore("&")

        val authorizationCodeRequest = spotifyApi.authorizationCode(code).build()
        val credentials = authorizationCodeRequest.execute()

        spotifyApi.accessToken = credentials.accessToken
        spotifyApi.refreshToken = credentials.refreshToken

        // 🔥 Das Refresh-Token für die Zukunft auf der Festplatte speichern
        tokenFile.writeText(credentials.refreshToken)
        println("✅ Spotify-Verbindung erfolgreich initialisiert und Token gespeichert!\n")
    }

    /**
     * Holt den aktuell spielenden Song.
     */
    fun getAktuellenSong(): Pair<String, String>? {
        try {
            // 1. Session-Token aktualisieren, damit Spotify uns nicht rauswirft
            val refreshRequest = spotifyApi.authorizationCodeRefresh().build()
            val credentials = refreshRequest.execute()
            spotifyApi.accessToken = credentials.accessToken

            // 2. Aktuellen Song abfragen
            val request = spotifyApi.usersCurrentlyPlayingTrack.build()
            val currentlyPlaying = request.execute()

            // 3. Prüfen, ob überhaupt Musik läuft
            if (currentlyPlaying != null && currentlyPlaying.is_playing) {
                val track = currentlyPlaying.item as? Track
                if (track != null) {
                    val artistName = track.artists[0].name
                    val standardSongName = track.name

                    println("\n🔍 [SPOTIFY DEBUG] --- Song erkannt! ---")
                    println("   Standard-Künstler: $artistName")
                    println("   Standard-Songname: '$standardSongName'")

                    var finalSongName = standardSongName

                    // --- SCHRITT A: ISRC-ABGLEICH VERSUCHEN ---
                    if (track.externalIds != null && track.externalIds.externalIds.containsKey("isrc")) {
                        val isrc = track.externalIds.externalIds["isrc"]
                        println("   ISRC-Code gefunden: $isrc")

                        try {
                            val japaSearch = spotifyApi.searchTracks("isrc:$isrc")
                                .market(com.neovisionaries.i18n.CountryCode.JP)
                                .build()
                                .execute()

                            if (japaSearch.items.isNotEmpty()) {
                                val japanTrack = japaSearch.items[0]
                                println("   Japan-Markt Ergebnis (via ISRC): '${japanTrack.name}'")

                                if (japanTrack.name != standardSongName) {
                                    println("   🎯 Treffer! Titel unterscheidet sich. Nutze Originalname: '${japanTrack.name}'")
                                    finalSongName = japanTrack.name
                                } else {
                                    println("   ℹ️ Der Name ist auch auf dem japanischen Markt identisch.")
                                }
                            } else {
                                println("   ❌ Keine Tracks unter dieser ISRC auf dem JP-Markt gefunden.")
                            }
                        } catch (e: Exception) {
                            println("   ❌ Fehler bei JP-Markt-Abfrage: ${e.message}")
                        }
                    } else {
                        // --- SCHRITT B: TEXT-FALLBACK (Wenn kein ISRC-Code da ist) ---
                        println("   ⚠️ Kein ISRC-Code hinterlegt. Starte Text-Fallback-Suche auf dem JP-Markt...")
                        try {
                            // Wir suchen im japanischen Spotify-Katalog nach "Künstler Songname"
                            val textSearch = spotifyApi.searchTracks("$artistName $standardSongName")
                                .market(com.neovisionaries.i18n.CountryCode.JP)
                                .limit(1)
                                .build()
                                .execute()

                            if (textSearch.items.isNotEmpty()) {
                                val japanTrack = textSearch.items[0]
                                println("   Japan-Markt Ergebnis (via Textsuche): '${japanTrack.name}'")

                                if (japanTrack.name != standardSongName) {
                                    println("   🎯 Treffer via Textsuche! Ändere Namen zu: '${japanTrack.name}'")
                                    finalSongName = japanTrack.name
                                } else {
                                    println("   ℹ️ Auch die Textsuche in Japan liefert denselben Namen.")
                                }
                            } else {
                                println("   ❌ Textsuche auf dem JP-Markt brachte keine Ergebnisse.")
                            }
                        } catch (e: Exception) {
                            println("   ❌ Fehler bei Text-Fallback-Suche: ${e.message}")
                        }
                    }

                    println("-----------------------------------------\n")

                    // ERFOLG: Wir geben das fertige Paar (Künstler, optimierter Songname) zurück
                    return Pair(artistName, finalSongName)
                }
            }
        } catch (e: Exception) {
            println("❌ Fehler beim Abfragen von Spotify: ${e.message}")
        }

        // FEHLSCHLAG: Wenn Spotify pausiert ist oder ein Fehler auftrat
        return null
    }
}