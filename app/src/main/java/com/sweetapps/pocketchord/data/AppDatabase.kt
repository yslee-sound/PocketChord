package com.sweetapps.pocketchord.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@TypeConverters(Converters::class)
@Database(entities = [ChordEntity::class, VariantEntity::class], version = 4)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chordDao(): ChordDao

    companion object {
        private const val DB_NAME = "pocket_chord.db"
        @Volatile private var instance: AppDatabase? = null

        // Migration 3 -> 4: add barresJson column to variants
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE variants ADD COLUMN barresJson TEXT")
            }
        }

        // Migration 1 -> 2:
        // 1) normalize NULL fingersCsv to empty string
        // 2) delete duplicate variants keeping the lowest id per (chordId, positionsCsv, fingersCsv)
        // 3) create a unique index on (chordId, positionsCsv, fingersCsv)
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // normalize null fingers
                db.execSQL("UPDATE variants SET fingersCsv = '' WHERE fingersCsv IS NULL")

                // Count duplicates for logging before removal
                try {
                    val cursor = db.query("SELECT SUM(cnt-1) as toDelete FROM (SELECT COUNT(*) AS cnt FROM variants GROUP BY chordId, positionsCsv, fingersCsv HAVING cnt>1)")
                    var toDelete = 0L
                    if (cursor != null) {
                        if (cursor.moveToFirst()) {
                            toDelete = if (!cursor.isNull(0)) cursor.getLong(0) else 0L
                        }
                        cursor.close()
                    }
                    Log.i("Migration1_2", "Duplicate variant rows to remove: $toDelete")
                } catch (t: Throwable) {
                    Log.w("Migration1_2", "Failed to count duplicate variants before cleanup", t)
                }

                // delete duplicates: keep smallest id per (chordId, positionsCsv, fingersCsv)
                db.execSQL(
                    "DELETE FROM variants WHERE id NOT IN (SELECT MIN(id) FROM variants GROUP BY chordId, positionsCsv, fingersCsv)"
                )

                // create unique index (if not exists) to enforce uniqueness going forward
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_variants_chord_positions_fingers ON variants (chordId, positionsCsv, fingersCsv)"
                )
            }
        }

        // Migration 2 -> 3: add seedOrder column to chords table
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add nullable integer column seedOrder; default NULL
                db.execSQL("ALTER TABLE chords ADD COLUMN seedOrder INTEGER")
            }
        }

        fun getInstance(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: buildDatabase(context).also { instance = it }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DB_NAME)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
        }
    }
}
