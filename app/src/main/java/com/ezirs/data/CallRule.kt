package com.ezirs.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ezirs.R

enum class RuleType(val displayName: String) {
    AWALAN("Awalan"),
    AKHIRAN("Akhiran"),
    MENGANDUNG("Mengandung"),
    TEPAT("Sama Persis");

    fun getStringRes(): Int {
        return when (this) {
            AWALAN -> R.string.rule_type_prefix
            AKHIRAN -> R.string.rule_type_suffix
            MENGANDUNG -> R.string.rule_type_contains
            TEPAT -> R.string.rule_type_exact
        }
    }
}

@Entity(tableName = "call_rules")
data class CallRule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: RuleType,
    val value: String
)
