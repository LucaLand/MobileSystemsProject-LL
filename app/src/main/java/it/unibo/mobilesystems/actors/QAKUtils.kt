package it.unibo.mobilesystems.actors

import android.content.res.Resources
import it.unibo.kactor.launchQak

suspend fun launchQakWithBuildTimeScan() {
    launchQak("ann_class_names" to mutableListOf(
        ContextConfiguration::class.qualifiedName!!,
        LocationManagerActor::class.qualifiedName!!))
}