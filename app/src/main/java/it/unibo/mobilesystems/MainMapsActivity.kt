package it.unibo.mobilesystems

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentSender
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomsheet.BottomSheetBehavior
import it.unibo.kactor.MsgUtil
import it.unibo.mobilesystems.bluetoothUtils.*
import it.unibo.mobilesystems.databinding.ActivityMapsBinding
import it.unibo.mobilesystems.debugUtils.Debugger
import it.unibo.mobilesystems.fileUtils.ConfigManager
import it.unibo.mobilesystems.permissionManager.PermissionType
import it.unibo.mobilesystems.permissionManager.PermissionsManager.permissionCheck
import it.unibo.mobilesystems.permissionManager.PermissionsManager.permissionsCheck
import it.unibo.mobilesystems.permissionManager.PermissionsManager.permissionsRequest
import it.unibo.mobilesystems.receivers.ActionHandler
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.*

const val BLUETOOTH_CONNECT_ACTIVITY_CODE = 486431
const val RESULT_DEVICE_NAME_CODE = "deviceName"
const val RESULT_DEVICE_ADDRESS_CODE = "deviceAddress"
const val RESULT_DEVICE_UUID_CODE = "deviceAddress"

const val CONFIG_FILE_NAME = "file.conf"
const val UUID_CONFIG = "UUID"
const val ROBOT_DEVICE_NAME = "DEVICE NAME"
const val ROBOT_DEVICE_ADDRESS = "DEVICE MAC"

const val ROBOT_FOUND_ACTION = "ROBOT_FOUND_ACTION"
const val SOCKET_OPENED_ACTION = "SOCKET_OPENED_ACTION"
const val SOCKET_CLOSED_ACTION = "SOCKET_CLOSED_ACTION"



class MainMapsActivity : AppCompatActivity(), LocationListener {

    //TODO: FIX the bottom pad animation and motion (can also be opened with a button, and do the animation programmatically)
    //TODO(-Sulla disconnessione del dispositivo (nella maps activity) devo bloccare l'utillizzo dell socket, usiamo la variabile enabled del MyService Object)
    //TODO(Implement the use of BluetoothCompanion - See Android Developer Guide)

    //TODO(LeScan non parte)

    private val bluetoothMessageHandler: BluetoothSocketMessagesHandler = BluetoothSocketMessagesHandler()
    private var myBluetoothManager : MyBluetoothManager? = null
    private var bluetoothActivityLauncher : ActivityResultLauncher<Intent>? = null

    private lateinit var binding: ActivityMapsBinding

    private lateinit var map : MapView
    private lateinit var mLocationOverlay : MyLocationNewOverlay
    private lateinit var locationProvider: String

    private lateinit var rssiProgressBarr : ProgressBar

    private var deviceName: String? = null

    var uuid : UUID? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //UI COMPONENTS
        rssiProgressBarr = findViewById(R.id.rssi_progress_bar)

        //PERMISSION
        internetPermissions()
        if(locationPermission()){
            enableLocationOnDevice()
        }

        setProvider()

        Configuration.getInstance().load(applicationContext, androidx.preference.PreferenceManager.getDefaultSharedPreferences(applicationContext))

        //CONFIG
        ConfigManager.init(this)

        deviceName = ConfigManager.getConfigString(ROBOT_DEVICE_NAME)
        MyBluetoothService.setServiceHandler(bluetoothMessageHandler)

        //Handlers For LeScanner Rssi Messages
        myBluetoothManager?.rssiHandler = BluetoothSocketMessagesHandler().setCallbackForMessage(MESSAGE_RSSI) {string ->
            updateRSSIValue(string?.toInt())
            Debugger.printDebug("RSSI Handler", "Recived RSSI Message - Updated RSSI Progress Bar")
        }

        //Handlers For Socket Messages
        bluetoothMessageHandler.setCallbackForMessage(MESSAGE_READ) { string ->
            Debugger.printDebug("Handler-Receive","Received MSG: $string")
            Toast.makeText(this, "Received: $string", 5 ).show()
        }
        bluetoothMessageHandler.setCallbackForMessage(MESSAGE_WRITE) { string ->
            Debugger.printDebug("Handler-Send","Sent MSG: $string")
            //Toast.makeText(this, "Sent: $string", 5 ).show()
        }
        bluetoothMessageHandler.setCallbackForMessage(MESSAGE_TOAST) { string ->
            Debugger.printDebug("Handler-Send","Sent MSG: $string")
            Toast.makeText(this, "Toast: $string", 5 ).show()
        }

        //HANDLERS for Socket Created and Socket Error (Closed)
        bluetoothMessageHandler.setCallbackForMessage(MESSAGE_CONNECTION_TRUE) {
            Debugger.printDebug("Maps-Activity", "RECEIVED MESSAGE_CONNECTION_TRUE - Sent Socket Opened Action")
            sendBroadcast(Intent().setAction(SOCKET_OPENED_ACTION))
        }
        bluetoothMessageHandler.setCallbackForMessage(MESSAGE_SOCKET_ERROR) {
            Debugger.printDebug("Maps-Activity", "Received Error Message: Socket Closed!")
            sendBroadcast(Intent().setAction(SOCKET_CLOSED_ACTION))
            MyBluetoothService.restartConnection() }


        //RECEIVER FOR ROBOT FOUND to get RSSI
        this.registerReceiver(ActionHandler(BluetoothDevice.ACTION_ACL_CONNECTED){context, intent ->
            Debugger.printDebug("MAPS Activity", "Received ACL_CONNECTED Action")
            val mIntentAction = intent!!.action
            if (BluetoothDevice.ACTION_ACL_CONNECTED == mIntentAction) {
                Debugger.printDebug("MAPS Activity", "Received ACL_CONNECTED Action")
                val RSSI =
                    intent!!.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()
                val mDeviceName = intent!!.getStringExtra(BluetoothDevice.EXTRA_NAME)
                if(mDeviceName == ConfigManager.getConfigString(ROBOT_DEVICE_NAME))
                    updateRSSIValue(RSSI)
            }
        }.createBroadcastReceiver(), IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED))



        bottomPadInit()

        map = startMap()

        //Check Bluetooth Permission and start BluetoothActivity
        if(bluetoothPermission()){
            //init BluetoothManager
            myBluetoothManager = MyBluetoothManager(this)
            startBluetoothActivity(initRegisterForBluetoothActivityResult())
        }else{
            bluetoothActivityLauncher = initRegisterForBluetoothActivityResult()
        }
    }

    private fun uuidInit() : UUID {
        val uuidString = ConfigManager.getConfigString(UUID_CONFIG)
        return if(uuidString == null)
            UUID.randomUUID()
        else
            UUID.fromString(uuidString)
    }

    private fun initRegisterForBluetoothActivityResult(): ActivityResultLauncher<Intent> {
        val startBluetoothActivityForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                Debugger.printDebug( "Bluetooth Activity Returned")
                // Handle the Intent
                val resultMap : MutableMap<String,String?> = DeviceInfoIntentResult.getDeviceResult(intent)
                //deviceName = resultMap[RESULT_DEVICE_NAME_CODE]
                //HANDLE RESULT FROM THE BLUETOOTH ACTIVITY
                myBluetoothManager!!.bluetoothLeScanner = myBluetoothManager!!.bluetoothAdapter.bluetoothLeScanner
                //Debugger.printDebug("MAPS Activity", "DeviceName: $deviceName")
                if (deviceName != null) {
                    Debugger.printDebug("MAPS Activity", "Starting LeScan")
                    myBluetoothManager?.leScan(deviceName!!)
                }else{

                }
            }
        }
        return startBluetoothActivityForResult
    }

    //TODO(Search Device of LeScan by MacAddress - becouse we just have the device connected, returning in maps Activity)
    private fun startBluetoothActivity(activityResoultLauncher :ActivityResultLauncher<Intent>) {
        val intent = Intent(this, BluetoothConnectionActivity::class.java)
        bluetoothActivityLauncher?.launch(intent)
    }


    @SuppressLint("MissingPermission")
    private fun startMap (): MapView {
        map = findViewById(R.id.mapview)
        map.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
        map.setMultiTouchControls(true)
        map.maxZoomLevel = 22.0
        map.minZoomLevel = 1.0

        mLocationOverlay = MyLocationNewOverlay(map)
        mLocationOverlay.enableMyLocation()
        map.overlays.add(this.mLocationOverlay)

        Debugger.printDebug("LocationProvider: $locationProvider ${mLocationOverlay.myLocationProvider}")

        //ITALY Center GeoPoint
        map.controller.setCenter(GeoPoint(42.820897, 12.532178))

        //MAP INITIALIZATION
        map.controller.zoomTo(8, 0)
        mLocationOverlay.enableFollowLocation()
        return map
    }

    private fun bottomPadInit(){

        val bottomPad : ConstraintLayout = findViewById(R.id.bottom_sheet_layout)
        val sheetBehavior = BottomSheetBehavior.from(bottomPad)

        sheetBehavior.isHideable = false

    }


    private fun enableLocationOnDevice() {
    //CODE BY : https://stackoverflow.com/questions/43518520/how-to-ask-user-to-enable-gps-at-the-launch-of-application

        val locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val result: Task<LocationSettingsResponse> = LocationServices.getSettingsClient(this).checkLocationSettings(builder.build())
        result.addOnCompleteListener { task ->
            try {
                val response = task.getResult(ApiException::class.java)
                // All location settings are satisfied. The client can initialize location
                // requests here.
            } catch (exception: ApiException) {
                when (exception.statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                        try {
                            // Cast to a resolvable exception.
                            val resolvable: ResolvableApiException = exception as ResolvableApiException
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            resolvable.startResolutionForResult(
                                this, LocationRequest.PRIORITY_HIGH_ACCURACY
                            )
                        } catch (e: IntentSender.SendIntentException) {
                            // Ignore the error.
                        } catch (e: ClassCastException) {
                            // Ignore, should be an impossible error.
                        }
                    }
                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                        // Location settings are not satisfied. But could be fixed by showing the
                        // user a dialog.

                        // Location settings are not satisfied. However, we have no way to fix the
                        // settings so we won't show the dialog.
                    }
                }
            }
        }
    }

    private fun internetPermissions(){
        val permissions = arrayOf(  Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE)

        if(!permissionsCheck(this, permissions))
            permissionsRequest(this, permissions, 4)
    }

    private fun locationPermission(): Boolean{
        return permissionCheck(PermissionType.Location, this)
    }

    private fun bluetoothPermission(): Boolean {
        return permissionCheck(PermissionType.Bluetooth, this)
    }

    private fun setProvider(){
        //Set Provider
        locationProvider = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            LocationManager.FUSED_PROVIDER
        }else{
            LocationManager.NETWORK_PROVIDER //locationManager.getBestProvider()
        }
    }


    override fun onLocationChanged(p0: Location) {
        Debugger.printDebug("Location changed [$p0]")
    }

    fun myPositionButton(v : View){
        val rnd = Random()
        v.setBackgroundColor(
            Color.valueOf(rnd.nextInt(256).toFloat(),
            rnd.nextInt(256).toFloat(), rnd.nextInt(256).toFloat()).toArgb())
        Debugger.printDebug("HELLO")

        if(map.zoomLevelDouble < 14)
            map.controller.zoomTo(14, 500)
        map.controller.animateTo(mLocationOverlay.myLocation)
        mLocationOverlay.enableFollowLocation()
    }

    fun bluetoothButton (view: View){
        MyBluetoothService.sendMsg("Ciao")
    }



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            PermissionType.Bluetooth.ordinal -> {
                myBluetoothManager = MyBluetoothManager(this)
                startBluetoothActivity(bluetoothActivityLauncher!!)
            }

            PermissionType.Location.ordinal -> {

            }
        }
    }

     fun onMoveButtonClick(view: View){
         //TEST ROBOT MSG
         /*
         val w = MsgUtil.buildDispatch("BeautifulViewActivity","cmd", "cmd(w)", "basicrobot").toString()
         val r = MsgUtil.buildDispatch("BeautifulViewActivity","cmd", "cmd(r)", "basicrobot").toString()
         val l = MsgUtil.buildDispatch("BeautifulViewActivity","cmd", "cmd(l)", "basicrobot").toString()
         val h = MsgUtil.buildDispatch("BeautifulViewActivity","cmd", "cmd(h)", "basicrobot").toString()
         val b = MsgUtil.buildDispatch("BeautifulViewActivity","cmd", "cmd(w)", "basicrobot").toString()
          */
         when(view.id){
            R.id.buttonForward -> {sendMessage(MsgUtil.buildDispatch("BeautifulViewActivity","cmd", "cmd(w)", "basicrobot").toString())}
            R.id.buttonRight -> {sendMessage(MsgUtil.buildDispatch("BeautifulViewActivity","cmd", "cmd(r)", "basicrobot").toString())}
            R.id.buttonLeft -> {sendMessage(MsgUtil.buildDispatch("BeautifulViewActivity","cmd", "cmd(l)", "basicrobot").toString())}
            R.id.buttonBack -> {sendMessage(MsgUtil.buildDispatch("BeautifulViewActivity","cmd", "cmd(s)", "basicrobot").toString())}
            R.id.haltButton -> {sendMessage(MsgUtil.buildDispatch("BeautifulViewActivity","cmd", "cmd(h)", "basicrobot").toString())}
        }
    }

    private fun sendMessage(s : String){
        MyBluetoothService.sendMsg(s)
    }

    private fun updateRSSIValue(rssiValue: Int?){
        if (rssiValue != null) {
            Debugger.printDebug("UI", "Updated RSSI")
            rssiProgressBarr.setProgress(rssiValue, true)
        }
    }

}

