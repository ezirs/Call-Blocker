package com.ezirs.service

import android.content.Context
import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import androidx.annotation.RequiresApi
import com.ezirs.data.AppDatabase
import com.ezirs.data.CallLogEntry
import com.ezirs.data.RuleType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@RequiresApi(Build.VERSION_CODES.Q)
class BlockCallScreeningService : CallScreeningService() {
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onScreenCall(callDetails: Call.Details) {
        val phoneNumber = callDetails.handle?.schemeSpecificPart ?: ""
        Log.d("CallScreening", "Incoming call from: $phoneNumber")

        // If it's outgoing, we skip. Call details tells us the direction in Android 10+
        if (callDetails.callDirection == Call.Details.DIRECTION_OUTGOING) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        val prefs = applicationContext.getSharedPreferences("call_blocker_prefs", Context.MODE_PRIVATE)
        val isPaused = prefs.getBoolean("is_paused", false)

        if (isPaused) {
            Log.d("CallScreening", "Service is paused. Allowing call: $phoneNumber")
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        scope.launch {
            val db = AppDatabase.getDatabase(applicationContext)

            val whitelists = db.callRuleDao().getAllWhitelistSync()
            var isWhitelisted = false
            for (w in whitelists) {
                val num = phoneNumber.replace(" ", "").replace("-", "")
                val wNum = w.number.replace(" ", "").replace("-", "")
                val cleanNum = if (num.startsWith("+62")) num.substring(3)
                               else if (num.startsWith("62")) num.substring(2)
                               else if (num.startsWith("0")) num.substring(1) else num
                
                val cleanWNum = if (wNum.startsWith("+62")) wNum.substring(3)
                                else if (wNum.startsWith("62")) wNum.substring(2)
                                else if (wNum.startsWith("0")) wNum.substring(1) else wNum
                                
                if (num == wNum || cleanNum == cleanWNum || num.endsWith(cleanWNum) || cleanNum.endsWith(cleanWNum)) {
                    isWhitelisted = true
                    break
                }
            }

            if (isWhitelisted) {
                Log.d("CallScreening", "Allowing call: $phoneNumber (Whitelisted)")
                withContext(kotlinx.coroutines.NonCancellable) {
                    db.callRuleDao().insertLog(CallLogEntry(
                        phoneNumber = phoneNumber,
                        timestamp = System.currentTimeMillis(),
                        isBlocked = false,
                        reason = "Lolos (Masuk Whitelist)"
                    ))
                }
                respondToCall(callDetails, CallResponse.Builder().build())
                return@launch
            }

            val rules = db.callRuleDao().getAllRulesSync()

            var shouldReject = false
            var matchedRuleText = ""

            for (rule in rules) {
                val value = rule.value
                val matched = when (rule.type) {
                    RuleType.AWALAN -> phoneNumber.startsWith(value)
                    RuleType.AKHIRAN -> phoneNumber.endsWith(value)
                    RuleType.MENGANDUNG -> phoneNumber.contains(value)
                    RuleType.TEPAT -> phoneNumber == value
                }
                
                if (matched) {
                    shouldReject = true
                    matchedRuleText = "Terblokir oleh rule: ${rule.type.displayName} ($value)"
                    Log.d("CallScreening", "Matched rule: $rule")
                    break
                }
            }

            val response = CallResponse.Builder()
            if (shouldReject) {
                Log.d("CallScreening", "Rejecting call: $phoneNumber")
                response.setDisallowCall(true)
                    .setRejectCall(true)
                    .setSkipCallLog(false)
                    .setSkipNotification(true)

                withContext(kotlinx.coroutines.NonCancellable) {
                    db.callRuleDao().insertLog(CallLogEntry(
                        phoneNumber = phoneNumber,
                        timestamp = System.currentTimeMillis(),
                        isBlocked = true,
                        reason = matchedRuleText
                    ))
                }
                showBlockedCallNotification(phoneNumber, matchedRuleText)
            } else {
                Log.d("CallScreening", "Allowing call: $phoneNumber")
                withContext(kotlinx.coroutines.NonCancellable) {
                    db.callRuleDao().insertLog(CallLogEntry(
                        phoneNumber = phoneNumber,
                        timestamp = System.currentTimeMillis(),
                        isBlocked = false,
                        reason = "Lolos dari filter"
                    ))
                }
            }

            respondToCall(callDetails, response.build())
        }
    }

    private fun showBlockedCallNotification(phoneNumber: String, reason: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        
        val channelId = "blocked_calls_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Log Pemblokiran Panggilan"
            // Menggunakan IMPORTANCE_LOW agar tidak mengambang (hanya muncul di notifikasi tray saja)
            val channel = android.app.NotificationChannel(channelId, channelName, android.app.NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }
        
        val builder = androidx.core.app.NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Panggilan Masuk Diblokir")
            .setContentText("Nomor: $phoneNumber - $reason")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
            .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText("Panggilan dari nomor $phoneNumber telah diblokir.\nAlasan: $reason"))
            .setAutoCancel(true)
            
        // Check permission strictly for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (applicationContext.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return // Belum ada permission
            }
        }
            
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}
