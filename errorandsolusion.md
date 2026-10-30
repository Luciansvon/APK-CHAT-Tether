# Catatan Error dan Solusi Proyek Tether

Dokumen ini mencatat log kesalahan dan solusi praktis selama implementasi kode:

## 1. Setup Awal & Inisialisasi Arsitektur
* **Error**: Direct image gen melalui endpoint eksternal gagal karena saldo/kredit atau 401 unauthenticated.
* **Solusi**: Integrasi langsung dengan instance lokal 9Router (`http://localhost:20128/v1`) menggunakan API key `sk-<REDACTED_LOCAL_KEY>` yang tersimpan di basis data router lokal.
* **Error**: Pembatasan modifikasi direktori di luar ruang kerja selama fase perencanaan (Plan Mode).
* **Solusi**: Menyimpan cetak biru sementara di direktori plan sebelum pengguna memberikan otorisasi untuk memulai coding loop implementasi di workspace project.

## 2. Jaringan & Konektivitas P2P (Wi-Fi Direct)
* **Error Potensial**: Benturan pengurutan pesan antara peer saat berganti dari mode offline P2P ke online relay.
* **Solusi**: Menggunakan Lamport Timestamps + UUID deterministik dan tabel idempotensi database Room (`OnConflictStrategy.IGNORE`) agar duplikasi pesan tereliminasi secara otomatis.
