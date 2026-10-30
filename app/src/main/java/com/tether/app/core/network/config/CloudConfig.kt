package com.tether.app.core.network.config

object CloudConfig {
    // 1. Firebase Project Endpoint (sama untuk kedua perangkat)
    var FIREBASE_API_KEY: String = ""
    var FIREBASE_DATABASE_URL: String = "" // contoh: "https://project-id-default-rtdb.asia-southeast1.firebasedatabase.app"

    // 2. Akun Khusus (Zero-UI: login berjalan di latar belakang tanpa form login di aplikasi)
    // Akun 1 (Untuk HP kamu)
    var USER_1_EMAIL: String = ""
    var USER_1_PASSWORD: String = ""

    // Akun 2 (Untuk HP pasanganmu)
    var USER_2_EMAIL: String = ""
    var USER_2_PASSWORD: String = ""

    // Set true saat compile untuk HP kamu, false saat compile untuk HP pasangan
    var IS_PRIMARY_USER: Boolean = true

    // Fallback jika ingin langsung mengisi single email/password
    var FIREBASE_EMAIL: String = ""
    var FIREBASE_PASSWORD: String = ""

    val ACTIVE_EMAIL: String
        get() = when {
            FIREBASE_EMAIL.isNotBlank() -> FIREBASE_EMAIL
            IS_PRIMARY_USER -> USER_1_EMAIL
            else -> USER_2_EMAIL
        }

    val ACTIVE_PASSWORD: String
        get() = when {
            FIREBASE_PASSWORD.isNotBlank() -> FIREBASE_PASSWORD
            IS_PRIMARY_USER -> USER_1_PASSWORD
            else -> USER_2_PASSWORD
        }

    // 3. Google Apps Script Web App URL untuk Drive 5 TB
    var DRIVE_UPLOAD_URL: String = "" // contoh: "https://script.google.com/macros/s/AKfycb.../exec"

    fun isFirebaseConfigured(): Boolean =
        FIREBASE_API_KEY.isNotBlank() &&
        FIREBASE_DATABASE_URL.isNotBlank() &&
        FIREBASE_DATABASE_URL.startsWith("https://") &&
        ACTIVE_EMAIL.isNotBlank() &&
        ACTIVE_PASSWORD.isNotBlank()

    fun isDriveConfigured(): Boolean =
        DRIVE_UPLOAD_URL.isNotBlank() && DRIVE_UPLOAD_URL.startsWith("https://script.google.com")
}
