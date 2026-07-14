package com.ezirs.data

import kotlinx.coroutines.flow.Flow

class CallRuleRepository(private val ruleDao: CallRuleDao) {
    val allRules: Flow<List<CallRule>> = ruleDao.getAllRules()
    val allLogs: Flow<List<CallLogEntry>> = ruleDao.getAllLogs()

    suspend fun insertRule(rule: CallRule) {
        ruleDao.insertRule(rule)
    }

    suspend fun deleteRule(rule: CallRule) {
        ruleDao.deleteRule(rule)
    }

    suspend fun insertLog(log: CallLogEntry) {
        ruleDao.insertLog(log)
    }

    suspend fun deleteLog(log: CallLogEntry) {
        ruleDao.deleteLog(log)
    }

    suspend fun deleteLogsByBlockedStatus(isBlocked: Boolean) {
        ruleDao.deleteLogsByBlockedStatus(isBlocked)
    }

    val allWhitelist: Flow<List<WhitelistNumber>> = ruleDao.getAllWhitelist()

    suspend fun insertWhitelist(whitelist: WhitelistNumber) {
        ruleDao.insertWhitelist(whitelist)
    }

    suspend fun deleteWhitelist(whitelist: WhitelistNumber) {
        ruleDao.deleteWhitelist(whitelist)
    }
}
