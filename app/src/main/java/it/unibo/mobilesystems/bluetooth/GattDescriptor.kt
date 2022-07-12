package it.unibo.mobilesystems.bluetooth

import android.bluetooth.BluetoothGatt

data class GattDescriptor(
    val gatt : BluetoothGatt,
    val gattCallback: LambdaGattCallback
)
