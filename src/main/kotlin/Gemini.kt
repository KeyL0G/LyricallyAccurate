package com.example

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class Gemini {


    fun optimiereSucheMitInternetGemini(rawArtist: String, rawSong: String, apiKey: String): Pair<String, String> {
        // Da eine Websuche im Hintergrund einen Moment dauern kann, erhöhen wir das Timeout auf 15 Sekunden
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

        // Schärferer Prompt: Internet-Recherche befehlen + Halluzinationen verbieten
        val prompt = """
    You are an expert J-Pop metadata translation tool. Your job is to find the official Japanese Kanji/Kana names for a given Romanized/English Spotify input using Google Search.

    Input Artist: "$rawArtist"
    Input Song: "$rawSong"

    CRITICAL WORKFLOW:
    Step 1: Use your Google Search tool to find the official Japanese artist profile on music sites (like uta-net.com, tower.jp, or ja.wikipedia.org). Identify their exact Japanese name.
    Step 2: Use your Google Search tool to find the exact Japanese song title belonging to this artist. Look for official tracklists or lyric pages.
    Step 3: Verify that BOTH the artist name and the song title are converted to their original Japanese writing (Kanji/Kana/Katakana).

    RULES:
    - You must translate BOTH fields if they exist in Japanese. 
    - Do not give up early. If you find the song title, the artist name is always on the same page. Extract both.
    - If a field is absolutely not available in Japanese (e.g. the artist officially uses an English name), only then keep the input.

    OUTPUT FORMAT:
    Respond ONLY with a raw JSON object. No markdown, no wrapping in ```json, no explanations.
    {"artist": "ACTUAL_JAPANESE_ARTIST", "song": "ACTUAL_JAPANESE_SONG"}
""".trimIndent()

        // Hier bauen wir das JSON-Objekt zusammen
        val jsonBody = JSONObject().apply {
            // 1. Der Prompt-Inhalt
            put("contents", org.json.JSONArray().put(JSONObject().apply {
                put("parts", org.json.JSONArray().put(JSONObject().apply {
                    put("text", prompt)
                }))
            }))

            // 🌐 🎯 DIE MAGIE: Hier aktivieren wir die integrierte Google-Websuche für Gemini!
            put("tools", org.json.JSONArray().put(JSONObject().apply {
                put("google_search", JSONObject())
            }))
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody(mediaType))
            .build()

        return try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            val jsonResponse = JSONObject(responseBody)

            // 🛠️ SICHERHEITS-CHECK 1: Prüfen, ob "candidates" überhaupt da ist
            if (!jsonResponse.has("candidates")) {
                println("⚠️ Google hat die JSON-Struktur geändert. Rohantwort: $responseBody")
                return Pair(rawArtist, rawSong) // Sicherer Fallback
            }

            val candidates = jsonResponse.getJSONArray("candidates")
            if (candidates.length() == 0) {
                return Pair(rawArtist, rawSong)
            }

            val candidate = candidates.getJSONObject(0)
            val content = candidate.optJSONObject("content")

            // 🛠️ SICHERHEITS-CHECK 2: Prüfen, ob die KI wegen Filtern blockiert wurde
            if (content == null || !content.has("parts")) {
                println("⚠️ Keine Antwort-Teile von Gemini generiert (evtl. Filter gegriffen).")
                return Pair(rawArtist, rawSong)
            }

            val parts = content.getJSONArray("parts")
            if (parts.length() == 0) {
                return Pair(rawArtist, rawSong)
            }

            // Den von Gemini generierten JSON-String herausholen
            val rawJsonText = parts.getJSONObject(0).getString("text").trim()

            // Falls Gemini die Markdown-Blöcke (```json ... ```) trotz Verbot mitgeliefert hat, putzen wir sie weg:
            val cleanJsonText = rawJsonText
                .replace("```json", "")
                .replace("```", "")
            .trim()

            // Das finale JSON parsen
            val resultJson = JSONObject(cleanJsonText)
            val finalArtist = resultJson.optString("artist", rawArtist)
            val finalSong = resultJson.optString("song", rawSong)

            Pair(finalArtist, finalSong)
        } catch (e: Exception) {
            println("⚠️ Fehler beim Parsen der Gemini-Antwort: ${e.message}")
            Pair(rawArtist, rawSong)
        }
    }
}