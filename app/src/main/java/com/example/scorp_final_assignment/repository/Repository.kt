package com.example.scorp_final_assignment.repository

import com.example.scorp_final_assignment.R
import java.time.LocalTime

object Repository {

    const val MinNickNameLength = 4
    //const val AppID = "dca16fbf522b4c6f96ecc88721800310"
    const val AppID = "09155caed6e0491482a59dd13ea8e8e2"
    val Token = null
    //const val ChannelID = "a44c58de-311b-4033-b70f-1570406c156c"
    const val ChannelID = "a44c58de-311b-4033-b70f-1570406c156a"

    val clubGift = byteArrayOf(0x01)
    val heartGift = byteArrayOf(0x02)
    val diamondGift = byteArrayOf(0x03)
    val spadeGift = byteArrayOf(0x04)


    val byteValueToImageDictionary = mapOf(
        clubGift.contentToString() to R.drawable.club,
        heartGift.contentToString() to R.drawable.heart,
        spadeGift.contentToString() to R.drawable.spade,
        diamondGift.contentToString() to R.drawable.diamond
    )

    data class Gift(
        val isSended: Boolean,
        val giftByteArray: ByteArray,
        val giftImage: Int?
    )


    data class Message(
        val content: String,
        val time: LocalTime
    )


/*
    val permissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )
     */
}