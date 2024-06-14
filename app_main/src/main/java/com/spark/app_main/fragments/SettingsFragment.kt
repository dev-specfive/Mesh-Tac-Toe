package com.spark.app_main.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import com.spark.app.ConfigProtos
import com.spark.app.ModuleConfigProtos
import com.spark.app.R
import com.spark.app.android.BuildUtils.debug
import com.spark.app.android.BuildUtils.info
import com.spark.app.android.BuildUtils.warn
import com.spark.app.android.GeeksvilleApplication
import com.spark.app.android.getBackgroundPermissions
import com.spark.app.android.getBluetoothPermissions
import com.spark.app.android.hasBackgroundPermission
import com.spark.app.android.hasCompanionDeviceApi
import com.spark.app.android.hideKeyboard
import com.spark.app.android.isGooglePlayAvailable
import com.spark.app.android.permissionMissing
import com.spark.app.android.rationaleDialog
import com.spark.app.android.shouldShowRequestPermissionRationale
import com.spark.app.databinding.ConnectivitySettingsFragmentBinding
import com.spark.app.model.BTScanModel
import com.spark.app.model.BluetoothViewModel
import com.spark.app.model.UIViewModel
import com.spark.app.model.getInitials
import com.spark.app.service.MeshService
import com.spark.app.ui.ScreenFragment
import com.spark.app.ui.SettingsFragment
import com.spark.app.util.exceptionToSnackbar
import com.spark.app.util.getAssociationResult
import com.spark.app_main.adapters.AvailableDeviceAdapter

class SettingsFragment : ScreenFragment("Nearby") {
    private lateinit var devicesAdapter: AvailableDeviceAdapter
    private lateinit var binding: ConnectivitySettingsFragmentBinding
    private val scanModel: BTScanModel by activityViewModels()
    private val bluetoothViewModel: BluetoothViewModel by activityViewModels()
    private val model: UIViewModel by activityViewModels()
    private lateinit var devicesList: java.util.ArrayList<BTScanModel.DeviceListEntry>
    private lateinit var myProfileName: String
    private var selectedDevice: BTScanModel.DeviceListEntry? = null

    private val regions = ConfigProtos.Config.LoRaConfig.RegionCode.entries.filter {
        it != ConfigProtos.Config.LoRaConfig.RegionCode.UNRECOGNIZED
    }.map {
        it.name
    }.sorted()
    private val hasCompanionDeviceApi by lazy { requireContext().hasCompanionDeviceApi() }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ConnectivitySettingsFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val regionSpinnerListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(
            parent: AdapterView<*>,
            view: View,
            position: Int,
            id: Long
        ) {
            val item = parent.getItemAtPosition(position) as String?
            val asProto = item!!.let { ConfigProtos.Config.LoRaConfig.RegionCode.valueOf(it) }
            exceptionToSnackbar(requireView()) {
                debug("regionSpinner onItemSelected $asProto")
                if (asProto != model.region) model.region = asProto
            }
            updateNodeInfo() // We might have just changed Unset to set
        }

        override fun onNothingSelected(parent: AdapterView<*>) {
            //TODO("Not yet implemented")
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setListeners()
        initCommonUI()
    }

    private var scanning = false
    private fun scanLeDevice() {
        if (!checkBTEnabled()) return

        if (!scanning) { // Stops scanning after a pre-defined scan period.
            Handler(Looper.getMainLooper()).postDelayed({
                scanning = false
                scanModel.stopScan()
            }, SettingsFragment.SCAN_PERIOD)
            scanning = true
            scanModel.startScan(requireActivity().takeIf { hasCompanionDeviceApi })
        } else {
            scanning = false
            scanModel.stopScan()
        }
    }

    private fun setListeners() {
//        binding.connectSwitch.setOnCheckedChangeListener(listenerSwitchChange)

        binding.ivEditProfile.setOnClickListener {
            binding.regionLayout.visibility = View.GONE
            binding.groupMyProfile.visibility = View.GONE
            binding.groupEditProfile.visibility = View.VISIBLE
            myProfileName = binding.etProfileName.text.trim().toString()
        }
        binding.btnSaveChanges.setOnClickListener {
            updateDeviceName()
        }
        val requestPermissionAndScanLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                if (permissions.entries.all { it.value }) {
                    info("Bluetooth permissions granted")
                    scanLeDevice()
                } else {
                    warn("Bluetooth permissions denied")
                    model.showSnackbar(requireContext().permissionMissing)
                }
                bluetoothViewModel.permissionsUpdated()
            }
        binding.btnPairNewDevice.setOnClickListener {
            val bluetoothPermissions = requireContext().getBluetoothPermissions()
            if (bluetoothPermissions.isEmpty()) {
                scanLeDevice()
            } else {
                requireContext().rationaleDialog(
                    shouldShowRequestPermissionRationale(bluetoothPermissions)
                ) {
                    requestPermissionAndScanLauncher.launch(bluetoothPermissions)
                }
            }
        }
    }

    private fun updateDeviceName() {
        val deviceUpdatedname = binding.etProfileName.text.trim().toString()
        if (!deviceUpdatedname.equals(myProfileName, true) && deviceUpdatedname.isNotBlank()) {
            model.ourNodeInfo.value?.user?.let {
                val user = it.copy(
                    longName = deviceUpdatedname,
                    shortName = getInitials(deviceUpdatedname)
                )
                if (deviceUpdatedname.isNotEmpty()) model.setOwner(user)
            }
            requireActivity().hideKeyboard()
        }
        binding.groupMyProfile.visibility = View.VISIBLE
        binding.regionLayout.visibility = View.VISIBLE
        binding.groupEditProfile.visibility = View.GONE
    }

    private fun initDevicesAdapter(
        isMeshConnected: Boolean,
        devices: ArrayList<BTScanModel.DeviceListEntry>
    ) {
        context?.let {
            devicesAdapter = AvailableDeviceAdapter(
                it,
                devices,
                scanModel.selectedNotNull,
                isMeshConnected,
                getPreSelectedIndex(devices)
            )
            devicesAdapter.setOnItemClickListener(object :
                AvailableDeviceAdapter.OnItemClickListener {
                override fun onItemClick(selectedDeviceNearby: BTScanModel.DeviceListEntry) {
                    binding.connectSwitch.setOnCheckedChangeListener(null)
                    selectedDevice = selectedDeviceNearby
                    binding.connectSwitch.isChecked =
                        selectedDevice?.fullAddress == scanModel.selectedNotNull
                    binding.connectSwitch.setOnCheckedChangeListener(listenerSwitchChange)
                }

            })
            binding.rvDevices.adapter = devicesAdapter
            binding.rvDevices.layoutManager = LinearLayoutManager(requireContext())
        }

    }

    private fun getPreSelectedIndex(devices: ArrayList<BTScanModel.DeviceListEntry>): Int {
        return devices.indexOf(devices.firstOrNull { it.fullAddress == scanModel.selectedNotNull })
    }

    private fun initCommonUI() {

        val associationResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) {
            it.data
                ?.getAssociationResult()
                ?.let { address -> scanModel.onSelectedBle(address) }
        }

        val requestBackgroundAndCheckLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                if (permissions.entries.any { !it.value }) {
                    debug("User denied background permission")
                    model.showSnackbar(getString(R.string.why_background_required))
                }
            }

        val requestLocationAndBackgroundLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                if (permissions.entries.all { it.value }) {
                    // Older versions of android only need Location permission
                    if (!requireContext().hasBackgroundPermission())
                        requestBackgroundAndCheckLauncher.launch(requireContext().getBackgroundPermissions())
                } else {
                    debug("User denied location permission")
                    model.showSnackbar(getString(R.string.why_background_required))
                }
            }

        // init our region spinner
        val spinner = binding.regionSpinner
        val regionAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, regions)
        regionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = regionAdapter

        binding.ivDropDown.setOnClickListener {
            spinner.performClick()
        }

        model.ourNodeInfo.asLiveData().observe(viewLifecycleOwner) { node ->
            binding.tvMyProfileName.text = node?.user?.longName.orEmpty()
            binding.etProfileName.setText(node?.user?.longName.orEmpty())
            debug(node?.user?.longName.orEmpty())
        }

        scanModel.devices.observe(viewLifecycleOwner) { devices ->
//            updateDevicesButtons(devices)
            updateDevices(devices)
            debug("devices" + devices.toString())
        }

        // Only let user edit their name or set software update while connected to a radio
        model.connectionState.observe(viewLifecycleOwner) {
            updateNodeInfo()
            updateChannelName()
        }

        model.localConfig.asLiveData().observe(viewLifecycleOwner) {
            if (!model.isConnected()) {
                val configCount = it.allFields.size
                val configTotal = ConfigProtos.Config.getDescriptor().fields.size
                if (configCount > 0)
                    scanModel.setErrorText("Device config ($configCount / $configTotal)")
            } else updateNodeInfo()
        }

        model.moduleConfig.asLiveData().observe(viewLifecycleOwner) {
            if (!model.isConnected()) {
                val moduleCount = it.allFields.size
                val moduleTotal = ModuleConfigProtos.ModuleConfig.getDescriptor().fields.size
                if (moduleCount > 0)
                    scanModel.setErrorText("Module config ($moduleCount / $moduleTotal)")
            } else updateNodeInfo()
        }

        model.channels.asLiveData().observe(viewLifecycleOwner) {
            if (!model.isConnected()) {
                val maxChannels = model.maxChannels
                if (!it.hasLoraConfig() && it.settingsCount > 0)
                    scanModel.setErrorText("Channels (${it.settingsCount} / $maxChannels)")
            }
        }

        // Also watch myNodeInfo because it might change later
        model.myNodeInfo.asLiveData().observe(viewLifecycleOwner) {
            updateNodeInfo()
        }

        scanModel.errorText.observe(viewLifecycleOwner) { errMsg ->
            if (errMsg != null) {
                errMsg.apply {
                    binding.tvDeviceStatus.visibility =
                        if (contains("not connected", true)) View.GONE else View.VISIBLE
                    binding.tvDeviceStatus.text = this
                    binding.scanProgressBar.visibility =
                        if (contains("connected to", true) || contains(
                                "not connected",
                                true
                            )
                        ) View.GONE else View.VISIBLE
                }

//                Toast.makeText(context, errMsg, Toast.LENGTH_SHORT).show()
            }
        }

        // show the spinner when [spinner] is true
        scanModel.spinner.observe(viewLifecycleOwner) { show ->
//            binding.changeRadioButton.isEnabled = !show
            binding.scanProgressBar.visibility = if (show) View.VISIBLE else View.GONE
        }

        scanModel.associationRequest.observe(viewLifecycleOwner) { request ->
            request?.let {
                associationResultLauncher.launch(request)
                scanModel.clearAssociationRequest()
            }
        }

        /* binding.usernameEditText.onEditorAction(EditorInfo.IME_ACTION_DONE) {
             debug("received IME_ACTION_DONE")
             val n = binding.usernameEditText.text.toString().trim()
             model.ourNodeInfo.value?.user?.let {
                 val user = it.copy(longName = n, shortName = getInitials(n))
                 if (n.isNotEmpty()) model.setOwner(user)
             }
             requireActivity().hideKeyboard()
         }*/

        val app = (requireContext().applicationContext as GeeksvilleApplication)
        val isGooglePlayAvailable = requireContext().isGooglePlayAvailable()
        val isAnalyticsAllowed = app.isAnalyticsAllowed && isGooglePlayAvailable

    }

    private fun updateChannelName() {
//        if (model.isConnected() ){
//            model.channelSet.primaryChannel?.name?.let {
//                if (!it.equals(getString(R.string.common_channel_name), true)){
//                    val setting = channelSettings {
//                        psk = Channel.default.settings.psk
//                        name = getString(R.string.common_channel_name)
//                        uplinkEnabled = true
//                        downlinkEnabled = true
//                    }
//                    val newSet = channelSet {
//                        settings.add(setting)
//                    }
//                    model.setChannels(newSet)
//                }
//            }
//        }
    }

    private fun updateDevices(devices: MutableMap<String, BTScanModel.DeviceListEntry>?) {
        // Remove the old radio buttons and repopulate

        if (devices == null) return
        devicesList = ArrayList(devices.values)
//        devicesList = ArrayList(devices.values.filter { it.fullAddress != "n" })
        val connectionState = model.connectionState.value
        val isConnected = connectionState == MeshService.ConnectionState.CONNECTED
        initDevicesAdapter(isConnected, ArrayList(devices.values.filter { it.fullAddress != "n" }))

        // get rid of the warning text once at least one device is paired.
        // If we are running on an emulator, always leave this message showing so we can test the worst case layout
        val curRadio = scanModel.selectedAddress

        if (curRadio != null && !scanModel.isMockInterfaceAddressValid) {
//            binding.warningNotPaired.visibility = View.GONE
        } else if (bluetoothViewModel.enabled.value == true) {
//            binding.warningNotPaired.visibility = View.VISIBLE
            scanModel.setErrorText(getString(R.string.not_paired_yet))
        }
    }


    override fun onResume() {
        super.onResume()
        if (scanModel.selectedBluetooth) checkBTEnabled()
    }

    private fun checkBTEnabled(): Boolean =
        (bluetoothViewModel.enabled.value == true).also { enabled ->
            if (!enabled) {
                warn("Telling user bluetooth is disabled")
                model.showSnackbar(R.string.bluetooth_disabled)
            }
        }

    val listenerSwitchChange = CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
        if (!isChecked) {
            binding.rvDevices.alpha = 0.5F
            val defaultNoneDevice = devicesList.firstOrNull { it.fullAddress == "n" }
            connectSelectedDevice(defaultNoneDevice)
            selectedDevice = null
        } else {
            binding.rvDevices.alpha = 1.0F
            connectSelectedDevice(selectedDevice)
            if (selectedDevice != null)
                binding.scanProgressBar.visibility = View.VISIBLE
        }
    }

    private fun connectSelectedDevice(device: BTScanModel.DeviceListEntry?) {
        device?.let { _device ->
            if (!device.bonded) // If user just clicked on us, try to bond
                binding.tvDeviceStatus.setText(R.string.starting_pairing)
            scanModel.onSelected(_device)
        } ?: run {
//            binding.connectSwitch.isChecked = false
        }

    }

    private fun updateNodeInfo() {
        val connectionState = model.connectionState.value
        val isConnected = connectionState == MeshService.ConnectionState.CONNECTED

        binding.myProfileView.visibility = if (isConnected) View.VISIBLE else View.GONE
        binding.connectSwitch.setOnCheckedChangeListener(null)
        binding.connectSwitch.isChecked = isConnected
        binding.scanProgressBar.visibility = if (isConnected) View.VISIBLE else View.GONE
        binding.connectSwitch.setOnCheckedChangeListener(listenerSwitchChange)
//        binding.usernameEditText.isEnabled = isConnected && !model.isManaged

        // update the region selection from the device
//        val region919Number = ConfigProtos.Config.LoRaConfig.RegionCode.MY_919.number
//        val asProto = region919Number.let { ConfigProtos.Config.LoRaConfig.RegionCode.valueOf(it) }
//        if (isConnected) {
//            if (asProto != model.region) model.region = asProto
//        }

//        val region = model.region
//
//        debug("current region is $region")

        // update the region selection from the device
        val region = model.region
        val spinner = binding.regionSpinner
        val unsetIndex = regions.indexOf(ConfigProtos.Config.LoRaConfig.RegionCode.UNSET.name)
        spinner.onItemSelectedListener = null

        debug("current region is $region")
        var regionIndex = regions.indexOf(region.name)
        if (regionIndex == -1) // Not found, probably because the device has a region our app doesn't yet understand.  Punt and say Unset
            regionIndex = unsetIndex

        // We don't want to be notified of our own changes, so turn off listener while making them
        spinner.setSelection(regionIndex, false)
        spinner.onItemSelectedListener = regionSpinnerListener
        spinner.isEnabled = !model.isManaged

        if (isConnected) {
            binding.tvRegion.text = String.format(getString(R.string.region_lbl), region.name)
            binding.etRegionName.setText(region.name)
        }
        // Update the status string (highest priority messages first)
        val info = model.myNodeInfo.value
        when (connectionState) {
            MeshService.ConnectionState.CONNECTED ->
                if (region.number == 0) R.string.must_set_region else R.string.connected_to

            MeshService.ConnectionState.DISCONNECTED -> R.string.not_connected
            MeshService.ConnectionState.DEVICE_SLEEP -> R.string.connected_sleeping
            else -> null
        }?.let {
            val firmwareString = info?.firmwareString ?: getString(R.string.unknown)
            scanModel.setErrorText(getString(it, firmwareString))
        }
    }

    companion object {
        const val SCAN_PERIOD: Long = 10000 // Stops scanning after 10 seconds
    }

}