# Laporan Audit & REA Performa: Telegram vs WhatsApp untuk Optimasi Tether

**Tanggal**: 30 Oktober 2026  
**Target Target Emulator**: `emulator-5554` (Android API 35, x86_64)  
**Paket yang Dianalisis**:
1. `org.telegram.messenger` (v13.1.1 / Build 72371)
2. `com.whatsapp` (v2.26.42.74 / Build 264207400)
3. Aplikasi Rujukan: **Tether** (`com.tether.app`)

---

## 1. Ringkasan Eksekutif & Komparasi Metrik Runtime

Audit runtime dan inspeksi proses via REA/ADB pada emulator menghasilkan perbandingan metrik sebagai berikut:

| Parameter | Telegram | WhatsApp | Tether (Kondisi Saat Ini) |
|---|---|---|---|
| **Total PSS Memori** | ~92.7 MB | ~203.4 MB | ~110 - 240 MB (berfluktuasi) |
| **Java (Dalvik) Heap** | **7.9 MB** (sangat rendah) | **22.6 MB** | ~35 - 90 MB (rentan spike GC) |
| **Native Heap** | 31.5 MB (`libtmessages.49.so`) | 33.1 MB (`libs.so`, Superpack) | ~15 - 25 MB |
| **Jumlah View Aktif** | **36 Views** (arsitektur kanvas/custom) | 82 Views | Seluruh tree Jetpack Compose |
| **Mode Journal Database** | SQLite kustom C++ (WAL) | `WAL` + dedicated `db_write` | SQLCipher Rollback Journal (`auto_vacuum = FULL`) |
| **Penanganan Kriptografi** | MTProto background queue | Dedicated `SignalExecutor` thread | Coroutine `viewModelScope` umum |
| **Payload Media Wire** | Micro-blur preview (byte kecil) + stream | Chunked transfer + `DiskCache` | Fallback Base64 utuh di DB & JSON |

---

## 2. Bedah Cara Kerja & Arsitektur Internal

### A. Telegram (`org.telegram.messenger`)
1. **Offloading Beban ke Native C++**:
   - Menggunakan library monolithic `libtmessages.49.so` (23.8 MB). 
   - Manipulasi buffer, framing paket MTProto, decoding gambar/stiker lottie, dan enkripsi dikerjakan di native heap. Hal ini menjaga Java Heap di bawah 10 MB sehingga aplikasi hampir tidak pernah mengalami jeda Garbage Collection (GC pause / frame drop).
2. **Partitioned Thread Queues**:
   - Telegram tidak memakai thread pool acak. Mereka memiliki thread spesifik:
     - `storageQueue_0..3`: Antrean I/O disk dan SQLite yang terpartisi.
     - `thumbGenerating` & `cacheThumbOutQu`: Pembuatan thumbnail resolusi sangat rendah secara asinkron.
     - `imageLoadQueue` & `fileUploadQueue`: Pemisahan jalur unduh dan unggah.
3. **Penyematan Micro-Blur Thumbnail**:
   - Gambar tidak pernah dikirim mentah di teks obrolan. Telegram menyematkan thumbnail mini 16x16 atau 20x20 pixel langsung pada metadata pesan. UI langsung menampilkan thumbnail buram secara instan saat pesan tiba tanpa menunggu file asli selesai diunduh.

### B. WhatsApp (`com.whatsapp`)
1. **Dedicated Concurrency Pipeline**:
   - **`SignalExecutor`**: Semua pembaruan status Double Ratchet, rotasi ephemeral key, dan komputasi ECDH dijalankan di satu thread terisolasi. Ini mencegah *race condition* antar paket yang datang bersamaan.
   - **`db_write` & `AsyncCommitThread`**: WhatsApp menerapkan pola *Single Writer, Multiple Readers*. Penulisan SQLite disalurkan lewat satu antrean sekuensial, sementara pembacaan pesan berjalan paralel di background worker tanpa memblokir thread UI.
2. **Receipt & Delivery State Machine**:
   - Memiliki thread `ReceiptProcessing`. Status pengiriman dipisahkan menjadi 3 fase terverifikasi:
     - *Sent*: Paket berhasil terkirim ke server/relay.
     - *Delivered*: ACK diterima dari perangkat tujuan (centang dua abu-abu).
     - *Read*: ACK diterima saat partner membuka thread chat (centang biru).
3. **Database Caching (`axolotl.db` & `msgstore.db`)**:
   - Database pre-key dan sesi Signal Protocol disimpan di `axolotl.db` terpisah dari database riwayat pesan `msgstore.db`. Cache hit `axolotl.db` mencapai 1.694 hits saat cold start berkat optimasi SQLite pager.

---

## 3. Titik Lemah Arsitektur Tether Saat Ini

1. **Query Kloning Seluruh Riwayat Pesan (`MessageDao`)**:
   - `getAllMessagesFlow()` menjalankan `SELECT * FROM messages ORDER BY ...`.
   - Setiap kali ada 1 pesan masuk atau status pengiriman berubah, seluruh baris pesan di database di-load ulang ke memori menjadi `List<MessageEntity>`. Pada 10.000 pesan, ini akan menimbulkan lag parah dan resiko Out Of Memory (OOM).
2. **Konfigurasi Pragma SQLCipher Menghambat I/O Disk**:
   - `PRAGMA auto_vacuum = FULL;` tanpa `PRAGMA journal_mode = WAL;`.
   - SQLite default (Rollback Journal) mengunci database secara eksklusif selama transaksi tulis, menyebabkan pembacaan UI tersendat (jank). `auto_vacuum = FULL` memperparah fragmentasi dan memperlambat penghapusan data.
3. **Payload Media Raksasa (Base64 Injection)**:
   - Jika Google Drive tidak terkonfigurasi, file foto di-encode ke Base64 dan disimpan langsung di `MessageEntity.content` serta `TetherPacket`.
   - String Base64 berukuran ratusan kilobyte membebani SQLite, string pool Android runtime, dan transmisi P2P/WebSocket.
4. **Batas Cache Bitmap Tidak Memperhitungkan Memori Nyata**:
   - `BitmapLoader.memoryCache` menggunakan batas statis 32 entri. Tiga puluh dua gambar berukuran 512x512 piksel ARGB_8888 memakan puluhan megabyte memori tanpa memperhitungkan batas RAM perangkat kelas bawah.

---

## 4. Rekomendasi Adaptasi & Optimasi untuk Tether

### Prioritas 1: Database & Disk I/O (Dampak Tinggi, Biaya Rendah)
- **Aktifkan WAL Mode di `TetherDatabase`**:
  ```kotlin
  db.query("PRAGMA journal_mode = WAL;").close()
  db.query("PRAGMA synchronous = NORMAL;").close()
  ```
  *Manfaat*: Pembacaan pesan di UI tidak pernah terblokir oleh penulisan pesan masuk atau pembaruan status.
- **Limitasi Kueri Pesan (Windowing / Paging)**:
  Tambahkan fungsi kueri terbatas di `MessageDao`:
  ```kotlin
  @Query("SELECT * FROM messages ORDER BY lamportTimestamp DESC, localTimestamp DESC LIMIT :limit")
  fun getRecentMessagesFlow(limit: Int = 100): Flow<List<MessageEntity>>
  ```
  Hanya render 50-100 pesan terkini ke memori, muat riwayat lama hanya saat pengguna menggulir ke atas.

### Prioritas 2: Media Pipeline & Memory Footprint (Dampak Signifikan)
- **Hapus Base64 dari Database**:
  Simpan gambar selalu sebagai file di direktori aplikasi internal (`context.filesDir/polaroids/`). Kolom `content` di database hanya boleh menyimpan path file lokal (`/data/user/0/.../polaroid_xxx.jpg`).
- **Adaptasi Micro-Blur Placeholder Telegram**:
  Buat thumbnail mini 16x16 atau 24x24 saat foto diambil, simpan representasi string mini (BlurHash atau Base64 mini < 100 bytes) di `MessageEntity.caption` / kolom khusus. Tampilkan ini seketika di canvas Polaroid selagi file utama disinkronkan.
- **Dynamic Memory Cache pada `BitmapLoader`**:
  Ubah ukuran `LruCache` agar berbasis kilobyte aktual:
  ```kotlin
  val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024).toInt()
  val cacheSizeKb = maxMemoryKb / 8 // Alokasikan 1/8 RAM bebas
  ```

### Prioritas 3: Concurrency & Pengiriman Pesan (Keandalan Tinggi)
- **Adaptasi `SignalExecutor` WhatsApp**:
  Buat coroutine dispatcher tunggal untuk operasi kriptografi pada `SessionCryptoEngine`:
  ```kotlin
  val CryptoDispatcher = Dispatchers.IO.limitedParallelism(1)
  ```
  Semua proses enkripsi/dekripsi dan derivasi kunci berjalan di antrean sekuensial yang aman dari race condition.
- **Protokol ACK Dua Arah**:
  Kirim paket ringan bertipe `DELIVERED_ACK` saat pesan diterima oleh peer. Saat pengirim menerima ACK ini, perbarui `isDelivered = true` di database.
