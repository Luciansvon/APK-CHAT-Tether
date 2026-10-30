# SQLCipher ProGuard Rules
-keep class net.zetetic.database.sqlcipher.** { *; }
-dontwarn net.zetetic.database.sqlcipher.**

# ZXing Rules
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# Room Rules & Database Entities
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# Network & Pairing Serialization Models
-keep class com.tether.app.core.network.model.TetherPacket { *; }
-keep class com.tether.app.core.pairing.PairingPayload { *; }
