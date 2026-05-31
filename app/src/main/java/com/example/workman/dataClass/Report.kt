package com.example.workman.dataClass

import com.google.firebase.Timestamp

data class Report(
    val reportId: String = "",
    val reporterId: String = "",
    val reportedEntityId: String = "", // jobId or userId
    val reportType: String = "JOB", // JOB or USER
    val reason: String = "",
    val timestamp: Timestamp = Timestamp.now()
)
