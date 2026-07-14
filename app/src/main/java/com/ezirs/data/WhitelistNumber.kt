package com.ezirs.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "whitelist_numbers")
data class WhitelistNumber(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val number: String
)
