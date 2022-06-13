package it.unibo.mobilesystems.bluetoothUtils

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import it.unibo.mobilesystems.debugUtils.Debugger
import it.unibo.mobilesystems.recivers.ActionHandler
import it.unibo.mobilesystems.recivers.BluetoothActionReceiver
import java.util.*

class MyBluetoothManager(val acitivity: AppCompatActivity) {


    private var bluetoothManager: BluetoothManager? = null
    private lateinit var bluetoothAdapter: BluetoothAdapter
    lateinit var myBluetoothService: MyBluetoothService
    var pairedDevicesList : Set<BluetoothDevice>? = null
    var foundedDevices : Set<BluetoothDevice>? = null

    //Parameters needed for receiver search
    lateinit var deviceNameOrAddress: String
    lateinit var deviceUUID : UUID
    lateinit var receiver : BluetoothActionReceiver

    //CONSTRUCTOR
    init {
        if(bluetoothInit())
            pairedDevicesList = getPairedDevices()
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
            null
        }
    }

    fun connectToDevice(device: BluetoothDevice, deviceUUID: UUID): MyBluetoothService{
        bluetoothAdapter.cancelDiscovery()
        val mac = device.address
        val name = device.name
        Debugger.printDebug("Trying Connection to Device [$name||$mac]")

        Debugger.printDebug("Creating SocketThread")
        myBluetoothService = MyBluetoothService(mac, deviceUUID, bluetoothAdapter)
        return myBluetoothService
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(deviceNameOrAddress: String, uuid: UUID){
        val device = isDevicePaired(deviceNameOrAddress)
        if(device == null)
            searchAndConnect(deviceNameOrAddress, uuid)
        else
            connectToDevice(device, uuid)
    }


    fun findDevice(deviceNameOrAddress: String, uuid: UUID) : BluetoothDevice?{
        val device = isDevicePaired(deviceNameOrAddress)
        if(device == null)
            search(deviceNameOrAddress, uuid)
        else
            return device
        return null
    }
    
    private fun initSearchReceiver() {
        TODO("Not yet implemented")
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
        this.acitivity.unregisterReceiver(receiver)
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
        initSearchReceiver()
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
            connectToDevice(device, this.deviceUUID)
        }
    }





}