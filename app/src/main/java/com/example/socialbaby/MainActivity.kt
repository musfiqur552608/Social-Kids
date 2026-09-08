package com.example.socialbaby

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.socialbaby.R
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.socialbaby.data.local.AppDatabase
import com.example.socialbaby.data.local.entity.MediaType
import com.example.socialbaby.data.local.entity.ShelfEntity
import com.example.socialbaby.data.local.entity.MediaItemEntity
import com.example.socialbaby.ui.child.home.HomeScreen
import com.example.socialbaby.ui.child.photos.PhotoGalleryScreen
import com.example.socialbaby.ui.child.player.PlayerScreen
import com.example.socialbaby.ui.child.shorts.ShortsFeedScreen
import com.example.socialbaby.ui.navigation.ChildRoute
import com.example.socialbaby.ui.navigation.ParentRoute
import com.example.socialbaby.ui.parent.addcontent.AddContentScreen
import com.example.socialbaby.ui.parent.auth.ParentUnlockScreen
import com.example.socialbaby.ui.parent.dashboard.ParentDashboardScreen
import com.example.socialbaby.ui.parent.history.WatchHistoryScreen
import com.example.socialbaby.ui.parent.manageshelf.ManageShelfScreen
import com.example.socialbaby.ui.parent.settings.ParentSettingsScreen
import com.example.socialbaby.ui.theme.SocialBabyTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        // System splash (Theme.SocialKids.Splash) shows logo centred for 900ms
        var keepSystemSplash by androidx.compose.runtime.mutableStateOf(true)
        splash.setKeepOnScreenCondition { keepSystemSplash }
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ keepSystemSplash = false }, 700)
        enableEdgeToEdge()
        seedIfNeeded()
        setContent {
            SocialBabyTheme {
                var showComposeSplash by remember { mutableStateOf(true) }
                // Dismiss compose splash after 1800ms with fade
                LaunchedEffect(Unit) { kotlinx.coroutines.delay(1800); showComposeSplash = false }
                androidx.compose.animation.AnimatedVisibility(
                    visible = showComposeSplash,
                    enter = androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.fadeOut()
                ) {
                    SplashContent()
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = !showComposeSplash,
                    enter = androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.fadeOut()
                ) {
                    Surface(modifier = Modifier.fillMaxSize(), color = androidx.compose.material3.MaterialTheme.colorScheme.background) {
                        KidTubeNavHost()
                    }
                }
            }
        }
    }

    @Composable
    private fun SplashContent() {
        androidx.compose.foundation.layout.BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            androidx.compose.ui.graphics.Color(0xFFFF5A7A),
                            androidx.compose.ui.graphics.Color(0xFFE8283E),
                            androidx.compose.ui.graphics.Color(0xFF9F0E26)
                        )
                    )
                )
        ) {
            val isCompact = maxWidth < 600.dp
            val isLandscape = maxWidth > maxHeight
            val logoSize = when {
                isLandscape -> 116.dp
                isCompact -> 148.dp
                else -> 192.dp // tablets / foldables open
            }
            val titleSize = when {
                isCompact -> 34.sp
                else -> 42.sp
            }
            val bottomPad = if (isCompact) 56.dp else 72.dp

            var startAnim by remember { mutableStateOf(false) }
            val scale by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (startAnim) 1f else 0.82f,
                animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.55f, stiffness = 300f), label = "logoScale"
            )
            val alpha by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (startAnim) 1f else 0f,
                animationSpec = androidx.compose.animation.core.tween(700), label = "alpha"
            )
            LaunchedEffect(Unit) { startAnim = true }

            // Center logo — responsive for phones / tablets / foldables
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 24.dp),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.social_kids_logo_red),
                    contentDescription = "Social Kids Logo",
                    modifier = Modifier
                        .size(logoSize)
                        .graphicsLayer(scaleX = scale, scaleY = scale, alpha = alpha)
                )
            }

            // Bottom branding — good font, always safe area, responsive
            androidx.compose.foundation.layout.Column(
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.BottomCenter)
                    .padding(bottom = bottomPad, start = 24.dp, end = 24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
            ) {
                androidx.compose.material3.Text(
                    text = "Social Kids",
                    fontSize = titleSize,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                    letterSpacing = (-0.5).sp,
                    color = androidx.compose.ui.graphics.Color.White,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.25f),
                            offset = androidx.compose.ui.geometry.Offset(0f, 2f),
                            blurRadius = 8f
                        )
                    ),
                    modifier = Modifier.graphicsLayer(alpha = alpha)
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(6.dp))
                androidx.compose.material3.Text(
                    text = "Safe  •  Curated  •  Fun",
                    fontSize = if (isCompact) 13.sp else 15.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    letterSpacing = 1.2.sp,
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.92f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.graphicsLayer(alpha = alpha)
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(10.dp))
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(3.dp)
                        .background(
                            androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f),
                            androidx.compose.foundation.shape.RoundedCornerShape(2.dp)
                        )
                )
            }
        }
    }

    private fun seedIfNeeded() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val shelfDao = db.shelfDao()
                val mediaDao = db.mediaItemDao()
                // One-time cleanup: TikTok/Facebook support was removed, so drop
                // rows of those types plus old YouTube rows with unplayable IDs
                // (valid YouTube IDs are always 11 chars). Runs every launch,
                // deletes nothing when data is already valid.
                try {
                    mediaDao.deleteUnsupportedTypes()
                    mediaDao.deleteInvalidYoutubeLinks()
                } catch (_: Exception) {}
                if (shelfDao.count() == 0) {
                    val longId = shelfDao.insert(ShelfEntity(name = "Long Videos", kind = "SHELF", sortOrder = 0))
                    val shortsId = shelfDao.insert(ShelfEntity(name = "Shorts", kind = "SHORTS_FEED", sortOrder = 1))
                    val photosId = shelfDao.insert(ShelfEntity(name = "Photos", kind = "PHOTOS", sortOrder = 2))
                    val bedtimeId = shelfDao.insert(ShelfEntity(name = "Bedtime", kind = "PLAYLIST", sortOrder = 3))

                    // Only seed if no media
                    if (mediaDao.count() == 0) {
                        // Direct in-app online video — uses ExoPlayer test asset (always 200, never 403)
                        mediaDao.insert(
                            MediaItemEntity(
                                type = MediaType.ONLINE_VIDEO.name,
                                title = "Sample Video (always works)",
                                thumbnailPath = null,
                                externalUrl = "https://storage.googleapis.com/exoplayer-test-media-1/mp4/android-screens-10s.mp4",
                                shelfId = longId,
                                sortOrder = 0
                            )
                        )
                        // Second sample — different reliable source
                        mediaDao.insert(
                            MediaItemEntity(
                                type = MediaType.ONLINE_VIDEO.name,
                                title = "Big Buck Bunny (sample)",
                                thumbnailPath = null,
                                externalUrl = "https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/720/Big_Buck_Bunny_720_10s_1MB.mp4",
                                shelfId = longId,
                                sortOrder = 1
                            )
                        )
                        // Direct image (plays via Coil, preview same URL)
                        mediaDao.insert(
                            MediaItemEntity(
                                type = MediaType.ONLINE_IMAGE.name,
                                title = "Cute Animals (in-app image)",
                                thumbnailPath = "https://picsum.photos/800/600",
                                externalUrl = "https://picsum.photos/800/600",
                                shelfId = photosId,
                                sortOrder = 0
                            )
                        )
                        // YouTube embed — use YouTube's own demo video (always embeddable, never 153)
                        mediaDao.insert(
                            MediaItemEntity(
                                type = MediaType.YOUTUBE_LINK.name,
                                title = "YouTube Demo (embeddable)",
                                thumbnailPath = "https://img.youtube.com/vi/YE7VzlLtp-4/hqdefault.jpg",
                                externalUrl = "https://www.youtube.com/watch?v=YE7VzlLtp-4",
                                platformVideoId = "YE7VzlLtp-4",
                                shelfId = shortsId,
                                sortOrder = 1
                            )
                        )
                        // Second YouTube for shorts
                        mediaDao.insert(
                            MediaItemEntity(
                                type = MediaType.YOUTUBE_LINK.name,
                                title = "YouTube Sample",
                                thumbnailPath = "https://img.youtube.com/vi/dQw4w9WgXcQ/hqdefault.jpg",
                                externalUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                                platformVideoId = "dQw4w9WgXcQ",
                                shelfId = shortsId,
                                sortOrder = 2
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

@Composable
fun KidTubeNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = ChildRoute.Home.route) {
        composable(ChildRoute.Home.route) {
            HomeScreen(
                onNavigateToParent = { nav.navigate(ParentRoute.Unlock.route) },
                onNavigateToPlayer = { id -> nav.navigate("${ChildRoute.Player.route}/$id") },
                onNavigateToShorts = { nav.navigate(ChildRoute.Shorts.route) },
                onNavigateToPhotos = { nav.navigate(ChildRoute.Photos.route) }
            )
        }
        composable(ChildRoute.Shorts.route) {
            ShortsFeedScreen(
                onNavigateBack = { nav.popBackStack() },
                onNavigateToPlayer = { id -> nav.navigate("${ChildRoute.Player.route}/$id") }
            )
        }
        composable(ChildRoute.Photos.route) {
            PhotoGalleryScreen(
                onNavigateBack = { nav.popBackStack() },
                onNavigateToPlayer = { id -> nav.navigate("${ChildRoute.Player.route}/$id") }
            )
        }
        composable("${ChildRoute.Player.route}/{mediaId}") { backStack ->
            val id = backStack.arguments?.getString("mediaId")?.toLongOrNull() ?: 0L
            PlayerScreen(mediaId = id, onNavigateBack = { nav.popBackStack() })
        }
        composable(ParentRoute.Unlock.route) {
            ParentUnlockScreen(
                onUnlocked = { nav.navigate(ParentRoute.Dashboard.route) { popUpTo(ParentRoute.Unlock.route) { inclusive = true } } },
                onBackToChild = { nav.popBackStack(ChildRoute.Home.route, inclusive = false) }
            )
        }
        composable(ParentRoute.Dashboard.route) {
            ParentDashboardScreen(
                onBackToChild = { nav.popBackStack(ChildRoute.Home.route, inclusive = false) },
                onNavigateToAdd = { nav.navigate(ParentRoute.AddContent.route) },
                onNavigateToShelf = { nav.navigate(ParentRoute.ManageShelf.route) },
                onNavigateToSettings = { nav.navigate(ParentRoute.Settings.route) },
                onNavigateToHistory = { nav.navigate(ParentRoute.History.route) },
                onPreviewItem = { id -> nav.navigate("${ChildRoute.Player.route}/$id") }
            )
        }
        composable(ParentRoute.AddContent.route) { AddContentScreen(onBack = { nav.popBackStack() }) }
        composable(ParentRoute.ManageShelf.route) { ManageShelfScreen(onBack = { nav.popBackStack() }) }
        composable(ParentRoute.Settings.route) { ParentSettingsScreen(onBack = { nav.popBackStack() }) }
        composable(ParentRoute.History.route) { WatchHistoryScreen(onBack = { nav.popBackStack() }) }
    }
}
