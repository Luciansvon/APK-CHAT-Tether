# SQLCipher ProGuard Rules
-keep class net.zetetic.database.sqlcipher.** { *; }
-dontwarn net.zetetic.database.sqlcipher.**

# ZXing Rules
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# Room Rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
