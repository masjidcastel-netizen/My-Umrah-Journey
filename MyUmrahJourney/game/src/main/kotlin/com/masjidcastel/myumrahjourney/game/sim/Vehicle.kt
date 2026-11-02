package com.masjidcastel.myumrahjourney.game.sim

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tan

/**
 * Mutable vehicle state. Heading 0 faces +Z; a positive heading turns towards +X, which is the
 * car's left (right-handed axes, Y up). Speed is signed: negative means reversing.
 */
class VehicleState(
    var x: Float = 0f,
    var z: Float = 0f,
    var heading: Float = 0f,
    var speed: Float = 0f,
    /** Current front-wheel angle in radians, positive = left. */
    var steerAngle: Float = 0f,
) {
    val forwardX: Float get() = sin(heading)
    val forwardZ: Float get() = cos(heading)
    val speedKmh: Float get() = abs(speed) * 3.6f

    fun reset(x: Float, z: Float, heading: Float) {
        this.x = x
        this.z = z
        this.heading = heading
        speed = 0f
        steerAngle = 0f
    }
}

/**
 * Control input for one step.
 * [steer] -1..1 (+1 = left), [throttle] 0..1, [brake] 0..1.
 * Holding the brake at a standstill reverses the car.
 */
data class VehicleInput(
    val steer: Float = 0f,
    val throttle: Float = 0f,
    val brake: Float = 0f,
)

data class VehicleParams(
    val maxSpeed: Float = 40f,
    val reverseMaxSpeed: Float = 9f,
    val engineAccel: Float = 8.5f,
    val brakeDecel: Float = 24f,
    val reverseAccel: Float = 4.5f,
    val rollingResistance: Float = 0.6f,
    val dragCoefficient: Float = 0.0010f,
    val maxSteerAngle: Float = 0.62f,
    /** Fraction of the steering angle lost at top speed (keeps high-speed driving stable). */
    val steerFalloff: Float = 0.65f,
    val steerRate: Float = 2.6f,
    val steerReturnRate: Float = 4.0f,
)

/** Arcade bicycle-model driving: simple, predictable and cheap enough for mid-range phones. */
class DrivingSimulation(
    val params: VehicleParams = VehicleParams(),
    val state: VehicleState = VehicleState(),
) {
    private val nearby = ArrayList<Collider>(32)

    fun maxSteerAt(speed: Float): Float {
        val t = min(1f, abs(speed) / params.maxSpeed)
        return params.maxSteerAngle * (1f - params.steerFalloff * t)
    }

    /**
     * Advances the car by [dt] seconds and resolves collisions against [world].
     * Returns the impact speed in m/s (0 when nothing was hit).
     */
    fun step(input: VehicleInput, dt: Float, world: ColliderGrid? = null): Float {
        val steerInput = input.steer.coerceIn(-1f, 1f)
        val throttle = input.throttle.coerceIn(0f, 1f)
        val brake = input.brake.coerceIn(0f, 1f)

        // Steering wheel moves towards the target angle at a limited rate.
        val target = steerInput * maxSteerAt(state.speed)
        val rate = if (steerInput == 0f) params.steerReturnRate else params.steerRate
        val maxDelta = rate * dt
        state.steerAngle += (target - state.steerAngle).coerceIn(-maxDelta, maxDelta)

        // Longitudinal acceleration from the pedals.
        var accel = 0f
        if (throttle > 0f) {
            accel += if (state.speed < -0.5f) {
                params.brakeDecel * throttle // pedal while reversing brakes first
            } else {
                params.engineAccel * throttle * (1f - (max(state.speed, 0f) / params.maxSpeed).coerceIn(0f, 1f))
            }
        }
        if (brake > 0f) {
            if (state.speed > 0.5f) {
                accel -= params.brakeDecel * brake
            } else if (throttle <= 0f) {
                accel -= params.reverseAccel * brake *
                    (1f - (max(-state.speed, 0f) / params.reverseMaxSpeed).coerceIn(0f, 1f))
            }
        }

        var speed = state.speed + accel * dt

        // Rolling resistance and air drag always oppose motion and never reverse it.
        val resistance = (params.rollingResistance + params.dragCoefficient * speed * speed) * dt
        speed = when {
            speed > resistance -> speed - resistance
            speed < -resistance -> speed + resistance
            accel == 0f -> 0f
            else -> speed
        }
        state.speed = speed.coerceIn(-params.reverseMaxSpeed, params.maxSpeed)

        // Bicycle model; heading is advanced by half a step first (midpoint) for stable curves.
        val yawRate = state.speed / VehicleSpec.WHEEL_BASE * tan(state.steerAngle)
        val midHeading = state.heading + yawRate * dt * 0.5f
        state.x += sin(midHeading) * state.speed * dt
        state.z += cos(midHeading) * state.speed * dt
        state.heading += yawRate * dt

        if (world == null) return 0f
        world.query(state.x, state.z, CollisionResolver.boundingRadius + 0.5f, nearby)
        return CollisionResolver.resolve(state, nearby)
    }
}
