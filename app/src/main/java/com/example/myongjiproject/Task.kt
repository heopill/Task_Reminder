package com.example.myongjiproject

data class Task(
    val id: String = "",
    val title: String = "",
    val dueDate: String = "",
    val courseName: String = "",
    val completed: Boolean = false,
    var notified3Days: Boolean = false,
    var notified1Day: Boolean = false
)
