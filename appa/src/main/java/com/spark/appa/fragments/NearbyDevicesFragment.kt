package com.spark.appa.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.asLiveData
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.spark.app.NodeInfo
import com.spark.app.databinding.FragmentNearbyDevicesBinding
import com.spark.app.model.UIViewModel
import com.spark.app.service.InviteState
import com.spark.app.ui.ScreenFragment
import com.spark.appa.Constants.ExpirationTimeForInvite
import com.spark.appa.adapters.NearbyDeviceAdapter
import com.spark.appa.data.prefs.PreferencesHelperImpl
import dagger.hilt.android.AndroidEntryPoint
import java.util.Timer
import java.util.TimerTask
import javax.inject.Inject

@AndroidEntryPoint
class NearbyDevicesFragment : ScreenFragment("Nearby") {
    private lateinit var devicesAdapter: NearbyDeviceAdapter
    private lateinit var binding: FragmentNearbyDevicesBinding
    private val model: UIViewModel by activityViewModels()
    private var timer: Timer? = null // TO update expiration time

    @Inject
    lateinit var preferencesHelperImpl: PreferencesHelperImpl
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNearbyDevicesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDevicesAdapter()
        setObservers()

        timer = Timer()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                activity?.runOnUiThread {
                    if (::devicesAdapter.isInitialized) {
                        devicesAdapter.notifyDataSetChanged()
                    }
                }
            }
        }, 2 * 1000, ExpirationTimeForInvite)
    }

    private val inviteReject = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            //TODO: On receive reject invite allow the sender to invite again to the user
        }
    }

    private fun setObservers() {

        model.nodeDB.nodeDBbyNum.asLiveData().observe(viewLifecycleOwner) {
            if (devicesAdapter.itemCount == 0)
                devicesAdapter.onNodesChanged(it.values.toTypedArray())
        }
        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(
                inviteReject,
                IntentFilter(InviteState.INVITE_REJECTED.title)
            )
    }

    private fun initDevicesAdapter() {
        context?.let {
            val nodes = arrayOf<NodeInfo>()
            devicesAdapter = NearbyDeviceAdapter(it, nodes, preferencesHelperImpl)
            devicesAdapter.setOnInviteClickListener(object :
                NearbyDeviceAdapter.OnInviteClickListener {
                override fun onInviteClick(contactKey: String) {
                    model.sendMessage(InviteState.INVITE_SENT.title, contactKey)
                }
            })
            binding.rvDevices.adapter = devicesAdapter
            binding.rvDevices.layoutManager = LinearLayoutManager(requireContext())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timer?.cancel()
    }
}