package com.juicy.main

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import juicy.retrofit.Converter
import juicy.retrofit.ConverterFactory
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import okio.Buffer
import java.io.File
import java.io.OutputStreamWriter
import java.lang.reflect.Type
import java.nio.charset.Charset

class GsonConverterFactory(private val gson: Gson) : ConverterFactory() {
    companion object {
        fun create(gson: Gson = Gson()): ConverterFactory = GsonConverterFactory(gson)
    }

    override fun requestBodyConverter(type: Type): Converter<*, RequestBody>? {
        return if (type is Class<*> && type == File::class.java) {
            FileRequestBodyConverter()
        } else {
            val adapter = gson.getAdapter(TypeToken.get(type))
            GsonRequestBodyConverter(gson, adapter)
        }
    }

    override fun responseBodyConverter(type: Type): Converter<ResponseBody, *>? {
        val adapter = gson.getAdapter(TypeToken.get(type))
        return GsonResponseBodyConverter(gson, adapter)
    }
}

private class GsonResponseBodyConverter<T>(private val gson: Gson, private val adapter: TypeAdapter<T>) :
    Converter<ResponseBody, T> {
    override fun convert(value: ResponseBody): T {
        val jsonReader = gson.newJsonReader(value.charStream())
        value.use { return adapter.read(jsonReader) }
    }
}

private class FileRequestBodyConverter : Converter<File, RequestBody> {
    private val mediaType = "application/octet-stream".toMediaTypeOrNull()
    override fun convert(value: File): RequestBody? = value.asRequestBody(mediaType)
}

private class GsonRequestBodyConverter<T>(private val gson: Gson, private val adapter: TypeAdapter<T>) :
    Converter<T, RequestBody> {
    private val mediaType = "application/json; charset=UTF-8".toMediaTypeOrNull()
    private val utf8 = Charset.forName("UTF-8")
    override fun convert(value: T): RequestBody {
        val buffer = Buffer()
        val writer = OutputStreamWriter(buffer.outputStream(), utf8)
        val jsonWriter = gson.newJsonWriter(writer)
        adapter.write(jsonWriter, value)
        jsonWriter.close()
        return buffer.readByteString().toRequestBody(mediaType)
    }
}