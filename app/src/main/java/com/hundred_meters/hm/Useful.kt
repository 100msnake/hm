package com.hundred_meters.hm

import android.util.Log


const val encryption_name: String = "A"

var labelToAdvertise : String = "100m"


fun randomNumber(): Int {
    return (0..10000000).random()
}


fun newLabel(): String {
    val numberName = randomNumber().toString()
    val label = "100m$encryption_name$numberName"
    Log.d(TAG, "new label: $label")
    labelToAdvertise = label
    return label
}





