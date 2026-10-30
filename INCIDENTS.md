# Project Incidents: Tether

Dokumen ini mencatat setiap kendala teknis, akar masalah, dan solusi yang ditemukan selama pengembangan aplikasi Tether.

| ID | Tanggal | Komponen | Deskripsi Kendala | Akar Masalah | Solusi | Status |
|---|---|---|---|---|---|---|
| INC-001 | 2026-10-30 | UI / Compose | Overdraw & frame drop pada rotasi kartu Scrapbook | `Modifier.rotate()` memicu rekomposisi penuh | Ganti dengan `Modifier.graphicsLayer { rotationZ = ... }` untuk transformasi di GPU | RESOLVED |
| INC-002 | 2026-10-30 | P2P Network | P2P failure pada Android 13+ tanpa GPS | Android 13+ mensyaratkan izin `NEARBY_WIFI_DEVICES` | Tambahkan deklarasi izin `NEARBY_WIFI_DEVICES` dengan `usesPermissionFlags="neverForLocation"` | RESOLVED |
| INC-003 | 2026-10-30 | Database | Residu data pesan sensitif di file database SQLite | SQLite standar tidak mengosongkan deleted pages | Terapkan `PRAGMA secure_delete = ON` dan `PRAGMA auto_vacuum = FULL` pada SQLCipher | RESOLVED |
