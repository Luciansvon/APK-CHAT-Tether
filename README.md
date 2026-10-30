# Tether 💌 — Private Couple Chat & Scrapbook App

Tether adalah aplikasi komunikasi privat khusus 1-on-1 untuk pasangan dengan estetika **Scrapbook & Polaroid Lo-Fi** dan konektivitas ganda: **Zero-Data P2P (Wi-Fi Direct)** saat berdekatan dan **E2EE WebSocket Relay** saat berjauhan.

---

## 🌟 Fitur Utama

1. **Scrapbook & Polaroid Feed**:
   - Foto otomatis dibungkus bingkai polaroid klasik dengan selotip washi tape dan catatan tulisan tangan (*handwritten script*).
   - Efek sapuan kabur (*Foggy Photo Reveal*) yang dapat diusap jari untuk membuka gambar.
2. **Konektivitas Dual-Mode ("Tether")**:
   - **P2P Zero-Data**: Komunikasi langsung via Wi-Fi Direct saat pasangan berada di ruangan yang sama tanpa kuota atau internet.
   - **E2EE Relay**: Sinkronisasi pesan berenkripsi ujung-ke-ujung (Signal Protocol Double Ratchet) saat berjauhan.
3. **Pesan Suara Kaset Bisik (*Whisper Voice Note*)**:
   - Tampilan kaset pita mini; audio hanya terdengar saat ponsel ditempelkan ke telinga memanfaatkan sensor proximity.
4. **Resonansi Sentuhan (*Digital Handhold / Haptic Resonance*)**:
   - Kanvas sentuh hening; motor getar berdenyut sinkron saat jari kedua pasangan menyentuh koordinat yang sama.
5. **Kupon Kasih Sayang (*Favor Tokens*)**:
   - Buku kupon dengan efek tiket sobek dan stempel `CLAIMED` miring merah.
6. **Kamera Analog Rol Bersama (*Dual-Shutter Disposable Roll*)**:
   - 1 rol film bersama berisi 24 jepretan per minggu; foto baru dicuci dan terbit otomatis setiap Minggu malam.

---

## 🛠️ Spesifikasi Teknis

- **Bahasa**: Kotlin (JVM 17)
- **UI Framework**: Jetpack Compose + Material3
- **Database**: Room Database + SQLCipher (`PRAGMA secure_delete = ON`)
- **Kriptografi**: AES-256-GCM, ECDH Curve25519, Android Keystore StrongBox
- **Min SDK**: API 26 (Android 8.0 Oreo)
- **Target SDK**: API 35 (Android 15)
- **Build System**: Gradle 8.7.2 + Kotlin 2.0.21

---

## 🚀 Memulai (Build & Run)

```bash
# Pastikan Android SDK dan JDK 17 terpasang
./gradlew testDebugUnitTest  # Menjalankan pengujian unit
./gradlew assembleDebug      # Menghasilkan APK Debug
```
