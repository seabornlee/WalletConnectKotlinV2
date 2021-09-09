package org.walletconnect.walletconnectv2.relay.data

import com.tinder.scarlet.Stream
import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import org.walletconnect.walletconnectv2.relay.data.model.Relay

interface RelayService {

    @Receive
    fun observeEvents(): Stream<WebSocket.Event>

//    @Send
//    fun publishRequest(publishRequest: Relay.Publish.Request)

    @Send
    fun sendText(message: String)

}