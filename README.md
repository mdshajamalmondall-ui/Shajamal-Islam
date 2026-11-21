# NEXORA AI

**Your Life. One Intelligence.** — *SMARTER LIVING STARTS HERE*

NEXORA AI is a bilingual (**বাংলা / English**) Android AI Super App built with **Kotlin**, **Jetpack Compose (Material 3)**, **Firebase AI Logic (Gemini)**, **Cloud Firestore**, and **Google Sign-In via Jetpack Credential Manager**. Designed as a single unified daily life partner from morning wake-up to bedtime, it supports **BDT (৳)** budgeting, voice interaction, multimodal vision, and permission-gated AI Agent workflows.

---

## Key Features

### Core Architecture & Navigation
- **5-Tab Adaptive Navigation**: `Home`, `Search`, `AI Brain`, `Tools`, and `Profile` (automatically switching between bottom `NavigationBar` on compact devices and `NavigationRail` on medium/expanded screens).
- **Bilingual Support (Bangla & English)**: Seamless runtime switching across UI strings, AI prompts, Voice Recognition (`bn-BD` / `en-US`), and Text-to-Speech.
- **Dark & Daylight Themes**: Custom Material 3 Obsidian/Cyan Neural theme and clean Daylight theme with DataStore persistence.

### 15 Core Life Modules
1. **Universal AI Search**: Text, photo, and screenshot Q&A with streaming Gemini responses.
2. **AI Voice Assistant**: On-device Android `SpeechRecognizer` & `TextToSpeech` integrated with NEXORA AI Brain.
3. **Smart Life Manager**: Tasks, daily routines, calendar events, birthdays, priorities, and AI Daily Planning.
4. **AI Health Assistant**: Medical report photo analysis, medicine reminders, checkup logs, first aid guide, and safety disclaimers.
5. **AI Education Center**: Personal AI Teacher, photo question solver, study notes summarizer, interactive MCQ / Model Tests, and Spoken English practice.
6. **Smart Finance Manager (BDT ৳)**: Income, expenses, bills, monthly budget tracking, savings goals, and AI spending analysis.
7. **Document AI**: PDF/TXT reader (`PdfRenderer`), image OCR, summaries, Bangla↔English translation, and CV/application drafting.
8. **AI Translator**: Bangla↔English & multi-language text, voice, and photo translation with grammar/pronunciation tips.
9. **Emergency & Safety Center**: One-tap emergency dial (`999` / `16263`), GPS location sharing, nearby hospital lookup, and emergency/blood donor contacts.
10. **AI Vision**: Object identification, text reading, food/ingredient analysis, and visual problem solving.
11. **AI Shopping Assistant**: Product comparisons, BDT budget recommendations, and persistent shopping lists.
12. **AI Travel Planner**: Structured day-by-day tour plans, BDT cost estimates, hotel/transport tips, checklists, and Google Maps intents.
13. **Smart Communication**: Email drafting, social captions, grammar fixes, tone rewriting, and system share integration.
14. **Smart Home Controller**: Device power/level controls (Lights, AC, Fan, Lock, Plug), automation rules, and AI routine suggestions.
15. **Personal Memory**: View, pin, add, and delete user memories that personalize AI responses.

### 10 Unique Intelligence Capabilities
- **AI Agent Mode**: Proposes structured tasks, BDT transactions, and memories from natural language—always requiring explicit user confirmation before executing.
- **Ask Anything About Screen**: Upload screenshots or photos directly from Search for instant context breakdown.
- **Morning Intelligence**: Cached daily AI briefing combining pending tasks, medicines, and memories.
- **Offline Smart Mode**: Offline scratchpad and cached briefing via Jetpack DataStore + Firestore offline cache.
- **Multi-AI Expert Switcher**: Switch between 6 specialized personas (`NEXORA Core`, `Health Info`, `AI Teacher`, `Finance Advisor`, `Career Expert`, `Travel Planner`).
- **Predictive AI**: Proactive next-step recommendations inside Digital Twin & Morning Intelligence.
- **Family AI Space**: Invite-code-based shared family workspaces for calendars, shopping lists, and reminders.
- **Adaptive Dashboard**: Automatically orders quick-access modules by usage frequency.
- **Meeting & Lecture Intelligence**: Voice/text transcripts converted into executive summaries, notes, and action items.
- **Personal Digital Twin & Data Control**: Subscription tier previews (`FREE` to `BUSINESS`) and one-tap permanent personal data wipe.

---

## Tech Stack

- **UI**: Jetpack Compose, Material 3, Navigation Compose, Coil
- **Language & Async**: Kotlin (`2.4.x`), Coroutines & StateFlow
- **AI**: Firebase AI Logic (`firebase-ai` / Gemini `gemini-3.8-flash` & `gemini-3.5-flash-lite`) + Firebase App Check (Debug / reCAPTCHA)
- **Backend & Auth**: Cloud Firestore (`firestore.rules` + offline persistence), Firebase Auth with Jetpack Credential Manager (`GetSignInWithGoogleOption`)
- **Local Storage & Device APIs**: Jetpack DataStore Preferences, Android `SpeechRecognizer`, `TextToSpeech`, `PdfRenderer`, Photo Picker (`PickVisualMedia`), Google Play Services Location
- **Build System**: Android Gradle Plugin `9.1.1`, `compileSdk 36`, `targetSdk 36`, `minSdk 24`, Java `11`

---

## Project Structure

```text
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/example/
│       │   ├── MainActivity.kt
│       │   ├── data/model/          # Firestore & AI structured models
│       │   ├── data/repository/     # Core, Modules, Preferences, GeminiAiService
│       │   ├── firebase/            # AppCheckHelper, AuthManager, FirestoreErrorHandler
│       │   ├── localization/        # Bilingual BN/EN strings
│       │   ├── ui/components/       # Reusable M3 & glowing orb components
│       │   ├── ui/navigation/       # Adaptive NavHost & auth gating
│       │   ├── ui/screens/          # Core tabs & 15+ module screens
│       │   ├── ui/theme/            # Color, Typography, Theme
│       │   └── ui/viewmodel/        # NexoraMainViewModel, AiBrainViewModel
│       └── res/                     # Adaptive launcher icons, fonts, drawables
├── firebase-blueprint.json          # Firestore data schema blueprint
├── firestore.rules                  # Zero-trust Firestore security rules
├── firestore.test.js                # Node emulator security rules test suite
└── .github/                         # CI workflow, issue & PR templates
```

---

## Getting Started (Local Build)

### 1. Prerequisites
- Android Studio (Meerkat / Narwhal or newer) with JDK 17+
- Android SDK Platform 36
- Firebase Project with **Authentication (Google Sign-In)**, **Cloud Firestore**, **Firebase AI Logic**, and **App Check** enabled

### 2. Firebase Configuration
1. Download `google-services.json` for your Android package (`com.aistudio.nexoraai.qxvmkp`) and place it in `app/google-services.json` (ensure your debug/release SHA-1 and SHA-256 fingerprints are registered in Firebase Console).
2. Ensure `app/src/main/res/values/firebase_applet_config.xml` contains your Firestore database ID (`firestore_database_id`) and reCAPTCHA site key (`recaptcha_site_key`).
3. Copy `.env.example` to `.env` if you configure optional Secrets Gradle Plugin properties.

### 3. Build & Test
```bash
# Build Debug APK (uses Gradle 9.3.1)
gradle :app:assembleDebug

# Run JVM & Robolectric unit tests
gradle :app:testDebugUnitTest

# Run Firestore security rules tests (requires Firebase CLI & Node.js)
firebase emulators:exec --only firestore "node --test firestore.test.js"
```

---

## Security & Privacy Notes
- Never commit `.env`, `*.jks`, `debug.keystore`, or production signing secrets.
- Every Firestore document and shared `family_spaces` collection is strictly validated by `firestore.rules` and gated behind Google Sign-In.
- AI Health and Smart Home screens include explicit medical and hardware-compatibility notices.

## License
Distributed under the MIT License. See [LICENSE](LICENSE) for details.
