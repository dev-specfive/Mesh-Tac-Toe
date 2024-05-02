package com.spark.appa.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.spark.app.AppOnlyProtos
import com.spark.app.DataPacket
import com.spark.app.MessageStatus
import com.spark.app.R
import com.spark.app.database.entity.Packet
import com.spark.app.model.Channel
import com.spark.app.model.NodeDB
import com.spark.appa.databinding.MessageInvitesItemLayoutBinding
import java.text.DateFormat
import java.util.Date

class MessagesInvitesAdapter(
    private val context: Context,
    private var packets: Array<Packet>,
    private var nodeDb: NodeDB,
    private var channelSet: AppOnlyProtos.ChannelSet,
    private var acceptInvite: (Packet) -> Unit = {}
) : RecyclerView.Adapter<MessagesInvitesAdapter.ViewHolder>(), Filterable {
    private var list = packets.toList()
    private var listener: OnInviteClickListener? = null


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(context)
        val packetView = MessageInvitesItemLayoutBinding.inflate(inflater, parent, false)
        return ViewHolder(packetView)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val packet = list[position]
        holder.deviceName.text = packet.getNameOfPacket()
        setMessageTime(holder, packet)

        if (packet.isInvited()) {
            holder.acceptInvite.visibility = View.VISIBLE
            holder.nodeDescripiton.apply {
                visibility = View.VISIBLE
                text = "${packet.data.from} has invited you to join a game."

            }
        } else {
            holder.acceptInvite.visibility = View.GONE
            holder.nodeDescripiton.apply {
                text = ""
                visibility = View.GONE
            }

        }
        holder.acceptInvite.setOnClickListener {
            acceptInvite.invoke(packet)
        }

    }

    private fun setMessageTime(holder: ViewHolder, packet: Packet) {
        if (packet.data.status?.name?.equals(MessageStatus.UNKNOWN.name, true) == false)
            holder.messageTime.text = getShortDateTime(Date(packet.data.time))
    }

    private val dateTimeFormat: DateFormat =
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
    private val timeFormat: DateFormat =
        DateFormat.getTimeInstance(DateFormat.SHORT)

    private fun getShortDateTime(time: Date): String {
        // return time if within 24 hours, otherwise date/time
        val oneDayMsec = 60 * 60 * 24 * 1000L
        return if (System.currentTimeMillis() - time.time > oneDayMsec) {
            dateTimeFormat.format(time)
        } else timeFormat.format(time)
    }

    class ViewHolder(itemView: MessageInvitesItemLayoutBinding) :
        RecyclerView.ViewHolder(itemView.root) {
        val deviceName = itemView.tvNodeName
        val acceptInvite = itemView.acceptInvite
        val nodeDescripiton = itemView.tvNodeDescription
        val messageTime = itemView.tvTime
//        val signalView = itemView.tvSignalView

    }

    fun setOnInviteClickListener(listener: OnInviteClickListener) {
        this.listener = listener
    }

    fun onContactsChanged(contacts: Map<String, Packet>?) {
        this.packets = contacts?.values?.toTypedArray()!!
        this.list = packets.toList()
        notifyDataSetChanged()
    }

    fun onChannelsChanged() {
        onContactsChanged(packets.associateBy { it.contact_key })
    }

    interface OnInviteClickListener {
        fun onInviteClick(contactKey: String)
    }

    fun Packet.isInvited(): Boolean {
        val contact = data
        val fromLocal = contact.from == DataPacket.ID_LOCAL

        return (contact.text.equals("invite", true) && !fromLocal)
    }

    fun Packet.getNameOfPacket(): String {
        val contact = data
        // Determine if this is my message (originated on this device)
        val fromLocal = contact.from == DataPacket.ID_LOCAL
        val toBroadcast = contact.to == DataPacket.ID_BROADCAST

        // grab usernames from NodeInfo
        val nodes = nodeDb.nodes.value
        val node = nodes[if (fromLocal) contact.to else contact.from]

        //grab channel names from DeviceConfig
        val channels = channelSet
        val channelName = if (channels.settingsCount > contact.channel)
            Channel(channels.settingsList[contact.channel], channels.loraConfig).name else null

        val longName = if (toBroadcast) channelName ?: context.getString(R.string.channel_name)
        else node?.user?.longName ?: context.getString(R.string.unknown_username)
        return longName
    }


    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val filteredResults = mutableListOf<Packet>()
                val query = constraint.toString().lowercase()
                if (query.isEmpty()) {
                    filteredResults.addAll(packets)
                } else {
                    for (item in packets) {
                        if (item.getNameOfPacket().lowercase()
                                .contains(query)
                        ) {
                            filteredResults.add(item)
                        }
                    }
                }
                val filterResults = FilterResults()
                filterResults.values = filteredResults
                return filterResults
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                @Suppress("UNCHECKED_CAST")
                list = (results?.values as List<Packet>).toList()
                notifyDataSetChanged()
            }
        }
    }
}