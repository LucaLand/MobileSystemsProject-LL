package it.unibo.mobilesystems.bluetooth

import android.app.AlertDialog
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.widget.ArrayAdapter
import it.unibo.mobilesystems.actors.APP_SCOPE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class BluetoothDeviceListDialogBuilder(
    private val context: Context,
    layout : Int,
    private val scope : CoroutineScope = APP_SCOPE
) {

    private val deviceList = mutableListOf<BluetoothDevice>()
    val deviceListAdapter = ArrayAdapter(context, layout, deviceList)

    fun buildNew(title : String, clearList : Boolean = true,
                 onDeviceSelected : suspend (BluetoothDevice) -> Unit) : AlertDialog {
        if(clearList) deviceList.clear()
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
        builder.setAdapter(deviceListAdapter) { _, which ->
            val device = deviceList[which]
            scope.launch {
                onDeviceSelected(device)
            }
        }
        return builder.create()
    }

}