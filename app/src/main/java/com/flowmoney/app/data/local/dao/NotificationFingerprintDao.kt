package com.flowmoney.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.flowmoney.app.data.local.entity.NotificationFingerprintEntity

@Dao
interface NotificationFingerprintDao {

    @Query("SELECT EXISTS(SELECT 1 FROM notification_fingerprints WHERE fingerprintHash = :hash)")
    suspend fun exists(hash: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(fingerprint: NotificationFingerprintEntity): Long

    @Query("DELETE FROM notification_fingerprints WHERE receivedAt < :olderThan")
    suspend fun deleteOlderThan(olderThan: Long)
}
