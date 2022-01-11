package com.walletconnect.walletconnectv2

import com.walletconnect.walletconnectv2.client.ClientTypes
import com.walletconnect.walletconnectv2.client.WalletConnectClientData
import com.walletconnect.walletconnectv2.client.WalletConnectClientListener
import com.walletconnect.walletconnectv2.client.WalletConnectClientListeners
import com.walletconnect.walletconnectv2.common.*
import com.walletconnect.walletconnectv2.di.*
import com.walletconnect.walletconnectv2.engine.EngineInteractor
import com.walletconnect.walletconnectv2.engine.model.EngineData
import com.walletconnect.walletconnectv2.engine.sequence.SequenceLifecycle
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.component.inject
import org.koin.core.context.startKoin

object WalletConnectClient : DIComponent {
    private val engineInteractor: EngineInteractor by inject()

    // TODO: add logic to check hostName for ws/wss scheme with and without ://
    fun initialize(initialParams: ClientTypes.InitialParams) = with(initialParams) {
        wcKoinApp = startKoin {
            androidContext(application)
            modules(wcModule())
            modules(networkRepositoryModule(useTls, hostName, projectId))
            modules(relayerModule())
            modules(cryptoModule())
            modules(storageModule())
            modules(engineModule(isController, metadata))
        }
    }

    fun setWalletConnectListener(walletConnectListener: WalletConnectClientListener) {
        scope.launch {
            engineInteractor.sequenceEvent.collect { event ->
                when (event) {
                    is SequenceLifecycle.OnSessionProposal -> walletConnectListener.onSessionProposal(event.proposal.toClientSessionProposal())
                    is SequenceLifecycle.OnSessionRequest -> walletConnectListener.onSessionRequest(event.request.toClientSessionRequest())
                    is SequenceLifecycle.OnSessionDeleted -> walletConnectListener.onSessionDelete(event.deletedSession.toClientDeletedSession())
                    is SequenceLifecycle.OnSessionNotification -> walletConnectListener.onSessionNotification(event.notification.toClientSessionNotification())
                    SequenceLifecycle.Default -> Unit
                }
            }
        }
    }

    fun pair(
        pairingParams: ClientTypes.PairParams,
        listener: WalletConnectClientListeners.Pairing
    ) {
        engineInteractor.pair(
            pairingParams.uri,
            { topic -> listener.onSuccess(WalletConnectClientData.SettledPairing(topic)) },
            { error -> listener.onError(error) })
    }

    fun approve(
        approveParams: ClientTypes.ApproveParams,
        listener: WalletConnectClientListeners.SessionApprove
    ) = with(approveParams) {
        engineInteractor.approve(
            proposal.toEngineSessionProposal(accounts),
            { settledSession -> listener.onSuccess(settledSession.toClientSettledSession()) },
            { error -> listener.onError(error) })
    }

    fun reject(
        rejectParams: ClientTypes.RejectParams,
        listener: WalletConnectClientListeners.SessionReject
    ) = with(rejectParams) {
        engineInteractor.reject(
            rejectionReason, proposalTopic,
            { (topic, reason) -> listener.onSuccess(WalletConnectClientData.RejectedSession(topic, reason)) },
            { error -> listener.onError(error) })
    }

    fun respond(
        responseParams: ClientTypes.ResponseParams,
        listener: WalletConnectClientListeners.SessionPayload
    ) = with(responseParams) {
        val jsonRpcEngineResponse = when (jsonRpcResponse) {
            is WalletConnectClientData.JsonRpcResponse.JsonRpcResult -> jsonRpcResponse.toEngineRpcResult()
            is WalletConnectClientData.JsonRpcResponse.JsonRpcError -> jsonRpcResponse.toEngineRpcError()
        }
        engineInteractor.respondSessionPayload(sessionTopic, jsonRpcEngineResponse) { error -> listener.onError(error) }
    }

    fun upgrade(
        upgradeParams: ClientTypes.UpgradeParams,
        listener: WalletConnectClientListeners.SessionUpgrade
    ) = with(upgradeParams) {
        engineInteractor.upgrade(
            topic, permissions.toEngineSessionPermissions(),
            { (topic, permissions) -> listener.onSuccess(WalletConnectClientData.UpgradedSession(topic, permissions.toClientPerms())) },
            { error -> listener.onError(error) })
    }

    fun update(
        updateParams: ClientTypes.UpdateParams,
        listener: WalletConnectClientListeners.SessionUpdate
    ) = with(updateParams) {
        engineInteractor.update(
            sessionTopic, sessionState.toEngineSessionState(),
            { (topic, accounts) -> listener.onSuccess(WalletConnectClientData.UpdatedSession(topic, accounts)) },
            { error -> listener.onError(error) })
    }

    fun ping(
        pingParams: ClientTypes.PingParams,
        listener: WalletConnectClientListeners.SessionPing
    ) {
        engineInteractor.ping(pingParams.topic,
            { topic -> listener.onSuccess(topic) },
            { error -> listener.onError(error) })
    }

    fun notify(
        notificationParams: ClientTypes.NotificationParams,
        listener: WalletConnectClientListeners.Notification
    ) = with(notificationParams) {
        engineInteractor.notify(topic, notification.toEngineNotification(),
            { topic -> listener.onSuccess(topic) },
            { error -> listener.onError(error) })
    }

    fun disconnect(
        disconnectParams: ClientTypes.DisconnectParams,
        listener: WalletConnectClientListeners.SessionDelete
    ) = with(disconnectParams) {
        engineInteractor.disconnect(
            sessionTopic, reason,
            { (topic, reason) -> listener.onSuccess(WalletConnectClientData.DeletedSession(topic, reason)) },
            { error -> listener.onError(error) })
    }

    fun getListOfSettledSessions(): List<WalletConnectClientData.SettledSession> {
        return engineInteractor.getListOfSettledSessions().map(EngineData.SettledSession::toClientSettledSession)
    }

    fun getListOfPendingSession(): List<WalletConnectClientData.SessionProposal> {
        return engineInteractor.getListOfPendingSessions().map(EngineData.SessionProposal::toClientSessionProposal)
    }

    fun shutdown() {
        scope.cancel()
        wcKoinApp.koin.close()
    }
}