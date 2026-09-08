package com.example.socialbaby.util

import android.net.Uri

object UrlParser {

    enum class Platform { YOUTUBE, ONLINE_VIDEO, ONLINE_IMAGE, GENERIC }

    data class ParsedLink(
        val platform: Platform,
        val videoId: String,
        val originalUrl: String,
        val defaultTitle: String
    )

    private val youtubeRegexes = listOf(
        Regex("""(?:youtube\.com/watch\?v=|youtu\.be/|youtube\.com/embed/|youtube\.com/shorts/)([A-Za-z0-9_-]{6,})"""),
        Regex("""youtube\.com.*[?&]v=([A-Za-z0-9_-]{6,})"""),
        Regex("""youtube\.com.*\/([A-Za-z0-9_-]{11})(?:[?&]|$)""")
    )

    private val videoExtensions = setOf("mp4", "mkv", "webm", "mov", "m3u8", "avi", "mpd")
    private val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "avif", "svg")

    fun parse(url: String): ParsedLink? {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return null
        // YouTube
        for (rx in youtubeRegexes) {
            val m = rx.find(trimmed)
            if (m != null) {
                val id = m.groupValues[1].take(11)
                if (id.length >= 6) return ParsedLink(Platform.YOUTUBE, id, trimmed, "YouTube Video $id")
            }
        }
        // Extra youtube fallback via Uri query param
        if (trimmed.contains("youtube.com") || trimmed.contains("youtu.be")) {
            extractYoutubeId(trimmed)?.let { id ->
                return ParsedLink(Platform.YOUTUBE, id, trimmed, "YouTube Video $id")
            }
            // If still not found, treat as generic web video (will load via WebView) rather than fake hash
            // This ensures embed doesn't use bogus id
            // Fall through to generic handling below but keep as ONLINE_VIDEO with full URL for WebView
        }
        // TikTok and Facebook are not supported — reject explicitly with a
        // clear message instead of mis-parsing them as generic web links.
        val lowerHost = trimmed.lowercase()
        if (lowerHost.contains("tiktok.com") || lowerHost.contains("facebook.com") || lowerHost.contains("fb.watch")) {
            return null
        }

        // Direct media detection - platform-agnostic, plays 100% in-app
        val lower = trimmed.lowercase()
        val clean = lower.substringBefore("?").substringBefore("#")
        val ext = clean.substringAfterLast(".", "").substringBefore("?")

        return when {
            ext in videoExtensions -> {
                val title = extractTitle(trimmed) ?: "Video"
                ParsedLink(Platform.ONLINE_VIDEO, trimmed, trimmed, title)
            }
            ext in imageExtensions -> {
                val title = extractTitle(trimmed) ?: "Image"
                ParsedLink(Platform.ONLINE_IMAGE, trimmed, trimmed, title)
            }
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> {
                // Generic platform link (e.g., your own site https://example.com/watch/123) — load via embedded WebView, NOT ExoPlayer
                // This fixes "configuration error 153" where HTML pages were tried as video files
                if (looksLikeImageUrl(trimmed) && !trimmed.contains("watch") && !trimmed.contains("video")) {
                    ParsedLink(Platform.ONLINE_IMAGE, trimmed, trimmed, extractTitle(trimmed) ?: "Image")
                } else {
                    // Use GENERIC so Player uses WebView (works for any site), not ExoPlayer
                    // Direct mp4/m3u8 already handled above, so remaining https is likely a page
                    ParsedLink(Platform.GENERIC, trimmed, trimmed, extractTitle(trimmed) ?: "Link")
                }
            }
            else -> null
        }
    }

    private fun extractYoutubeId(url: String): String? {
        return try {
            val uri = Uri.parse(url)
            // youtu.be/<id>
            if (uri.host?.contains("youtu.be") == true) {
                val seg = uri.lastPathSegment?.substringBefore("?")?.substringBefore("&")
                if (!seg.isNullOrBlank() && seg.matches(Regex("[A-Za-z0-9_-]{6,}"))) return seg.take(11)
            }
            // youtube.com? v= param
            uri.getQueryParameter("v")?.let { v ->
                if (v.matches(Regex("[A-Za-z0-9_-]{6,}"))) return v.take(11)
            }
            // shorts or embed last segment
            val last = uri.lastPathSegment?.substringBefore("?")?.substringBefore("&")
            if (!last.isNullOrBlank() && last.matches(Regex("[A-Za-z0-9_-]{11}"))) return last
            null
        } catch (e: Exception) { null }
    }

    private fun extractTitle(url: String): String? {
        return try {
            val withoutQuery = url.substringBefore("?").substringBefore("#")
            val last = withoutQuery.substringAfterLast("/").takeIf { it.isNotBlank() } ?: url.substringAfter("://").substringBefore("/")
            last.replace("-", " ").replace("_", " ").replace("%20", " ").take(60).ifBlank { null }
        } catch (e: Exception) { null }
    }

    private fun looksLikeImageUrl(url: String): Boolean {
        val l = url.lowercase()
        return l.contains("picsum") || l.contains("unsplash") || l.contains(".jpg") || l.contains(".png") || l.contains("image")
    }

    fun isSupported(url: String): Boolean = parse(url) != null

    fun isDirectMedia(url: String): Boolean {
        val p = parse(url) ?: return false
        return p.platform == Platform.ONLINE_VIDEO || p.platform == Platform.ONLINE_IMAGE
    }
}
