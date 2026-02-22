package com.example.payman.data.model

import java.util.*

data class Person(val id: String = UUID.randomUUID().toString(), val name: String)
