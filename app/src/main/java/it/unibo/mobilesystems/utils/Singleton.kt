package it.unibo.mobilesystems.utils

import it.unibo.mobilesystems.debugUtils.Debugger

class Singleton<T> {

    private var obj : T? = null

    fun set(obj : T) {
        if(this.obj != null)
            throw IllegalStateException("Value is already set: unable to set it again")

        this.obj = obj
    }

    fun get() : T {
        if(this.obj == null)
            throw IllegalStateException("Value is not set: unable to get")

        return obj!!
    }

}