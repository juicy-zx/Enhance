package com.juicy.main

import com.google.gson.JsonPrimitive
import com.google.gson.TypeAdapter
import com.google.gson.internal.Streams
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

class IntegerDefaultAdapter : TypeAdapter<Int>() {
    override fun write(writer: JsonWriter, value: Int?) {
        if (value == null) {
            writer.nullValue()
            return
        }
        Streams.write(JsonPrimitive(value), writer)
    }

    override fun read(reader: JsonReader): Int {
        val value = Streams.parse(reader)
        if (value.isJsonNull || value.asString == "" || value.asString == "null") {
            return 0
        }
        return value.asInt
    }
}

class DoubleDefaultAdapter : TypeAdapter<Double>() {
    override fun write(writer: JsonWriter, value: Double?) {
        if (value == null) {
            writer.nullValue()
            return
        }
        Streams.write(JsonPrimitive(value), writer)
    }

    override fun read(reader: JsonReader): Double {
        val value = Streams.parse(reader)
        if (value.isJsonNull|| value.asString == "" || value.asString == "null") {
            return 0.00
        }
        return value.asDouble
    }
}

class LongDefaultAdapter : TypeAdapter<Long>() {
    override fun write(writer: JsonWriter, value: Long?) {
        if (value == null) {
            writer.nullValue()
            return
        }
        Streams.write(JsonPrimitive(value), writer)
    }

    override fun read(reader: JsonReader): Long {
        val value = Streams.parse(reader)
        if (value.isJsonNull|| value.asString == "" || value.asString == "null") {
            return 0
        }
        return value.asLong
    }
}

class FloatDefaultAdapter : TypeAdapter<Float>() {
    override fun write(writer: JsonWriter, value: Float?) {
        if (value == null) {
            writer.nullValue()
            return
        }
        Streams.write(JsonPrimitive(value), writer)
    }

    override fun read(reader: JsonReader): Float {
        val value = Streams.parse(reader)
        if (value.isJsonNull|| value.asString == "" || value.asString == "null") {
            return 0.00f
        }
        return value.asFloat
    }
}

class BooleanDefaultAdapter : TypeAdapter<Boolean>() {
    override fun write(writer: JsonWriter, value: Boolean?) {
        if (value == null) {
            writer.nullValue()
            return
        }
        Streams.write(JsonPrimitive(value), writer)
    }

    override fun read(reader: JsonReader): Boolean {
        val value = Streams.parse(reader)
        if (value.isJsonNull) {
            return false
        }
        when (value.asString) {
            "", "null" -> return false
            "y", "yes", "1" -> return true
        }
        return value.asBoolean
    }
}

class StringDefaultAdapter : TypeAdapter<String>() {
    override fun write(writer: JsonWriter, value: String?) {
        if (value == null) {
            writer.nullValue()
            return
        }
        Streams.write(JsonPrimitive(value), writer)
    }

    override fun read(reader: JsonReader): String {
        val value = Streams.parse(reader)
        if (value.isJsonNull || value.asString == "null") {
            return ""
        }
        return value.asString
    }
}