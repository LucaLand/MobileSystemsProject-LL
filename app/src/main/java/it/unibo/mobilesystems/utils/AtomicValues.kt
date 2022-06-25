package it.unibo.mobilesystems.utils

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class MutableNullableVar<T>(
    var value : T?
)

data class MutableVar<T>(
    var value : T
)

interface AtomicNullableVar<T> {
    suspend fun set(value : T?)
    suspend fun get() : T?
    suspend fun withValue(action : MutableNullableVar<T>.(T?) -> Unit)
    suspend fun <R> map(mapper : MutableNullableVar<T>.(T?) -> R) : R
}

interface AtomicVar<T> {
    suspend fun set(value : T)
    suspend fun get() : T
    suspend fun withValue(action : MutableVar<T>.(T) -> Unit)
    suspend fun <R> map(mapper : MutableVar<T>.(T) -> R) : R
}

class AtomicNullableSharedMemoryVar<T> (
    value : T? = null
) : AtomicNullableVar<T> {

    private val mutex = Mutex()
    private var mutNullVar = MutableNullableVar(value)

    override suspend fun set(value : T?) {
        mutex.withLock {
            this.mutNullVar.value = value
        }
    }

    override suspend fun get() : T? {
        mutex.withLock {
            return this.mutNullVar.value
        }
    }

    override suspend fun withValue(action : MutableNullableVar<T>.(T?) -> Unit) {
        mutex.withLock{
            mutNullVar.action(mutNullVar.value)
        }
    }

    override suspend fun <R> map(mapper : MutableNullableVar<T>.(T?) -> R) : R {
        return mutex.withLock {
            mutNullVar.mapper(mutNullVar.value)
        }
    }

}

class AtomicSharedMemoryVar<T>(
    var value : T
) : AtomicVar<T> {

    private val mutex = Mutex()
    private val mutVar = MutableVar(value)

    override suspend fun set(value : T) {
        mutex.withLock {
            this.value = value
        }
    }

    override suspend fun get() : T {
        mutex.withLock {
            return this.value
        }
    }

    override suspend fun withValue(action : MutableVar<T>.(T) -> Unit) {
        mutex.withLock{
            this.mutVar.action(mutVar.value)
        }
    }

    override suspend fun <R> map(mapper : MutableVar<T>.(T) -> R) : R {
        return mutex.withLock {
            this.mutVar.mapper(mutVar.value)
        }
    }

}

fun <T> atomicNullableVar(value : T? = null) : AtomicNullableVar<T> {
    return AtomicNullableSharedMemoryVar(value)
}

fun <T> atomicVar(value : T) : AtomicVar<T> {
    return AtomicSharedMemoryVar(value)
}