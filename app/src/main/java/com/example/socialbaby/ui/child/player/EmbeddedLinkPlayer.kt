package com.example.socialbaby.ui.child.player

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.util.Log
import android.os.Build
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EmbeddedLinkPlayer(
    videoId: String,
    platform: String, // YOUTUBE, YOUTUBE_FULL, GENERIC
    externalUrl: String? = null,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = true,
    isMuted: Boolean = true,
    isCurrentPage: Boolean = true
) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var triedFallback by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    // Blank-page detector (stripped YouTube path): the page reported "finished"
    // but painted nothing (white screen). A JS probe checks body text seconds
    // later; empty means stuck — show Retry instead of mystery white.
    var blankPage by remember { mutableStateOf(false) }
    // Temporary diagnostics: shown on failure screens so one screenshot
    // identifies the exact failing stage (platform • host • readyState • chars).
    var pageDiag by remember { mutableStateOf<String?>(null) }

    // Always unmuted as requested — app + embedded both unmuted (user wants no mute)
    val embedUrl = remember(videoId, platform, externalUrl) {
        when (platform) {
            "YOUTUBE" -> {
                val id = videoId.trim()
                if (id.matches(Regex("[A-Za-z0-9_-]{6,}")) && id.length in 6..20) {
                    // Regular youtube.com embed (not nocookie): nocookie embeds show
                    // "configuration error" screens on some devices/WebViews.
                    // No origin param — this page loads top-level, so any declared
                    // origin would mismatch and be rejected the same way.
                    // enablejsapi=1 stays: our mute/play postMessages need it.
                    "https://www.youtube.com/embed/$id?playsinline=1&rel=0&modestbranding=1&controls=1&iv_load_policy=3&autoplay=1&mute=0&enablejsapi=1"
                } else {
                    externalUrl ?: "https://www.youtube.com/watch?v=$id"
                }
            }
            "YOUTUBE_FULL" -> {
                // Stripped full-page mode for embedding-disabled videos: load the
                // original watch/shorts URL, then hide all YouTube chrome via CSS
                // and block every navigation that leaves this video.
                externalUrl ?: videoId
            }
            "GENERIC" -> externalUrl ?: videoId
            else -> externalUrl ?: videoId
        }
    }

    // CSS that hides YouTube chrome on watch + shorts pages: top bar (logo,
    // search), up-next rail, comments, subscribe/share buttons, end cards,
    // shorts nav arrows. The <video> element itself is never hidden.
    // Re-applied periodically because YouTube is an SPA that mutates the DOM.
    val youtubeStripCss = remember {
        ("#masthead-container,#masthead,ytd-masthead," +
            "ytd-searchbox,#search,#search-icon-legacy," +
            "#secondary,#secondary-inner," +
            "#comments,ytd-comments," +
            "#subscribe-button,ytd-subscribe-button-renderer," +
            "ytd-watch-metadata #actions,ytd-video-primary-info-renderer #actions," +
            "ytd-menu-renderer.ytd-video-primary-info-renderer," +
            ".ytp-youtube-button,.ytp-chrome-top-buttons,.ytp-cards-button," +
            ".ytp-ce-element,.ytp-cards-teaser,.ytp-endscreen-content," +
            "#navigation-button-down,#navigation-button-up,.navigation-container," +
            "ytd-guide-button-renderer,#guide-button," +
            "ytd-reel-shelf-renderer,ytd-rich-shelf-renderer"
            ).trimIndent()
    }
    val youtubeStripJs = remember(youtubeStripCss) {
        "(function(){try{" +
            "var css='$youtubeStripCss{display:none !important;}';" +
            "var s=document.getElementById('kidtube-strip');" +
            "if(!s){s=document.createElement('style');s.id='kidtube-strip';document.head.appendChild(s);}" +
            "s.textContent=css;" +
            "}catch(e){}})();"
    }

    // YouTube 152/153 ("This video is unavailable") = missing/invalid Referer.
    // A bare WebView loadUrl() sends no referrer context, so YouTube rejects the
    // embed. The documented Android fix: load an HTML shell holding the iframe
    // via loadDataWithBaseURL with a real youtube.com base + referrerpolicy.
    val youtubeShellHtml: String? = remember(videoId, platform) {
        if (platform != "YOUTUBE") return@remember null
        val id = videoId.trim()
        if (!id.matches(Regex("[A-Za-z0-9_-]{6,}")) || id.length !in 6..20) return@remember null
        """
        <!DOCTYPE html><html><head>
        <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
        <meta name="referrer" content="strict-origin-when-cross-origin">
        <style>html,body{margin:0;padding:0;background:#000;height:100%;overflow:hidden}iframe{width:100%;height:100%;border:0;display:block}</style>
        </head><body>
        <iframe src="https://www.youtube-nocookie.com/embed/$id?playsinline=1&rel=0&modestbranding=1&controls=1&iv_load_policy=3&autoplay=1&mute=0&enablejsapi=1"
        allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; fullscreen"
        referrerpolicy="strict-origin-when-cross-origin" allowfullscreen></iframe>
        </body></html>
        """.trimIndent()
    }

    // Single choke point for every WebView load in this player.
    fun WebView.loadTarget(url: String) {
        val shell = youtubeShellHtml
        if (shell != null && (url.contains("youtube.com/embed/") || url.contains("youtube-nocookie.com/embed/"))) {
            loadDataWithBaseURL("https://www.youtube.com", shell, "text/html", "utf-8", null)
        } else {
            loadUrl(url)
        }
    }

    // Track current URL for fallback logic
    var currentUrl by remember(embedUrl) { mutableStateOf(embedUrl) }
    // Last URL WE asked the WebView to load. Never compare against wv.url:
    // SPA pages (YouTube watch pages) rewrite their own URL via pushState while
    // booting — comparing wv.url caused an infinite reload loop (white screen).
    var lastLoadedUrl by remember(embedUrl) { mutableStateOf(embedUrl) }
    LaunchedEffect(embedUrl) {
        currentUrl = embedUrl
        triedFallback = false
        error = null
        blankPage = false
        pageDiag = null
        loading = true
    }

    Box(modifier.background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    webViewRef = this
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        allowFileAccessFromFileURLs = true
                        allowUniversalAccessFromFileURLs = true
                        javaScriptCanOpenWindowsAutomatically = true
                        setSupportMultipleWindows(false) // block popups that would leave app
                        mediaPlaybackRequiresUserGesture = false
                        // LOAD_CACHE_ELSE_NETWORK speeds up reloads (fixes huge load time)
                        cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        builtInZoomControls = false
                        displayZoomControls = false
                        setSupportZoom(false)
                        blockNetworkImage = false
                        loadsImagesAutomatically = true
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    }
                    CookieManager.getInstance().setAcceptCookie(true)
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false
                    webChromeClient = object : WebChromeClient() {
                        override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message?): Boolean {
                            return false
                        }
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            loading = true
                            if (!triedFallback) error = null
                        }
                        override fun onPageFinished(view: WebView?, url: String?) {
                            loading = false
                            // Stripped YouTube page: hide chrome immediately and keep it
                            // hidden (SPA re-renders). Video element itself untouched.
                            if (platform == "YOUTUBE_FULL") {
                                view?.evaluateJavascript(youtubeStripJs, null)
                                // Blank-page probe: if the finished page still has no
                                // readable text seconds later, it never rendered.
                                // Also records readyState + host for the on-screen
                                // diagnostics line, so one screenshot identifies it.
                                CoroutineScope(Dispatchers.Main).launch {
                                    delay(8000)
                                    try {
                                        view?.evaluateJavascript(
                                            "(function(){try{var b=document.body;if(!b)return 'none|-1';var t=(b.innerText||'').replace(/\\s+/g,'');return (document.readyState||'?')+'|'+t.length;}catch(e){return 'err|-1';}})();"
                                        ) { result ->
                                            try {
                                                val raw = (result ?: "").trim().trim('"')
                                                val parts = raw.split("|")
                                                val rs = parts.getOrNull(0) ?: "?"
                                                val len = parts.getOrNull(1)?.toIntOrNull() ?: -1
                                                val host = try {
                                                    android.net.Uri.parse(currentUrl).host ?: "?"
                                                } catch (_: Exception) { "?" }
                                                pageDiag = "full:$host • $rs • chars=$len"
                                                if (len == 0) blankPage = true
                                            } catch (_: Exception) {}
                                        }
                                    } catch (_: Exception) {}
                                }
                            }
                            // Use coroutine delay instead of Handler (fixes leak)
                            CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                delay(600)
                                if (isCurrentPage) {
                                    if (isMuted) {
                                        view?.evaluateJavascript("(function(){try{ document.querySelectorAll('video').forEach(v=>{v.muted=true; v.volume=0;}); var auds=document.querySelectorAll('audio');auds.forEach(a=>a.muted=true);}catch(e){}})();", null)
                                        view?.evaluateJavascript("try{ var ifr=document.querySelector('iframe'); if(ifr) ifr.contentWindow.postMessage('{\"event\":\"command\",\"func\":\"mute\",\"args\":[]}', '*'); }catch(e){}", null)
                                    } else {
                                        view?.evaluateJavascript("(function(){try{ document.querySelectorAll('video').forEach(v=>{v.muted=false; v.volume=1; v.play().catch(()=>{});}); var auds=document.querySelectorAll('audio');auds.forEach(a=>{a.muted=false;});}catch(e){}})();", null)
                                        view?.evaluateJavascript("try{ var ifr=document.querySelector('iframe'); if(ifr){ ifr.contentWindow.postMessage('{\"event\":\"command\",\"func\":\"unMute\",\"args\":[]}', '*'); ifr.contentWindow.postMessage('{\"event\":\"command\",\"func\":\"playVideo\",\"args\":[]}', '*'); } }catch(e){}", null)
                                        view?.evaluateJavascript("(function(){try{ var ifr=document.querySelector('iframe'); if(ifr && ifr.contentDocument){ var v=ifr.contentDocument.querySelector('video'); if(v){v.muted=false; v.volume=1; v.play().catch(()=>{});} } }catch(e){}})();", null)
                                    }
                                } else {
                                    view?.evaluateJavascript("(function(){try{ document.querySelectorAll('video').forEach(v=>v.pause()); }catch(e){}})();", null)
                                    view?.evaluateJavascript("try{ var ifr=document.querySelector('iframe'); if(ifr) ifr.contentWindow.postMessage('{\"event\":\"command\",\"func\":\"pauseVideo\",\"args\":[]}', '*'); }catch(e){}", null)
                                }
                            }
                        }
                        // Shared handling for both error callbacks. minSdk is 24, so the
                        // modern callback below is always the one Android calls for
                        // main-frame failures — the deprecated one alone misses them
                        // entirely (page "finishes" blank with no error shown).
                        fun handleLoadError(view: WebView?, errorCode: Int?, description: String?, failingUrl: String?) {
                            Log.w("KidTubePlayer", "WV error code=$errorCode desc=$description url=$failingUrl platform=$platform")
                            // Deep-link schemes (fb://fullscreen_video, intent://, tel:) can never
                            // load in WebView (net::ERR_UNKNOWN_URL_SCHEME). Ignore silently,
                            // stay on video.
                            if (failingUrl != null && !failingUrl.startsWith("http://") && !failingUrl.startsWith("https://")) {
                                loading = false
                                return
                            }
                            if (!triedFallback && externalUrl != null && failingUrl != externalUrl &&
                                platform == "YOUTUBE" && failingUrl?.contains("youtube.com/embed") == true
                            ) {
                                triedFallback = true
                                currentUrl = externalUrl
                                lastLoadedUrl = externalUrl
                                view?.loadTarget(externalUrl)
                                return
                            }
                            loading = false
                            blankPage = false
                            error = description ?: ("Load failed " + (errorCode?.toString() ?: ""))
                        }
                        @Deprecated("Deprecated in Java")
                        override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                            handleLoadError(view, errorCode, description, failingUrl)
                        }
                        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                            // Sub-frame noise (ads/trackers) must never kill the page.
                            if (request != null && !request.isForMainFrame) return
                            handleLoadError(view, error?.errorCode, error?.description?.toString(), request?.url?.toString())
                        }
                        // Block taps on YouTube logo / channel / watch links from leaving the app.
                        // NOTE: correct name is shouldOverrideUrlLoading (not shouldOverrideUrlRequest).
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            // Sub-frame loads (player iframes nested inside embeds) must
                            // never be blocked — only top-level navigations can leave the
                            // app. Blocking sub-frames breaks nested players (black screen).
                            if (request != null && !request.isForMainFrame) return false
                            val url = request?.url?.toString() ?: return false
                            return shouldBlockNavigation(url)
                        }
                        @Deprecated("Deprecated in Java")
                        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                            if (url == null) return false
                            return shouldBlockNavigation(url)
                        }
                        private fun shouldBlockNavigation(url: String): Boolean {
                            val u = url.lowercase()
                            // App deep links (intent://, tel:, mailto:, custom schemes) —
                            // consume silently so WebView never shows ERR_UNKNOWN_URL_SCHEME.
                            if (!u.startsWith("http://") && !u.startsWith("https://")) {
                                Log.d("KidTubePlayer", "WV blocked scheme: ${url.take(90)}")
                                return true
                            }
                            // Never leave the app for the Play Store. Stay on the video.
                            if (u.contains("play.google.com")) {
                                Log.d("KidTubePlayer", "WV blocked store: ${url.take(90)}")
                                return true
                            }
                            // Stripped YouTube page: only this video may load. Block home,
                            // search, channels, other videos/shorts — child can never
                            // wander off the parent-approved video.
                            if (platform == "YOUTUBE_FULL") {
                                // Video IDs are case-SENSITIVE base64: compare against the
                                // lowercased id too, or every navigation (including
                                // YouTube's own www->m redirect) gets killed and the
                                // page stays blank white.
                                val id = videoId.trim().lowercase(java.util.Locale.ROOT)
                                if (!u.contains("youtube.com") && !u.contains("youtu.be") &&
                                    !u.contains("googlevideo.com") && !u.contains("ytimg.com") &&
                                    !u.contains("gstatic.com") && !u.contains("google.com")
                                ) return false
                                // Always allow the mobile-host boot redirect of this video.
                                if (u.contains("m.youtube.com/watch") && u.contains(id)) return false
                                return !u.contains(id)
                            }
                            return when (platform) {
                                "YOUTUBE" -> {
                                    // Stay inside youtube-nocookie embed; block watch/channel/user/search leaving embed
                                    u.contains("youtube.com/watch") ||
                                        u.contains("m.youtube.com/watch") ||
                                        u.contains("youtube.com/channel/") ||
                                        u.contains("youtube.com/c/") ||
                                        u.contains("youtube.com/user/") ||
                                        u.contains("youtube.com/@") ||
                                        u.contains("youtube.com/results") ||
                                        u.contains("youtu.be/")
                                }
                                else -> false // GENERIC: allow in-WebView navigation (your own site)
                            }
                        }
                    }
                    // NOTE: no forced LAYER_TYPE_HARDWARE here. Forcing a hardware
                    // layer is a known cause of blank-white WebViews on several
                    // Samsung/Mali GPUs; the activity is already hardware-
                    // accelerated via the manifest, which is sufficient.
                    try {
                        val host = android.net.Uri.parse(currentUrl).host ?: currentUrl.take(60)
                        Log.d("KidTubePlayer", "WV load platform=$platform host=$host")
                    } catch (_: Exception) {}
                    loadTarget(currentUrl)
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { wv ->
                webViewRef = wv
                // Reload ONLY when our requested URL changed — never because the
                // page rewrote its own URL (YouTube SPA does this on boot).
                if (currentUrl != lastLoadedUrl) {
                    lastLoadedUrl = currentUrl
                    wv.loadTarget(currentUrl)
                } else {
                    // Immediate play/pause/mute handling on swipe (fixes previous sound overlap + next paused need tap)
                    if (isCurrentPage) {
                        if (isMuted) {
                            wv.evaluateJavascript("(function(){try{ document.querySelectorAll('video').forEach(v=>{v.muted=true; v.volume=0; v.pause();}); }catch(e){}})();", null)
                            wv.evaluateJavascript("try{ var ifr=document.querySelector('iframe'); if(ifr) ifr.contentWindow.postMessage('{\"event\":\"command\",\"func\":\"mute\",\"args\":[]}', '*'); }catch(e){}", null)
                        } else {
                            wv.evaluateJavascript("(function(){try{ document.querySelectorAll('video').forEach(v=>{v.muted=false; v.volume=1; v.play().catch(()=>{});}); }catch(e){}})();", null)
                            wv.evaluateJavascript("try{ var ifr=document.querySelector('iframe'); if(ifr){ ifr.contentWindow.postMessage('{\"event\":\"command\",\"func\":\"unMute\",\"args\":[]}', '*'); ifr.contentWindow.postMessage('{\"event\":\"command\",\"func\":\"playVideo\",\"args\":[]}', '*'); } }catch(e){}", null)
                        }
                    } else {
                        wv.evaluateJavascript("(function(){try{ document.querySelectorAll('video').forEach(v=>v.pause()); }catch(e){}})();", null)
                        wv.evaluateJavascript("try{ var ifr=document.querySelector('iframe'); if(ifr) ifr.contentWindow.postMessage('{\"event\":\"command\",\"func\":\"pauseVideo\",\"args\":[]}', '*'); }catch(e){}", null)
                    }
                }
            }
        )

        // YOUTUBE_FULL: swallow taps on YouTube's own chrome (top logo/search/
        // menu bar + right action rail) so a child can never tap out to YouTube.
        // Transparent native overlays sit above the WebView but below our own
        // loading/error UIs and the app's buttons (drawn later, on top).
        // Navigation blocking in shouldOverrideUrlLoading remains as second layer.
        if (platform == "YOUTUBE_FULL") {
            // Top bar: YouTube logo + search + overflow menu.
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(64.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { }
            )
            // Right action rail: like / comments / share / sound buttons.
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(64.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { }
            )
        }

        // Handle Shorts swipe: ensure always unmuted when current — use coroutine delay (fixes Handler)
        LaunchedEffect(isCurrentPage, isMuted) {
            kotlinx.coroutines.delay(120)
            webViewRef?.let { wv ->
                if (isCurrentPage) {
                    if (isMuted) {
                        wv.evaluateJavascript("(function(){try{ document.querySelectorAll('video').forEach(v=>{v.muted=true; v.volume=0;}); }catch(e){}})();", null)
                        wv.evaluateJavascript("try{ var ifr=document.querySelector('iframe'); if(ifr) ifr.contentWindow.postMessage('{\"event\":\"command\",\"func\":\"mute\",\"args\":[]}', '*'); }catch(e){}", null)
                    } else {
                        wv.evaluateJavascript("(function(){try{ document.querySelectorAll('video').forEach(v=>{v.muted=false; v.volume=1; v.play().catch(()=>{});}); var auds=document.querySelectorAll('audio');auds.forEach(a=>{a.muted=false;}); }catch(e){}})();", null)
                        wv.evaluateJavascript("try{ var ifr=document.querySelector('iframe'); if(ifr){ ifr.contentWindow.postMessage('{\"event\":\"command\",\"func\":\"unMute\",\"args\":[]}', '*'); ifr.contentWindow.postMessage('{\"event\":\"command\",\"func\":\"playVideo\",\"args\":[]}', '*'); } }catch(e){}", null)
                        wv.evaluateJavascript("(function(){try{ var ifr=document.querySelector('iframe'); if(ifr && ifr.contentDocument){ var v=ifr.contentDocument.querySelector('video'); if(v){v.muted=false; v.volume=1; v.play().catch(()=>{});} } }catch(e){}})();", null)
                    }
                } else {
                    wv.evaluateJavascript("(function(){try{ document.querySelectorAll('video').forEach(v=>v.pause()); }catch(e){}})();", null)
                    wv.evaluateJavascript("try{ var ifr=document.querySelector('iframe'); if(ifr) ifr.contentWindow.postMessage('{\"event\":\"command\",\"func\":\"pauseVideo\",\"args\":[]}', '*'); }catch(e){}", null)
                }
            }
        }

        // Keep unmuted persistent — fixes "after unmute it mute after some second" (WebView/YouTube re-mutes on loop/buffer)
        LaunchedEffect(isCurrentPage, isMuted) {
            while (isCurrentPage && !isMuted) {
                kotlinx.coroutines.delay(1800)
                webViewRef?.let { wv ->
                    wv.evaluateJavascript("(function(){try{ document.querySelectorAll('video').forEach(v=>{ if(v.muted){v.muted=false; v.volume=1; v.play().catch(()=>{});} }); }catch(e){}})();", null)
                    wv.evaluateJavascript("try{ var ifr=document.querySelector('iframe'); if(ifr) ifr.contentWindow.postMessage('{\"event\":\"command\",\"func\":\"unMute\",\"args\":[]}', '*'); }catch(e){}", null)
                }
            }
        }

        // Keep YouTube chrome stripped on the full-page fallback (SPA re-renders).
        LaunchedEffect(isCurrentPage) {
            if (platform == "YOUTUBE_FULL") {
                while (isCurrentPage) {
                    kotlinx.coroutines.delay(2500)
                    webViewRef?.evaluateJavascript(youtubeStripJs, null)
                }
            }
        }

        if (loading) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        error?.let { msg ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.88f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Video can't load", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text(msg, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text("Tried: $currentUrl", color = Color.White.copy(alpha = 0.45f), fontSize = 9.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            error = null
                            loading = true
                            triedFallback = false
                            currentUrl = embedUrl
                            lastLoadedUrl = embedUrl
                            webViewRef?.loadTarget(embedUrl)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                    ) { Text("Retry") }
                    if (externalUrl != null && externalUrl != currentUrl) {
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                error = null
                                loading = true
                                triedFallback = true
                                currentUrl = externalUrl
                                lastLoadedUrl = externalUrl
                                webViewRef?.loadTarget(externalUrl)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f), contentColor = Color.White)
                        ) { Text("Open original link", fontSize = 12.sp) }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Tip: Copy link directly from YouTube → Share → Copy link.", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp, textAlign = TextAlign.Center)
                }
            }
        }

        if (blankPage && error == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Page didn't load properly", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "The video page stayed blank. Check internet and try again.",
                        color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp, textAlign = TextAlign.Center
                    )
                    pageDiag?.let { diag ->
                        Spacer(Modifier.height(6.dp))
                        Text(diag, color = Color.White.copy(alpha = 0.45f), fontSize = 9.sp, textAlign = TextAlign.Center)
                    }
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = {
                            blankPage = false
                            loading = true
                            lastLoadedUrl = currentUrl
                            webViewRef?.loadTarget(currentUrl)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                    ) { Text("Retry") }
                }
            }
        }

        // Debug hint (parent can see what was pasted)
        if (!loading && error == null) {
            // Small overlay showing platform for debugging — remove in production if not needed
            // Keep subtle
        }
    }
}
