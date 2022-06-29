package it.unibo.mobilesystems.actors

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import it.unibo.kactor.IQActorBasicFsm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import androidx.lifecycle.LifecycleCoroutineScope
import it.unibo.kactor.ActorBasic
import it.unibo.kactor.annotations.CustomScope

/**
 * An activity that also is a [IQActorBasicFsm] instance. So, it also is
 * a finite state machine actor with a kotlin coroutine that works
 * *behind the scene* and that can transit over states by receiving messages
 */
abstract class ActorAppCompactActivity : AppCompatActivity(), IQActorBasicFsm {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setInstanceAndStart(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        terminate()
    }

    /**
     * Launches a coroutine using [LifecycleCoroutineScope] that executes
     * the passed [block]
     * @param block the block that as to be executed
     */
    protected fun lifecycleLaunch(block : suspend CoroutineScope.() -> Unit) {
        lifecycleScope.launch {
            block(this)
        }
    }

    /**
     * Launches a coroutine using the [CoroutineScope] of the [ActorBasic]
     * encapsulated in this [IQActorBasicFsm] instance
     * @param block the block that has to be executed
     */
    protected fun actorLaunch(block : suspend CoroutineScope.() -> Unit) {
        getActorBasic().scope.launch {
            block()
        }
    }

    /**
     * Switch the [Dispatchers] of the current coroutine in order
     * to execute the passed [update] block on the **main** thread.
     * This let to easily executes ui updates outside the main thread.
     * ** Notice that this function is useless if this class is marked with
     * [CustomScope] annotation with value `"MAIN"` like:
     * ```
     * @CustomScope("MAIN")
     * class MyActivity : ActorAppCompactActity(), IQActorBasicFsm ... {
     *      ...
     * }
     * ```
     * @param update the block that has to be called to update the ui
     */
    protected suspend fun updateUi(update : () -> Unit) {
        withContext(Dispatchers.Main) {
            update()
        }
    }

}