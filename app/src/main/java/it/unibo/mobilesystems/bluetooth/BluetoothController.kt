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
import it.unibo.mobilesystems.debugUtils.Debugger
import java.io.IOException
import java.util.*

const val REQUEST_ENABLE_BT = 1

class BluetoothController(
    private val activity : AppCompatActivity
) {

    private var bluetoothManager : BluetoothManager? = null
    private var bluetoothAdapter : BluetoothAdapter? = null

    private val isBluetoothSupported : Boolean
    var isBluetoothSetup = false
        private set
    var isDiscovering = false
        private set
    private var isBluetoothSetupRequested = false

    init {
        var isBluetoothSupported = false
        bluetoothManager = activity.getSystemService(BluetoothManager::class.java)
        if(bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager!!.adapter
            if(bluetoothAdapter != null)
                isBluetoothSupported = true
        }
        this.isBluetoothSupported = isBluetoothSupported
    }

    private lateinit var bluetoothEnableActivityResultLancher : ActivityResultLauncher<Intent>
    private val bluetoothDiscoveryBroadcastReceiver = BluetoothDiscoveryBroadcastReceiver()

    private fun checkSupportOrThrow() {
        if(!isBluetoothSupported)
            throw IOException("bluetooth not supported")
    }

    private fun checkSetupOrThrow() {
        checkSupportOrThrow()
        if(!isBluetoothSetup)
            throw IllegalStateException("bluetooth is not set up")
    }

    fun asyncSetupBluetooth(onBluetoothSetup : (ActivityResult) -> Unit) {
        if(!isBluetoothSetup && !isBluetoothSetupRequested) {
            isBluetoothSetupRequested = true
            if(!bluetoothAdapter!!.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                //activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT, bundle)
                bluetoothEnableActivityResultLancher = activity.registerForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) {
                    when (it.resultCode) {
                        Activity.RESULT_OK -> this@BluetoothController.isBluetoothSetup = true
                    }
                    onBluetoothSetup(it)
                }
                bluetoothEnableActivityResultLancher.launch(enableBtIntent)
            } else {
                isBluetoothSetup = true
                onBluetoothSetup(ActivityResult(Activity.RESULT_OK, null))
            }
        }
    }

    fun startAsyncDiscoveringDevices(onDeviceDiscovered : (BluetoothDevice) -> Unit) {
        checkSetupOrThrow()
        if(!isDiscovering) {
            bluetoothDiscoveryBroadcastReceiver.addOnDeviceDiscovered(onDeviceDiscovered)
            val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
            activity.registerReceiver(bluetoothDiscoveryBroadcastReceiver, filter)
            bluetoothAdapter!!.startDiscovery()
            isDiscovering = true
        }
    }

    fun stopDiscoveringDevices() {
        checkSetupOrThrow()
        if(isDiscovering) {
            activity.unregisterReceiver(bluetoothDiscoveryBroadcastReceiver)
            bluetoothAdapter!!.cancelDiscovery()
            bluetoothDiscoveryBroadcastReceiver.removeAllOnDeviceDiscovered()
            isDiscovering = false
        }
    }

    fun asyncDiscoverySearch(searchOptBuilder: DiscoverySearchOptions.() -> Unit) {
        checkSetupOrThrow()
        val options = DiscoverySearchOptionsImpl()
        options.searchOptBuilder()
        try {
            options.searcType
        } catch (npe : NullPointerException) {
            throw IllegalArgumentException("search type not set")
        }

        val callback = generateDiscoverySearchCallback(options)
        startAsyncDiscoveringDevices(callback)
    }

    private fun generateDiscoverySearchCallback(options: DiscoverySearchOptionsImpl) :
                (BluetoothDevice) -> Unit {
        return when(options.searcType) {

            DiscoverySearchType.FIND_FIRST_ADDRESS -> {
                    device ->
                if(device.address == options.address) {
                    options.whenFound(device)
                    if(options.disableDiscoveryAfterFound)
                        activity.runOnUiThread { this@BluetoothController.stopDiscoveringDevices() }
                }
            }

            DiscoverySearchType.FIND_FIRST_NAME -> {
                    device ->
                if(device.name == options.name) {
                    options.whenFound(device)
                    if(options.disableDiscoveryAfterFound)
                        activity.runOnUiThread { this@BluetoothController.stopDiscoveringDevices() }
                }
            }

            DiscoverySearchType.FIND_FIRST_NAME_OR_ADDRESS -> {
                    device ->
                if(device.address == options.address || device.name == options.name) {
                    options.whenFound(device)
                    if(options.disableDiscoveryAfterFound)
                        activity.runOnUiThread { this@BluetoothController.stopDiscoveringDevices() }
                }
            }

            DiscoverySearchType.FIND_FIRST_NAME_AND_ADDRESS -> {
                    device ->
                if(device.address == options.address && device.name == options.name) {
                    options.whenFound(device)
                    if(options.disableDiscoveryAfterFound)
                        activity.runOnUiThread { this@BluetoothController.stopDiscoveringDevices() }
                }
            }

            DiscoverySearchType.FIND_FIRST_OFFERING_SERVICE -> {
                    device ->
                if(device.uuids.find { it.uuid == options.uuid } != null) {
                    options.whenFound(device)
                    if(options.disableDiscoveryAfterFound)
                        activity.runOnUiThread { this@BluetoothController.stopDiscoveringDevices() }
                }

            }
        }
    }

    fun asyncConnect(device: BluetoothDevice, uuid: String,
                     onSocketConnection : suspend (Result<BluetoothSocket>) -> Unit) {
        BluetoothSocketViewModel(device, uuid).connectSocket(onSocketConnection)
    }

    fun asyncDiscoverySearchAndConnect(address: String, uuid : String,
                     onSocketConnection : suspend (Result<BluetoothSocket>) -> Unit) {
        asyncDiscoverySearch {
            findFirstThatHasAddress(address)
            whenFound { device ->
                BluetoothSocketViewModel(device, uuid).connectSocket(onSocketConnection)
            }
        }
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