package juicy.retrofit

import okhttp3.RequestBody
import okhttp3.ResponseBody
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class Retrofit<T>(private val type: Type) {
    private val mParamsBuilder = ParamsBuilder()
    val paramsBuilder: IParamsBuilder get() = mParamsBuilder
    fun newCall(callFactory: okhttp3.Call.Factory): Call<T> =
        OkHttpCall(callFactory, mParamsBuilder, responseBodyConverter())

    @Suppress("UNCHECKED_CAST")
    private fun responseBodyConverter(): Converter<ResponseBody, T> {
        var converter: Converter<ResponseBody, T>? = null
        for (converterFactory in converterFactories) {
            converter = converterFactory.responseBodyConverter(type) as? Converter<ResponseBody, T>
            if (converter != null) {
                break
            }
        }
        return requireNotNull(converter) { "Could not locate ResponseBody converter for $type" }
    }

    companion object {
        internal var converterFactories: MutableList<ConverterFactory> = mutableListOf(DefaultConverterFactory)
        fun addConverterFactory(converterFactory: ConverterFactory) = converterFactories.add(converterFactory)

        @Suppress("UNCHECKED_CAST")
        internal fun <T> requestBodyConverter(type: Type): Converter<T, RequestBody> {
            var converter: Converter<T, RequestBody>? = null
            for (converterFactory in converterFactories) {
                converter =
                    converterFactory.requestBodyConverter(type) as? Converter<T, RequestBody>
                if (converter != null) {
                    break
                }
            }
            return requireNotNull(converter) { "Could not locate RequestBody converter for $type" }
        }

        @Suppress("UNCHECKED_CAST")
        internal fun <T> stringConverter(type: Type): Converter<T, String> {
            var converter: Converter<T, String>? = null
            for (converterFactory in converterFactories) {
                converter = converterFactory.stringConverter(type) as? Converter<T, String>
                if (converter != null) {
                    break
                }
            }
            return requireNotNull(converter) { "Could not locate String converter for $type" }
        }

        fun <T> create(): Retrofit<T> {
            val base = object : TypeBase<T>() {}
            val superType = base::class.java.genericSuperclass!!
            val type = (superType as ParameterizedType).actualTypeArguments.first()!!
            return Retrofit(type)
        }
    }
}
