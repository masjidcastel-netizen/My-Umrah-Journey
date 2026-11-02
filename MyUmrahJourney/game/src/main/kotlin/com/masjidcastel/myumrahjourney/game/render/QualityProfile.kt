package com.masjidcastel.myumrahjourney.game.render

import com.masjidcastel.myumrahjourney.core.player.GraphicsQuality

/**
 * What each graphics preset changes. Everything except [msaaSamples] can be switched while the
 * game runs; antialiasing is fixed when the OpenGL surface is created, so it applies next launch.
 */
class QualityProfile(
    /** Detailed world chunks beyond this distance (metres) are not drawn. */
    val drawDistance: Float,
    val shadows: Boolean,
    val shadowMapSize: Int,
    val msaaSamples: Int,
) {
    companion object {
        /** Camera far plane. Fixed so distance haze looks the same on every preset and hills stay visible. */
        const val FAR_PLANE = 650f

        fun of(quality: GraphicsQuality): QualityProfile = when (quality) {
            GraphicsQuality.LOW -> QualityProfile(drawDistance = 200f, shadows = false, shadowMapSize = 0, msaaSamples = 0)
            GraphicsQuality.MEDIUM -> QualityProfile(drawDistance = 320f, shadows = true, shadowMapSize = 1024, msaaSamples = 0)
            GraphicsQuality.HIGH -> QualityProfile(drawDistance = 480f, shadows = true, shadowMapSize = 2048, msaaSamples = 2)
        }
    }
}
