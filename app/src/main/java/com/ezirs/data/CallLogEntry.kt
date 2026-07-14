package com.ezirs.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "call_logs")
data class CallLogEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val phoneNumber: String,
    val timestamp: Long,
    val isBlocked: Boolean,
    val reason: String
)
