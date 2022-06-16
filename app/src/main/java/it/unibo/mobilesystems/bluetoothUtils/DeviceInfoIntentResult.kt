package it.unibo.mobilesystems.bluetoothUtils

import android.bluetooth.BluetoothDevice
import android.content.Intent
import it.unibo.mobilesystems.RESULT_DEVICE_ADDRESS_CODE
import it.unibo.mobilesystems.RESULT_DEVICE_NAME_CODE
import it.unibo.mobilesystems.RESULT_DEVICE_UUID_CODE
import java.util.*

object DeviceInfoIntentResult {


    fun createIntentResult(extrasCodeAndValue: MutableMap<String, String>): Intent{
        val intent = Intent()
        extrasCodeAndValue.forEach{entry ->
            intent.putExtra(entry.key, entry.value)
        }
        return intent
    }


    fun createIntentResult(deviceName: String, macAddress: String, uuid: String): Intent{
        val intent = Intent()
        intent.putExtra(RESULT_DEVICE_NAME_CODE, deviceName)
        intent.putExtra(RESULT_DEVICE_ADDRESS_CODE, macAddress)
        intent.putExtra(RESULT_DEVICE_UUID_CODE, uuid)
        return intent
    }

    fun createIntentResult(bluetoothDevice: BluetoothDevice, otherExtra: MutableMap<String, String>): Intent{
        val deviceName = bluetoothDevice.name
        val macAddress = bluetoothDevice.address

        val intent = Intent()
        intent.putExtra(RESULT_DEVICE_NAME_CODE, deviceName)
        intent.putExtra(RESULT_DEVICE_ADDRESS_CODE, macAddress)
        otherExtra.forEach{entry ->
            intent.putExtra(entry.key, entry.value)
        }
        return intent
    }

    fun createIntentResult(bluetoothDevice: BluetoothDevice): Intent{
        val deviceName = bluetoothDevice.name
        val macAddress = bluetoothDevice.address

        val intent = Intent()
        intent.putExtra(RESULT_DEVICE_NAME_CODE, deviceName)
        intent.putExtra(RESULT_DEVICE_ADDRESS_CODE, macAddress)
        return intent
    }

    fun getDeviceResult(intent: Intent): MutableMap<String, String?> {
        val map: MutableMap<String, String?> = mutableMapOf()
        map[RESULT_DEVICE_NAME_CODE] = intent.getStringExtra(RESULT_DEVICE_NAME_CODE)
        map[RESULT_DEVICE_ADDRESS_CODE] = intent.getStringExtra(RESULT_DEVICE_ADDRESS_CODE)
        map[RESULT_DEVICE_UUID_CODE] = intent.getStringExtra(RESULT_DEVICE_UUID_CODE)
        return map
    }

}