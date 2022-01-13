package com.walletconnect.walletconnectv2.di

import com.walletconnect.walletconnectv2.crypto.Codec
import com.walletconnect.walletconnectv2.crypto.CryptoManager
import com.walletconnect.walletconnectv2.crypto.codec.AuthenticatedEncryptionCodec
import com.walletconnect.walletconnectv2.crypto.managers.BouncyCastleCryptoManager
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance

@field:JvmSynthetic
internal val cryptoModule = DI.Module("cryptoManager") {

    bindSingleton<Codec> { AuthenticatedEncryptionCodec() }

    bindSingleton<CryptoManager> { BouncyCastleCryptoManager(instance()) }
}