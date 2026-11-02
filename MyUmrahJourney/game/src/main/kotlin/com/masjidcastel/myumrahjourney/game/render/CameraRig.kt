package com.masjidcastel.myumrahjourney.game.render

import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import com.masjidcastel.myumrahjourney.game.sim.VehicleSpec
import com.masjidcastel.myumrahjourney.game.sim.VehicleState
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

enum class CameraMode(val label: String) {
    CHASE("CHASE"),
    FAR("FAR"),
    DRIVER("DRIVER");

    fun next(): CameraMode = entries[(ordinal + 1) % entries.size]
}

/**
 * Third-person follow camera plus a driver's-eye view. The follow yaw lags slightly behind the
 * car's heading, so the car visibly turns inside the frame before the view swings round.
 */
class CameraRig(val camera: PerspectiveCamera) {
    var mode: CameraMode = CameraMode.CHASE
        private set

    private var followYaw = 0f
    private var shake = 0f
    private val target = Vector3()
    private val desired = Vector3()

    /** The car body is hidden in driver view so it does not block the windscreen. */
    val showsCarBody: Boolean get() = mode != CameraMode.DRIVER

    fun snapTo(state: VehicleState) {
        followYaw = state.heading
        update(state, 1f)
    }

    fun cycleMode() {
        mode = mode.next()
    }

    /** Adds camera shake (0..1) for impacts; it decays on its own. */
    fun kick(amount: Float) {
        shake = min(1f, shake + amount)
    }

    fun update(state: VehicleState, dt: Float) {
        val rate = when (mode) {
            CameraMode.CHASE -> 5.0f
            CameraMode.FAR -> 3.2f
            CameraMode.DRIVER -> 40f
        }
        followYaw += wrap(state.heading - followYaw) * (1f - exp(-rate * dt))

        val speedFraction = min(1f, abs(state.speed) / 30f)
        val dirX = sin(followYaw)
        val dirZ = cos(followYaw)

        when (mode) {
            CameraMode.CHASE -> {
                val back = 6.6f + speedFraction * 1.2f
                desired.set(state.x - dirX * back, 2.5f, state.z - dirZ * back)
                target.set(state.x + dirX * 4f, 1.3f, state.z + dirZ * 4f)
                camera.fieldOfView = 62f + speedFraction * 10f
            }
            CameraMode.FAR -> {
                val back = 11.5f + speedFraction * 2f
                desired.set(state.x - dirX * back, 4.8f, state.z - dirZ * back)
                target.set(state.x + dirX * 3f, 1.0f, state.z + dirZ * 3f)
                camera.fieldOfView = 58f + speedFraction * 8f
            }
            CameraMode.DRIVER -> {
                // Seat position: slightly behind the bonnet, eye height above the road.
                val forward = 0.15f
                desired.set(state.x + dirX * forward, 1.22f, state.z + dirZ * forward)
                target.set(desired.x + dirX * 20f, 1.05f, desired.z + dirZ * 20f)
                camera.fieldOfView = 72f
            }
        }

        if (shake > 0.001f) {
            desired.add(
                MathUtils.random(-1f, 1f) * 0.22f * shake,
                MathUtils.random(-1f, 1f) * 0.16f * shake,
                MathUtils.random(-1f, 1f) * 0.22f * shake,
            )
            shake *= exp(-6f * dt)
        } else {
            shake = 0f
        }

        camera.position.set(desired)
        camera.up.set(Vector3.Y)
        camera.lookAt(target)
        camera.update()
    }

    /** Wraps an angle difference into -PI..PI so the camera takes the short way round. */
    private fun wrap(angle: Float): Float {
        var a = angle
        while (a > MathUtils.PI) a -= MathUtils.PI2
        while (a < -MathUtils.PI) a += MathUtils.PI2
        return a
    }

    companion object {
        /** Vertical aim point used to centre the shadow map on the car. */
        val CAR_CENTRE_HEIGHT: Float = VehicleSpec.WHEEL_RADIUS
    }
}
