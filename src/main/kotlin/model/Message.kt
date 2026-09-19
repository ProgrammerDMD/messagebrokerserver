package me.mihaidubceac.model

import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val topic: String,
    val content: String,
    val timestamp: Long,
)