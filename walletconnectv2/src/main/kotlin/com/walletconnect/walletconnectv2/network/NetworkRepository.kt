@file:JvmSynthetic

package com.walletconnect.walletconnectv2.network

import com.tinder.scarlet.WebSocket
import com.walletconnect.walletconnectv2.common.model.vo.SubscriptionIdVO
import com.walletconnect.walletconnectv2.common.model.vo.TopicVO
import com.walletconnect.walletconnectv2.network.model.RelayDTO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

internal interface NetworkRepository {
    val eventsFlow: SharedFlow<WebSocket.Event>
    val subscriptionRequest: Flow<RelayDTO.Subscription.Request>

    fun publish(topic: TopicVO, message: String, onResult: (Result<RelayDTO.Publish.Acknowledgement>) -> Unit = {})

    fun subscribe(topic: TopicVO, onResult: (Result<RelayDTO.Subscribe.Acknowledgement>) -> Unit)

    fun unsubscribe(topic: TopicVO, subscriptionId: SubscriptionIdVO, onResult: (Result<RelayDTO.Unsubscribe.Acknowledgement>) -> Unit)
}