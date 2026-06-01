package com.example

import se.michaelthelin.spotify.SpotifyApi
import se.michaelthelin.spotify.model_objects.specification.Track
import java.io.File
import java.net.URI

class SpotifyService(clientId: String, clientSecret: String) {

    private val tokenFile = File("spotify_tokens.txt")
    private val spotifyApi: SpotifyApi = SpotifyApi.Builder()
        .setClientId(clientId)
        .setClientSecret(clientSecret)
        .setRedirectUri(URI.create("http://127.0.0.1:8080/callback"))
        .build()

    /**
     * Versucht den automatischen Login über das gespeicherte Refresh-Token.
     */
    fun versucheAutomatischenLogin(): Boolean {
        if (tokenFile.exists()) {
            try {
                val savedRefreshToken = tokenFile.readText().trim()
                if (savedRefreshToken.isEmpty()) {
                    tokenFile.delete()
                    return false
                }

                // 1. WICHTIG: Setze Client-Daten und Refresh-Token
                spotifyApi.refreshToken = savedRefreshToken

                // 2. Den Request bauen
                val refreshRequest = spotifyApi.authorizationCodeRefresh().build()
                val credentials = refreshRequest.execute()

                if (credentials.accessToken != null) {
                    spotifyApi.accessToken = credentials.accessToken
                    println("🔄 Automatisch via Refresh-Token eingeloggt! (Token: ${credentials.accessToken.take(10)}...)")

                    if (credentials.refreshToken != null) {
                        spotifyApi.refreshToken = credentials.refreshToken
                        tokenFile.writeText(credentials.refreshToken)
                    }
                    return true
                }
            } catch (e: Exception) {
                println("⚠️ Automatischer Login fehlgeschlagen (${e.message}). Lösche ungültiges Token...")
                try {
                    tokenFile.delete()
                } catch (delEx: Exception) {}
            }
        }
        return false
    }

    /**
     * Generiert den Spotify-Link für das Compose-Popup mit den RICHTIGEN Scopes.
     */
    fun holeLoginLink(): String {
        // 🎯 HIER: Wir fordern explizit die Berechtigung an, den aktuellen Song zu lesen!
        val authorizationCodeUriRequest = spotifyApi.authorizationCodeUri()
            .scope("user-read-currently-playing")
            .show_dialog(true) // Zwingt Spotify, dir den "Erlauben"-Button anzuzeigen
            .build()
        return authorizationCodeUriRequest.execute().toString()
    }

    /**
     * 🌟 NEU: Verarbeitet die vom User eingefügte Localhost-URL und speichert das Token.
     */
    fun verarbeiteAntwortUrl(antwortUrl: String): Boolean {
        return try {
            val code = antwortUrl.substringAfter("code=").substringBefore("&")
            val authorizationCodeRequest = spotifyApi.authorizationCode(code).build()
            val credentials = authorizationCodeRequest.execute()

            spotifyApi.accessToken = credentials.accessToken
            spotifyApi.refreshToken = credentials.refreshToken

            // Token für die Zukunft sichern
            tokenFile.writeText(credentials.refreshToken)
            println("✅ Spotify-Verbindung erfolgreich initialisiert und Token gespeichert!")
            true
        } catch (e: Exception) {
            println("❌ Fehler beim Verarbeiten der URL: ${e.message}")
            false
        }
    }

    /**
     * Holt den aktuell spielenden Song (Sichere Version ohne Spamming).
     */
    fun getAktuellenSong(): Pair<String, String>? {
        var versuche = 0
        val maximaleVersuche = 2

        while (versuche < maximaleVersuche) {
            try {
                val request = spotifyApi.usersCurrentlyPlayingTrack.build()
                val currentlyPlaying = request.execute()

                // Wenn die API antwortet, aber aktuell einfach keine Musik läuft
                if (currentlyPlaying == null || !currentlyPlaying.is_playing) {
                    return null
                }

                val track = currentlyPlaying.item as? Track
                if (track != null) {
                    val artistName = track.artists[0].name
                    val standardSongName = track.name

                    println("\n🔍 [SPOTIFY DEBUG] --- Song erkannt! ---")
                    println("   Standard-Künstler: $artistName")
                    println("   Standard-Songname: '$standardSongName'")

                    var finalSongName = standardSongName

                    // --- SCHRITT A: ISRC-ABGLEICH ---
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
                                }
                            }
                        } catch (e: Exception) {
                            println("   ❌ Fehler bei JP-Markt-Abfrage: ${e.message}")
                        }
                    } else {
                        // --- SCHRITT B: TEXT-FALLBACK ---
                        println("   ⚠️ Kein ISRC-Code hinterlegt. Starte Text-Fallback-Suche auf dem JP-Markt...")
                        try {
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
                                }
                            }
                        } catch (e: Exception) {
                            println("   ❌ Fehler bei Text-Fallback-Suche: ${e.message}")
                        }
                    }

                    println("-----------------------------------------\n")
                    return Pair(artistName, finalSongName)
                }

                // Falls 'item' kein Track war (z.B. Podcast), Loop beenden
                break

            } catch (e: se.michaelthelin.spotify.exceptions.SpotifyWebApiException) {
                val ist401 =
                    e.message?.contains("401") == true || e.message?.contains("unauthorized", ignoreCase = true) == true

                if (ist401 && versuche == 0) {
                    println("🔄 Access-Token abgelaufen. Erneuere Session einmalig...")
                    try {
                        val refreshRequest = spotifyApi.authorizationCodeRefresh().build()
                        val credentials = refreshRequest.execute()

                        if (credentials.accessToken != null) {
                            spotifyApi.accessToken = credentials.accessToken
                            versuche++
                            continue // Springt zurück an den Anfang der While-Schleife
                        }
                    } catch (refreshEx: Exception) {
                        println("❌ Notfall-Refresh fehlgeschlagen: ${refreshEx.message}")
                        break
                    }
                } else {
                    println("❌ Spotify-API-Fehler: ${e.message}")
                    break
                }
            } catch (e: Exception) {
                println("❌ Allgemeiner Netzwerkfehler beim Abfragen von Spotify: ${e.message}")
                break
            }
        }
        return null // Fallback, falls die Schleife ohne Return durchbricht
    }
}