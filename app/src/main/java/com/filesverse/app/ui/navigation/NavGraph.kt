package com.filesverse.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.filesverse.app.ui.screens.AiAssistantScreen
import com.filesverse.app.ui.screens.ArchiveScreen
import com.filesverse.app.ui.screens.CloudScreen
import com.filesverse.app.ui.screens.FileBrowserScreen
import com.filesverse.app.ui.screens.HomeScreen
import com.filesverse.app.ui.screens.SettingsScreen
import com.filesverse.app.ui.screens.UsageMapScreen
import com.filesverse.app.ui.screens.VisorScreen

object Routes {
    const val HOME = "home"
    const val BROWSE = "browse"
    const val BROWSE_PATH = "browse/{path}"
    const val CLOUD = "cloud"
    const val VISOR = "visor"
    const val USAGE_MAP = "usage_map"
    const val AI_ASSISTANT = "ai_assistant"
    const val ARCHIVE = "archive"
    const val SETTINGS = "settings"
}

@Composable
fun FilesverseNavHost(
    navController: NavHostController = androidx.navigation.compose.rememberNavController(),
    onOpenFile: (String) -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        enterTransition = { enterNavTransition() },
        exitTransition = { exitNavTransition() }
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToBrowse = { path ->
                    if (path != null) {
                        navController.navigate("${Routes.BROWSE_PATH}?path=${android.net.Uri.encode(path)}")
                    } else {
                        navController.navigate(Routes.BROWSE)
                    }
                },
                onNavigateToCloud = { navController.navigate(Routes.CLOUD) },
                onNavigateToVisor = { navController.navigate(Routes.VISOR) },
                onNavigateToUsageMap = { navController.navigate(Routes.USAGE_MAP) },
                onNavigateToAi = { navController.navigate(Routes.AI_ASSISTANT) },
                onNavigateToArchive = { navController.navigate(Routes.ARCHIVE) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.BROWSE) {
            FileBrowserScreen(
                initialPath = null,
                onNavigateUp = { navController.popBackStack() },
                onOpenFile = onOpenFile,
                onFileClick = { path ->
                    navController.navigate("${Routes.BROWSE_PATH}?path=${android.net.Uri.encode(path)}")
                }
            )
        }

        composable(Routes.BROWSE_PATH) { backStackEntry ->
            val path = backStackEntry.arguments?.getString("path")
            FileBrowserScreen(
                initialPath = path,
                onNavigateUp = { navController.popBackStack() },
                onOpenFile = onOpenFile,
                onFileClick = { newPath ->
                    navController.navigate("${Routes.BROWSE_PATH}?path=${android.net.Uri.encode(newPath)}")
                }
            )
        }

        composable(Routes.CLOUD) {
            CloudScreen(onNavigateUp = { navController.popBackStack() })
        }

        composable(Routes.VISOR) {
            VisorScreen(onNavigateUp = { navController.popBackStack() })
        }

        composable(Routes.USAGE_MAP) {
            UsageMapScreen(onNavigateUp = { navController.popBackStack() })
        }

        composable(Routes.AI_ASSISTANT) {
            AiAssistantScreen(onNavigateUp = { navController.popBackStack() })
        }

        composable(Routes.ARCHIVE) {
            ArchiveScreen(onNavigateUp = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onNavigateUp = { navController.popBackStack() })
        }
    }
}

@Composable
private fun enterNavTransition(): androidx.compose.animation.EnterTransition {
    return androidx.compose.animation.slideInHorizontally(
        initialOffsetX = { (it * 0.1).toInt() }
    ) + androidx.compose.animation.fadeIn(
        animationSpec = androidx.compose.animation.core.tween(300)
    )
}

@Composable
private fun exitNavTransition(): androidx.compose.animation.ExitTransition {
    return androidx.compose.animation.slideOutHorizontally(
        targetOffsetX = { -(it * 0.1).toInt() }
    ) + androidx.compose.animation.fadeOut(
        animationSpec = androidx.compose.animation.core.tween(300)
    )
}
