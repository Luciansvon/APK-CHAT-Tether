# Project Architecture: Tether (Private Couple App)

## Project identity

- Name: Tether
- Repository: https://github.com/Luciansvon/APK-CHAT-Tether
- Type: Native Android Mobile Application
- Platforms: Android 8.0+ (API 26 s/d API 35)
- Primary runtime/toolchain: Kotlin 2.0.21, Jetpack Compose, JDK 17, Android Gradle Plugin 8.7.2

## System overview

```text
[Touchpad / Audio / Photo / Text Input]
                  │
                  ▼
         [E2EE Crypto Engine] (Signal Protocol / Double Ratchet + Keystore)
                  │
         ┌────────┴────────┐
         ▼                 ▼
   [P2P Wi-Fi Direct]  [WebSocket Relay]
   (Zero-Data Offline) (Cloud Fallback)
         │                 │
         └────────┬────────┘
                  ▼
         [Decryption & Anti-Tamper]
                  │
                  ▼
     [Encrypted Room Database] (SQLCipher PRAGMA secure_delete)
                  │
                  ▼
    [Scrapbook & Polaroid Lo-Fi UI] (Jetpack Compose with GPU Layers)
```

## Major components

| Component | Responsibility | Technology | Notes |
|---|---|---|---|
| `core:crypto` | Manajemen kunci privat, Double Ratchet, X3DH, dan proteksi Android Keystore | Java Cryptography Architecture (JCA), AES-GCM, ECDH Curve25519 | Zero-knowledge to server |
| `core:database` | Penyimpanan pesan, media frame, kupon, dan status sesi | Room Database + SQLCipher | `PRAGMA secure_delete = ON` |
| `core:network:p2p` | Konektivitas langsung tanpa kuota saat berdekatan | Wi-Fi Direct (P2P WiFiManager) + NSD (mDNS) | Zero-Data Mode |
| `core:network:relay` | Fallback komunikasi saat pasangan berjauhan | OkHttp WebSocket + Protocol Buffers / JSON | E2EE payload |
| `core:media` | Perekaman & pemutaran audio bisik serta haptik sinkron | Android AudioRecord, AudioTrack, VibrationEffect, SensorManager | Proximity sensor routing |
| `core:designsystem` | Desain tema Scrapbook, komponen Polaroid, washi tape, kupon sobek | Jetpack Compose, Material3 | Hardware-accelerated graphics layer |
| `feature:chat` | Feed timeline chat dan scrapbook kenangan | Jetpack Compose LazyColumn, GraphicsLayer | Anti-jank cached rendering |
| `feature:pairing` | Inisialisasi pairing offline dan pertukaran pre-key | CameraX + ZXing QR scanner & generator | Tanpa registrasi akun publik |

## External dependencies

- APIs: Custom lightweight E2EE WebSocket Relay (opsional saat remote)
- Services: Tidak ada layanan pihak ketiga berbayar / analitik pelacak (Zero Tracking)
- Hardware: Kamera (QR & Polaroid), Mikrofon (Whisper audio), Haptic Motor (Touch resonance), Sensor Proximity (Ear playback)
- Storage: SQLCipher Encrypted SQLite internal app sandbox
- Models/assets: Font lokal (`Caveat`, `Patrick Hand`, `Plus Jakarta Sans`), Vector drawables

## Build / test / release interface

- Build command: `./gradlew assembleDebug` (atau `gradlew.bat assembleDebug`)
- Unit test command: `./gradlew testDebugUnitTest`
- Integration test command: `./gradlew connectedCheck`
- Benchmark command: `./gradlew macrobenchmark`
- Artifact path: `app/build/outputs/apk/debug/app-debug.apk`
- Runner requirements: JDK 17, Android SDK Platform 35, Build-Tools 35.0.0

## Constraints

- Hardware: Memerlukan sensor getar (haptics) dan proximity untuk pengalaman penuh
- OS: Android 8.0 (Oreo / API 26) ke atas
- Memory: RAM minimal 2GB; memory footprint dijaga di bawah 120MB
- Network: Berjalan 100% offline via Wi-Fi P2P maupun online via Internet
- Security: `FLAG_SECURE` pada window sensitif, enkripsi database at-rest, E2EE in-transit
- Offline requirements: Dapat melakukan pairing dan chatting lengkap tanpa koneksi internet sama sekali

## B.I.M.A-DEV-INFRA integration

Shared workflows used:
- Repository audit & hygiene
- Fail-closed artifact identity contract
- Shared test & build verification

Project-specific adapters/config:
- GitHub Actions CI matrix: `ubuntu-24.04` & `windows-2025`
- Gradle wrapper caching

## Architecture decisions

- ADR-0001: Adopsi Jetpack Compose dengan `Modifier.graphicsLayer` untuk render estetika Scrapbook tanpa jank.
- ADR-0002: Arsitektur konektivitas ganda (P2P Wi-Fi Direct primer + WebSocket relay sekunder) dengan sinkronisasi Lamport Timestamps.
- ADR-0003: Model otentikasi berbasis QR-Code Ephemeral Key Pair tanpa nomor telepon / akun publik.
