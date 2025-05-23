package com.github.sidedev.sidekick.api.websocket

import com.github.sidedev.sidekick.models.flowEvent.FlowEvent
import com.github.sidedev.sidekick.api.response.ApiResponse
import com.github.sidedev.sidekick.api.response.ApiError
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.Logger
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.encodeToString

/**
 * Concrete WebSocket session implementation for handling Flow Events.
 * Expects messages of type ChatMessageDelta.
 */
class FlowEventsSession(
    client: HttpClient, // Provided HttpClient instance
    private val baseUrl: String, // Base URL for the API endpoint
    private val workspaceId: String, // Specific identifiers for the endpoint
    private val flowId: String,
    dispatcher: CoroutineDispatcher = Dispatchers.Default, // Optional dispatcher for scope
    logger: Logger = logger<FlowEventsSession>(),
) : SidekickWebSocketSession(client, dispatcher, logger) { // Pass required params to base

    /**
     * Constructs the specific WebSocket URL for Flow Events.
     */
    override fun buildWsUrl(): String {
        return "$baseUrl/ws/v1/workspaces/$workspaceId/flows/$flowId/events"
            .replace("http://", "ws://") // Convert http(s) to ws(s)
            .replace("https://", "wss://")
    }

    /**
     * Establishes a connection for FlowEvents, setting up handlers for ChatMessageDelta messages.
     * This provides a type-safe public interface, delegating the core connection
     * and listening logic to the generic implementation in the base class.
     *
     * @param onMessage Lambda invoked for each successfully received ChatMessageDelta.
     * @param onError Lambda invoked on connection or message processing errors.
     * @param onClose Lambda invoked when the connection is closed.
     * @return Deferred<Unit> completes successfully on connection, exceptionally on failure.
     */
    /**
     * Subscribes to flow events for a specific parent ID by sending a subscription message.
     */
    suspend fun subscribeToParent(parentId: String): ApiResponse<Unit, ApiError> {
        val message = mapOf("parentId" to parentId)
        return send(json.encodeToString(message))
    }

    fun connect(
        onMessage: suspend (FlowEvent) -> Unit,
        onError: suspend (Throwable) -> Unit,
        onClose: suspend (code: Short, reason: String) -> Unit = { _, _ -> }
    ): Deferred<Unit> {
        // Call the generic connect method from the base class, specifying the expected message type
        return super.connectGeneric<FlowEvent>(
            onMessage = onMessage,
            onError = onError,
            onClose = onClose
        )
    }
}