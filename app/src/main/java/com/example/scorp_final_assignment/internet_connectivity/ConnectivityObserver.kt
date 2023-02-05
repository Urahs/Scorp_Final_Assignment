package com.example.scorp_final_assignment.internet_connectivity

import kotlinx.coroutines.flow.Flow

interface ConnectivityObserver {

    fun observe(): Flow<Status>

    enum class Status{
        Avaliable,
        Lost
    }
}