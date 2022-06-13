package it.unibo.mobilesystems


/*
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.material.floatingactionbutton.FloatingActionButton
import it.unibo.mobilesystems.debugUtils.Debugger
import it.unibo.mobilesystems.fileUtils.FileSupport
import it.unibo.mobilesystems.permissionManager.PermissionType
import it.unibo.mobilesystems.permissionManager.PermissionsManager.permissionCheck
import java.util.*


open class BluetoothActivity : AppCompatActivity() {


    private lateinit var bluetoothBtn : FloatingActionButton
    lateinit var sendButton : FloatingActionButton



    private lateinit var backBtn : FloatingActionButton

    private lateinit var boxListPairedDevices : LinearLayout
    private lateinit var boxListDiscoveredDevices : LinearLayout

    lateinit var deviceTextView: TextView
    lateinit var longClickListener : View.OnLongClickListener

    lateinit var loadingBar : ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth)

        /** UI ITEM **/
        backBtn = findViewById(R.id.backButton)
        boxListPairedDevices = findViewById(R.id.pairedDevicesBox)
        boxListDiscoveredDevices = findViewById(R.id.discoveredDevicesBox)
        deviceTextView = findViewById(R.id.deviceTextView)
        loadingBar = findViewById(R.id.progressBar)
        //SETTING LISTENERS
        backBtn.setOnClickListener{backButton(backBtn)}
        loadingBar.animate()
        bluetoothBtn = findViewById(R.id.bluetoothButton)
        sendButton = findViewById(R.id.sendButton)
        //LISTENER
        bluetoothBtn.setOnClickListener{refreshBluetoothPage()}
        longClickListener = View.OnLongClickListener{ view : View -> onDeviceClick(view)}
        sendButton.setOnClickListener({bluetoothSendData(bluetoothThread, "CIAOOOOOO!!!")})

        permissionCheck(PermissionType.Bluetooth, this)
    }

//MAIN CLASS


    @SuppressLint("MissingPermission")
    fun printPairedDevices(devices : Set<BluetoothDevice>?){
        devices?.forEach { device ->
            val deviceName = device.name
            val deviceHardwareAddress = device.address // MAC address
            Debugger.printDebug("Nome: $deviceName - MAC: $deviceHardwareAddress")
            printPairedDevice(deviceToString(device))
        }
    }

    private fun printPairedDevice(s: String) {
        boxListPairedDevices.addView(createDeviceTextView(s))
        boxListPairedDevices.addView(LayoutInflater.from(this).inflate(R.layout.separator_line, null) as View)
    }

    @SuppressLint("MissingPermission")
    fun printDiscoveredDevices(devices : MutableList<BluetoothDevice?>){
        Debugger.printDebug("FOUNDED DEVICES:")
        devices.forEach { device ->
            val deviceName = device?.name
            var s = deviceToString(device)
            Debugger.printDebug(s)
            if(deviceName!=null)printDiscoveredDevice(s)
        }
    }


    fun printDiscoveredDevice(s : String){
        val label = findViewById<TextView>(R.id.textView2)
        if(!label.isVisible) label.isVisible = true

        boxListDiscoveredDevices.addView(createDeviceTextView(s))
        boxListDiscoveredDevices.addView(LayoutInflater.from(this).inflate(R.layout.separator_line, null) as View)
    }

    private fun createDeviceTextView(s : String) : TextView{
        val view: View = LayoutInflater.from(this).inflate(R.layout.device_text_view, null)
        (view as TextView).text = s
        view.setOnLongClickListener(longClickListener)
        return view
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Debugger.printDebug("PERMISSION RESULT: Code:|$requestCode| - |${permissions}| - |$grantResults|")
    }

    fun pageClean(){
        boxListPairedDevices.removeAllViews()
        boxListDiscoveredDevices.removeAllViews()
    }

    @SuppressLint("MissingPermission")
    fun deviceToString(device : BluetoothDevice?) : String{
        val deviceName = device?.name               //NOME
        val deviceHardwareAddress = device?.address // MAC address
        return "Nome: $deviceName || MAC: $deviceHardwareAddress"
    }


    /** ----------------ON CLICK FUNCTIONS------------------ **/

    fun backButton(view : View){
        val intent = Intent(this, MainMapsActivity::class.java)
        startActivity(intent)
    }





}

 */

