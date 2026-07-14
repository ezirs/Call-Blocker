package com.ezirs.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CallRuleDao {
    @Query("SELECT * FROM call_rules")
    fun getAllRules(): Flow<List<CallRule>>

    @Query("SELECT * FROM call_rules")
    suspend fun getAllRulesSync(): List<CallRule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: CallRule)

    @Delete
    suspend fun deleteRule(rule: CallRule)

    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<CallLogEntry>>

    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC")
    suspend fun getAllLogsSync(): List<CallLogEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: CallLogEntry)

    @Delete
    suspend fun deleteLog(log: CallLogEntry)

    @Query("DELETE FROM call_logs WHERE isBlocked = :isBlocked")
    suspend fun deleteLogsByBlockedStatus(isBlocked: Boolean)
    @Query("SELECT * FROM whitelist_numbers")
    fun getAllWhitelist(): Flow<List<WhitelistNumber>>

    @Query("SELECT * FROM whitelist_numbers")
    suspend fun getAllWhitelistSync(): List<WhitelistNumber>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWhitelist(whitelist: WhitelistNumber)

    @Delete
    suspend fun deleteWhitelist(whitelist: WhitelistNumber)
}
