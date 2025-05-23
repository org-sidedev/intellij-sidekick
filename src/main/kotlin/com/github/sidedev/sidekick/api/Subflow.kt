package com.github.sidedev.sidekick.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class SubflowStatus {
    @SerialName("started")
    STARTED,
    @SerialName("complete")
    COMPLETE,
    @SerialName("failed")
    FAILED,
}

@Serializable
data class Subflow(
    val workspaceId: String,
    val id: String,
    val name: String,
    val type: String? = null, // note: temporary, until we migrate old subflows to all have a type
    val description: String? = null,
    val status: SubflowStatus,
    val parentSubflowId: String? = null,
    val flowId: String,
    val result: String? = null,
)

@Serializable
data class SubflowResponse(
    val subflow: Subflow,
)

@Serializable
data class SubflowsResponse(
    val subflows: List<Subflow>,
)