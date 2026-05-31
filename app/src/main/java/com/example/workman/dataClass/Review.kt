package com.example.workman.dataClass

data class Review(
    var reviewId: String = "",
    var jobId: String = "",
    var reviewerId: String = "",
    var reviewerName: String = "",
    var revieweeId: String = "", // The worker being reviewed
    var rating: Float = 0f,
    var comment: String = "",
    var timestamp: Any? = null
) {
    // No-argument constructor for Firebase
    constructor() : this("", "", "", "", "", 0f, "", null)
}
