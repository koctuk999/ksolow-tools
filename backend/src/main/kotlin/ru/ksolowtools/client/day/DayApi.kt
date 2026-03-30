package ru.ksolowtools.client.day

import retrofit2.Call
import retrofit2.http.GET


interface DayApi {

    @GET("today")
    fun today(): Call<Int>

    @GET("tomorrow")
    fun tomorrow(): Call<Int>
}


