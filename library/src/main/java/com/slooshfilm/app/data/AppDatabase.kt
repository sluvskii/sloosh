package com.slooshfilm.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.slooshfilm.app.data.model.Movie
import com.slooshfilm.app.data.models.WatchHistoryItem

@Database(entities = [Movie::class, WatchHistoryItem::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun movieDao(): MovieDao
    abstract fun watchHistoryDao(): WatchHistoryDao
}
