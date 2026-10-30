package com.tether.app.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tether.app.core.database.dao.CoupleLevelDao
import com.tether.app.core.database.dao.CouponDao
import com.tether.app.core.database.dao.MessageDao
import com.tether.app.core.database.dao.PartnerProfileDao
import com.tether.app.core.database.entity.CoupleLevelEntity
import com.tether.app.core.database.entity.CouponEntity
import com.tether.app.core.database.entity.MessageEntity
import com.tether.app.core.database.entity.PartnerProfileEntity
import com.tether.app.core.security.KeystoreManager
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [
        MessageEntity::class,
        CouponEntity::class,
        PartnerProfileEntity::class,
        CoupleLevelEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class TetherDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun couponDao(): CouponDao
    abstract fun partnerProfileDao(): PartnerProfileDao
    abstract fun coupleLevelDao(): CoupleLevelDao

    companion object {
        @Volatile
        private var INSTANCE: TetherDatabase? = null

        fun getInstance(context: Context): TetherDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: run {
                    val passphrase = KeystoreManager.getOrCreateDatabasePassphrase(context.applicationContext)
                    // clearPassphrase = true agar array byte passphrase langsung di-zero setelah koneksi dibuka
                    val factory = SupportOpenHelperFactory(passphrase, null, true)

                    val migration2to3 = object : androidx.room.migration.Migration(2, 3) {
                        override fun migrate(db: SupportSQLiteDatabase) {
                            db.execSQL(
                                """
                                CREATE TABLE IF NOT EXISTS `couple_level` (
                                    `id` INTEGER NOT NULL,
                                    `currentXp` INTEGER NOT NULL,
                                    `currentLevel` INTEGER NOT NULL,
                                    `totalMessagesSent` INTEGER NOT NULL,
                                    `totalTugsSent` INTEGER NOT NULL,
                                    `totalCouponsClaimed` INTEGER NOT NULL,
                                    `lastInteractionTimestamp` INTEGER NOT NULL,
                                    PRIMARY KEY(`id`)
                                )
                                """.trimIndent()
                            )
                        }
                    }

                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        TetherDatabase::class.java,
                        "tether_enc.db"
                    )
                        .openHelperFactory(factory)
                        .addMigrations(migration2to3)
                        .addCallback(object : Callback() {
                            override fun onCreate(db: SupportSQLiteDatabase) {
                                super.onCreate(db)
                                applyHardeningPragmas(db)
                            }
                            override fun onOpen(db: SupportSQLiteDatabase) {
                                super.onOpen(db)
                                applyHardeningPragmas(db)
                            }
                        })
                        .fallbackToDestructiveMigrationOnDowngrade()
                        .build()

                    INSTANCE = instance
                    instance
                }
            }
        }

        private fun applyHardeningPragmas(db: SupportSQLiteDatabase) {
            try {
                db.query("PRAGMA secure_delete = ON;").close()
                db.query("PRAGMA auto_vacuum = FULL;").close()
            } catch (_: Exception) {}
        }
    }
}
