package com.flowmoney.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.flowmoney.app.data.local.entity.DailyReviewEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyReviewDao {

    @Query("SELECT * FROM daily_reviews WHERE dateString = :dateString LIMIT 1")
    suspend fun getReviewByDate(dateString: String): DailyReviewEntity?

    @Query("SELECT * FROM daily_reviews WHERE dateString = :dateString LIMIT 1")
    fun getReviewByDateFlow(dateString: String): Flow<DailyReviewEntity?>

    @Query("SELECT * FROM daily_reviews ORDER BY dateString DESC LIMIT 1")
    suspend fun getLatestReview(): DailyReviewEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(review: DailyReviewEntity): Long

    @Update
    suspend fun update(review: DailyReviewEntity)
}
