package com.spark.appa

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Bundle
import android.os.RemoteException
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.spark.app.IMeshService
import com.spark.app.MyNodeInfo
import com.spark.app.android.BindFailedException
import com.spark.app.android.Logging
import com.spark.app.android.ServiceClient
import com.spark.app.android.getBluetoothPermissions
import com.spark.app.android.getNotificationPermissions
import com.spark.app.android.hasBluetoothPermission
import com.spark.app.android.hasNotificationPermission
import com.spark.app.android.permissionMissing
import com.spark.app.android.rationaleDialog
import com.spark.app.android.shouldShowRequestPermissionRationale
import com.spark.app.concurrent.handledLaunch
import com.spark.app.model.BluetoothViewModel
import com.spark.app.model.DeviceVersion
import com.spark.app.model.UIViewModel
import com.spark.app.model.primaryChannel
import com.spark.app.model.toChannelSet
import com.spark.app.repository.radio.BluetoothInterface
import com.spark.app.service.EXTRA_ACCEPTED_CHANNEL_KEY
import com.spark.app.service.InviteState
import com.spark.app.service.MeshService
import com.spark.app.service.ServiceRepository
import com.spark.app.service.startService
import com.spark.app.util.Exceptions
import com.spark.appa.databinding.ActivityMainTabBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import javax.inject.Inject

@AndroidEntryPoint
class MainTabActivity : AppCompatActivity(), Logging {
    private lateinit var binding: ActivityMainTabBinding
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var navHostFragment: NavHostFragment
    private lateinit var navController: NavController
    private val bluetoothViewModel: BluetoothViewModel by viewModels()
    private var requestedEnable = false
    private val model: UIViewModel by viewModels()
    private val mainScope = CoroutineScope(Dispatchers.Main + Job())
    private var acceptedChannel: String? = null
    private var requestedChannelUrl: Uri? = null
    private var currentFragmentID: Int = -1

    @Inject
    internal lateinit var serviceRepository: ServiceRepository
    private val bleRequestEnable = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        requestedEnable = false
    }

    private val bluetoothPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result.entries.all { it.value }) {
                info("Bluetooth permissions granted")
            } else {
                warn("Bluetooth permissions denied")
                showSnackbar(permissionMissing)
            }
            requestedEnable = false
            bluetoothViewModel.permissionsUpdated()
        }

    private val notificationPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result.entries.all { it.value }) {
                info("Notification permissions granted")
            } else {
                warn("Notification permissions denied")
                showSnackbar(getString(R.string.notification_denied))
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainTabBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Set the Toolbar as the action bar
        setUpToolbar()
        bottomNavigationView = findViewById(R.id.bottom_navigation_view)
        navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragmnet) as NavHostFragment
        navController = navHostFragment.navController
        NavigationUI.setupWithNavController(
            bottomNavigationView, navController
        )
        var isConfirmedBack = true
        val appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            currentFragmentID = destination.id
            val isGameFragment = destination.id == R.id.gamepad_fragment
            supportActionBar?.setDisplayHomeAsUpEnabled(isGameFragment)
            binding.toolbar.findViewById<ImageView>(R.id.iv_spec5_icon).visibility =
                if (isGameFragment) View.GONE else View.VISIBLE
//            binding.bottomNavigationView.visibility =
//                if (isGameFragment) View.GONE else View.VISIBLE
            isConfirmedBack = isGameFragment
        }
        handleIntent(intent)

        // Set up back press listener
        onBackPressedDispatcher.addCallback(this) {
            if (isConfirmedBack) {
                showExitConfirmationDialog {
                    isConfirmedBack = false
                    if (currentFragmentID == R.id.gamepad_fragment) {
                        model.sendMessage(InviteState.LEFT_GAME.title)
                    }
                    navController.navigateUp()
                }
            } else {
                navController.navigateUp()
            }
        }

        bottomNavigationView.setOnItemSelectedListener { item ->
            if (bottomNavigationView.selectedItemId != item.itemId && currentFragmentID == R.id.gamepad_fragment) {
                onBackPressedDispatcher.onBackPressed()
                return@setOnItemSelectedListener false
            } else {


                if (currentFragmentID != R.id.nearby_fragment) {
                    navController.navigateUp()
                }

                navController.navigate(item.itemId)
                return@setOnItemSelectedListener true
            }
        }


    }

    private fun showExitConfirmationDialog(onBack: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("Exit Confirmation")
            .setMessage("Are you sure you want to exit?")
            .setPositiveButton("Yes") { dialog, which ->
                // Handle exit action here
                onBack.invoke()
            }
            .setNegativeButton("No") { dialog, which ->
                // Do nothing, dismiss the dialog
            }
            .show()
    }

    private fun setUpToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.toolbar.navigationIcon = ContextCompat.getDrawable(this, R.drawable.arrow_back)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return false
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val appLinkAction = intent.action
        val appLinkData: Uri? = intent.data

        when (appLinkAction) {
            Intent.ACTION_VIEW -> {
                debug("Asked to open a channel URL - ask user if they want to switch to that channel.  If so send the config to the radio")
                requestedChannelUrl = appLinkData

                // if the device is connected already, process it now
                perhapsChangeChannel()

                // We now wait for the device to connect, once connected, we ask the user if they want to switch to the new channel
            }

            UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                showSettingsPage()
            }

            Intent.ACTION_MAIN -> {
            }

            else -> {
                warn("Unexpected action $appLinkAction")
            }
        }
    }

    private fun showSnackbar(msgId: Int) {
        try {
            Snackbar.make(binding.root, msgId, Snackbar.LENGTH_LONG).show()
        } catch (ex: IllegalStateException) {
            errormsg("Snackbar couldn't find view for msgId $msgId")
        }
    }

    private fun showSnackbar(msg: String) {
        try {
            Snackbar.make(binding.root, msg, Snackbar.LENGTH_INDEFINITE)
                .apply { view.findViewById<TextView>(R.id.snackbar_text).isSingleLine = false }
                .setAction(R.string.okay) {
                    // dismiss
                }
                .show()
        } catch (ex: IllegalStateException) {
            errormsg("Snackbar couldn't find view for msgString $msg")
        }
    }

    private var connectionJob: Job? = null
    override fun onStop() {
        unbindMeshService()

        model.connectionState.removeObservers(this)
        bluetoothViewModel.enabled.removeObservers(this)
        model.requestChannelUrl.removeObservers(this)
        model.snackbarText.removeObservers(this)
        model.currentTab.removeObservers(this)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(inviteAcceptedReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(inviteReceiver)

        super.onStop()
    }

    private val inviteAcceptedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            acceptedChannel = intent?.getStringExtra(EXTRA_ACCEPTED_CHANNEL_KEY)
            if (!acceptedChannel.isNullOrBlank()) {
                model.setAccepted(acceptedChannel!!, true)
                model.setContactKey(acceptedChannel!!)
                showGamePlay()
            }
        }
    }

    private val inviteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            bottomNavigationView.getOrCreateBadge(R.id.message_fragment).backgroundColor =
                ContextCompat.getColor(
                    this@MainTabActivity,
                    R.color.badge_color
                )
        }
    }

    fun resetBadge() {
        bottomNavigationView.getOrCreateBadge(R.id.message_fragment).backgroundColor =
            ContextCompat.getColor(
                this,
                R.color.zxing_transparent
            )
    }

    private fun unbindMeshService() {
        // If we have received the service, and hence registered with
        // it, then now is the time to unregister.
        // if we never connected, do nothing
        debug("Unbinding from mesh service!")
        connectionJob?.let { job ->
            connectionJob = null
            warn("We had a pending onConnection job, so we are cancelling it")
            job.cancel("unbinding")
        }
        mesh.close()
        serviceRepository.setMeshService(null)
    }

    private val mesh = object :
        ServiceClient<IMeshService>({
            IMeshService.Stub.asInterface(it)
        }) {
        override fun onConnected(service: IMeshService) {
            connectionJob = mainScope.handledLaunch {
                serviceRepository.setMeshService(service)

                try {
                    val connectionState =
                        MeshService.ConnectionState.valueOf(service.connectionState())

                    // We won't receive a notify for the initial state of connection, so we force an update here
                    onMeshConnectionChanged(connectionState)
                } catch (ex: RemoteException) {
                    errormsg("Device error during init ${ex.message}")
                } finally {
                    connectionJob = null
                }

                debug("connected to mesh service, connectionState=${model.connectionState.value}")
            }
        }

        override fun onDisconnected() {
            serviceRepository.setMeshService(null)
        }
    }

    override fun onStart() {
        super.onStart()

        model.connectionState.observe(this) { state ->
            onMeshConnectionChanged(state)
            updateConnectionStatusImage(state)
        }

        bluetoothViewModel.enabled.observe(this) { enabled ->
            if (!enabled && !requestedEnable && model.selectedBluetooth) {
                requestedEnable = true
                if (hasBluetoothPermission()) {
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    bleRequestEnable.launch(enableBtIntent)
                } else {
                    val bluetoothPermissions = getBluetoothPermissions()
                    rationaleDialog(shouldShowRequestPermissionRationale(bluetoothPermissions)) {
                        bluetoothPermissionsLauncher.launch(bluetoothPermissions)
                    }
                }
            }
        }

        // Call perhapsChangeChannel() whenever [requestChannelUrl] updates with a non-null value
        model.requestChannelUrl.observe(this) { url ->
            url?.let {
                requestedChannelUrl = url
                model.clearRequestChannelUrl()
                perhapsChangeChannel()
            }
        }

        // Call showSnackbar() whenever [snackbarText] updates with a non-null value
        model.snackbarText.observe(this) { text ->
            if (text is Int) showSnackbar(text)
            if (text is String) showSnackbar(text)
            if (text != null) model.clearSnackbarText()
        }


        try {
            bindMeshService()
        } catch (ex: BindFailedException) {
            // App is probably shutting down, ignore
            errormsg("Bind of MeshService failed")
        }

        val bonded = model.bondedAddress != null
        if (!bonded) showSettingsPage()

        LocalBroadcastManager.getInstance(this)
            .registerReceiver(
                inviteAcceptedReceiver,
                IntentFilter(InviteState.INVITE_ACCEPTED.title)
            )
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(inviteReceiver, IntentFilter(InviteState.INVITE_SENT.title))
    }

    private fun bindMeshService() {
        debug("Binding to mesh service!")
        // we bind using the well known name, to make sure 3rd party apps could also
        if (serviceRepository.meshService != null) {
            /* This problem can occur if we unbind, but there is already an onConnected job waiting to run.  That job runs and then makes meshService != null again
            I think I've fixed this by cancelling connectionJob.  We'll see!
             */
            Exceptions.reportError("meshService was supposed to be null, ignoring (but reporting a bug)")
        }

        try {
            MeshService.startService(this) // Start the service so it stays running even after we unbind
        } catch (ex: Exception) {
            // Old samsung phones have a race condition andthis might rarely fail.  Which is probably find because the bind will be sufficient most of the time
            errormsg("Failed to start service from activity - but ignoring because bind will work ${ex.message}")
        }

        // ALSO bind so we can use the api
        mesh.connect(
            this,
            MeshService.createIntent(),
            Context.BIND_AUTO_CREATE + Context.BIND_ABOVE_CLIENT
        )
    }

    private fun perhapsChangeChannel(url: Uri? = requestedChannelUrl) {
        // if the device is connected already, process it now
        if (url != null && model.isConnected()) {
            requestedChannelUrl = null
            try {
                val channels = url.toChannelSet()
                val primary = channels.primaryChannel
                if (primary == null)
                    showSnackbar(R.string.channel_invalid)
                else {

                    MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.new_channel_rcvd)
                        .setMessage(getString(R.string.do_you_want_switch).format(primary.name))
                        .setNeutralButton(R.string.cancel) { _, _ ->
                            // Do nothing
                        }
                        .setPositiveButton(R.string.accept) { _, _ ->
                            debug("Setting channel from URL")
                            try {
                                model.setChannels(channels)
                            } catch (ex: RemoteException) {
                                errormsg("Couldn't change channel ${ex.message}")
                                showSnackbar(R.string.cant_change_no_radio)
                            }
                        }
                        .show()
                }
            } catch (ex: Throwable) {
                errormsg("Channel url error: ${ex.message}")
                showSnackbar("${getString(R.string.channel_invalid)}: ${ex.message}")
            }
        }
    }

    private fun updateConnectionStatusImage(connected: MeshService.ConnectionState) {
        if (model.actionBarMenu == null) return

        val (image, tooltip) = when (connected) {
            MeshService.ConnectionState.CONNECTED -> R.drawable.cloud_on to R.string.connected
            MeshService.ConnectionState.DEVICE_SLEEP -> R.drawable.ic_twotone_cloud_upload_24 to R.string.device_sleeping
            MeshService.ConnectionState.DISCONNECTED -> R.drawable.cloud_off to R.string.disconnected
        }

        val item = model.actionBarMenu?.findItem(R.id.connectStatusImage)
        if (item != null) {
            item.setIcon(image)
            item.setTitle(tooltip)
        }
    }

    private fun onMeshConnectionChanged(newConnection: MeshService.ConnectionState) {
        if (newConnection == MeshService.ConnectionState.CONNECTED) {
            serviceRepository.meshService?.let { service ->
                try {
                    val info: MyNodeInfo? = service.myNodeInfo // this can be null

                    if (info != null) {
                        val isOld = info.minAppVersion > 30221
                        if (isOld)
                            showAlert(R.string.app_too_old, R.string.must_update)
                        else {
                            // If we are already doing an update don't put up a dialog or try to get device info
                            val isUpdating = service.updateStatus >= 0
                            if (!isUpdating) {
                                val curVer = DeviceVersion(info.firmwareVersion ?: "0.0.0")

                                if (curVer < MeshService.minDeviceVersion)
                                    showAlert(R.string.firmware_too_old, R.string.firmware_old)
                                else {
                                    // If our app is too old/new, we probably don't understand the new DeviceConfig messages, so we don't read them until here

                                    // we have a connection to our device now, do the channel change
                                    perhapsChangeChannel()
                                }
                            }
                        }
                    } else if (BluetoothInterface.invalidVersion) {
                        showAlert(R.string.firmware_too_old, R.string.firmware_old)
                    }
                } catch (ex: RemoteException) {
                    warn("Abandoning connect $ex, because we probably just lost device connection")
                }
                // if provideLocation enabled: Start providing location (from phone GPS) to mesh
                if (model.provideLocation.value == true)
                    service.startProvideLocation()
            }

            if (!hasNotificationPermission()) {
                val notificationPermissions = getNotificationPermissions()
                rationaleDialog(
                    shouldShowRequestPermissionRationale(notificationPermissions),
                    R.string.notification_required,
                    getString(R.string.why_notification_required),
                ) {
                    notificationPermissionsLauncher.launch(notificationPermissions)
                }
            }
        }
    }

    private fun showAlert(titleText: Int, messageText: Int) {

        // make links clickable per https://stackoverflow.com/a/62642807
        // val messageStr = getText(messageText)

        val builder = MaterialAlertDialogBuilder(this)
            .setCancelable(false)
            .setTitle(titleText)
            .setMessage(messageText)
            .setPositiveButton(R.string.okay) { _, _ ->
                info("User acknowledged")
            }

        val dialog = builder.show()

        // Make the textview clickable. Must be called after show()
        val view = (dialog.findViewById(android.R.id.message) as TextView?)!!
        // Linkify.addLinks(view, Linkify.ALL) // not needed with this method
        view.movementMethod = LinkMovementMethod.getInstance()

        showSettingsPage() // Default to the settings page in this case
    }

    private fun showSettingsPage() {
//        navController.navigate(R.id.settings_fragment)
    }

    fun showGamePlay() {
        navController.navigate(R.id.gamepad_fragment)
    }

}