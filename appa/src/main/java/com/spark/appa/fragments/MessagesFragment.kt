package com.spark.appa.fragments

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
import com.spark.app.databinding.MessagesInvitesFragmentBinding
import com.spark.app.model.UIViewModel
import com.spark.app.service.InviteState
import com.spark.app.ui.ScreenFragment
import com.spark.app.ui.adapters.MessagesInvitesAdapter
import com.spark.appa.MainTabActivity
import kotlin.random.Random

class MessagesFragment : ScreenFragment("Messages") {
    private lateinit var messagesAdapter: MessagesInvitesAdapter
    private lateinit var binding: MessagesInvitesFragmentBinding
    private val model: UIViewModel by activityViewModels()

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
        model.contacts.observe(viewLifecycleOwner) {
//            debug("New contacts received: ${it.size}")
            messagesAdapter.onContactsChanged(it)
        }

        model.channels.asLiveData().observe(viewLifecycleOwner) {
            messagesAdapter.onChannelsChanged()
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                messagesAdapter.filter.filter(s.toString())
            }
        })

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
                        InviteState.INVITE_ACCEPTED.title + "-${getRandomUserName()}-${getRandomUserName()}",
                        packet.contact_key
                    )
                    model.setAccepted(packet.contact_key, true)
                    (activity as MainTabActivity).showGamePlay()
                    (activity as MainTabActivity).resetBadge()
                })
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
}