package it.unibo.mobilesystems.utils

import android.app.AlertDialog
import android.content.Context
import android.widget.ArrayAdapter

class AdapterDialog<T>(
    context : Context,
    title : String,
    layout : Int,
    private val stringifier : (T) -> String
) {

    val androidDialog : AlertDialog

    private val items = mutableMapOf<String, T>()
    private val stringItems = mutableListOf<String>()
    private val adapter = ArrayAdapter(context, layout, stringItems)
    private val onSelection = mutableListOf<(T) -> Unit>()

    init {
        androidDialog = AlertDialog.Builder(context).setTitle(title).setAdapter(adapter) { _, which ->
            onSelection.forEach { it(items[stringItems[which]]!!) }
        }.create()
    }

    fun addItem(item : T) : Boolean {
        val key = stringifier(item)
        if(items.containsKey(key))
            return false

        items[key] = item
        adapter.add(key)
        return true
    }

    fun removeItem(item : T) : T? {
        val key = stringifier(item)
        if(!items.containsKey(key))
            return null
        items.remove(key)
        adapter.remove(key)
        return item
    }

    fun getAllItems() : Set<T> {
        return items.values.toSet()
    }

    fun addOnSelection(onSelection : (T) -> Unit) {
        this.onSelection.add(onSelection)
    }

    fun removeOnSelection(onSelection: (T) -> Unit) {
        this.onSelection.remove(onSelection)
    }

    fun clear() {
        this.onSelection.clear()
        this.adapter.clear()
        this.items.clear()
    }

    fun show() {
        androidDialog.show()
    }

}