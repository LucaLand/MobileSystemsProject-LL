package it.unibo.mobilesystems.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

suspend fun onMain(block : suspend CoroutineScope.() -> Unit) {
    withContext(Dispatchers.Main) {
        block()
    }
}

fun onMain(scope : CoroutineScope, block: suspend CoroutineScope.() -> Unit) {
    scope.launch(Dispatchers.Main, block = block)
}