package juicy.enhance

import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.omg.CORBA.Object
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

@Suppress("UNCHECKED_CAST")
class Enhance(
    val converterFactories: List<ConverterFactory>,
    val callFactory: okhttp3.Call.Factory
) {
    inline fun <reified T> newCall(builder: IParamsBuilder.() -> Unit): Call<T> {
        val base = object : TypeBase<T> {}
        val superType = base.javaClass.genericInterfaces[0]
        val realType = (superType as ParameterizedType).actualTypeArguments.first()
        val paramsBuilder = ParamsBuilder(this@Enhance).apply(builder)
        return OkHttpCall(callFactory, paramsBuilder, responseBodyConverter(realType))
    }

    fun <T> responseBodyConverter(type: Type): Converter<ResponseBody, T> {
        var converter: Converter<ResponseBody, T>? = null
        for (converterFactory in converterFactories) {
            converter = converterFactory.responseBodyConverter(type) as? Converter<ResponseBody, T>
            if (converter != null) {
                break
            }
        }
        return requireNotNull(converter) { "Could not locate ResponseBody converter for $type" }
    }

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

    fun newBuilder() = Builder(this)
    class Builder() {
        private var converterFactories: MutableList<ConverterFactory> = mutableListOf(DefaultConverterFactory)
        var callFactory: okhttp3.Call.Factory = OkHttpClient()

        internal constructor(enhance: Enhance) : this() {
            this.converterFactories = ArrayList(enhance.converterFactories)
            this.callFactory = enhance.callFactory
        }

        fun addConverterFactory(converterFactory: ConverterFactory) = converterFactories.add(converterFactory)
        fun build() = Enhance(converterFactories, callFactory)
    }
}