# ULTRON Android — Technical Architecture Guide

**Package:** `com.ultron.ai`  
**Version:** `0.21.0` (`versionCode = 26`)  
**Platform:** Android 8.0+ (`minSdk = 26`, `compileSdk = 35`, `targetSdk = 35`, ABI: `arm64-v8a`)  
**Codebase:** 317 Kotlin source files · 91 JVM/Robolectric unit test suites

---

## 1. High-Level Layers

ULTRON follows a **Clean Architecture + MVVM (`StateFlow`)** structure wired via **Koin 4.0**:

| Layer | Package(s) | Responsibility |
|---|---|---|
| **Presentation (Compose UI)** | `ui.*`, `controlcenter.presentation`, `vision.ui` | Jetpack Compose Material 3 screens, dark obsidian/crimson HUD theme, ViewModels |
| **Domain** | `domain.model`, `domain.repository`, `domain.usecase` | Core business models, repository contracts, command/chat use cases |
| **AI Control & Routing** | `controlcenter.core`, `controlcenter.failover`, `controlcenter.health`, `controlcenter.usage`, `controlcenter.data` | Model registry, capability verification, failover routing, latency/health telemetry, Room usage store |
| **Vision AI** | `vision.camera`, `vision.image`, `vision.ocr`, `vision.barcode`, `vision.translation`, `vision.ai` | CameraX pipeline, EXIF-aware bitmap normalization, ML Kit analyzers, multimodal Vision VQA |
| **Agent & Offline Router** | `agent`, `command`, `core.*` | Offline natural-language command matching (English & Tanglish), streaming Groq LLM agent, 24+ device & web tools |
| **Data & Persistence** | `data.local.*`, `data.remote.*`, `data.repository`, `data.backup` | Room databases, Retrofit/OkHttp clients, Keystore AES-256 secure storage, local backups |
| **Background Services** | `service.*` | Wake-word foreground listener, Notification Listener, WorkManager briefings & errand watchers |

---

## 2. Major Feature Modules

### 2.1 ULTRON AI Control Center (`com.ultron.ai.controlcenter`)
- **Screens:** `ControlCenterHostScreen`, `ControlCenterOverviewScreen`, `ControlCenterUsageScreen`, `ControlCenterModelsScreen`, `ControlCenterHealthScreen` backed by `ControlCenterViewModel`.
- **Routing & Failover:** `AIModelRouter` routes requests across user-selected roles (Primary, Backup, Vision, Voice, Lightweight, Reasoning). `ErrorClassifier` + `RetryPolicy` detect recoverable HTTP errors (`429`, `502`, `503`, `504`) and timeouts, automatically switching to the configured backup model.
- **Telemetry & Transparency:** `UsageTracker` persists every request's model ID, input/output tokens, latency, and error status into `AIControlCenterDatabase` (`AIUsageEntity` / `AIUsageDao`). Rate-limit metrics come from live API headers, and billing balance explicitly states `"Billing balance unavailable through API."` when unsupported rather than estimating or fabricating credits.
- **Capabilities:** `ModelCapability` tracks verified badges (`TEXT`, `TOOL CALLING`, `REASONING`, `VISION`, `AUDIO`, `STREAMING`) in a compatibility registry.

### 2.2 ULTRON Vision AI (`com.ultron.ai.vision`)
- **Camera Pipeline:** `CameraController` wraps CameraX `1.4.1` (`Preview`, `ImageCapture`, flash control, front/rear lens switching) while `ImageProcessor` downsamples, corrects EXIF rotation, and encodes JPEGs to Base64 for multimodal vision inference.
- **On-Device ML Kit:** `TextRecognitionService` (OCR), `BarcodeScanner` (QR/barcodes), and `TranslationService` (language ID & translation).
- **Multimodal VQA:** `VisionAIService` + `VisionContextManager` maintain image context across multi-turn chats inside `VisionChatScreen`.

### 2.3 Voice, Wake Word & Speaker Verification
- **Offline Wake Word:** Vosk `0.3.47` (`vosk-model-small-en-us-0.15`) runs grammar-constrained spotting locally before opening any network connection.
- **Speaker Verification:** ONNX Runtime `1.20.0` runs the wespeaker ResNet34 model against the owner's enrolled 256-D voice embedding stored on-device.
- **Speech-to-Text & TTS:** Groq Whisper (`large-v3-turbo`) with English/Tanglish enforcement (`UltronLanguageEnforcer`), paired with Microsoft Edge Neural TTS voices.

### 2.4 Add Context & Tool Integration (`com.ultron.ai.core.context`)
- `ContextManager` + `ContextModels` manage attachments added via the `(+)` button in `ChatInputBar`:
  - SAF document attachments
  - Directory inspection (`FolderAnalyzer`)
  - Web content extraction (`BrowserExtractor`)
  - Google Drive integration (`GoogleDriveManager`)
  - Predefined context actions (`ContextActionExecutor`)

### 2.5 Security & Permission Architecture (`com.ultron.ai.ui.security`, `com.ultron.ai.data.local.secure`)
- `SecurityDashboard` exposes runtime permission states (14 Android permissions) and special privileges (5 system settings), coupled with capability diagnostics (`CapabilityProbe`, `UltronCapabilities`, `ToolRequirements`).
- API keys and sensitive preferences are stored inside Android Keystore AES-256 storage; CI runs automated regex secret scanning on every push.

---

## 3. CI/CD & Quality Assurance

| Workflow | Trigger | Outputs |
|---|---|---|
| [android-build.yml](../.github/workflows/android-build.yml) | Push / PR / Dispatch | `Ultron-v0.21.0-debug.apk` artifact + automated GitHub Release on `main` + commit diagnostics on failure |
| [android-health-check.yml](../.github/workflows/android-health-check.yml) | Push / PR (`main`, `development`) / Dispatch | Secret scan, clean build, 91 unit test suites, Android Lint, Detekt, APK verification, and `ULTRON_ANDROID_HEALTH_REPORT.md` |

---

## 4. R8 & Release Integrity

Release builds (`./gradlew ownerRelease`) run `verifyReleaseKeepRules` in [app/build.gradle.kts](../app/build.gradle.kts) to guarantee JNI and persisted classes (ONNX Runtime, JNA, Vosk, `libphonenumber`, Room command results, and WorkManager workers) are preserved unrenamed by R8.
