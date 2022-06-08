package it.unibo.mobilesystems


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
    private lateinit var boxList : LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth)

        backBtn = findViewById(R.id.backButton)
        backBtn.setOnClickListener{backButton(backBtn)}
        boxList = findViewById(R.id.deviceListBox)

        permissionCheck(PermissionType.Bluetooth, this)
    }

    fun backButton(view : View){
        view.isVisible = false
        var intent = Intent(this, MapsActivity::class.java)
        startActivity(intent)
    }

    fun printDevice(device : String){
        var txtDevice : TextView = TextView(this)
        txtDevice.setText(device)
        //txtDevice.id =
        boxList.addView(txtDevice)
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

