package com.walletconnect.walletconnectv2.di

import com.walletconnect.walletconnectv2.engine.EngineInteractor
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance

@field:JvmSynthetic
internal val engineModule = DI.Module("engineModule") {

    bindSingleton { EngineInteractor(instance(), instance(), instance(), instance(tag = Tags.METADATA), instance(tag = Tags.CONTROLLER_TYPE)) }
}