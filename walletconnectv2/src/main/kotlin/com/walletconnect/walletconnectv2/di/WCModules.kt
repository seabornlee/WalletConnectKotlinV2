@file:JvmSynthetic

package com.walletconnect.walletconnectv2.di

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tinder.scarlet.utils.getRawType
import com.walletconnect.walletconnectv2.common.Expiry
import com.walletconnect.walletconnectv2.common.SubscriptionId
import com.walletconnect.walletconnectv2.common.Topic
import com.walletconnect.walletconnectv2.common.Ttl
import com.walletconnect.walletconnectv2.common.network.adapters.*
import com.walletconnect.walletconnectv2.jsonrpc.model.JsonRpcResponse
import org.json.JSONObject
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance

@field:JvmSynthetic
val wcModule = DI.Module("wcModule") {
    bindSingleton {
        PolymorphicJsonAdapterFactory.of(JsonRpcResponse::class.java, "type")
            .withSubtype(JsonRpcResponse.JsonRpcResult::class.java, "result")
            .withSubtype(JsonRpcResponse.JsonRpcError::class.java, "error")
    }

    bindSingleton(tag = Tags.KotlinJsonAdapter) { KotlinJsonAdapterFactory() }

    bindSingleton {
        Moshi.Builder()
            .addLast { type, _, _ ->
                when (type.getRawType().name) {
                    Expiry::class.qualifiedName -> ExpiryAdapter
                    JSONObject::class.qualifiedName -> JSONObjectAdapter
                    SubscriptionId::class.qualifiedName -> SubscriptionIdAdapter
                    Topic::class.qualifiedName -> TopicAdapter
                    Ttl::class.qualifiedName -> TtlAdapter
                    else -> null
                }
            }
            .addLast(instance(Tags.KotlinJsonAdapter))
            .add(instance<PolymorphicJsonAdapterFactory<JsonRpcResponse>>())
            .build()
    }
}