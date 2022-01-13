package com.walletconnect.walletconnectv2.di

import android.app.Application
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.squareup.sqldelight.ColumnAdapter
import com.squareup.sqldelight.EnumColumnAdapter
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.SqlDriver
import com.walletconnect.walletconnectv2.Database
import com.walletconnect.walletconnectv2.storage.KeyChain
import com.walletconnect.walletconnectv2.storage.KeyStore
import com.walletconnect.walletconnectv2.storage.StorageRepository
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance
import org.walletconnect.walletconnectv2.storage.data.dao.MetaDataDao
import org.walletconnect.walletconnectv2.storage.data.dao.PairingDao
import org.walletconnect.walletconnectv2.storage.data.dao.SessionDao

@field:JvmSynthetic
internal val storageModule = DI.Module("storageModule") {
    val sharedPrefsFileKeyStore = "wc_key_store"
    val keyGenParameterSpec = MasterKeys.AES256_GCM_SPEC
    val mainKeyAlias = MasterKeys.getOrCreate(keyGenParameterSpec)
    val listOfStringsAdapter = object : ColumnAdapter<List<String>, String> {

        override fun decode(databaseValue: String) =
            if (databaseValue.isEmpty()) {
                listOf()
            } else {
                databaseValue.split(",")
            }

        override fun encode(value: List<String>) = value.joinToString(separator = ",")
    }

    bindSingleton(tag = Tags.KEY_STORE) {
        EncryptedSharedPreferences.create(
            sharedPrefsFileKeyStore,
            mainKeyAlias,
            instance<Application>(tag = Tags.APPLICATION),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    bindSingleton<KeyStore> { KeyChain(instance(tag = Tags.KEY_STORE)) }

    bindSingleton<SqlDriver> {
        AndroidSqliteDriver(
            schema = Database.Schema,
            context = instance<Application>(tag = Tags.APPLICATION),
            name = "WalletConnectV2.db"
        )
    }

    bindSingleton {
        Database(
            instance(),
            PairingDaoAdapter = PairingDao.Adapter(
                statusAdapter = EnumColumnAdapter(),
                controller_typeAdapter = EnumColumnAdapter()
            ),
            SessionDaoAdapter = SessionDao.Adapter(
                permissions_chainsAdapter = listOfStringsAdapter,
                permissions_methodsAdapter = listOfStringsAdapter,
                permissions_typesAdapter = listOfStringsAdapter,
                accountsAdapter = listOfStringsAdapter,
                statusAdapter = EnumColumnAdapter(),
                controller_typeAdapter = EnumColumnAdapter()
            ),
            MetaDataDaoAdapter = MetaDataDao.Adapter(iconsAdapter = listOfStringsAdapter)
        )
    }

    bindSingleton { StorageRepository(instance()) }
}