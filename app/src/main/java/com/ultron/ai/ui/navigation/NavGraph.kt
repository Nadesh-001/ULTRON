package com.ultron.ai.ui.navigation

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ultron.ai.MainActivity
import com.ultron.ai.core.FileAnalyzer
import com.ultron.ai.service.capture.ScreenCapture
import com.ultron.ai.ui.chat.ChatScreen
import com.ultron.ai.ui.chat.ChatViewModel
import com.ultron.ai.ui.dashboard.LazuriDashboardScreen
import com.ultron.ai.ui.dashboard.UltronHomeDashboard
import com.ultron.ai.ui.diagnostics.DiagnosticsScreen
import com.ultron.ai.ui.security.SecurityDashboard
import com.ultron.ai.ui.settings.SettingsScreen
import com.ultron.ai.ui.voice.UltronVoiceAssistantScreen
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun ULTRONNavGraph(activity: MainActivity) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            UltronHomeDashboard(
                onNavigateToChat = { navController.navigate("chat") },
                onNavigateToVoice = { navController.navigate("voice") },
                onOpenSettings = { navController.navigate("settings") }
            )
        }
        composable("voice") {
            UltronVoiceAssistantScreen(
                onNavigateToHome = { navController.navigate("home") },
                onNavigateToChat = { navController.navigate("chat") },
                onOpenSettings = { navController.navigate("settings") }
            )
        }
        composable("chat") {
            val viewModel: ChatViewModel = koinViewModel()
            val screenCapture: ScreenCapture = koinInject()
            val fileAnalyzer: FileAnalyzer = koinInject()

            // Consent, denied or given, goes to the view model: it reports a refusal and runs the capture.
            val screenCaptureLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result -> viewModel.onScreenConsent(result.resultCode, result.data) }

            // Only the URI is handled here; the file is read off the main thread by the view model.
            val filePickerLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) result.data?.data?.let(viewModel::onFilePicked)
            }

            LaunchedEffect(Unit) {
                activity.onWakeWordActivated = {
                    viewModel.onMicClick()
                }
                viewModel.onAnalyzeScreenRequested = {
                    val intent = screenCapture.consentIntent(activity)
                    screenCaptureLauncher.launch(intent)
                }
                viewModel.onAnalyzeFileRequested = {
                    val intent = fileAnalyzer.getFilePickerIntent()
                    filePickerLauncher.launch(intent)
                }
            }

            // A page asked about in the notebook arrives here when the chat comes back.
            val inbox: com.ultron.ai.ui.notebook.NotebookInbox = koinInject()
            LaunchedEffect(Unit) { inbox.take()?.let(viewModel::onNotebook) }

            ChatScreen(
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToModes = { navController.navigate("modes") },
                onNavigateToNotebook = { navController.navigate("notebook") },
                viewModel = viewModel
            )
        }
        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenDiagnostics = { navController.navigate("diagnostics") },
                onOpenModes = { navController.navigate("modes") }
            )
        }
        composable("notebook") {
            com.ultron.ai.ui.notebook.NotebookScreen(
                onNavigateBack = { navController.popBackStack() },
                onAsk = { navController.popBackStack() }
            )
        }
        composable("modes") {
            com.ultron.ai.ui.modes.ModesScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable("diagnostics") {
            DiagnosticsScreen(
                viewModel = koinViewModel(),
                onNavigateBack = { navController.popBackStack() },
                onOpenSettings = {
                    // Settings is where the screen is opened from; going "back" lands there.
                    if (!navController.popBackStack("settings", inclusive = false)) navController.navigate("settings")
                }
            )
        }
        composable("lazuri_dashboard") {
            LazuriDashboardScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable("security") {
            com.ultron.ai.ui.security.SecurityDashboard(
                viewModel = koinViewModel(),
                onBack = { navController.popBackStack() }
            )
        }
    }
}
