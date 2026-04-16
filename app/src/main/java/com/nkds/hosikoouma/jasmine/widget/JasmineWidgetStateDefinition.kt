package com.nkds.hosikoouma.jasmine.widget

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import androidx.glance.state.GlanceStateDefinition
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream

object JasmineWidgetStateDefinition : GlanceStateDefinition<JasmineWidgetState> {
    private const val DATASTORE_FILE_NAME = "jasmine_widget_state"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val Context.jasmineWidgetDataStore: DataStore<JasmineWidgetState> by dataStore(
        fileName = DATASTORE_FILE_NAME,
        serializer = JasmineWidgetStateSerializer(json)
    )

    override suspend fun getDataStore(context: Context, fileKey: String): DataStore<JasmineWidgetState> {
        return context.jasmineWidgetDataStore
    }

    override fun getLocation(context: Context, fileKey: String): File {
        return File(context.filesDir, "datastore/$DATASTORE_FILE_NAME")
    }
}

class JasmineWidgetStateSerializer(private val json: Json) : Serializer<JasmineWidgetState> {
    override val defaultValue: JasmineWidgetState = JasmineWidgetState()

    override suspend fun readFrom(input: InputStream): JasmineWidgetState {
        try {
            val string = input.bufferedReader().use { it.readText() }
            if (string.isBlank()) return defaultValue
            return json.decodeFromString(JasmineWidgetState.serializer(), string)
        } catch (exception: SerializationException) {
            throw CorruptionException("Cannot read json.", exception)
        }
    }

    override suspend fun writeTo(t: JasmineWidgetState, output: OutputStream) {
        output.bufferedWriter().use {
            it.write(json.encodeToString(JasmineWidgetState.serializer(), t))
        }
    }
}
