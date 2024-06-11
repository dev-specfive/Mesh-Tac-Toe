package com.spark.app_main.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.spark.app.NodeInfo
import com.spark.app.R
import com.spark.app_main.Constants
import com.spark.app_main.data.prefs.PreferencesHelperImpl
import com.spark.app_main.databinding.NearbyNodeItemLayoutBinding
import com.spark.app_main.model.NodeInvite
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class NearbyDeviceAdapter(
    private val context: Context,
    private var devices: Array<NodeInfo>,
    var pref: PreferencesHelperImpl
) : RecyclerView.Adapter<NearbyDeviceAdapter.ViewHolder>() {
    private var listener: OnInviteClickListener? = null

    //    private var isClickable = true
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(context)
        val deviceView = NearbyNodeItemLayoutBinding.inflate(inflater, parent, false)
        return ViewHolder(deviceView)
    }

    override fun getItemCount(): Int = devices.size
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val node = devices[position]
        val ourNodeInfo = devices[0]
        val user = node.user
        holder.deviceName.text = user?.longName ?: context.getString(R.string.unknown_username)
        println("is online: " + node.isOnline)
        renderSignal(holder, node, ourNodeInfo)

        val list = (pref.getList())

        val nodeLastInviteSent = list.firstOrNull { it.nodeName == node.user?.longName }
        var allowInvite = true
        nodeLastInviteSent?.inviteSentTime?.let {
            val differenceTime = System.currentTimeMillis() - nodeLastInviteSent.inviteSentTime
            allowInvite = differenceTime > Constants.ExpirationTimeForInvite
        }


        holder.sendInvite.apply {
            visibility = if (node.num == ourNodeInfo.num) View.GONE else View.VISIBLE
            text = if (allowInvite) "Send Invite" else "Invite Sent"
            alpha = if (allowInvite) 1f else 0.4f
        }
        holder.clockIcon.visibility = if (allowInvite) View.GONE else View.VISIBLE

        renderBattery(node.batteryLevel, node.voltage, holder)
        holder.sendInvite.setOnClickListener {
            notifyDataSetChanged()

            val isAnyInviteSent = pref.getList().any {
                (System.currentTimeMillis() - it.inviteSentTime) < Constants.ExpirationTimeForInvite
            }
            if (isAnyInviteSent.not()) {
                
                val nodeInviteArrayList = (pref.getList()) as ArrayList
                nodeInviteArrayList.removeIf { it.nodeName == node.user?.longName }
                nodeInviteArrayList.add(
                    NodeInvite(
                        node.user?.longName ?: "",
                        System.currentTimeMillis()
                    )
                )
                GlobalScope.launch {
                    pref.saveList(nodeInviteArrayList)
                }
                notifyItemChanged(position)

                val meshUser = node.user
                val channelId = node.channel
                if (!meshUser?.id.isNullOrBlank()) {
                    meshUser?.id?.let { userid ->
//                    model.sendMessage(InviteState.INVITE_SENT.title, "$channelId$userid")
                        listener?.onInviteClick("$channelId$userid")
                    }
                } else
                    Toast.makeText(context, "Something went wrong", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Invite already sent", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun renderSignal(holder: ViewHolder, node: NodeInfo, ourNodeInfo: NodeInfo) {
        if (node.num == ourNodeInfo.num) {
            val text = "ChUtil %.1f%% AirUtilTX %.1f%%".format(
                node.deviceMetrics?.channelUtilization,
                node.deviceMetrics?.airUtilTx
            )
            holder.signalView.text = text
            holder.signalView.visibility = View.VISIBLE
        } else {
            val text = buildString {
                if (node.channel > 0) append("ch:${node.channel}")
                if (node.snr < 100f && node.rssi < 0) {
                    if (isNotEmpty()) append(" ")
                    append("rssi:%d snr:%.1f".format(node.rssi, node.snr))
                }
            }
            if (text.isNotEmpty()) {
                holder.signalView.text = text
                holder.signalView.visibility = View.VISIBLE
            } else {
                holder.signalView.visibility = View.INVISIBLE
            }
        }
    }

    fun onNodesChanged(nodesIn: Array<NodeInfo>) {
        if (nodesIn.size > 1)
            nodesIn.sortWith(compareByDescending { it.lastHeard }, 1)
        devices = nodesIn
        notifyDataSetChanged()
    }

    private fun renderBattery(
        battery: Int?,
        voltage: Float?,
        holder: ViewHolder
    ) {

        val (image, text) = when (battery) {
            in 0..100 -> R.drawable.bolt to "%d%% %.2fV".format(battery, voltage)
            101 -> R.drawable.power to ""
            else -> R.drawable.bolt to "?"
        }

        holder.batteryPctView.text = text
        holder.batteryIcon.setImageDrawable(context?.let {
            ContextCompat.getDrawable(it, image)
        })
    }

    class ViewHolder(itemView: NearbyNodeItemLayoutBinding) :
        RecyclerView.ViewHolder(itemView.root) {
        val deviceName = itemView.tvNodeName
        val signalView = itemView.tvSignalView
        val sendInvite = itemView.tvSendInvite
        val clockIcon = itemView.clockIcon
        val batteryPctView = itemView.batteryPercentageView
        val batteryIcon = itemView.batteryIcon

//        val logo = itemView.ivLogo
    }

    fun setOnInviteClickListener(listener: OnInviteClickListener) {
        this.listener = listener
    }

    interface OnInviteClickListener {
        fun onInviteClick(contactKey: String)
    }
}