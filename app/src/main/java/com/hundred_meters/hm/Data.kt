package com.hundred_meters.hm

import kotlinx.serialization.Serializable

@Serializable
data class Blah(

    var topic: String = "",
    var body: String = "",
    val randomNumberID: Int = 0

)


