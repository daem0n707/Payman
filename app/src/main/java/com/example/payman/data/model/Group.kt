package com.example.payman.data.model

import java.util.*

data class Group(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val memberIds: List<String> = emptyList()
)
