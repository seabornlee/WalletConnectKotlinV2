package com.walletconnect.walletconnectv2.di

import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.core.component.KoinComponent

internal lateinit var wcKoinApp: KoinApplication

internal interface DIComponent: KoinComponent {
    override fun getKoin(): Koin = wcKoinApp.koin
}