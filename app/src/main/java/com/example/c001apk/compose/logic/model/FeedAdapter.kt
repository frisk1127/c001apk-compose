package com.example.c001apk.compose.logic.model

import com.example.c001apk.compose.logic.model.HomeFeedResponse.Feed
import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.io.IOException

object FeedAdapterFactory : TypeAdapterFactory {
    @Suppress("UNCHECKED_CAST")
    override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T> {
        return FeedAdapter(gson) as TypeAdapter<T>
    }
}

class FeedAdapter(private val gson: Gson) : TypeAdapter<Feed?>() {
    @Throws(IOException::class)
    override fun write(jsonWriter: JsonWriter, feed: Feed?) {
        throw RuntimeException("Not implemented")
    }

    @Throws(IOException::class)
    override fun read(jsonReader: JsonReader): Feed? {
        return when (jsonReader.peek()) {
            JsonToken.NUMBER -> Feed(id = jsonReader.nextInt().toString())

            JsonToken.BEGIN_OBJECT ->
                gson.fromJson(jsonReader, Feed::class.java)

            else -> throw RuntimeException("Expected object or int, not " + jsonReader.peek())
        }
    }
}
