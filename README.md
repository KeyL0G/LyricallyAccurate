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

1. **Freischaltung anfordern:** Schreibe mir eine kurze Mail/Nachricht mit der E-Mail-Adresse, die mit deinem Spotify-Account verknüpft ist.
2. **Hinzufügen abwarten:** Ich füge dich im Spotify Developer Dashboard als offiziellen Tester hinzu.
3. **App starten:** Sobald du freigeschaltet bist, kannst du die `.exe` einfach herunterladen und direkt starten – es müssen keine eigenen API-Keys im Code hinterlegt werden!

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
