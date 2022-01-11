package com.walletconnect.walletconnectv2.jsonrpc.history

import android.annotation.SuppressLint
import android.content.SharedPreferences
import com.walletconnect.walletconnectv2.common.Topic
import com.walletconnect.walletconnectv2.di.DIComponent
import com.walletconnect.walletconnectv2.util.Logger

class JsonRpcHistory(private val sharedPreferences: SharedPreferences) : DIComponent {

    @SuppressLint("ApplySharedPref")
    fun setRequest(requestId: Long, topic: Topic): Boolean {
        return if (!sharedPreferences.contains(requestId.toString())) {
            sharedPreferences.edit().putString(requestId.toString(), topic.value).commit()
        } else {
            Logger.log("Duplicated JsonRpc RequestId: $requestId\tTopic: ${topic.value}")
            false
        }
    }

    fun deleteRequests(topic: Topic) {
        sharedPreferences.all.entries
            .filter { entry -> entry.value == topic.value }
            .forEach { entry -> sharedPreferences.edit().remove(entry.key).apply() }
    }
}