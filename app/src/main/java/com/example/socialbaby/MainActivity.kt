package com.example.socialbaby

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        seedIfNeeded()
        setContent {
            SocialBabyTheme { // system auto (light/dark) default
                Surface(modifier = Modifier.fillMaxSize(), color = androidx.compose.material3.MaterialTheme.colorScheme.background) {
                    KidTubeNavHost()
                }
            }
        }
    }

    private fun seedIfNeeded() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val shelfDao = db.shelfDao()
                val mediaDao = db.mediaItemDao()
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
