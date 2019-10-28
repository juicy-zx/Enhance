package com.juicy.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import juicy.retrofit.IParamsBuilder
import juicy.retrofit.Retrofit
import kotlinx.coroutines.*
import okhttp3.Call
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

fun ViewModel.newScope(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
): Job = viewModelScope.launch(context, start) { supervisorScope(block) }

inline fun <T> CoroutineScope.httpAsync(
    context: CoroutineContext = Dispatchers.IO,
    callFactory: Call.Factory = HttpUtils.okHttpClient,
    crossinline builder: IParamsBuilder.() -> Unit
): Deferred<T?> {
    return async(context, CoroutineStart.LAZY) {
        val retrofit = Retrofit.create<T>()
        retrofit.paramsBuilder.apply(builder)
        retrofit.newCall(callFactory).execute()
    }
}

fun ViewModel.cancelAll() {
    viewModelScope.coroutineContext.cancelChildren()
}

suspend fun <E> Deferred<E?>.getResult(result: (E) -> Unit) = getResult(result, null)

suspend fun <T> Deferred<T?>.getResult(result: (T) -> Unit, error: ((Exception) -> Unit)? = null) {
    try {
        val t = await()
        t ?: throw NullPointerException("Server return null")
        result(t)
    } catch (e: Exception) {
        e.printStackTrace()
        if (e !is CancellationException) {
            error?.invoke(e)
        }
    }
}