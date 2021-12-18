package com.example.calculatorapp

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.calculatorapp.dao.HistoryDao
import com.example.calculatorapp.model.History

@Database(entities = [History::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
}