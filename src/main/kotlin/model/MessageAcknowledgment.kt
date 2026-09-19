package me.mihaidubceac.model

import kotlinx.serialization.Serializable

@Serializable
data class MessageAcknowledgment(
    val userId: String,
    val messageId: String,
)