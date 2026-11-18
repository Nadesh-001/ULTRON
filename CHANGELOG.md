# Changelog

All notable changes to **ULTRON** (`com.ultron.ai`) are documented in this file.

---

## [v0.21.0] — 2026-11-18 (versionCode 26)

**Download:** [Ultron-v0.21.0-debug.apk](https://github.com/Nadesh-001/ULTRON/releases/download/v0.21.0/Ultron-v0.21.0-debug.apk)

### Added
- **ULTRON AI Control Center (`com.ultron.ai.controlcenter`):**
  - **Overview Telemetry Screen:** Groq & backup provider connection status, average response latency, 24h local token overview cards, RPM/TPM rate-limit meters, active model assignments, and quick actions.
  - **AI Usage & Analytics Screen:** Total requests, input/output/total tokens, timeframe filters (`Today`, `7d`, `30d`, custom), token charts, model-wise breakdown, error frequency, and transparent billing distinction (*displays `"Billing balance unavailable through API."` instead of fabricating credits*).
  - **Model Manager Screen:** Role-based configuration for Primary, Backup, Vision, Voice, Lightweight, and Complex Reasoning models with verified capability badges (`TEXT`, `VISION`, `TOOL CALLING`, `REASONING`, `AUDIO`, `STREAMING`) and primary model switch confirmation dialog.
  - **Model Health & Failover Engine:** `AIModelRouter`, `ErrorClassifier`, `RetryPolicy`, `ModelHealthMonitor`, and `UsageTracker` backed by `AIControlCenterDatabase` (Room) with automatic failover on HTTP 429, 502/503/504, and network timeouts.
- **ULTRON Vision AI (`com.ultron.ai.vision`):**
  - Google Lens-inspired `VisionCameraScreen` using CameraX `1.4.1` with cybernetic HUD reticle, flash toggle, camera flip, gallery import, and EXIF-aware `ImageProcessor`.
  - 6 specialized visual intelligence modes: Object Recognition, OCR Text Recognition (`mlkit:text-recognition`), Live Translation & Language ID (`mlkit:language-id`), Math & Homework Solver, QR/Barcode Scanner (`mlkit:barcode-scanning`), and Document Intelligence.
  - Multi-turn **Visual Question Answering (`VisionChatScreen`)** preserving visual context with quick follow-up prompt chips.
- **Add Context & Tool Integration (`com.ultron.ai.core.context`):**
  - Modern `(+)` context popup in `ChatInputBar` supporting SAF file attachments, directory analysis (`FolderAnalyzer`), `@` mentions, predefined actions (`ContextActionExecutor`), web page content extraction (`BrowserExtractor`), and Google Drive (`GoogleDriveManager`).
- **Centralized Security & Permission Architecture (`com.ultron.ai.ui.security`, `com.ultron.ai.data.local.secure`):**
  - Unified `SecurityDashboard` covering 14 runtime permissions and 5 special Android privileges.
  - Android Keystore AES-256 vault, tri-level AI action risk authorization, and hardware/automation kill switches.
- **Dark-Metallic HUD UI (`com.ultron.ai.ui.dashboard`, `com.ultron.ai.ui.voice`):**
  - Cinematic `UltronHomeDashboard` and kinetic waveform `UltronVoiceAssistantScreen`.

### Fixed
- Enforced pure **English & Tanglish** voice transcription in Whisper STT and improved microphone button handling.
- Eliminated recursive self-referencing typealiases (`UltronApp`, `UltronCapabilities`, `UltronDatabase`) introduced during rebranding.
- Sanitized 151 Kotlin source files from Windows UTF-8 BOM encoding.
- Resolved Kotlin compilation errors across `DateTimeParser`, `Schedules`, `ChatInputBar`, `UltronHomeDashboard`, `SettingsScreen`, `CameraController`, and `ImageProcessor`.
- Updated CI workflows (`android-build.yml` & `android-health-check.yml`) with diagnostic commit commenting on failures and automated v0.21.0 release publishing.

---

## [v0.20.0]

### Added
- 100% pure English UI localization across screens and diagnostics.
- Official vector mask ULTRON launcher icon.
- Automated English GitHub Release publishing in CI.

---

## [v0.19.0]

### Added
- Automated Android Build Health Check & QA Pipeline (`android-health-check.yml`) with secret leak scanning, unit tests, Android Lint, Detekt, and Markdown health reports.
- Standardized APK naming to `Ultron-v<version>-<variant>.apk`.

---

## [v0.18.0]

### Added
- Complete ULTRON rebranding across package namespace (`com.ultron.ai`), theme tokens, dark obsidian/crimson UI, and build artifacts.
- Core offline Vosk wake word, wespeaker ResNet34 ONNX speaker verification, dual-tier offline/LLM command routing, smart SMS/WhatsApp/Telegram comms, Open-Meteo weather, Public APIs catalog, and iLovePDF workflows.
