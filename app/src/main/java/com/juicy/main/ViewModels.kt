package com.juicy.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import juicy.enhance.IParamsBuilder
import juicy.enhance.Enhance
import juicy.enhance.ParamsBuilder
import kotlinx.coroutines.*
import okhttp3.Call
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

fun ViewModel.newScope(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
): Job = viewModelScope.launch(context, start) { supervisorScope(block) }

inline fun <reified T> CoroutineScope.httpAsync(
    context: CoroutineContext = Dispatchers.IO,
    crossinline builder: IParamsBuilder.() -> Unit
): Deferred<T?> {
    return async(context, CoroutineStart.LAZY) {
        HttpUtils.enhance.newCall<T>(builder).execute()
    }
}

fun ViewModel.cancelAll() {
    viewModelScope.coroutineContext.cancelChildren()
}

suspend inline fun <E> Deferred<E?>.getResult(result: (E) -> Unit) = getResult(result, {})

suspend inline fun <T> Deferred<T?>.getResult(result: (T) -> Unit, error: ((Exception) -> Unit)) {
    try {
        val t = await()
        t ?: throw NullPointerException("Server return null")
        result(t)
    } catch (e: Exception) {
        e.printStackTrace()
        if (e !is CancellationException) {
            error.invoke(e)
        }
    }
}