package com.walletconnect.sample.wallet.ui

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.walletconnect.sample.R
import com.walletconnect.sample.databinding.SessionItemBinding
import com.walletconnect.walletconnectv2.client.WalletConnectClientData

class SessionsAdapter(
    private val disconnect: (String) -> Unit,
    private val update: (WalletConnectClientData.SettledSession) -> Unit,
    private val upgrade: (WalletConnectClientData.SettledSession) -> Unit,
    private val ping: (WalletConnectClientData.SettledSession) -> Unit,
    private val showSessionDetails: (WalletConnectClientData.SettledSession) -> Unit
) : ListAdapter<WalletConnectClientData.SettledSession, SessionsAdapter.SessionViewHolder>(DIFF_UTIL) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder =
        SessionViewHolder(SessionItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SessionViewHolder(private val binding: SessionItemBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(session: WalletConnectClientData.SettledSession) = with(binding) {
            root.setOnClickListener {
                showSessionDetails(session)
            }

            Glide.with(root.context)
                .load(Uri.parse(session.peerAppMetaData?.icons?.first()))
                .into(icon)

            name.text = session.peerAppMetaData?.name
            uri.text = session.peerAppMetaData?.url

            menu.setOnClickListener {
                with(PopupMenu(root.context, menu)) {
                    menuInflater.inflate(R.menu.session_menu, menu)
                    setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.disconnect -> disconnect(session.topic)
                            R.id.update -> update(session)
                            R.id.upgrade -> upgrade(session)
                            R.id.ping -> ping(session)
                        }
                        true
                    }
                    show()
                }
            }
        }
    }

    private companion object {
        val DIFF_UTIL = object : DiffUtil.ItemCallback<WalletConnectClientData.SettledSession>() {
            override fun areItemsTheSame(oldItem: WalletConnectClientData.SettledSession, newItem: WalletConnectClientData.SettledSession): Boolean = oldItem == newItem

            override fun areContentsTheSame(oldItem: WalletConnectClientData.SettledSession, newItem: WalletConnectClientData.SettledSession): Boolean = oldItem == newItem
        }
    }
}
