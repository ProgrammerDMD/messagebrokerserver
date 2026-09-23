package me.mihaidubceac.model

import kotlinx.serialization.Serializable

@Serializable
data class ApiMessageRequest(
    val requestId: String,
    val userId: String,
    val topic: String,
    val content: String,
)