package com.spark.appa.fragments

import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import com.spark.app.NodeInfo
import com.spark.app.R
import com.spark.app.databinding.FragmentNearbyDevicesBinding
import com.spark.app.model.UIViewModel
import com.spark.app.service.InviteState
import com.spark.app.ui.ScreenFragment
import com.spark.app.ui.adapters.NearbyDeviceAdapter

class NearbyDevicesFragment : ScreenFragment("Nearby") {
    private lateinit var devicesAdapter: NearbyDeviceAdapter
    private lateinit var binding: FragmentNearbyDevicesBinding
    private val model: UIViewModel by activityViewModels()

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
    }

    private fun setObservers() {

        model.nodeDB.nodeDBbyNum.asLiveData().observe(viewLifecycleOwner) {
            if (devicesAdapter.itemCount == 0)
                devicesAdapter.onNodesChanged(it.values.toTypedArray())
        }
    }

    private fun initDevicesAdapter() {
        context?.let {
            val nodes = arrayOf<NodeInfo>()
            devicesAdapter = NearbyDeviceAdapter(it, nodes)
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
}