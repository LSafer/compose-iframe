package net.lsafer.compose.iframe

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlin.jvm.JvmName

@JvmName("emit")
suspend fun IframeState.emit(data: JsonElement, targetOrigin: String = "*") {
    outgoing.send(IframeOutgoingEvent(data, targetOrigin))
}

@JvmName("emitT")
suspend inline fun <reified T> IframeState.emit(data: T, targetOrigin: String = "*") {
    outgoing.send(IframeOutgoingEvent(Json.encodeToJsonElement(data), targetOrigin))
}

@JvmName("consumeEach")
suspend fun IframeState.consumeEach(block: (JsonElement, String) -> Unit) {
    for (event in incoming) block(event.data, event.origin)
}

@JvmName("consumeEachT")
suspend inline fun <reified T> IframeState.consumeEach(block: (T, String) -> Unit) {
    for (event in incoming) {
        val data = try {
            Json.decodeFromJsonElement<T>(event.data)
        } catch (_: SerializationException) {
            return
        } catch (_: IllegalArgumentException) {
            return
        }

        block(data, event.origin)
    }
}
