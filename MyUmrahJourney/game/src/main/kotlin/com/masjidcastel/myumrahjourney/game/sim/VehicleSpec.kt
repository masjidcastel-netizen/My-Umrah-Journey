package com.masjidcastel.myumrahjourney.game.sim

/**
 * Physical size of the car in metres. Shared by the simulation (collision footprint) and the
 * 3D model, so what you see is what collides.
 *
 * The vehicle origin is the centre of its footprint on the ground. Forward is +Z at heading 0.
 */
object VehicleSpec {
    const val HALF_LENGTH = 2.3f
    const val HALF_WIDTH = 0.95f
    const val WHEEL_BASE = 2.7f
    const val WHEEL_RADIUS = 0.34f
    const val WHEEL_WIDTH = 0.26f

    /** Distance of each axle from the vehicle centre. */
    const val AXLE_OFFSET = WHEEL_BASE / 2f

    /** Distance of each wheel centre from the vehicle centre line. */
    const val TRACK_HALF = 0.82f
}
