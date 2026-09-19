package me.mihaidubceac.model

import kotlinx.serialization.Serializable

@Serializable
data class UserMessagesQuery(
    val topics: List<String>
)