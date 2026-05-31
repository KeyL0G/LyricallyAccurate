package com.example

import kotlinx.coroutines.*
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions


fun main() = runBlocking {
    println("Programm gestartet!")

    // 1. Spotify Service mit deinen Secrets starten
    val clientId = "66343fabfd474311af66d606bf8cf353"
    val clientSecret = "540f82d94f3b4fd28249343495fcf11a"

    val spotifyService = SpotifyService(clientId, clientSecret)

    // Authentifizierung starten (Triggers den Konsolen-Input)
    spotifyService.authentifizieren()

    // 2. Aktuellen Song abfragen
    val aktuellerSong = spotifyService.getAktuellenSong()

    if (aktuellerSong != null) {
        val (artist, song) = aktuellerSong
        println("🎶 Spotify-Treffer! Starte Suche für: $artist - $song")
    } else {
        println("📭 Spotify läuft gerade nicht, ist pausiert oder der Song konnte nicht gelesen werden.")
    }

    println("Suche Test-Lyrics für: ${aktuellerSong!!.first.trim()} - ${aktuellerSong.second.trim()}")
    fetchJPopLyricsWithSelenium(aktuellerSong.first, aktuellerSong.second)
}

fun getSiteScraped(htmlString: String): Elements {
    val googleDoc = Jsoup.parse(htmlString)

    // Jetzt funktioniert unser Filter perfekt, weil die echten Links im HTML existieren!
    return googleDoc.select("a")
}

fun analysiereNachGeniusErgebnissen(htmlString: String): String? {

    var finaleLyricsUrl: String? = null

    val googleLinks = getSiteScraped(htmlString)

    println("--- Analysiere Google-Ergebnisse via Selenium ---")
    for (link in googleLinks) {
        val href = link.attr("href").trim()
        val text = link.text()

        // Google verpackt Links im modernen Layout oft in Weiterleitungen oder Daten-Pfaden
        if (href.contains("uta-net.com")) {

            // Sauber machen, falls Google einen Redirect-Müll drangehängt hat
            var saubereUrl = href
            if (href.contains("/url?q=")) {
                saubereUrl = href.substringAfter("/url?q=").substringBefore("&")
            } else if (href.contains("?url=")) {
                saubereUrl = href.substringAfter("?url=").substringBefore("&")
            }

            // URL dekodieren, falls Zeichen wie slashes verschlüsselt wurden (%2F -> /)
            try {
                saubereUrl = java.net.URLDecoder.decode(saubereUrl, java.nio.charset.StandardCharsets.UTF_8.toString())
            } catch (_: Exception) {}

            // Wir wollen nur die echte Song-Seite treffen!
            // Wenn es die japanische Seite ist auf Global umstellen
            if (saubereUrl.contains("/song/")) {
                val songId = saubereUrl.substringAfter("/song/").trimEnd('/')

                finaleLyricsUrl = "https://www.uta-net.com/global/en/lyric/$songId/"
                break
            } else if(saubereUrl.contains("/lyric/")) { //Sonst normal global Link
                // Absicherung gegen relative Protokolle
                if (saubereUrl.startsWith("//")) saubereUrl = "https:$saubereUrl"

                finaleLyricsUrl = saubereUrl
                println("🎯 Treffer im HTML entdeckt: '$text' -> $finaleLyricsUrl")
                break
            }
        }
    }
    return finaleLyricsUrl
}

fun analysiereNachUtaErgebnissen(htmlString: String): String? {

    var finaleLyricsUrl: String? = null

    val googleLinks = getSiteScraped(htmlString)


    println("--- Analysiere Google-Ergebnisse via Selenium ---")
    for (link in googleLinks) {
        val href = link.attr("href").trim()
        val text = link.text()

        // Google verpackt Links im modernen Layout oft in Weiterleitungen oder Daten-Pfaden
        if (href.contains("uta-net.com")) {

            // Sauber machen, falls Google einen Redirect-Müll drangehängt hat
            var saubereUrl = href
            if (href.contains("/url?q=")) {
                saubereUrl = href.substringAfter("/url?q=").substringBefore("&")
            } else if (href.contains("?url=")) {
                saubereUrl = href.substringAfter("?url=").substringBefore("&")
            }

            // URL dekodieren, falls Zeichen wie slashes verschlüsselt wurden (%2F -> /)
            try {
                saubereUrl = java.net.URLDecoder.decode(saubereUrl, java.nio.charset.StandardCharsets.UTF_8.toString())
            } catch (_: Exception) {}

            // Wir wollen nur die echte Song-Seite treffen!
            // Wenn es die japanische Seite ist auf Global umstellen
            if (saubereUrl.contains("/song/")) {
                val songId = saubereUrl.substringAfter("/song/").trimEnd('/')

                finaleLyricsUrl = "https://www.uta-net.com/global/en/lyric/$songId/"
                break
            } else if(saubereUrl.contains("/lyric/")) { //Sonst normal global Link
                // Absicherung gegen relative Protokolle
                if (saubereUrl.startsWith("//")) saubereUrl = "https:$saubereUrl"

                finaleLyricsUrl = saubereUrl
                println("🎯 Treffer im HTML entdeckt: '$text' -> $finaleLyricsUrl")
                break
            }
        }
    }
    return finaleLyricsUrl
}

fun fetchJPopLyricsWithSelenium(artist: String, song: String) {

    val driver = getProtectedGoogleDriver()

    try {

        val utaString = """$artist $song ${SITE.UTA.searchSuffix}"""
        val geniusString = """$artist $song ${SITE.GENIUS.searchSuffix}"""
        val nonString = """$artist $song ${SITE.LYRICAL_NONSENSE.searchSuffix}"""


        println("🚀 Starte getarnten Chrome-Browser...")
        driver.get("https://www.google.com/search?q=$utaString")
        println("Ich suche das:https://www.google.com/search?q=$utaString")

        // Falls ein Captcha kommt Debug
        if (driver.pageSource!!.contains("g-recaptcha")) {
            println("⚠️ Google fordert ein CAPTCHA!...")
        }

        val finaleUtaLyricsURL: String? = analysiereNachUtaErgebnissen(driver.pageSource!!)

        if (finaleUtaLyricsURL == null) {
            println("❌ Auch in der Tiefenanalyse wurde kein Uta-Net Link gefunden.")

            // 💡 ULTIMATIVER NOTFALL-LOG: Lass uns das HTML im Projektordner speichern,
            // um zu sehen, was Google vor uns versteckt!
            java.io.File("google_debug.html").writeText(driver.pageSource!!)
            println("📝 Debug-HTML gespeichert unter 'google_debug.html'. Schau da mal rein!")

            //Weiter mit Genius Suche
            println("\n Starte zweite Suche mit Genius!")
            driver.get("https://www.google.com/search?q=$geniusString")
            println("Ich suche das:https://www.google.com/search?q=$geniusString")

            // Falls ein Captcha kommt Debug
            if (driver.pageSource!!.contains("g-recaptcha")) {
                println("⚠️ Google fordert ein CAPTCHA!...")
            }

            val finaleGeniusLyricsURL: String? = analysiereNachGeniusErgebnissen(driver.pageSource!!)

            return
        }

        utaLyricsLaden(finaleUtaLyricsURL, artist, song)

    } catch (e: Exception) {
        println("❌ Fehler im Selenium-Ablauf: ${e.message}")
    } finally {
        // Ganz wichtig: Browser am Ende wieder schließen, sonst bleibt er im RAM hängen
        driver.quit()
        println("🤖 Browser wieder geschlossen.")
    }
}

fun utaLyricsLaden(finaleUtaLyricsURL: String, artist: String, song: String) {
    // Lyrics-Seite über Jsoup laden
    println("Rufe Lyrics ab von: $finaleUtaLyricsURL")
    val lyricsDoc = Jsoup.connect(finaleUtaLyricsURL)
        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
        .get()

    println("\n====== J-POP LYRICS FOUND ======")
    println("Künstler: $artist | Song: $song")
    println("--------------------------------")

    // Wir prüfen erst die globale ID (#kashi-area-roma) und als Backup die japanische ID (#kashi_area)
    val lyricElement = lyricsDoc.select("#kashi-area-roma, #kashi_area, .lyric-text").first()

    if (lyricElement != null) {
        // 1. Rohes HTML holen
        var rohesHtml = lyricElement.html()

        // 2. <br> Tags in Zeilenumbrüche umwandeln
        rohesHtml = rohesHtml.replace("(?i)<br\\s*/?>".toRegex(), "\n")

        // 3. Alle verbleibenden HTML-Tags löschen
        var cleanLyrics = rohesHtml.replace("<[^>]*>".toRegex(), "").trim()

        // 4. DER DOPPEL-BREAK-KILLER:
        // Ersetzt 3 oder mehr aufeinanderfolgende Zeilenumbrüche (\n\n\n...) durch maximal zwei (\n\n)
        // Das hält die Strophen getrennt, löscht aber den hässlichen XXL-Abstand!
        cleanLyrics = cleanLyrics.replace("(\\n\\s*){3,}".toRegex(), "\n\n")

        // 5. Schöne, perfekt formatierte Ausgabe
        println(cleanLyrics)

    } else {
        println("❌ Songtext-Container konnte nicht gefunden werden.")
    }

    println("================================\n")
}

fun getProtectedGoogleDriver(): ChromeDriver {
    val options = ChromeOptions()

    // Wir lassen Headless testweise aktiv!
    options.addArguments("--headless=new")

    // 🔥 DER TRICK: Hier verknüpfst du dein echtes Chrome-Profil.
    // ERSETZE 'DeinBenutzername' durch deinen echten Windows-Namen (z.B. C:\Users\max\...)
    options.addArguments("--user-data-dir=C:\\Users\\Kaan\\AppData\\Local\\Google\\Chrome\\User Data")

    // Das 'Default'-Profil ist meistens dein Hauptprofil, wo du eingeloggt bist.
    options.addArguments("--profile-directory=Default")

    // Die restlichen Tarnungen bleiben drin
    options.addArguments("--disable-blink-features=AutomationControlled")
    options.setExperimentalOption("excludeSwitches", listOf("enable-automation"))
    options.setExperimentalOption("useAutomationExtension", false)
    options.addArguments("--window-size=1920,1080")
    options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")

    val driver = ChromeDriver(options)
    return driver
}