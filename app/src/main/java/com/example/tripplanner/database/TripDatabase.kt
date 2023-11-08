package com.example.tripplanner.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [TripEntity::class], version = 1, exportSchema = false)
abstract class TripDatabase: RoomDatabase() {
    abstract val tripDatabaseDao: TripDatabaseDao
    companion object {
        @Volatile
        private var INSTANCE: TripDatabase? = null
        fun getInstance(context: Context): TripDatabase {
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        TripDatabase::class.java, "trip_db"
                    ).allowMainThreadQueries().build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}