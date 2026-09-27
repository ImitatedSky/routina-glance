package com.routina.glance.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Conversation::class, Message::class, MessageFts::class, RawEvent::class],
    version = 1,
    exportSchema = true
)
abstract class GlanceDatabase : RoomDatabase() {

    abstract fun dao(): GlanceDao

    companion object {
        fun create(context: Context): GlanceDatabase =
            Room.databaseBuilder(context, GlanceDatabase::class.java, "glance.db").build()
    }
}
