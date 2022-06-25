package it.unibo.mobilesystems.actors

import it.unibo.kactor.launchQak
import it.unibo.mobilesystems.bluetooth.QakBluetoothConnection
import it.unibo.mobilesystems.utils.atomicNullableVar

suspend fun launchQakWithBuildTimeScan() {
    launchQak("ann_class_names" to mutableListOf(
        ContextConfiguration::class.qualifiedName!!,
        LocationManagerActor::class.qualifiedName!!,
        GitBertoActor::class.qualifiedName!!
    ))
}

val qakBluetoothConnection = atomicNullableVar<QakBluetoothConnection>()

fun String.removeFirstAndLast() : String {
    return this.substring(1, this.length - 1)
}