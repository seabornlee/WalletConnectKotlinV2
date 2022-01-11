package com.walletconnect.walletconnectv2.di

import android.app.Application
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.squareup.sqldelight.ColumnAdapter
import com.squareup.sqldelight.EnumColumnAdapter
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.SqlDriver
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.lifecycle.android.AndroidLifecycle
import com.tinder.scarlet.messageadapter.moshi.MoshiMessageAdapter
import com.tinder.scarlet.retry.LinearBackoffStrategy
import com.tinder.scarlet.utils.getRawType
import com.tinder.scarlet.websocket.okhttp.newWebSocketFactory
import com.walletconnect.walletconnectv2.Database
import com.walletconnect.walletconnectv2.common.*
import com.walletconnect.walletconnectv2.common.network.adapters.*
import com.walletconnect.walletconnectv2.crypto.Codec
import com.walletconnect.walletconnectv2.crypto.CryptoManager
import com.walletconnect.walletconnectv2.crypto.codec.AuthenticatedEncryptionCodec
import com.walletconnect.walletconnectv2.crypto.managers.BouncyCastleCryptoManager
import com.walletconnect.walletconnectv2.engine.EngineInteractor
import com.walletconnect.walletconnectv2.jsonrpc.JsonRpcSerializer
import com.walletconnect.walletconnectv2.jsonrpc.history.JsonRpcHistory
import com.walletconnect.walletconnectv2.jsonrpc.model.JsonRpcResponse
import com.walletconnect.walletconnectv2.relay.waku.RelayService
import com.walletconnect.walletconnectv2.relay.waku.WakuNetworkRepository
import com.walletconnect.walletconnectv2.relay.walletconnect.WalletConnectRelayer
import com.walletconnect.walletconnectv2.storage.KeyChain
import com.walletconnect.walletconnectv2.storage.KeyStore
import com.walletconnect.walletconnectv2.storage.StorageRepository
import com.walletconnect.walletconnectv2.util.adapters.FlowStreamAdapter
import okhttp3.OkHttpClient
import org.json.JSONObject
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.walletconnect.walletconnectv2.storage.data.dao.MetaDataDao
import org.walletconnect.walletconnectv2.storage.data.dao.PairingDao
import org.walletconnect.walletconnectv2.storage.data.dao.SessionDao
import java.util.concurrent.TimeUnit

internal fun wcModule() = module(createdAtStart = false) {

    single<PolymorphicJsonAdapterFactory<JsonRpcResponse>> {
        PolymorphicJsonAdapterFactory.of(JsonRpcResponse::class.java, "type")
            .withSubtype(JsonRpcResponse.JsonRpcResult::class.java, "result")
            .withSubtype(JsonRpcResponse.JsonRpcError::class.java, "error")
    }

    single {
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
            .addLast(KotlinJsonAdapterFactory())
            .add(get(PolymorphicJsonAdapterFactory::class))
            .build()
    }
}

internal fun networkRepositoryModule(useTls: Boolean, hostName: String, projectId: String) = module(createdAtStart = false) {

    single(named("serverUrl")) {
        ((if (useTls) "wss" else "ws") + "://$hostName/?projectId=$projectId").trim()
    }

    single {
        OkHttpClient.Builder()
            .writeTimeout(TIMEOUT_TIME, TimeUnit.MILLISECONDS)
            .readTimeout(TIMEOUT_TIME, TimeUnit.MILLISECONDS)
            .callTimeout(TIMEOUT_TIME, TimeUnit.MILLISECONDS)
            .connectTimeout(TIMEOUT_TIME, TimeUnit.MILLISECONDS)
            .build()
    }

    single() {
        get<OkHttpClient>().newWebSocketFactory(get<String>(named("serverUrl")))
    }

    single {
        Scarlet.Builder()
            .backoffStrategy(LinearBackoffStrategy(TimeUnit.MINUTES.toMillis(DEFAULT_BACKOFF_MINUTES)))
            .webSocketFactory(get())
            .lifecycle(AndroidLifecycle.ofApplicationForeground(androidContext() as Application)) // TODO: Maybe have debug version of scarlet w/o application and release version of scarlet w/ application once DI is setup
            .addMessageAdapterFactory(MoshiMessageAdapter.Factory(get()))
            .addStreamAdapterFactory(FlowStreamAdapter.Factory())
            .build()
    }

    single {
        get<Scarlet>().create(RelayService::class.java)
    }

    single { WakuNetworkRepository(get()) }
}

internal fun relayerModule() = module(createdAtStart = false) {

    single(named("rpcStore")) {
        EncryptedSharedPreferences.create(
            sharedPrefsFileRpcStore,
            mainKeyAlias,
            androidContext(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    single { JsonRpcHistory(get(named("rpcStore"))) }

    single { JsonRpcSerializer(get(), get(), get()) }

    single { WalletConnectRelayer(get(), get(), get()) }
}

internal fun cryptoModule() = module(createdAtStart = false) {

    single<Codec> { AuthenticatedEncryptionCodec() }

    single<CryptoManager> { BouncyCastleCryptoManager(get()) }
}

internal fun storageModule() = module(createdAtStart = false) {

    single(named("keyStore")) {
        EncryptedSharedPreferences.create(
            sharedPrefsFileKeyStore,
            mainKeyAlias,
            androidContext(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    single<KeyStore> { KeyChain(get(named("keyStore"))) }

    single<SqlDriver> {
        AndroidSqliteDriver(
            schema = Database.Schema,
            context = androidContext(),
            name = "WalletConnectV2.db"
        )
    }

    single { StorageRepository(get()) }

    single {
        Database(
            get(),
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
}

internal fun engineModule(isController: Boolean, metadata: AppMetaData) = module(createdAtStart = false) {

    single { metadata }

    single { if (isController) ControllerType.CONTROLLER else ControllerType.NON_CONTROLLER }

    single { EngineInteractor(get(), get(), get(), get(), get()) }
}

private const val TIMEOUT_TIME: Long = 5000L
private const val DEFAULT_BACKOFF_MINUTES: Long = 5L
private val listOfStringsAdapter = object : ColumnAdapter<List<String>, String> {

    override fun decode(databaseValue: String) =
        if (databaseValue.isEmpty()) {
            listOf()
        } else {
            databaseValue.split(",")
        }

    override fun encode(value: List<String>) = value.joinToString(separator = ",")
}

private const val sharedPrefsFileKeyStore: String = "wc_key_store"
private const val sharedPrefsFileRpcStore: String = "wc_rpc_store"
private val keyGenParameterSpec = MasterKeys.AES256_GCM_SPEC
private val mainKeyAlias = MasterKeys.getOrCreate(keyGenParameterSpec)