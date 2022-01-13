package com.walletconnect.walletconnectv2.di

import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.lifecycle.android.AndroidLifecycle
import com.tinder.scarlet.messageadapter.moshi.MoshiMessageAdapter
import com.tinder.scarlet.retry.LinearBackoffStrategy
import com.tinder.scarlet.websocket.okhttp.newWebSocketFactory
import com.walletconnect.walletconnectv2.relay.waku.RelayService
import com.walletconnect.walletconnectv2.relay.waku.WakuNetworkRepository
import com.walletconnect.walletconnectv2.util.adapters.FlowStreamAdapter
import okhttp3.OkHttpClient
import org.kodein.di.DI
import org.kodein.di.bindProvider
import org.kodein.di.bindSingleton
import org.kodein.di.instance
import java.util.concurrent.TimeUnit

@field:JvmSynthetic
internal val networkRepositoryModule = DI.Module("networkRepositoryModule") {
    val TIMEOUT_TIME: Long = 5000L
    val DEFAULT_BACKOFF_MINUTES: Long = 5L

    bindSingleton {
        OkHttpClient.Builder()
            .writeTimeout(TIMEOUT_TIME, TimeUnit.MILLISECONDS)
            .readTimeout(TIMEOUT_TIME, TimeUnit.MILLISECONDS)
            .callTimeout(TIMEOUT_TIME, TimeUnit.MILLISECONDS)
            .connectTimeout(TIMEOUT_TIME, TimeUnit.MILLISECONDS)
            .build()
    }

    bindSingleton(tag = Tags.WEBSOCKET_FACTORY) {
        instance<OkHttpClient>().newWebSocketFactory(instance<String>(tag = Tags.SERVER_URL))
    }

    bindSingleton {
        Scarlet.Builder()
            .backoffStrategy(LinearBackoffStrategy(TimeUnit.MINUTES.toMillis(DEFAULT_BACKOFF_MINUTES)))
            .webSocketFactory(instance(tag = Tags.WEBSOCKET_FACTORY))
            .lifecycle(AndroidLifecycle.ofApplicationForeground(instance(tag = Tags.APPLICATION)))
            .addMessageAdapterFactory(MoshiMessageAdapter.Factory(instance()))
            .addStreamAdapterFactory(FlowStreamAdapter.Factory())
            .build()
    }

    bindProvider { instance<Scarlet>().create(RelayService::class.java) }

    bindSingleton { WakuNetworkRepository(instance()) }
}