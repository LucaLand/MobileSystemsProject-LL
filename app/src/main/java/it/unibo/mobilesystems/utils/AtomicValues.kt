package it.unibo.mobilesystems.utils

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface AtomicNullableVar<T> {
    suspend fun set(value : T?)
    suspend fun get() : T?
    suspend fun withValue(action : (T?) -> Unit)
    suspend fun <R> map(mapper : (T?) -> R) : R
}

interface AtomicVar<T> {
    suspend fun set(value : T)
    suspend fun get() : T
    suspend fun withValue(action : (T) -> Unit)
    suspend fun <R> map(mapper : (T) -> R) : R
}

class AtomicNullableSharedMemoryVar<T> (
    private var value : T? = null
) : AtomicNullableVar<T> {

    private val mutex = Mutex()

    override suspend fun set(value : T?) {
        mutex.withLock {
            this.value = value
        }
    }

    override suspend fun get() : T? {
        mutex.withLock {
            return this.value
        }
    }

    override suspend fun withValue(action : (T?) -> Unit) {
        mutex.withLock{
            action(this.value)
        }
    }

    override suspend fun <R> map(mapper : (T?) -> R) : R {
        return mutex.withLock {
            mapper(this.value)
        }
    }

}

class AtomicSharedMemoryVar<T>(
    private var value : T
) : AtomicVar<T> {

    private val mutex = Mutex()

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

    override suspend fun withValue(action : (T) -> Unit) {
        mutex.withLock{
            action(this.value)
        }
    }

    override suspend fun <R> map(mapper : (T) -> R) : R {
        return mutex.withLock {
            mapper(this.value)
        }
    }

}

fun <T> atomicNullableVar(value : T? = null) : AtomicNullableVar<T> {
    return AtomicNullableSharedMemoryVar(value)
}

fun <T> atomicVar(value : T) : AtomicVar<T> {
    return AtomicSharedMemoryVar(value)
}