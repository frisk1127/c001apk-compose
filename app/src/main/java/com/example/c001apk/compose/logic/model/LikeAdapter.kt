package com.example.c001apk.compose.logic.model

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.io.IOException

object LikeAdapterFactory : TypeAdapterFactory {
    @Suppress("UNCHECKED_CAST")
    override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T> {
        return LikeAdapter(gson) as TypeAdapter<T>
    }
}

class LikeAdapter(private val gson: Gson) : TypeAdapter<LikeResponse.Data?>() {
    @Throws(IOException::class)
    override fun write(jsonWriter: JsonWriter, feed: LikeResponse.Data?) {
        throw RuntimeException("Not implemented")
    }

    @Throws(IOException::class)
    override fun read(jsonReader: JsonReader): LikeResponse.Data? {
        return when (jsonReader.peek()) {
            JsonToken.NUMBER -> LikeResponse.Data(count = jsonReader.nextInt().toString())

            JsonToken.STRING -> LikeResponse.Data(count = jsonReader.nextString())

            JsonToken.BEGIN_OBJECT ->
                gson.fromJson(jsonReader, LikeResponse.Data::class.java)

            else -> throw RuntimeException("Expected object or int, not " + jsonReader.peek())
        }
    }
}
