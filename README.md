<div align="center">

<img src="docs/assets/banner.svg" alt="ULTRON — an advanced AI assistant for Android" width="100%">

<br>

[![Release](https://img.shields.io/github/v/release/n1dlee/friday-mobile?include_prereleases&style=for-the-badge&logo=github&color=FF2233&labelColor=090203)](https://github.com/n1dlee/friday-mobile/releases)
[![CI](https://img.shields.io/github/actions/workflow/status/n1dlee/friday-mobile/ci.yml?branch=main&style=for-the-badge&logo=githubactions&logoColor=white&label=tests&labelColor=090203)](https://github.com/n1dlee/friday-mobile/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/license-MIT-FF2233?style=for-the-badge&labelColor=090203)](LICENSE)

[![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=flat-square&logo=android&logoColor=white)](#-installation)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-FF2233?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Groq](https://img.shields.io/badge/LLM-Groq-F55036?style=flat-square)](https://groq.com)
[![ONNX Runtime](https://img.shields.io/badge/ONNX%20Runtime-on--device-005CED?style=flat-square&logo=onnx&logoColor=white)](https://onnxruntime.ai)

<br>

*Say "Ultron". He answers — with ruthless precision and unmatched capability.*

<br>

</div>

---

**ULTRON** is an intelligent, autonomous hands-free personal AI assistant for Android. Designed with a striking dark red and obsidian tactical aesthetic, Ultron operates with speed and precision. He listens exclusively to his owner's verified voice, carries out everyday device operations offline in milliseconds, and leverages a high-performance LLM agent with tools for complex reasoning.

Ultron natively supports **English** and **Tanglish** (Tamil-English transliteration).

> [!NOTE]
> Ultron requires a free [Groq API key](https://console.groq.com/keys) and a 64-bit Android device (Android 8.0+).

---

## ✨ Highlights

<table>
<tr>
<td width="50%" valign="top">

### 🎙️ Silent Wake Word
Offline keyword spotting using [Vosk](https://alphacephei.com/vosk/) with grammar-constrained decoding. No audio leaves your phone until you say **"Ultron"** or **"Hey Ultron"**.

### 🔐 Biometric Speaker Verification
An on-device ResNet34 speaker verification model (wespeaker, ONNX Runtime) validates both the wake word and the command against your enrolled voice vector. Strangers and background noise are strictly ignored.

### ⚡ Dual-Tier Intelligence
Over 40 everyday commands run directly on the phone in milliseconds without touching the network. Everything else escalates to a tool-augmented LLM agent.

</td>
<td width="50%" valign="top">

### 🗣️ English & Tanglish Fluency
Understands and responds fluently in both English and Tanglish (e.g. *"Ultron, torch podu"*, *"Climate epdi irukku?"*, *"Phone pannu dad-ku"*).

### 🧭 Intelligent Comm Routing
"Text Mom" automatically chooses between SMS, WhatsApp, or Telegram based on international country codes and installed apps. "Call Dad abroad" shifts seamlessly to a WhatsApp call.

### 💬 Chat Monitoring & Hands-Free Replies
Monitors incoming messages from WhatsApp, Telegram, and SMS quietly. Ask *"Any unread messages?"* to hear who messaged; reply hands-free directly through notifications.

### 🌐 Live Information & Calculations
Fetches real-time web search via Groq, accurate weather forecasts from [Open-Meteo](https://open-meteo.com), and offloads arithmetic to an exact local calculator.

### 🔌 Public APIs Search (GitHub `public-apis`)
Ultron integrates the entire free [public-apis/public-apis](https://github.com/public-apis/public-apis) catalog. Ask *"Find weather APIs"*, *"Search cryptocurrency APIs"*, or *"Find an API for anime"*, and Ultron looks up matching public APIs, endpoint URLs, authentication requirements, HTTPS, and CORS support.

### 📄 iLovePDF Automation (`ilovepdf.com`)
Automates document operations via [iLovePDF](https://www.ilovepdf.com/). Ultron triggers actions including:
- **Merge PDF**, **Split PDF**, **Compress PDF**
- **PDF to Word**, **Word to PDF**, **PDF to JPG**, **JPG to PDF**
- **PDF to Excel**, **PDF to PowerPoint**
- **Watermark**, **Rotate**, **Unlock**, and **Protect PDF**
- **Sign PDF** and **Organize PDF**
Say: *"Merge my PDFs"*, *"Compress this PDF"*, *"Convert PDF to Word"*, or *"Sign PDF"*.

</td>
</tr>
</table>

---

## 🗣️ Try Saying

| You say | Ultron |
|---|---|
| *"Ultron… set an alarm for 7:30 and tell Mom I'll be late"* | Sets the alarm and drafts the WhatsApp message to Mom |
| *"Call Dad"* / *"Dad-ku phone pannu"* | Places direct cellular or WhatsApp call based on location |
| *"What did Mom write?"* → *"Reply: I am on my way"* → *"Yes"* | Reads incoming chat, reads back your draft, and sends it |
| *"Play Believer on Spotify"* · *"Paatu stop pannu"* | Controls media playback and now-playing status |
| *"Volume to 50"* · *"Torch podu"* · *"Open Bluetooth"* | Executes instantly on-device with zero latency |
| *"Time enna aachu?"* / *"What time is it?"* | Reports exact time instantly |
| *"Weather epdi irukku tomorrow?"* / *"Weather in Chennai"* | Fetches real-time forecast from Open-Meteo |
| *"Create rest mode: silent, DND, lowest brightness"* | Automates device states into a custom callable mode |
| *"Remind me tomorrow at 9 to call the doctor"* | Creates a calendar event with an alarm reminder |

---

## 🧠 Architecture Flow

```mermaid
flowchart TD
    A([🎙️ Microphone]) --> B{Wake Word<br/><sub>Vosk · Offline</sub>}
    B -->|"Ultron"| C{Owner's Voice?<br/><sub>ONNX ResNet34</sub>}
    C -->|No| X([Ignored])
    C -->|Yes| D[Whisper<br/><sub>Groq API</sub>]
    D --> E{Command Router<br/><sub>English & Tanglish</sub>}
    E -->|Match| F[⚡ Direct Device Actions]
    E -->|Learned Cache| F
    E -->|Complex / Query| G[🧠 LLM Agent<br/><sub>Groq · 24 Tools</sub>]
    G -->|Tool Calls| F
    G -.->|Learns Pattern| E
    F --> H[🔊 Neural TTS<br/><sub>Edge Audio</sub>]
    G --> H
```

- **Offline First:** Common actions never ping the model, saving battery, tokens, and time.
- **Tool Honesty:** The agent may only act through tools and must accurately reflect execution outcomes.
- **Deep Red Aesthetic:** Complete UI themed in dark obsidian and crimson red, mirroring Ultron's visual signature.

---

## 📥 Installation & Setup

1. Download the latest **`Ultron.apk`** from [Releases](https://github.com/n1dlee/friday-mobile/releases/latest) and install it on your device.
2. Open **Settings** in Ultron:
   - Enter your free **Groq API key** ([console.groq.com/keys](https://console.groq.com/keys));
   - Download the offline voice model (~40 MB) and enable **Wake Word**;
   - Record your voice profile (8 calibration phrases, ~1 minute).
3. Grant permissions as prompted: Microphone, Overlay, Notifications, Contacts, and Location.
4. Say **"Ultron"** or **"Hey Ultron"**.

---

## 🔐 Privacy & Security

* **Zero recording before the wake word:** Keyword detection runs 100% locally and offline.
* **On-device biometrics:** Your voice profile is a 256-dimensional numerical vector stored exclusively on your device.
* **Transient chat data:** Conversations and notifications are processed in-memory and never sent to third-party databases.
* **Keystore encryption:** All user secrets and API keys are encrypted via Android Keystore.

---

## 🛠️ Tech Stack

| Component | Technology |
|---|---|
| **UI** | Kotlin 2.1, Jetpack Compose, Material 3, Dark Red / Obsidian Theme |
| **Architecture** | Clean Architecture, MVVM + StateFlow, Koin DI |
| **Local Storage** | Room Database, EncryptedSharedPreferences, WorkManager |
| **Wake Word Engine** | Vosk (`vosk-model-small-en-us-0.15`), grammar-constrained decoding |
| **Biometric Voice ID** | wespeaker ResNet34 ONNX Runtime, Kotlin Kaldi fbank |
| **STT** | Whisper large-v3-turbo via Groq |
| **LLM Agent** | Groq streaming tool execution (`openai/gpt-oss-120b` / `20b`) |
| **TTS** | Microsoft Edge Neural Voices (`en-US-ChristopherNeural` & `en-IN-PrabhatNeural`) |
| **Testing** | 88 test suites (600+ unit tests with JUnit, MockK, Turbine, Robolectric) |

---

## 🔨 Building The APK

### Option A: One-Click Build Script (Windows)
Double-click `build_apk.bat` in the project root:
```cmd
build_apk.bat
```
It auto-detects your JDK 17, launches Gradle, and outputs:
```
app\build\outputs\apk\debug\app-debug.apk
```

### Option B: Automated Cloud Build (GitHub Actions)
Push the code or fork this repository on GitHub. The pre-configured GitHub Actions workflow (`.github/workflows/build.yml`) builds the APK in ~2 minutes and makes `Ultron-Debug-APK` downloadable directly under the **Actions** tab.

### Option C: Command Line
Prerequisites: **JDK 17 or 21**, Android SDK 35.

```bash
git clone https://github.com/n1dlee/friday-mobile.git
cd friday-mobile
./gradlew assembleDebug          # APK generated in app/build/outputs/apk/debug/
./gradlew assembleRelease        # Minified with R8, in app/build/outputs/apk/release/
./gradlew testDebugUnitTest      # Run unit test suite
./gradlew detekt                 # Static code analysis
```

---

## 📄 License

Released under the [MIT License](LICENSE).

<div align="center">
<sub>Engineered with precision. All systems operational.</sub>
</div>
