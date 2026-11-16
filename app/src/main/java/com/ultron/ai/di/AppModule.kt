package com.ultron.ai.di

import androidx.room.Room
import com.ultron.ai.core.AppLauncher
import com.ultron.ai.core.CommandRouter
import com.ultron.ai.core.BriefComposer
import com.ultron.ai.core.CalendarWriter
import com.ultron.ai.core.DeviceController
import com.ultron.ai.core.ContactsReader
import com.ultron.ai.core.FileAnalyzer
import com.ultron.ai.core.SystemPromptBuilder
import com.ultron.ai.core.VoiceInputManager
import com.ultron.ai.data.local.FridayDatabase
import com.ultron.ai.data.local.Migrations
import com.ultron.ai.data.remote.GroqApiService
import com.ultron.ai.data.remote.LazuriApiService
import com.ultron.ai.data.remote.NearbyPlacesService
import com.ultron.ai.data.remote.WeatherApiService
import com.ultron.ai.service.FridayMemory
import com.ultron.ai.service.ProactiveBriefService
import com.ultron.ai.service.SessionSummarizer
import com.ultron.ai.data.repository.AssistantRepositoryImpl
import com.ultron.ai.domain.repository.AssistantRepository
import com.ultron.ai.domain.usecase.AnalyzeContentUseCase
import com.ultron.ai.domain.usecase.GetChatHistoryUseCase
import com.ultron.ai.domain.usecase.SendMessageUseCase
import com.ultron.ai.ui.chat.ChatViewModel
import com.ultron.ai.ui.dashboard.LazuriDashboardViewModel
import com.ultron.ai.ui.settings.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import kotlinx.coroutines.launch
import org.koin.dsl.bind
import org.koin.dsl.module

val appModule = module {

    // Database
    single {
        Room.databaseBuilder(
            androidContext(),
            FridayDatabase::class.java,
            "friday_db"
        )
            // No destructive fallback: a missing migration must fail loudly
            // rather than quietly wipe the user's memory and history.
            .addMigrations(*Migrations.ALL)
            .build()
    }
    single { get<FridayDatabase>().chatMessageDao() }
    // API keys are sealed with Android Keystore before they reach the table.
    single {
        com.ultron.ai.data.local.secure.SecurePreferenceDao(
            get<FridayDatabase>().userPreferenceDao(), com.ultron.ai.data.local.secure.KeystoreCipher()
        )
    } bind com.ultron.ai.data.local.dao.UserPreferenceDao::class
    single { get<FridayDatabase>().memoryDao() }
    single { get<FridayDatabase>().interactionDao() }
    single { get<FridayDatabase>().notificationDao() }
    single { get<FridayDatabase>().sessionSummaryDao() }
    single { get<FridayDatabase>().errandDao() }
    // Moving to another phone: everything in one passphrase-encrypted file.
    single {
        com.ultron.ai.data.backup.SettingsTransfer(
            get<FridayDatabase>().backupDao(),
            get<com.ultron.ai.data.local.secure.SecurePreferenceDao>(),
            appVersion = com.ultron.ai.BuildConfig.VERSION_NAME
        )
    }

    // Network
    single { GroqApiService.create() }
    single { LazuriApiService.create() }
    single { WeatherApiService.create() }
    single { NearbyPlacesService.create() }

    // Core
    // Tool-carrying prompts get the phone's context: country, installed apps.
    single { SystemPromptBuilder(deviceContext = { get<com.ultron.ai.core.DeviceContext>().describe() }) }
    single { VoiceInputManager(androidContext(), get(), get(), get()) }
    single { CommandRouter() }
    single { ContactsReader(androidContext()) }
    single { CalendarWriter(androidContext()) }
    single { DeviceController(com.ultron.ai.core.AndroidDeviceKnobs(androidContext())) }
    single { com.ultron.ai.core.MediaLauncher(androidContext()) }
    single { com.ultron.ai.core.MediaSessions(androidContext()) }
    single { com.ultron.ai.core.PlaybackStarter(androidContext(), get(), get(), get()) }
    single { com.ultron.ai.core.MediaControls(androidContext(), get(), get()) }
    single { com.ultron.ai.service.mail.GmailAuth(androidContext()) }
    single { com.ultron.ai.data.remote.GmailApi.create() }
    single { com.ultron.ai.service.mail.MailRetelling(get(), get(), get()) }
    single { com.ultron.ai.service.mail.GmailConnector(get(), get(), get()) }
    single { com.ultron.ai.service.mail.MailAssistant(get(), get(), get(), get(), get()) }

    // Commands: one implementation shared by the chat and the voice service.
    single { com.ultron.ai.core.AlarmSetter(androidContext()) }
    single {
        com.ultron.ai.core.DeviceContext(
            androidContext(), com.ultron.ai.core.capabilities.CapabilityProbe(
                androidContext(), get(), modes = { get<com.ultron.ai.core.modes.ModeStore>().all().map { it.name } }
            )
        )
    }
    single { com.ultron.ai.core.people.PeopleDirectory(get(), get()) }
    single {
        com.ultron.ai.core.people.Messenger(
            androidContext(), get(), get(), get(),
            sms = com.ultron.ai.core.people.SmsConfirmation(
                com.ultron.ai.service.messages.AndroidSms(androidContext())
            )
        )
    }
    single { com.ultron.ai.core.MediaSearch(androidContext(), get(), get(), get()) }
    single { com.ultron.ai.service.WeatherHere(androidContext(), get(), get()) }
    single { com.ultron.ai.core.people.Caller(androidContext(), get(), get(), get(), get(), get()) }
    single { com.ultron.ai.service.messages.SpamCheck(get()) }
    single { com.ultron.ai.service.SpeechHints(get(), get()) }
    single { com.ultron.ai.service.links.QuickLinkStore(get()) }
    single { com.ultron.ai.service.links.LinkOpener(androidContext()) }
    single { com.ultron.ai.service.messages.MessengerInbox(get<com.ultron.ai.service.messages.SpamCheck>()::isSpam) }
    single { com.ultron.ai.service.messages.MessageSummarizer(get(), get(), get()) }
    single { com.ultron.ai.service.messages.Announcer() }
    single {
        val summarizer = get<com.ultron.ai.service.messages.MessageSummarizer>()
        com.ultron.ai.service.messages.MessageAssistant(androidContext(), get(), get(), summarizer::summarize)
    }
    single {
        com.ultron.ai.command.PhoneActions(
            get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), people = get()
        )
    }
    single {
        com.ultron.ai.command.PlannerActions(get(), get(), get(), watchErrands = {
            com.ultron.ai.service.ErrandWatchWorker.schedule(androidContext())
        })
    }
    single { com.ultron.ai.service.WebResearch(get(), get(), get()) }
    single { com.ultron.ai.command.InfoActions(get(), get(), get(), get(), get()) }
    single {
        val writes = kotlinx.coroutines.CoroutineScope(
            kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO
        )
        val device = get<com.ultron.ai.core.DeviceContext>()
        com.ultron.ai.agent.LearnedCommands(
            get(), writes,
            available = { tool ->
                com.ultron.ai.core.capabilities.ToolRequirements.met(tool, device.capabilities.value)
            }
        ).apply { load() }
    }
    // Modes the owner creates by voice: stored, compiled once by the model, then run without it.
    single { get<FridayDatabase>().modeDao() }
    single {
        val writes = kotlinx.coroutines.CoroutineScope(
            kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO
        )
        com.ultron.ai.core.modes.ModeStore(get()).also { store -> writes.launch { store.load() } }
    }
    single {
        val device = get<com.ultron.ai.core.DeviceContext>()
        val prefs = get<com.ultron.ai.data.local.dao.UserPreferenceDao>()
        val catalog = get<com.ultron.ai.service.ModelCatalog>()
        com.ultron.ai.core.modes.ModeCompiler(
            get(),
            access = {
                com.ultron.ai.core.modes.ModeCompiler.Access(
                    apiKey = prefs.get(com.ultron.ai.data.repository.AssistantRepositoryImpl.PREF_API_KEY).orEmpty(),
                    model = catalog.model(com.ultron.ai.core.GroqModels.Role.CHAT),
                    backup = catalog.model(com.ultron.ai.core.GroqModels.Role.FAST)
                )
            },
            capabilities = { device.refresh() },
            phone = { device.describe() }
        )
    }
    single {
        val alarms = com.ultron.ai.service.ModeAlarms(androidContext())
        com.ultron.ai.core.modes.ModeSchedules(
            get<FridayDatabase>().modeScheduleDao(), get(),
            onChanged = { now, before -> alarms.rebook(now, before) }
        )
    }
    single { com.ultron.ai.service.AndroidNfcTags(androidContext(), get()) }
    single {
        val compiler = get<com.ultron.ai.core.modes.ModeCompiler>()
        com.ultron.ai.core.modes.ModeEngine(
            get(), compiler::compile, get(), schedules = get(),
            links = com.ultron.ai.service.AndroidPhoneLinks(androidContext()),
            nfc = get<com.ultron.ai.service.AndroidNfcTags>()
        )
    }
    single {
        val keyguard = androidContext().getSystemService(android.app.KeyguardManager::class.java)
        val prefs = get<com.ultron.ai.data.local.dao.UserPreferenceDao>()
        com.ultron.ai.command.CommandExecutor(
            get(), get(), get(), get(), get(), messages = get(), learned = get(), modes = get(),
            links = get(), opener = get(),
            lock = {
                val locked = keyguard?.isKeyguardLocked == true
                // Only a profile that checks commands, not just the wake word, says it's the owner.
                val verified = !locked || com.ultron.ai.core.VoiceProfile
                    .deserialise(prefs.get(com.ultron.ai.service.FridayWakeWordService.PREF_VOICE_PROFILE))
                    ?.coversCommands == true
                locked to verified
            },
            lockLevel = {
                com.ultron.ai.command.LockPolicy.Level.of(prefs.get(com.ultron.ai.command.LockPolicy.Level.PREF))
            },
            unlocker = com.ultron.ai.service.lock.LockScreen.unlocker(androidContext())
        )
    }
    single {
        val device = get<com.ultron.ai.core.DeviceContext>()
        com.ultron.ai.agent.FridayAgent(get(), get(), learned = get(), capabilities = { device.refresh() })
    }
    single { ProactiveBriefService(androidContext(), get(), get(), get()) }
    single { com.ultron.ai.service.ModelCatalog(get(), get()) }
    single { SessionSummarizer(get(), get(), get(), get(), get()) }
    single { AppLauncher(androidContext(), get()) }
    single { FridayMemory(get(), get(), get(), get(), get(), get()) }
    single { com.ultron.ai.service.capture.ScreenCapture(androidContext()) }
    single { FileAnalyzer(androidContext()) }

    // Repository
    single<AssistantRepository> {
        AssistantRepositoryImpl(
            groqApi = get(),
            chatDao = get(),
            prefDao = get(),
            promptBuilder = get(),
            memory = get(),
            catalog = get(),
            agent = get()
        )
    }

    // Use Cases
    factory { SendMessageUseCase(repository = get()) }
    factory { GetChatHistoryUseCase(repository = get()) }
    factory { AnalyzeContentUseCase(repository = get()) }

    // ViewModels
    viewModel {
        ChatViewModel(
            sendMessage = get(),
            getChatHistory = get(),
            analyzeContent = get(),
            voiceInput = get(),
            commands = get(),
            memory = get(),
            files = get(),
            screen = get()
        )
    }
    viewModel { com.ultron.ai.ui.diagnostics.DiagnosticsViewModel(get()) }
    viewModel { com.ultron.ai.ui.modes.ModesViewModel(get(), get(), get()) }
    single { com.ultron.ai.ui.notebook.NotebookInbox() }
    viewModel { com.ultron.ai.ui.settings.TransferViewModel(androidContext(), get(), get(), get()) }
    viewModel { com.ultron.ai.ui.settings.LocationViewModel(get()) }
    viewModel { com.ultron.ai.ui.settings.LockLevelViewModel(get()) }
    viewModel { com.ultron.ai.ui.settings.QuickLinksViewModel(get()) }
    viewModel {
        LazuriDashboardViewModel(
            memoryDao = get(),
            chatDao = get(),
            summaryDao = get(),
            summarizer = get()
        )
    }
    viewModel {
        SettingsViewModel(
            prefDao = get(),
            appContext = androidContext(),
            gmailConnector = get(),
            catalog = get(),
            lazuriApi = get(),
            memory = get(),
            learned = get()
        )
    }
}
