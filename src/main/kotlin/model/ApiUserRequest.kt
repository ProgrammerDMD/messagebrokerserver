package me.mihaidubceac.model

import kotlinx.serialization.Serializable

@Serializable
data class ApiUserRequest(
    val id: String,
)