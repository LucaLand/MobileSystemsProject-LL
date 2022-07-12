package it.unibo.mobilesystems.actors

import androidx.lifecycle.LifecycleCoroutineScope
import it.unibo.kactor.annotations.CUSTOM_SCOPES
import it.unibo.kactor.launchQak
import it.unibo.kactor.parameters.mutableParameterMap
import it.unibo.kactor.utils.addAnnotatedClassesParams
import it.unibo.kactor.utils.addBlockIOParam
import it.unibo.kactor.utils.addSystemScope
import it.unibo.mobilesystems.BluetoothConnectionActivity
import it.unibo.mobilesystems.MainMapsActivity
import it.unibo.mobilesystems.bluetooth.QakBluetoothConnection
import it.unibo.mobilesystems.utils.atomicNullableVar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob

val APP_SCOPE = CoroutineScope(SupervisorJob() + Dispatchers.Default)
val DEFAULT_PARAMS = mutableParameterMap()
    .addBlockIOParam()
    .addAnnotatedClassesParams(
        ContextConfiguration::class.java,
        LocationManagerActor::class.java,
        GitBertoActor::class.java,
        MainMapsActivity::class.java,
        BluetoothConnectionActivity::class.java
    )
    .addSystemScope(APP_SCOPE)
    .apply {
        CUSTOM_SCOPES["MAIN"] = MainScope()
    }

val qakBluetoothConnection = atomicNullableVar<QakBluetoothConnection>()

fun String.removeFirstAndLast() : String {
    return this.substring(1, this.length - 1)
}