package com.roxanasultan.memoraid.models

interface Repeatable {
    val date: String
    val frequency: String
    val everyXDays: Int?
    val weeklyDays: List<String>?
    val monthlyDay: Int?
    val skippedDates: List<String>?
    val endDate: String?
}