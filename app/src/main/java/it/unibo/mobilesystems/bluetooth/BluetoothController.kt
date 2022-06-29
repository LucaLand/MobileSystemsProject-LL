package it.unibo.mobilesystems.bluetooth

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.IntentFilter
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import it.unibo.kactor.utils.LateSingleInit
import it.unibo.kactor.utils.lateSingleInit
import it.unibo.mobilesystems.utils.atomicVar
import it.unibo.mobilesystems.utils.onMain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.*

const val REQUEST_ENABLE_BT = 1

/**
 * A bluetooth controller that **must** be associated with an activity.
 * **This class is not thread/coroutine safe** (apart for the methods
 * [discoverDevices] and [cancelDiscovery] that can be invoked concurrently
 * from multiple coroutines)
 */
class BluetoothController (
    private val activity : AppCompatActivity,
    private val scope : CoroutineScope = activity.lifecycleScope
) {

    private var bluetoothManager : BluetoothManager? = null
    private var bluetoothAdapter : BluetoothAdapter? = null
    private val resultChan = Channel<Any>()

    /**
     * Is `true` only if this device supports bluetooth
     */
    val isBluetoothSupported : Boolean

    /**
     * Is `true` if this controller has already setup the bluetooth
     */
    var isBluetoothSetup = false
        private set

    /**
     * Is `true` only if this controller is actually discovering bluetooth
     * device
     */
    val isDiscovering = atomicVar(false)
    private var isBluetoothSetupRequested = false

    init {
        var isBluetoothSupported = false
        onMain(scope) {bluetoothManager = activity.getSystemService(BluetoothManager::class.java)}
        if(bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager!!.adapter
            if(bluetoothAdapter != null)
                isBluetoothSupported = true
        }
        this.isBluetoothSupported = isBluetoothSupported
    }

    private lateinit var bluetoothEnableActivityResultLancher : ActivityResultLauncher<Intent>
    private val bluetoothDiscoveryBroadcastReceiver = BluetoothDiscoveryBroadcastReceiver()
    private val controllerOnDeviceDiscovered : (BluetoothDevice) -> Unit = { device ->
        scope.launch { resultChan.send(device) }
    }
    private val controllerOnDiscoveryFinished : () -> Unit = {
        scope.launch { resultChan.send(BluetoothAdapter.ACTION_DISCOVERY_FINISHED) }
    }

    /**
     * Checks if the device supports bluetooth or throws an exception if not
     * @exception [IOException] if the device does not support bluetooth
     */
    private fun checkSupportOrThrow() {
        if(!isBluetoothSupported)
            throw IOException("bluetooth not supported")
    }

    /**
     * Checks if the bluetooth has been setup by this controller and throws
     * an exception if not. Notice that this method throws an exception also if
     * this device does not support bluetooth
     * @exception [IOException] if the device does not support bluetooth
     * @exception [IllegalStateException] if the bluetooth has not been setup
     */
    private fun checkSetupOrThrow() {
        checkSupportOrThrow()
        if(!isBluetoothSetup)
            throw IllegalStateException("bluetooth is not set up")
    }

    /**
     * Setup the bluetooth. The invocation of this method is required before
     * calling other methods that requires bluetooth
     * @return `true` if bluetooth has correctly been setup
     */
    suspend fun setupBluetooth() : Boolean {
        if(!isBluetoothSetup && !isBluetoothSetupRequested) {
            isBluetoothSetupRequested = true
            if(!bluetoothAdapter!!.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                //activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT, bundle)
                bluetoothEnableActivityResultLancher = activity.registerForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    scope.launch { resultChan.send(result) }
                }
                bluetoothEnableActivityResultLancher.launch(enableBtIntent)
            } else {
                isBluetoothSetup = true
            }
        }
        val res = resultChan.receive() as ActivityResult
        isBluetoothSetup = res.resultCode == Activity.RESULT_OK
        return isBluetoothSetup
    }

    /**
     * Discovers bluetooth devices and invoke [onDeviceDiscovered] when a device is found.
     * This method waits until the discover is finished and returns the discovered devices
     * @exception [IllegalStateException] if this controller is already discovering devices or if
     * bluetooth has not been setup
     * @exception [IOException] if bluetooth is not supported
     * @param onDeviceDiscovered the action that will be invoked when a device is found
     * @return a [Set] that contains all the discovered devices
     */
    suspend fun discoverDevices(onDeviceDiscovered : (BluetoothDevice) -> Unit) : Set<BluetoothDevice> {
        isDiscovering.withValue {
            if(this.value)
                throw IllegalStateException("already discovering")
            this.value = true
        }

        try {
            checkSetupOrThrow()
            bluetoothDiscoveryBroadcastReceiver.addOnDeviceDiscovered(onDeviceDiscovered)
            bluetoothDiscoveryBroadcastReceiver.addOnDeviceDiscovered(controllerOnDeviceDiscovered)
            bluetoothDiscoveryBroadcastReceiver.addOnDiscoveryFinished(controllerOnDiscoveryFinished)

            onMain {
                val actionFoundFilter = IntentFilter(BluetoothDevice.ACTION_FOUND)
                activity.registerReceiver(bluetoothDiscoveryBroadcastReceiver, actionFoundFilter)
                val discoveryFinishedFilter = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                activity.registerReceiver(bluetoothDiscoveryBroadcastReceiver, discoveryFinishedFilter)
                bluetoothAdapter!!.startDiscovery()
            }

            val devices = mutableSetOf<BluetoothDevice>()
            var res : Any
            do {
                res = resultChan.receive()
                when(res) {
                    is BluetoothDevice -> {
                        devices.add(res)
                    }
                }

            } while (res is String && res != BluetoothAdapter.ACTION_DISCOVERY_FINISHED)

            bluetoothDiscoveryBroadcastReceiver.removeOnDeviceDiscovered(onDeviceDiscovered)
            bluetoothDiscoveryBroadcastReceiver.removeOnDeviceDiscovered(controllerOnDeviceDiscovered)
            bluetoothDiscoveryBroadcastReceiver.removeOnDiscoveryFinished(controllerOnDiscoveryFinished)
            onMain {
                activity.unregisterReceiver(bluetoothDiscoveryBroadcastReceiver)
            }

            return devices
        } finally {
            isDiscovering.set(false)
        }
    }

    /**
     * Cancels the running discovery of the devices.
     * This method can be used concurrently while the [discoverDevices] method is running so, for
     * example, it can be used to *stop* the device discovering when a certain device is found.
     * If this method is called while [discoverDevices] is performing, then [discoverDevices] returns
     * the list of the devices already discovered until [cancelDiscovery] is invoked
     */
    suspend fun cancelDiscovery() {
        isDiscovering.withValue {
            if(this.value)
                onMain { bluetoothAdapter?.cancelDiscovery() }
        }
    }

    /**
     * Performs the research of one (or more) devices using the discovery and returns an [Optional]
     * that represents the result. If it is empy, so no device with the passed parameters has been
     * found. The parameters of the research can be set using [searchOptBuilder]
     *
     * For example, if you want to search for a device with `"myDeviceName"` name:
     * ```
     * bluetoothController.discoverySearch {
     *      findFirstThatHasName("myDeviceName")
     *      whenFound { device ->
     *          show(device)
     *      }
     * }
     * ```
     * @exception [IOException] if the device does not support bluetooth
     * @exception [IllegalStateException] if the bluetooth has not been setup
     * @param searchOptBuilder a builder that can be used to set the parameter of the research
     * @return an [Optional] that contains the found device or that is empty if not found
     */
    suspend fun discoverySearch(searchOptBuilder: DiscoverySearchOptions.() -> Unit) : Optional<BluetoothDevice> {
        checkSetupOrThrow()
        val options = DiscoverySearchOptionsImpl()
        options.searchOptBuilder()
        try {
            options.searcType
        } catch (npe : NullPointerException) {
            throw IllegalArgumentException("search type not set")
        }

        val foundDevice = lateSingleInit<BluetoothDevice>()
        val callback = generateDiscoverySearchCallback(foundDevice, options)
        discoverDevices(callback)
        return Optional.ofNullable(foundDevice.getOrNull())
    }

    private suspend fun generateDiscoverySearchCallback(
        discoveredDeviceContainer : LateSingleInit<BluetoothDevice>,
        options: DiscoverySearchOptionsImpl) :
                (BluetoothDevice) -> Unit {
        return when(options.searcType) {

            DiscoverySearchType.FIND_FIRST_ADDRESS -> {
                    device ->
                if(device.address == options.address) {
                    options.whenFound(device)
                    discoveredDeviceContainer.set(device)
                    if(options.disableDiscoveryAfterFound)
                        scope.launch { this@BluetoothController.cancelDiscovery() }
                }
            }

            DiscoverySearchType.FIND_FIRST_NAME -> {
                    device ->
                if(device.name == options.name) {
                    options.whenFound(device)
                    discoveredDeviceContainer.set(device)
                    if(options.disableDiscoveryAfterFound)
                        scope.launch { this@BluetoothController.cancelDiscovery() }
                }
            }

            DiscoverySearchType.FIND_FIRST_NAME_OR_ADDRESS -> {
                    device ->
                if(device.address == options.address || device.name == options.name) {
                    options.whenFound(device)
                    discoveredDeviceContainer.set(device)
                    if(options.disableDiscoveryAfterFound)
                        scope.launch { this@BluetoothController.cancelDiscovery() }
                }
            }

            DiscoverySearchType.FIND_FIRST_NAME_AND_ADDRESS -> {
                    device ->
                if(device.address == options.address && device.name == options.name) {
                    options.whenFound(device)
                    discoveredDeviceContainer.set(device)
                    if(options.disableDiscoveryAfterFound)
                        scope.launch { this@BluetoothController.cancelDiscovery() }
                }
            }

            DiscoverySearchType.FIND_FIRST_OFFERING_SERVICE -> {
                    device ->
                if(device.uuids.find { it.uuid == options.uuid } != null) {
                    options.whenFound(device)
                    discoveredDeviceContainer.set(device)
                    if(options.disableDiscoveryAfterFound)
                        scope.launch { this@BluetoothController.cancelDiscovery() }
                }

            }
        }
    }

    /**
     * Performs a connection to the RFCOMM service offered by the device
     * passed as param with the given [uuid]
     * @exception [IOException] if the device does not support bluetooth or on error,
     * for example Bluetooth not available, or insufficient permissions or connection fails
     * @exception [IllegalStateException] if the bluetooth has not been setup
     * @param device the remote device to connect to
     * @param uuid a string that contains the `uuid` of the RFCOMM service
     */
    suspend fun connectToRfcommService(device: BluetoothDevice, uuid: String) : BluetoothSocket {
        checkSetupOrThrow()
        BluetoothSocketViewModel(device, uuid).connectToRfcommService{
            resultChan.send(it)
        }
        return (resultChan.receive() as Result<BluetoothSocket>).getOrThrow()
    }

    suspend fun searchAndConnect(address: String, uuid : String) : Optional<BluetoothSocket> {
        val discoverRes = discoverySearch {
            findFirstThatHasAddress(address)
        }

        if(!discoverRes.isPresent)
            return Optional.empty()

        return Optional.of(connectToRfcommService(discoverRes.get(), uuid))
    }

    /* BLUETOOTH UTILITY FUNCTION ******************************************************* */

    fun getPairedDevices() : Set<BluetoothDevice> {
        checkSetupOrThrow()
        return bluetoothAdapter!!.bondedDevices
    }

    fun getPairedDeviceByAddress(address : String) : Optional<BluetoothDevice> {
        checkSupportOrThrow()
        return Optional.ofNullable(bluetoothAdapter!!.bondedDevices.find { it.address == address })
    }

    fun getPairedDevicesByName(name : String) : Set<BluetoothDevice> {
        checkSetupOrThrow()
        return bluetoothAdapter!!.bondedDevices.filter { it.name == name }.toSet()
    }

    fun getPairedDevice(address: String, name : String) : Optional<BluetoothDevice> {
        return Optional.ofNullable(bluetoothAdapter!!.bondedDevices.find { it.address == address && it.name == name })
    }

    fun getPairedDevicesOfferingService(uuidString: String) : Set<BluetoothDevice> {
        val uuid = UUID.fromString(uuidString)
        return bluetoothAdapter!!.bondedDevices.filter {
                device ->
            device.uuids.find { devUuid -> devUuid.uuid == uuid } != null
        }.toSet()
    }

    fun isAddressOfPairedDevice(address : String) : Boolean {
        checkSupportOrThrow()
        return bluetoothAdapter!!.bondedDevices.find { it.address == address } != null
    }

    fun isNameOfPairedDevice(name : String) : Boolean {
        checkSupportOrThrow()
        return bluetoothAdapter!!.bondedDevices.find { it.name == name } != null
    }

    fun isPairedDevice(address: String, name: String) : Boolean {
        checkSupportOrThrow()
        return bluetoothAdapter!!.bondedDevices.find { it.address == address && it.name == name } != null
    }

}