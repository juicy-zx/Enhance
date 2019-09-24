package com.juicy.main

data class WxArticle(
    val courseId: Long = 0,
    val id: Long = 0,
    val name: String = "",
    val order: Long = 0,
    val parentChapterId: Long = 0,
    val userControlSetTop: Boolean = false,
    val visible: Int = 0
)
