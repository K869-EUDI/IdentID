# IdentID

IdentID — Android EUDI wallet and identity manager

Overview

- IdentID is an Android app (Kotlin + Jetpack Compose) implementing an EUDI wallet core and local storage for identity data. It provides secure wallet features, onboarding, and local DB storage for credentials.

Key technologies

- **Language:** Kotlin
- **UI:** Jetpack Compose
- **Architecture:** Android app module (`app/`), modular Gradle build (Kotlin DSL)
- **Storage:** Local database (Room / generated schemas present under `app/schemas`)
- **EUDI Wallet Core:** integrated wallet/core functionality for EUDI operations
- **Build system:** Gradle

Getting started
Prerequisites:

- Android Studio (latest stable) or command-line Android SDK
- JDK 11 or newer
- Android SDK and platform tools configured in `local.properties`

Build and run (from project root):

```bash
./gradlew assembleDebug
./gradlew installDebug
```

Open the project in Android Studio and run the `app` module on a device/emulator.

Configuration

- Secrets defaults are in `secrets.defaults.properties` — copy or override with your own secure values as needed.
- `local.properties` should point to your Android SDK installation.

Project layout (high level)

- `app/` — main Android module
- `app/src/main` — source code and resources
- `app/schemas` — generated DB schema artifacts
- `build.gradle.kts`, `settings.gradle.kts` — top-level Gradle/Kotlin DSL config

License

- See the `LICENSE` file in the repository root for license details.

Contact

- Repo: K689-EUDI/IdentID
