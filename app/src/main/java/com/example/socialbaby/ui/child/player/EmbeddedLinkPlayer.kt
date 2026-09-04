package com.example.socialbaby.ui.child.player

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Build
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
    platform: String, // YOUTUBE, TIKTOK, FACEBOOK, GENERIC
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

    // Build embed URL — always muted autoplay for preload (fixes huge load time), unmute via JS when current
    // isMuted/autoPlay are handled via JS after load, not via URL reload, to avoid reload on swipe
    val embedUrl = remember(videoId, platform, externalUrl) {
        when (platform) {
            "YOUTUBE" -> {
                val id = videoId.trim()
                if (id.matches(Regex("[A-Za-z0-9_-]{6,}")) && id.length in 6..20) {
                    // Always muted autoplay for preload, will unmute via JS if needed
                    "https://www.youtube-nocookie.com/embed/$id?playsinline=1&rel=0&modestbranding=1&controls=1&iv_load_policy=3&autoplay=1&mute=1&enablejsapi=1&origin=https://www.youtube.com"
                } else {
                    externalUrl ?: "https://www.youtube.com/watch?v=$id"
                }
            }
            "TIKTOK" -> {
                val id = videoId.trim()
                if (id.matches(Regex("\\d{8,}"))) {
                    "https://www.tiktok.com/embed/v2/$id?lang=en-US"
                } else {
                    externalUrl ?: "https://www.tiktok.com/embed/v2/$id"
                }
            }
            "FACEBOOK" -> {
                val href = externalUrl?.takeIf { it.contains("facebook.com") || it.contains("fb.watch") } ?: run {
                    if (videoId.all { it.isDigit() } && videoId.length >= 5) "https://www.facebook.com/video.php?v=$videoId"
                    else externalUrl ?: "https://www.facebook.com/video.php?v=$videoId"
                }
                val enc = try { java.net.URLEncoder.encode(href, "UTF-8") } catch (e: Exception) { href }
                "https://www.facebook.com/plugins/video.php?href=$enc&show_text=false&autoplay=true&allowfullscreen=true&width=560"
            }
            "GENERIC" -> externalUrl ?: videoId
            else -> externalUrl ?: videoId
        }
    }

    // Track current URL for fallback logic
    var currentUrl by remember(embedUrl) { mutableStateOf(embedUrl) }
    LaunchedEffect(embedUrl) {
        currentUrl = embedUrl
        triedFallback = false
        error = null
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
                            // Ensure always unmuted when requested (user wants app+embedded unmuted)
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                if (isCurrentPage) {
                                    if (isMuted) {
                                        view?.evaluateJavascript("(function(){try{ document.querySelectorAll('video').forEach(v=>{v.muted=true; v.volume=0;}); var auds=document.querySelectorAll('audio');auds.forEach(a=>a.muted=true);}catch(e){}})();", null)
                                        view?.evaluateJavascript("try{ var ifr=document.querySelector('iframe'); if(ifr) ifr.contentWindow.postMessage('{\"event\":\"command\",\"func\":\"mute\",\"args\":[]}', '*'); }catch(e){}", null)
                                    } else {
                                        view?.evaluateJavascript("(function(){try{ document.querySelectorAll('video').forEach(v=>{v.muted=false; v.volume=1; v.play().catch(()=>{});}); var auds=document.querySelectorAll('audio');auds.forEach(a=>{a.muted=false;});}catch(e){}})();", null)
                                        view?.evaluateJavascript("try{ var ifr=document.querySelector('iframe'); if(ifr){ ifr.contentWindow.postMessage('{\"event\":\"command\",\"func\":\"unMute\",\"args\":[]}', '*'); ifr.contentWindow.postMessage('{\"event\":\"command\",\"func\":\"playVideo\",\"args\":[]}', '*'); } }catch(e){}", null)
                                        // Extra attempt for TikTok/Facebook generic video tag inside iframe
                                        view?.evaluateJavascript("(function(){try{ var ifr=document.querySelector('iframe'); if(ifr && ifr.contentDocument){ var v=ifr.contentDocument.querySelector('video'); if(v){v.muted=false; v.volume=1; v.play().catch(()=>{});} } }catch(e){}})();", null)
                                    }
                                } else {
                                    view?.evaluateJavascript("(function(){try{ document.querySelectorAll('video').forEach(v=>v.pause()); }catch(e){}})();", null)
                                    view?.evaluateJavascript("try{ var ifr=document.querySelector('iframe'); if(ifr) ifr.contentWindow.postMessage('{\"event\":\"command\",\"func\":\"pauseVideo\",\"args\":[]}', '*'); }catch(e){}", null)
                                }
                            }, 600)
                        }
                        override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                            if (!triedFallback && externalUrl != null && failingUrl != externalUrl && (
                                        (platform == "YOUTUBE" && failingUrl?.contains("youtube-nocookie") == true) ||
                                                platform == "TIKTOK" || platform == "FACEBOOK")
                            ) {
                                triedFallback = true
                                currentUrl = externalUrl
                                view?.loadUrl(externalUrl)
                                return
                            }
                            loading = false
                            error = description ?: "Load failed $errorCode"
                        }
                    }
                    setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
                    loadUrl(currentUrl)
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { wv ->
                webViewRef = wv
                if (wv.url != currentUrl) {
                    wv.loadUrl(currentUrl)
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

        // Handle Shorts swipe: ensure always unmuted when current (user request) — unmute both app and embedded
        LaunchedEffect(isCurrentPage, isMuted) {
            webViewRef?.let { wv ->
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
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
                }, 120)
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
                            webViewRef?.loadUrl(embedUrl)
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
                                webViewRef?.loadUrl(externalUrl)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f), contentColor = Color.White)
                        ) { Text("Open original link", fontSize = 12.sp) }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Tip: Copy link directly from YouTube → Share → Copy link. For Facebook, use the video's direct link, not a share token. Your own platform links now load directly as GENERIC.", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp, textAlign = TextAlign.Center)
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
