package it.unibo.mobilesystems.bluetoothUtils

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import it.unibo.mobilesystems.ROBOT_FOUND_ACTION
import it.unibo.mobilesystems.debugUtils.Debugger
import it.unibo.mobilesystems.receivers.ActionHandler
import it.unibo.mobilesystems.receivers.BluetoothActionReceiver
import java.util.*

const val MESSAGE_RSSI = 147

class MyBluetoothManager(val acitivity: AppCompatActivity) {

    private var bluetoothManager: BluetoothManager? = null
    lateinit var bluetoothAdapter: BluetoothAdapter
    lateinit var bluetoothLeScanner: BluetoothLeScanner

    var rssiHandler : BluetoothSocketMessagesHandler? = null

    var pairedDevicesList : Set<BluetoothDevice>? = null
    var foundedDevices : Set<BluetoothDevice>? = null

    //Parameters needed for receiver search
    lateinit var deviceNameOrAddress: String
    lateinit var deviceUUID : UUID
    var receiver : BluetoothActionReceiver? = null

    //CONSTRUCTOR
    init {
        if(bluetoothInit())
            pairedDevicesList = getPairedDevices()
        bluetoothEnable()
    }

    /** PUBLIC FUNCTIONS **/

    fun bluetoothInit(): Boolean {
        bluetoothManager = acitivity.getSystemService(BluetoothManager::class.java)
        return if(bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager!!.adapter
            true
        } else {
            false
        }
    }

    fun getPairedDevices() : Set<BluetoothDevice>? {
        return if(pairedDevicesList == null)
                    bluetoothAdapter.bondedDevices
                else
                    pairedDevicesList
    }

    fun isDevicePaired(deviceNameOrMac: String) : BluetoothDevice?{
        pairedDevicesList?.forEach { bluetoothDevice ->
            if (bluetoothDevice.name == deviceNameOrMac || bluetoothDevice.address == deviceNameOrMac) {
                return bluetoothDevice
            }
        }
        return null
    }

    fun bluetoothEnable() : Intent?{
        return if (!bluetoothAdapter.isEnabled) {
            Debugger.printDebug("Bluetooth State: DISABLED")
            Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        }else{
            Debugger.printDebug("Bluetooth State: ENABLED")
            bluetoothAdapter.enable()
            bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
            null
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(deviceNameOrAddress: String, uuid: UUID): BluetoothDevice?{
        val device = isDevicePaired(deviceNameOrAddress)
        if(device == null)
            searchAndConnect(deviceNameOrAddress, uuid)
        else
            return device
        return null
    }

    /*private fun tryDeviceConnection(device: BluetoothDevice, deviceUUID: UUID): MyBluetoothService{
        bluetoothAdapter.cancelDiscovery()
        val mac = device.address
        val name = device.name
        Debugger.printDebug("Trying Connection to Device [$name||$mac]")
        return myBluetoothService
        //IF CONNECTED SEND A DEVICE CONNECTED ACTION
    }*/

    fun findDevice(deviceNameOrAddress: String, uuid: UUID) : BluetoothDevice?{
        val device = isDevicePaired(deviceNameOrAddress)
        if(device == null)
            search(deviceNameOrAddress, uuid)
        else
            return device
        return null
    }

    /** PRIVATE FUNCTION **/

    private fun initConnectReceiver(){
        val deviceFoundHandler = ActionHandler(BluetoothDevice.ACTION_FOUND) { context, intent ->
            deviceFoundConnect(
                intent
            )
        }

        val actionHandlerList = mutableListOf(deviceFoundHandler)
        //Unregister the previous
        if(receiver!=null){
            this.acitivity.unregisterReceiver(receiver)
        }
        //Save and register the new
        receiver = BluetoothActionReceiver(actionHandlerList)
        this.acitivity.registerReceiver(receiver, IntentFilter())
    }

    private fun searchAndConnect(deviceNameOrAddress: String, uuid: UUID){
        bluetoothAdapter.cancelDiscovery()
        setDeviceParameters(deviceNameOrAddress, uuid)
        initConnectReceiver()
        bluetoothAdapter.startDiscovery()
    }

    @SuppressLint("MissingPermission")
    private fun search(deviceNameOrAddress: String, uuid: UUID){
        bluetoothAdapter.cancelDiscovery()
        setDeviceParameters(deviceNameOrAddress, uuid)
        //initSearchReceiver()
        bluetoothAdapter.startDiscovery()
    }

    private fun setDeviceParameters(deviceNameOrMac: String, uuid: UUID){
        this.deviceNameOrAddress = deviceNameOrMac
        this.deviceUUID = uuid
    }

    //CALL BACK FOR HANDLER
    private fun deviceFoundConnect(intent: Intent?){
        val device: BluetoothDevice? =
            intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        if(device?.name == this.deviceNameOrAddress || device?.address == this.deviceNameOrAddress) {
            Debugger.printDebug("DEVICE FOUND! - ${device.name} || [${device.address}]")
            this.acitivity.sendBroadcast(DeviceInfoIntentResult.createIntentResult(device).setAction(
                ROBOT_FOUND_ACTION))
            //tryDeviceConnection(device, this.deviceUUID)
        }
    }

    private fun deviceFound(intent: Intent?){
        val device: BluetoothDevice? =
            intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        if(device?.name == this.deviceNameOrAddress || device?.address == this.deviceNameOrAddress) {
            val deviceRSSI = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)
            Debugger.printDebug("DEVICE FOUND! - ${device.name} || ${device.address} || [RSSI:$deviceRSSI]")
            val extras = mutableMapOf<String,String>()
            extras[BluetoothDevice.EXTRA_DEVICE] = deviceRSSI.toString()
            this.acitivity.sendBroadcast(DeviceInfoIntentResult.createIntentResult(device, extras).setAction(
                ROBOT_FOUND_ACTION))
        }
    }

    private val SCAN_PERIOD: Long = 10000
    private var scanning = false

    fun leScan(deviceName: String){
        val leScanCallback: ScanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                super.onScanResult(callbackType, result)
                if(result.device.name == deviceName && rssiHandler != null){
                    val msg = rssiHandler!!.obtainMessage(MESSAGE_RSSI, result.rssi)
                    Debugger.printDebug("leScan()", "LeScan found: ${result.device.name} - Address: ${result.device.address} - [RSSI: ${result.rssi}")
                    Debugger.printDebug("leScan()", "Sending RSSI Message to Handler")
                    msg.sendToTarget()
                }
            }
        }

        if (!scanning && rssiHandler != null) { // Stops scanning after a pre-defined scan period.
            rssiHandler!!.postDelayed({
                scanning = false
                bluetoothLeScanner.stopScan(leScanCallback)
            }, SCAN_PERIOD)
            scanning = true
            Debugger.printDebug("leScan()", "Started LeScan")
            bluetoothLeScanner.startScan(leScanCallback)
        } else {
            scanning = false
            bluetoothLeScanner.stopScan(leScanCallback)
        }
    }




}