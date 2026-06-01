# Lyrically Accurate 🕵️‍♂️🎶

**Lyrically Accurate** ist eine moderne Desktop-Anwendung (gebaut mit Kotlin und Compose Multiplatform), die automatisch den aktuell auf Spotify abgespielten Song erkennt und die dazugehörigen J-Pop-Songtexte (Lyrics) per Web-Scraping ermittelt.

Das Besondere: Die App gleicht Titel über den **japanischen Musikmarkt** ab, um auch bei westlich/romanisiert formatierten Songtiteln die originalen japanischen Songtexte (z. B. in Kanji/Kana) fehlerfrei zu finden.

---

## 🌟 Features

* **Echtzeit-Spotify-Abgleich:** Erkennt automatisch und ohne Performance-Spamming, welcher Song gerade aktiv abgespielt wird.
* **Intelligente Japan-Markt-Suche:** Nutzt den ISRC-Code (International Standard Recording Code) oder eine gezielte Textsuche auf dem japanischen Spotify-Markt, um Originaltitel und -künstler (z. B. von *Zutomayo* oder *Bungei Tengoku*) zu matchen.
* **Automatisches Scraping:** Startet im Hintergrund einen unsichtbaren Selenium-Browser (Chromedriver), um die exakten Lyrics direkt abzurufen.
* **Sicheres Session-Management:** Speichert ein OAuth-Refresh-Token lokal ab. Nach dem ersten Login läuft die Verbindung bei jedem App-Start vollautomatisch im Hintergrund.
* **Zombie-Prozess-Schutz:** Beim Schließen der App über das rote "X" werden alle im Hintergrund geöffneten Webdriver-Ressourcen garantiert beendet.

---

## 🛠️ Technische Details & Stack

* **UI-Framework:** Compose Multiplatform (Desktop) mit Material Design 3 (Dark Mode)
* **Sprache:** Kotlin
* **API-Anbindung:** `se.michaelthelin.spotify` (Spotify Web API Java-Wrapper)
* **Web-Automation:** Selenium Webdriver (Chromedriver)
* **Concurrency:** Kotlin Coroutines (`Dispatchers.IO` für asynchrones Scraping ohne UI-Lag)

---

## 🚀 Setup & Installation

### Prerogative: Spotify Developer Dashboard
Um die App zu nutzen, benötigst du eigene API-Zugangsdaten von Spotify:
1. Gehe in das [Spotify Developer Dashboard](https://developer.spotify.com/).
2. Erstelle eine neue App.
3. Setze die **Redirect URI** exakt auf: `http://127.0.0.1:8080/callback`
4. Kopiere deine `Client ID` und dein `Client Secret`.

### Code anpassen
Füge deine Keys in der `Main.kt` beim Initialisieren des Services ein:
```kotlin
val spotifyService = SpotifyService("DEINE_CLIENT_ID", "DEIN_CLIENT_SECRET")
