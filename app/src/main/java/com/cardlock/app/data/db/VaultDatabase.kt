package com.cardlock.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.cardlock.app.data.model.CardEntity

@Database(entities = [CardEntity::class], version = 1, exportSchema = false)
abstract class VaultDatabase : RoomDatabase() {

    abstract fun cardDao(): CardDao

    companion object {
        @Volatile
        private var INSTANCE: VaultDatabase? = null

        fun getDatabase(context: Context): VaultDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VaultDatabase::class.java,
                    "cardlock_vault.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
