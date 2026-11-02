package com.masjidcastel.myumrahjourney.core.player

/** 3D game graphics preset. The game module maps each level to draw distance, shadows and antialiasing. */
enum class GraphicsQuality(val code: String) {
    LOW("low"),
    MEDIUM("medium"),
    HIGH("high");

    fun next(): GraphicsQuality = entries[(ordinal + 1) % entries.size]

    companion object {
        val DEFAULT: GraphicsQuality = MEDIUM

        fun fromCode(code: String?): GraphicsQuality? = entries.firstOrNull { it.code == code }
    }
}
