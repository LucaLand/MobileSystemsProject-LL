package it.unibo.mobilesystems.debugUtils

import java.util.*

object debugger {

    fun printDebug(obj : Any){
        var msg = obj.toString()
        printDebug(msg)
    }
    fun printDebug(msg : String){
        println("LL-MobileSystemProj debug: $msg")
    }
}