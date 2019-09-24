package com.juicy.main

data class BaseBo<T>(
    val errorCode: Int = 0,
    val data: T? = null,
    val errorMsg: String = ""
) {
    fun isSuccess(): Boolean = errorCode == 0
}

data class ReportBo(val success: Boolean, val errorMsg: String = "")