package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.GabbaiDao
import com.example.data.model.*

@Database(
    entities = [
        TransactionEntity::class,
        MemberEntity::class,
        AliyahEntity::class,
        KollelStudentEntity::class,
        KollelAttendanceEntity::class,
        TzedakahEntity::class,
        NeedyBeneficiaryEntity::class,
        SynagogueEventEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gabbaiDao(): GabbaiDao

    companion object {
        /** v2.4 -> v2.5: every money record remembers its currency; existing rows keep working as GEL. */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                listOf("transactions", "members", "aliyot", "kollel_students", "tzedakah_funds", "needy_beneficiaries", "synagogue_events").forEach { t ->
                    db.execSQL("ALTER TABLE `$t` ADD COLUMN `currency` TEXT NOT NULL DEFAULT 'GEL'")
                }
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gabbai_cosmos_database.db"
                ).addMigrations(MIGRATION_4_5).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
