package com.walletconnect.walletconnectv2.di

import android.app.Application
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.walletconnect.walletconnectv2.jsonrpc.JsonRpcSerializer
import com.walletconnect.walletconnectv2.jsonrpc.history.JsonRpcHistory
import com.walletconnect.walletconnectv2.relay.walletconnect.WalletConnectRelayer
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance

@field:JvmSynthetic
internal val relayerModule = DI.Module("relayerModule") {
    val sharedPrefsFileRpcStore = "wc_rpc_store"
    val keyGenParameterSpec = MasterKeys.AES256_GCM_SPEC
    val mainKeyAlias = MasterKeys.getOrCreate(keyGenParameterSpec)

    bindSingleton(tag = Tags.RPC_STORE) {
        EncryptedSharedPreferences.create(
            sharedPrefsFileRpcStore,
            mainKeyAlias,
            instance<Application>(tag = Tags.APPLICATION),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    bindSingleton { JsonRpcHistory(instance(tag = Tags.RPC_STORE)) }

    bindSingleton { JsonRpcSerializer(instance(), instance(), instance()) }

    bindSingleton { WalletConnectRelayer(instance(), instance(), instance()) }
}