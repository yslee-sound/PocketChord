package com.sweetapps.pocketchord.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ChordEntity::class, VariantEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chordDao(): ChordDao

    companion object {
        private const val DB_NAME = "pocket_chord.db"
        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: buildDatabase(context).also { instance = it }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DB_NAME)
                .addTypeConverter(Converters)
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}

