package com.ezirs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ezirs.data.AppDatabase
import com.ezirs.data.CallLogEntry
import com.ezirs.data.CallRule
import com.ezirs.data.CallRuleRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: CallRuleRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = CallRuleRepository(database.callRuleDao())
    }

    val rules: StateFlow<List<CallRule>> = repository.allRules
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val logs: StateFlow<List<CallLogEntry>> = repository.allLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addRule(rule: CallRule) {
        viewModelScope.launch {
            repository.insertRule(rule)
        }
    }

    fun deleteRule(rule: CallRule) {
        viewModelScope.launch {
            repository.deleteRule(rule)
        }
    }

    fun deleteLog(log: CallLogEntry) {
        viewModelScope.launch {
            repository.deleteLog(log)
        }
    }

    fun deleteLogsByBlockedStatus(isBlocked: Boolean) {
        viewModelScope.launch {
            repository.deleteLogsByBlockedStatus(isBlocked)
        }
    }

    val whitelists: StateFlow<List<com.ezirs.data.WhitelistNumber>> = repository.allWhitelist
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun exportToJson(context: android.content.Context, uri: android.net.Uri, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(context)
                val allRulesSync = db.callRuleDao().getAllRulesSync()
                val allLogsSync = db.callRuleDao().getAllLogsSync()
                val allWhitelistSync = db.callRuleDao().getAllWhitelistSync()

                val rootObj = org.json.JSONObject()
                
                val rulesArray = org.json.JSONArray()
                for (rule in allRulesSync) {
                    val obj = org.json.JSONObject()
                    obj.put("type", rule.type.name)
                    obj.put("value", rule.value)
                    rulesArray.put(obj)
                }
                rootObj.put("rules", rulesArray)
                
                val logsArray = org.json.JSONArray()
                for (log in allLogsSync) {
                    val obj = org.json.JSONObject()
                    obj.put("phoneNumber", log.phoneNumber)
                    obj.put("timestamp", log.timestamp)
                    obj.put("isBlocked", log.isBlocked)
                    obj.put("reason", log.reason ?: "")
                    logsArray.put(obj)
                }
                rootObj.put("logs", logsArray)

                val whitelistArray = org.json.JSONArray()
                for (wl in allWhitelistSync) {
                    val obj = org.json.JSONObject()
                    obj.put("name", wl.name)
                    obj.put("number", wl.number)
                    whitelistArray.put(obj)
                }
                rootObj.put("whitelist", whitelistArray)
                
                val jsonString = rootObj.toString(2)
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(jsonString.toByteArray())
                }
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete(true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete(false)
                }
            }
        }
    }

    fun importFromJson(context: android.content.Context, uri: android.net.Uri, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val jsonString = context.contentResolver.openInputStream(uri)?.use {
                    it.bufferedReader().readText()
                } ?: return@launch
                
                val rootObj = org.json.JSONObject(jsonString)
                val db = AppDatabase.getDatabase(context)
                
                // Read Rules
                if (rootObj.has("rules")) {
                    db.query("DELETE FROM call_rules", null)?.close()
                    val rulesArray = rootObj.getJSONArray("rules")
                    for (i in 0 until rulesArray.length()) {
                        val obj = rulesArray.getJSONObject(i)
                        val typeName = obj.optString("type")
                        val value = obj.optString("value")
                        val type = try { com.ezirs.data.RuleType.valueOf(typeName) } catch(e:Exception) { null }
                        if (type != null) {
                            db.callRuleDao().insertRule(CallRule(type = type, value = value))
                        }
                    }
                }
                
                // Read Logs
                if (rootObj.has("logs")) {
                    db.query("DELETE FROM call_logs", null)?.close()
                    val logsArray = rootObj.getJSONArray("logs")
                    for (i in 0 until logsArray.length()) {
                        val obj = logsArray.getJSONObject(i)
                        db.callRuleDao().insertLog(CallLogEntry(
                            phoneNumber = obj.optString("phoneNumber"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            isBlocked = obj.optBoolean("isBlocked"),
                            reason = obj.optString("reason")
                        ))
                    }
                }
                
                // Read Whitelist
                if (rootObj.has("whitelist")) {
                    db.query("DELETE FROM whitelist_numbers", null)?.close()
                    val wlArray = rootObj.getJSONArray("whitelist")
                    for (i in 0 until wlArray.length()) {
                        val obj = wlArray.getJSONObject(i)
                        db.callRuleDao().insertWhitelist(com.ezirs.data.WhitelistNumber(
                            name = obj.optString("name"),
                            number = obj.optString("number")
                        ))
                    }
                }
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete(true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete(false)
                }
            }
        }
    }

    fun addWhitelist(whitelist: com.ezirs.data.WhitelistNumber) {
        viewModelScope.launch {
            repository.insertWhitelist(whitelist)
        }
    }

    fun deleteWhitelist(whitelist: com.ezirs.data.WhitelistNumber) {
        viewModelScope.launch {
            repository.deleteWhitelist(whitelist)
        }
    }
}
