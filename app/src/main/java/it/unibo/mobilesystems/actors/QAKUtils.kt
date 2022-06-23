package it.unibo.mobilesystems.actors

import android.content.res.Resources
import it.unibo.kactor.launchQak

suspend fun launchQakWithBuildTimeScan() {
    launchQak("ann_class_names" to mutableListOf(
        ContextConfiguration::class.qualifiedName!!,
        LocationManagerActor::class.qualifiedName!!,
        GitBertoActor::class.qualifiedName!!
    ))
}

fun String.removeFirstAndLast() : String {
    return this.substring(1, this.length - 1)
}