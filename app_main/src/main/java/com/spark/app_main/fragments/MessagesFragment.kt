package com.spark.app_main.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import com.spark.app.database.entity.Packet
import com.spark.app.model.UIViewModel
import com.spark.app.service.InviteState
import com.spark.app.ui.ScreenFragment
import com.spark.app_main.Constants.ExpirationTimeForInvite
import com.spark.app_main.MainTabActivity
import com.spark.app_main.adapters.MessagesInvitesAdapter
import com.spark.app_main.databinding.MessagesInvitesFragmentBinding
import java.util.Timer
import java.util.TimerTask
import kotlin.random.Random

class MessagesFragment : ScreenFragment("Messages") {
    private lateinit var messagesAdapter: MessagesInvitesAdapter
    private lateinit var binding: MessagesInvitesFragmentBinding
    private val model: UIViewModel by activityViewModels()
    private var timer: Timer? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = MessagesInvitesFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initMessagesAdapter()
        var msgListMap = emptyMap<String, Packet>()
        model.contacts.observe(viewLifecycleOwner) {
//            debug("New contacts received: ${it.size}")
            msgListMap = it
            messagesAdapter.onContactsChanged(msgListMap)
        }


        model.channels.asLiveData().observe(viewLifecycleOwner) {
            messagesAdapter.onChannelsChanged()
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (s.toString().isNotEmpty()) messagesAdapter.filter.filter(s.toString())
                else {
                    messagesAdapter.onContactsChanged(msgListMap)
                }
            }
        })
        timer = Timer()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                activity?.runOnUiThread {
                    if (::messagesAdapter.isInitialized) {
                        messagesAdapter.notifyDataSetChanged()
                        (activity as MainTabActivity?)?.resetBadge()
                    }
                }
            }
        },  2 * 1000, ExpirationTimeForInvite)
    }

    private fun initMessagesAdapter() {
        context?.let {
            val packets = arrayOf<Packet>()
            messagesAdapter = MessagesInvitesAdapter(
                it,
                packets = packets,
                nodeDb = model.nodeDB,
                channelSet = model.channelSet,
                acceptInvite = { packet ->
                    //Invite message sent as: "invite_accepted-X-X" where first X is username and second one is random first player
                    model.sendMessage(
                        InviteState.INVITE_ACCEPTED.title +"-${getRandomUserName()}-${getRandomUserName()}",
                        packet.contact_key
                    )
                    model.setAccepted(packet.contact_key, true)
                    (activity as MainTabActivity).showGamePlay()
                    (activity as MainTabActivity).resetBadge()
                },
                rejectInvite = { packet ->
                    model.sendMessage(
                        InviteState.INVITE_REJECTED.title,
                        packet.contact_key
                    )
                    (activity as MainTabActivity).resetBadge()
                }
            )
            /*  devicesAdapter.setOnInviteClickListener(object :
                  NearbyDeviceAdapter.OnInviteClickListener {
                  override fun onInviteClick(contactKey: String) {
                      model.sendMessage(InviteState.INVITE_SENT.title, contactKey)
                  }
              })*/
            binding.rvMessages.adapter = messagesAdapter
            binding.rvMessages.layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun getRandomUserName(): String {
        val values = UIViewModel.Player.entries.toTypedArray()
        val randomIndex = Random.nextInt(values.size)
        return values[randomIndex].name
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timer?.cancel()
    }
}