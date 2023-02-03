package com.example.scorp_final_assignment.repository

import com.example.scorp_final_assignment.R

object Repository {

    const val MinNickNameLength = 4
    //const val AppID = "dca16fbf522b4c6f96ecc88721800310"
    const val AppID = "09155caed6e0491482a59dd13ea8e8e2"
    val Token = null
    const val ChannelID = "a44c58de-311b-4033-b70f-1570406c156c"

    val clubGift = byteArrayOf(0x01)
    val heartGift = byteArrayOf(0x02)
    val diamondGift = byteArrayOf(0x03)
    val spadeGift = byteArrayOf(0x04)

    val giftImageDictionary = mapOf(
        "club" to R.drawable.club,
        "heart" to R.drawable.heart,
        "spade" to R.drawable.spade,
        "diamond" to R.drawable.diamond
    )

/*
    val permissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )
     */
}