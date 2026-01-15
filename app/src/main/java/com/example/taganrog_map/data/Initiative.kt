package com.example.taganrog_map.data

import com.google.gson.annotations.SerializedName

data class Initiative(
    val id: String,
    val lat: Double,
    val lon: Double,
    val title: String,
    val status: Status,
    val description: String = "",
    val address: String = "",
    val category: String = "",
    val author: Author = Author("", "Гражданин", "Гражданин"),
    val createdAt: String = "",
    val updatedAt: String = ""
)

data class Author(
    val id: String = "",
    val name: String,
    val role: String
)

enum class Status { 
    RED, 
    YELLOW, 
    GREEN
}

// DTO для API
data class InitiativeResponse(
    val id: Int,
    val lat: Double,
    val lon: Double,
    val title: String,
    val status: String,
    val description: String,
    val address: String,
    val category: String,
    @SerializedName("author_name") val authorName: String,
    @SerializedName("author_role") val authorRole: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
) {
    fun toInitiative(): Initiative {
        return Initiative(
            id = id.toString(),
            lat = lat,
            lon = lon,
            title = title,
            status = when (status) {
                "RED" -> Status.RED
                "YELLOW" -> Status.YELLOW
                "GREEN" -> Status.GREEN
                else -> Status.RED
            },
            description = description,
            address = address,
            category = category,
            author = Author("", authorName, authorRole),
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}

data class InitiativeCreateRequest(
    val title: String,
    val description: String = "",
    val status: String = "RED",
    val category: String = "",
    val lat: Double,
    val lon: Double,
    @SerializedName("author_name") val authorName: String = "Гражданин",
    @SerializedName("author_role") val authorRole: String = "Гражданин"
)
