package com.tether.app.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tether.app.core.database.dao.CouponDao
import com.tether.app.core.database.dao.MessageDao
import com.tether.app.core.database.dao.PartnerProfileDao
import com.tether.app.core.database.entity.CouponEntity
import com.tether.app.core.database.entity.MessageEntity
import com.tether.app.core.database.entity.PartnerProfileEntity
import com.tether.app.core.security.KeystoreManager
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [
        MessageEntity::class,
        CouponEntity::class,
        PartnerProfileEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class TetherDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun couponDao(): CouponDao
    abstract fun partnerProfileDao(): PartnerProfileDao

    companion object {
        @Volatile
        private var INSTANCE: TetherDatabase? = null

        fun getInstance(context: Context): TetherDatabase {
            return INSTANCE ?: synchronized(this) {
                val passphrase = KeystoreManager.getOrCreateDatabasePassphrase(context.applicationContext)
                val factory = SupportOpenHelperFactory(passphrase)

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TetherDatabase::class.java,
                    "tether_enc.db"
                )
                    .openHelperFactory(factory)
                    .addCallback(object : Callback() {
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            try {
                                db.query("PRAGMA secure_delete = ON;").close()
                            } catch (_: Exception) {}
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
