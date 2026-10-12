package com.cardlock.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cardlock.app.data.model.CardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {

    @Query("SELECT * FROM cards ORDER BY updatedAt DESC")
    fun getAllCardsFlow(): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards ORDER BY updatedAt DESC")
    suspend fun getAllCards(): List<CardEntity>

    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun getCardById(id: String): CardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCard(card: CardEntity)

    @Query("DELETE FROM cards WHERE id = :id")
    suspend fun deleteCardById(id: String)

    @Query("DELETE FROM cards")
    suspend fun clearAllCards()
}
