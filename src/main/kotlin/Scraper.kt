package com.example

import kotlinx.coroutines.*
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode



class Scraper(
    private val apiKeys: List<String>,
    private val key: Int
){

    fun getSiteScraped(htmlString: String): Elements {
        val googleDoc = Jsoup.parse(htmlString)

        // Jetzt funktioniert unser Filter perfekt, weil die echten Links im HTML existieren!
        return googleDoc.select("a")
    }

    fun getSiteGScraped(htmlString: String): Elements {
        // Wir nutzen Jsoup, um das von Selenium übergebene HTML zu parsen
        val doc = org.jsoup.Jsoup.parse(htmlString)

        // 🔍 Der magische Selector: Sucht nach allen Links innerhalb von organischen Suchtreffern
        // Google nutzt für die Haupt-Links meistens das 'jsname'-Attribut oder bestimmte Container
        return doc.select("a[href]")

    }

    fun analysiereNachGeniusErgebnissen(htmlString: String): String? {
        var finaleLyricsUrl: String? = null

        val googleLinks = getSiteGScraped(htmlString)

        println("--- Analysiere Google-Ergebnisse via Selenium ---")
        for (link in googleLinks) {
            var href = link.attr("href").trim()
            val text = link.text().trim()

            // 1. Such-Suffix prüfen (z.B. "genius.com")
            if (href.contains("genius.com")) {

                // 2. Brutales Aufräumen von Google-Redirect-Müll
                if (href.contains("/url?q=")) {
                    href = href.substringAfter("/url?q=").substringBefore("&")
                } else if (href.contains("?url=")) {
                    href = href.substringAfter("?url=").substringBefore("&")
                }

                // Falls Google Tracking-Müll hinten an die Genius-URL gehängt hat (z.B. ?ved=...)
                if (href.contains("?")) {
                    href = href.substringBefore("?")
                }

                // 3. URL dekodieren (%2F -> /)
                try {
                    href = java.net.URLDecoder.decode(href, java.nio.charset.StandardCharsets.UTF_8.toString())
                } catch (_: Exception) {}

                // 4. Absicherung des Protokolls
                if (href.startsWith("//")) href = "https:$href"

                // 5. Überprüfung: Ist es wirklich eine Lyrics-Seite?
                // Manche Genius-URLs enden nicht exakt auf "-lyrics", sondern haben noch Nummern (z.B. -lyrics-12345)
                if (href.contains("genius.com/") && !href.contains("/search") && !href.contains("/artists")) {
                    finaleLyricsUrl = href
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

    fun fetchJPopLyricsWithSelenium(artist: String, song: String): String {

        val driver = getProtectedGoogleDriver()

        // 🌟 NEU: Dem Compose-Fenster direkt die Info rüberschieben, dass der Browser jetzt läuft
        javaDriverFuerNotfall = driver

        try {
            val geniusString = """$artist "$song" ${SITE.GENIUS.searchSuffix}"""
            val utaString = """$artist "$song" ${SITE.UTA.searchSuffix}"""
            val nonString = """$artist $song ${SITE.LYRICAL_NONSENSE.searchSuffix}"""

            // === 1. SUCHE: GENIUS ===
            println("🚀 Starte getarnten Chrome-Browser...")
            println("\n Starte erste Suche mit Genius!")
            driver.get("https://www.google.com/search?q=$geniusString")
            println("Ich suche das: https://www.google.com/search?q=$geniusString")

            // Falls ein Captcha kommt Debug
            if (driver.pageSource!!.contains("g-recaptcha")) {
                println("⚠️ Google fordert ein CAPTCHA!...")
            }

            val finaleGeniusLyricsURL: String? = analysiereNachGeniusErgebnissen(driver.pageSource!!)

            if (finaleGeniusLyricsURL != null) {
                return geniusLyricsLaden(finaleGeniusLyricsURL, artist, song)
            }

            println("❌ In der Genius-Analyse wurde kein Link gefunden.")


            // === 2. SUCHE: UTA-NET (OHNE AI) ===
            println("\n Starte zweite Suche mit Uta-Net (Normal)!")
            driver.get("https://www.google.com/search?q=$utaString")
            println("Ich suche das: https://www.google.com/search?q=$utaString")

            // Falls ein Captcha kommt Debug
            if (driver.pageSource!!.contains("g-recaptcha")) {
                println("⚠️ Google fordert ein CAPTCHA!...")
            }

            val finaleUtaLyricsURL: String? = analysiereNachUtaErgebnissen(driver.pageSource!!)

            if (finaleUtaLyricsURL != null) {
                return utaLyricsLaden(finaleUtaLyricsURL, artist, song)
            }

            println("❌ Auch in der Tiefenanalyse wurde kein Uta-Net Link gefunden.")


            // === 3. SUCHE: UTA-NET (MIT AI TRANSLATION) ===
            /* Tägliches limit erreicht, aber logische Reihenfolge stimmt jetzt:

            val aiUta = Gemini().optimiereSucheMitInternetGemini(artist, song, apiKeys[key])
            val aiUtaString = """${aiUta.first} ${aiUta.second} ${SITE.UTA.searchSuffix}"""
            println("\n Starte dritte Suche mit translated Input (AI)!")
            driver.get("https://www.google.com/search?q=$aiUtaString")
            println("Ich suche das: https://www.google.com/search?q=$aiUtaString")

            if (driver.pageSource!!.contains("g-recaptcha")) {
                println("⚠️ Google fordert ein CAPTCHA!...")
            }

            val finaleAiUtaLyricsURL: String? = analysiereNachUtaErgebnissen(driver.pageSource!!)

            if (finaleAiUtaLyricsURL != null) {
                return utaLyricsLaden(finaleAiUtaLyricsURL, artist, song)
            }
            println("❌ Auch in der AI-Analyse wurde kein Uta-Net Link gefunden.")
            */

            return "AI = AUS"

        } catch (e: Exception) {
            println("❌ Fehler im Selenium-Ablauf: ${e.message}")
        } finally {
            // Ganz wichtig: Browser am Ende wieder schließen, sonst bleibt er im RAM hängen
            driver.quit()

            // 🌟 NEU: Variable wieder leeren, da der Browser zu ist
            javaDriverFuerNotfall = null
            println("🤖 Browser wieder geschlossen.")
        }
        return "NIX"
    }

    fun utaLyricsLaden(finaleUtaLyricsURL: String, artist: String, song: String):String {
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
            return cleanLyrics

        } else {
            println("❌ Songtext-Container konnte nicht gefunden werden.")
        }

        println("================================\n")
        return "NIX utaladen"
    }

    fun extrahiereGeniusLyricsMitJsoupUndAbsaetzen(htmlQuelltext: String): String {
        val doc: Document = Jsoup.parse(htmlQuelltext)
        val containers = doc.select("div[data-lyrics-container=true]")
        val lyricsBuilder = StringBuilder()

        for (container in containers) {
            for (node in container.childNodes()) {
                when (node) {
                    is TextNode -> lyricsBuilder.append(node.text())
                    is Element -> {
                        if (node.tagName() == "br") {
                            lyricsBuilder.append("\n")
                        } else {
                            lyricsBuilder.append(node.text())
                        }
                    }
                }
            }
            lyricsBuilder.append("\n\n")
        }

        val roherText = lyricsBuilder.toString()

        // 🌟 Hier filtern wir die Contributors-Zeilen heraus
        val gesaeuberterText = roherText.lines()
            .filterNot { zeile ->
                zeile.contains("Contributors", ignoreCase = true) ||
                        zeile.contains("Lyrics", ignoreCase = true) && zeile.length < 30
            }
            .joinToString("\n")

        return gesaeuberterText
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }

    fun geniusLyricsLaden(finaleGeniusLyricsURL: String, artist: String, song: String): String {
        // Lyrics-Seite über Jsoup laden
        println("Rufe Lyrics ab von: $finaleGeniusLyricsURL")
        val lyricsDoc = Jsoup.connect(finaleGeniusLyricsURL)
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .get()

        println("\n====== J-POP LYRICS FOUND ======")
        println("Künstler: $artist | Song: $song")
        println("--------------------------------")
        println(extrahiereGeniusLyricsMitJsoupUndAbsaetzen(lyricsDoc.html()))
        return extrahiereGeniusLyricsMitJsoupUndAbsaetzen(lyricsDoc.html())
    }

    fun getProtectedGoogleDriver(): ChromeDriver {
        val options = ChromeOptions()
        options.addArguments("--headless=new")

        // 🌟 AUTOMATISCH: Holt den Windows-Namen des aktuellen Nutzers (z.B. "Kaan" oder "Max")
        val windowsUser = System.getProperty("user.name")

        // Dynamischer Pfad zu den Chrome-User-Daten des jeweiligen PCs
        val chromePath = "C:\\Users\\$windowsUser\\AppData\\Local\\Google\\Chrome\\User Data"
        options.addArguments("--user-data-dir=$chromePath")
        options.addArguments("--profile-directory=Default")

        // Die restlichen Tarnungen gegen Captchas
        options.addArguments("--disable-blink-features=AutomationControlled")
        options.setExperimentalOption("excludeSwitches", listOf("enable-automation"))
        options.setExperimentalOption("useAutomationExtension", false)
        options.addArguments("--window-size=1920,1080")
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")

        val driver = ChromeDriver(options)
        return driver
    }
}