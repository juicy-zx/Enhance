package com.juicy.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import juicy.retrofit.IParamsBuilder
import juicy.retrofit.Retrofit
import kotlinx.coroutines.*
import okhttp3.Call
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.coroutineContext

fun ViewModel.newScope(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
): Job = viewModelScope.launch(context, start) { supervisorScope(block) }

inline fun <reified T> CoroutineScope.httpAsync(
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
        if (!coroutineContext.isActive) return
        result(t)
    } catch (e: Exception) {
        if (!coroutineContext.isActive) return
        e.printStackTrace()
        if (e !is CancellationException) {
            error?.invoke(e)
        }
    }
}

suspend fun <T1, T2> getResult2(
    deferred1: Deferred<T1>,
    deferred2: Deferred<T2>,
    result: (T1, T2) -> Unit,
    error: ((Exception) -> Unit)? = null
) {
    deferred1.start()
    deferred2.start()
    try {
        val result1 = deferred1.await()
        val result2 = deferred2.await()
        if (!coroutineContext.isActive) return
        result(result1, result2)
    } catch (e: Exception) {
        if (!coroutineContext.isActive) return
        if (e !is CancellationException) {
            error?.invoke(e)
        }
    }
}