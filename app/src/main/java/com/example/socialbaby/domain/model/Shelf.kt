package com.example.socialbaby.domain.model

data class Shelf(
    val id: Long = 0,
    val name: String,
    val kind: ShelfKind = ShelfKind.SHELF,
    val sortOrder: Int = 0
)

enum class ShelfKind {
    SHELF,
    PLAYLIST,
    SHORTS_FEED,
    PHOTOS
}

data class ParentSettings(
    val dailyLimitMinutes: Int = 60,
    val bedtimeEnabled: Boolean = false,
    val bedtimeStart: String = "20:00",
    val bedtimeEnd: String = "07:00",
    val maxSessionMinutes: Int = 30,
    val shortsEnabled: Boolean = true,
    val onlineLinksEnabled: Boolean = true
)
