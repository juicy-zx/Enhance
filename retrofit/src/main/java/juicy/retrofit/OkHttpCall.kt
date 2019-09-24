package juicy.retrofit

import okhttp3.*
import okio.*

internal class OkHttpCall<T>(
    private val callFactory: okhttp3.Call.Factory,
    private val requestBuilder: ParamsBuilder,
    private val responseConverter: Converter<ResponseBody, T>
) : Call<T> {
    private var rawCall: okhttp3.Call? = null
    private var creationFailure: Throwable? = null
    @Volatile
    private var canceled = false
    private var executed = false

    override fun isExecuted(): Boolean = executed
    override fun clone(): Call<T> = OkHttpCall(callFactory, requestBuilder, responseConverter)
    override fun isCanceled(): Boolean =
        if (canceled) true else synchronized(this) { rawCall?.isCanceled() == true }

    override fun cancel() {
        canceled = true
        synchronized(this) { rawCall?.cancel() }
    }

    override fun request(): Request {
        val call = rawCall
        if (call != null) {
            return call.request()
        }
        val throwable = creationFailure
        if (throwable != null) {
            when (throwable) {
                is IOException -> throw RuntimeException("Unable to create request.", throwable)
                is RuntimeException -> throw throwable
                else -> throw throwable as Error
            }
        }
        try {
            val tCall = createRawCall()
            rawCall = tCall
            return tCall.request()
        } catch (e: Throwable) {
            throwIfFatal(e) // Do not assign a fatal error to creationFailure.
            creationFailure = e
            if (e is IOException) {
                throw RuntimeException("Unable to create request.", e)
            } else {
                throw e
            }
        }
    }

    override fun execute(): T? {
        val call: okhttp3.Call
        synchronized(this) {
            check(!executed) { "Already executed." }
            executed = true
            val throwable = creationFailure
            if (throwable != null) {
                when (throwable) {
                    is IOException -> throw RuntimeException("Unable to create request.", throwable)
                    is RuntimeException -> throw throwable
                    else -> throw throwable as Error
                }
            }
            if (rawCall != null) {
                call = rawCall!!
            } else {
                try {
                    call = createRawCall()
                    rawCall = call
                } catch (e: Throwable) {
                    throwIfFatal(e) //  Do not assign a fatal error to creationFailure.
                    creationFailure = e
                    throw e
                }
            }
        }
        if (canceled) {
            call.cancel()
        }
        return parseResponse(call.execute())
    }

    override fun enqueue(onSuccess: (T?) -> Unit, onFailure: (Throwable) -> Unit) {
        val call: okhttp3.Call?
        val throwable: Throwable?
        synchronized(this) {
            check(!executed) { "Already executed." }
            executed = true
            if (rawCall == null && creationFailure == null) {
                try {
                    rawCall = createRawCall()
                } catch (t: Throwable) {
                    throwIfFatal(t)
                    creationFailure = t
                }
            }
            call = rawCall
            throwable = creationFailure
        }
        if (throwable != null) {
            onFailure(throwable)
            return
        }
        if (canceled) {
            call?.cancel()
        }
        call?.enqueue(object : Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) = callFailure(e)
            private fun callFailure(t: Throwable) =
                try {
                    onFailure(t)
                } catch (t: Throwable) {
                    throwIfFatal(t)
                    t.printStackTrace()
                }

            override fun onResponse(call: okhttp3.Call, response: Response) {
                val parseResponse = try {
                    parseResponse(response)
                } catch (e: Throwable) {
                    throwIfFatal(e)
                    callFailure(e)
                    return
                }
                try {
                    onSuccess(parseResponse)
                } catch (e: Throwable) {
                    throwIfFatal(e)
                    e.printStackTrace()
                }
            }
        })
    }

    private fun parseResponse(rawResponse: Response): T? {
        val rawBody = rawResponse.body
        return when (val code = rawResponse.code) {
            204, 205 -> {
                rawBody?.close()
                null
            }
            in 200 until 300 -> {
                rawBody ?: return null
                val catchingBody = ExceptionCatchingResponseBody(rawBody)
                try {
                    responseConverter.convert(catchingBody)
                } catch (e: RuntimeException) {
                    catchingBody.throwIfCaught()
                    throw e
                }
            }
            else -> {
                rawBody?.close()
                throw IOException("Service return code is $code")
            }
        }
    }

    private fun createRawCall(): okhttp3.Call = callFactory.newCall(requestBuilder.create())

    private class ExceptionCatchingResponseBody(private val delegate: ResponseBody) : ResponseBody() {
        private var thrownException: IOException? = null
        override fun contentType(): MediaType? = delegate.contentType()
        override fun contentLength(): Long = delegate.contentLength()
        override fun close() = delegate.close()
        override fun source(): BufferedSource {
            return object : ForwardingSource(delegate.source()) {
                override fun read(sink: Buffer, byteCount: Long): Long {
                    try {
                        return super.read(sink, byteCount)
                    } catch (e: IOException) {
                        thrownException = e
                        throw e
                    }
                }
            }.buffer()
        }

        fun throwIfCaught() {
            thrownException?.also { throw it }
        }
    }
}