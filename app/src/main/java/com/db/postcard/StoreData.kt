package com.db.postcard

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream


object CharacterListSerializer : Serializer<CharacterList> {
    override val defaultValue: CharacterList = CharacterList.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): CharacterList {
        try {
            return CharacterList.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(
        t: CharacterList,
        output: OutputStream
    ) = t.writeTo(output)
}

val Context.CharacterListDataStore: DataStore<CharacterList> by dataStore(
    fileName = "settings.pb",
    serializer = CharacterListSerializer
)

