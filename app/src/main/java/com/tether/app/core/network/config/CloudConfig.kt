package com.tether.app.core.network.config

object CloudConfig {
    // 1. Firebase Configuration (ambil dari Firebase Console > Project Settings > Web App)
    var FIREBASE_API_KEY: String = ""
    var FIREBASE_DATABASE_URL: String = "" // contoh: "https://project-id-default-rtdb.asia-southeast1.firebasedatabase.app"

    // Akun email & password yang dibuat di Firebase Auth khusus kalian berdua
    var FIREBASE_EMAIL: String = ""
    var FIREBASE_PASSWORD: String = ""

    // 2. Google Apps Script Web App URL untuk Drive 5 TB
    var DRIVE_UPLOAD_URL: String = "" // contoh: "https://script.google.com/macros/s/AKfycb.../exec"

    fun isFirebaseConfigured(): Boolean =
        FIREBASE_API_KEY.isNotBlank() && FIREBASE_DATABASE_URL.isNotBlank() && FIREBASE_DATABASE_URL.startsWith("https://")

    fun isDriveConfigured(): Boolean =
        DRIVE_UPLOAD_URL.isNotBlank() && DRIVE_UPLOAD_URL.startsWith("https://script.google.com")
}
