package com.example.calculatorapp.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.calculatorapp.model.History

@Dao
interface HistoryDao{
    @Query("SELECT * FROM history")
    fun getAll(): List<History>

    @Insert
    fun insertHistory(history : History)

    @Query("DELETE FROM history")
    fun deleteAll()

    @Delete
    fun delete(history : History)

    @Query("SELECT * From history WHERE result LIKE :result") // :result는 매개변수로 전달된 값을 의미
    fun findByResult(result: String): List<History>
}