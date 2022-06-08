package it.unibo.mobilesystems


import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.google.android.material.floatingactionbutton.FloatingActionButton
import it.unibo.mobilesystems.debugUtils.debugger
import it.unibo.mobilesystems.permissionManager.PermissionType
import it.unibo.mobilesystems.permissionManager.PermissionsManager.permissionCheck



open class BluetoothActivity : AppCompatActivity() {

    private lateinit var backBtn : FloatingActionButton

    private lateinit var boxListPairedDevices : LinearLayout
    private lateinit var boxListDiscoveredDevices : LinearLayout


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth)

        backBtn = findViewById(R.id.backButton)
        backBtn.setOnClickListener{backButton(backBtn)}

        boxListPairedDevices = findViewById(R.id.pairedDevicesBox)
        boxListDiscoveredDevices = findViewById(R.id.discoveredDevicesBox)

        permissionCheck(PermissionType.Bluetooth, this)
    }

    fun backButton(view : View){
        view.isVisible = false
        val intent = Intent(this, MapsActivity::class.java)
        startActivity(intent)
    }

    @SuppressLint("MissingPermission")
    fun printPairedDevices(devices : Set<BluetoothDevice>?){
        if (devices != null) {
            devices.forEach { device ->
                val deviceName = device.name
                val deviceHardwareAddress = device.address // MAC address
                debugger.printDebug("Nome: $deviceName - MAC: $deviceHardwareAddress")
                printPairedDevice("Nome: $deviceName - MAC: $deviceHardwareAddress")
            }
        }
    }

    private fun printPairedDevice(s: String) {
        val txtDevice = TextView(this)
        txtDevice.setText(s)
        boxListPairedDevices.addView(txtDevice)
    }

    @SuppressLint("MissingPermission")
    fun printDiscoveredDevices(devices : MutableList<BluetoothDevice?>){
        devices.forEach { device ->
            val deviceName = device?.name
            val deviceHardwareAddress = device?.address // MAC address
            debugger.printDebug("FOUNDED DEVICES:")
            debugger.printDebug("Nome: $deviceName - MAC: $deviceHardwareAddress")
            printDiscoveredDevice("Nome: $deviceName - MAC: $deviceHardwareAddress")
        }
    }

    private fun printDiscoveredDevice(device : String){
        val label = findViewById<TextView>(R.id.textView2)
        if(!label.isVisible) label.isVisible = true

        val txtDevice = TextView(this)
        txtDevice.setText(device)
        boxListDiscoveredDevices.addView(txtDevice)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        debugger.printDebug("PERMISSION RESULT: Code:|$requestCode| - |${permissions}| - |$grantResults|")
    }

}

