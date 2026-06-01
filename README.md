# Lyrically Accurate 🕵️‍♂️🎶

**Lyrically Accurate** ist eine moderne Desktop-Anwendung (gebaut mit Kotlin und Compose Multiplatform), die automatisch den aktuell auf Spotify abgespielten Song erkennt und die dazugehörigen J-Pop-Songtexte (Lyrics) per Web-Scraping ermittelt.

Das Besondere: Die App gleicht Titel über den **japanischen Musikmarkt** ab, um auch bei westlich/romanisiert formatierten Songtiteln die originalen japanischen Songtexte (z. B. in Kanji/Kana) fehlerfrei zu finden.

---

## 🌟 Features

* **Echtzeit-Spotify-Abgleich:** Erkennt automatisch und ohne Performance-Spamming, welcher Song gerade aktiv abgespielt wird.
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

## 🚀 Setup & Installation (Für Tester)

Da sich die App aktuell im Spotify-Entwicklungsmodus befindet, können nur autorisierte Accounts die Spotify-Schnittstelle nutzen.

1. **Freischaltung anfordern:** Schreib mir eine kurze Nachricht mit der E-Mail-Adresse, die mit deinem Spotify-Account verknüpft ist, damit ich dich im Spotify Developer Dashboard freischalten kann.
2. **App herunterladen:** Lade dir die fertige Windows-Version direkt hier über **[Google Drive]((https://drive.google.com/drive/folders/10344WcocCe8dA2uGUTH4lyujony_r06_?hl=DE))** herunter.
3. **Starten:** Verschiebe die heruntergeladene `.exe` einfach an einen ungeschützten Ort (z. B. direkt auf deinen Desktop oder in deinen Benutzerordner) und starte sie per Doppelklick.

*Hinweis: Bitte die `.exe` nicht im Windows-Systemordner (`C:\Program Files\`) ausführen, da die App dort keine Schreibrechte hat, um die lokale Anmeldung (`spotify_tokens.txt`) zu sichern.*

---

## 📝 Erstmaliger Login-Ablauf

Die App führt dich komplett selbstständig durch den Verbindungsprozess:

1. Beim allerersten Start der App öffnet sich automatisch ein integriertes Login-Popup.
2. Klicke dort auf den Button **🔗 Im Browser einloggen**.
3. Dein Standard-Browser öffnet sich und leitet dich zu Spotify weiter. Klicke auf **Akzeptieren / Erlauben**, damit die App den aktuell spielenden Song lesen darf.
4. Nach dem Klick wirst du auf eine (leere) Localhost-Seite weitergeleitet.
5. Kopiere die **gesamte URL** aus der Adresszeile des Browsers (die Struktur sieht so aus: `http://127.0.0.1:8080/callback?code=...`).
6. Füge diese URL einfach in das Textfeld der App ein und klicke auf **Verbinden**.

**Hinweis:** Das musst du nur ein einziges Mal machen! Die App speichert ein sicheres Refresh-Token lokal auf deiner Festplatte und loggt dich ab dem nächsten Start vollautomatisch im Hintergrund ein.
