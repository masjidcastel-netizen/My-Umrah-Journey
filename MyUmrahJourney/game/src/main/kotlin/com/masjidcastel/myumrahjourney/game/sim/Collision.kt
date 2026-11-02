package com.masjidcastel.myumrahjourney.game.sim

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

/**
 * Obstacles live on the ground plane (X/Z) and are either axis-aligned boxes (buildings,
 * barriers, boundary walls) or circles (tree trunks, lamp posts, boulders).
 * The vehicle is an oriented rectangle. All of this is plain Kotlin with no libGDX types,
 * so it is covered by ordinary JVM unit tests.
 */
sealed class Collider {
    abstract val minX: Float
    abstract val maxX: Float
    abstract val minZ: Float
    abstract val maxZ: Float

    /** Used by [ColliderGrid] to avoid returning the same collider twice from one query. */
    internal var queryStamp: Int = 0
}

class BoxCollider(
    override val minX: Float,
    override val minZ: Float,
    override val maxX: Float,
    override val maxZ: Float,
) : Collider()

class CircleCollider(
    val x: Float,
    val z: Float,
    val radius: Float,
) : Collider() {
    override val minX: Float get() = x - radius
    override val maxX: Float get() = x + radius
    override val minZ: Float get() = z - radius
    override val maxZ: Float get() = z + radius
}

/** Result of one overlap test. [nx]/[nz] is the unit normal pointing from the obstacle to the vehicle. */
class Contact {
    var nx = 0f
    var nz = 0f
    var depth = 0f

    fun set(nx: Float, nz: Float, depth: Float) {
        this.nx = nx
        this.nz = nz
        this.depth = depth
    }

    fun copyFrom(other: Contact) = set(other.nx, other.nz, other.depth)
}

/** Uniform grid so that only obstacles near the vehicle are tested each step. */
class ColliderGrid(private val cellSize: Float = 32f) {
    private val cells = HashMap<Long, ArrayList<Collider>>()
    private var stamp = 0

    var count = 0
        private set

    fun add(collider: Collider) {
        val x0 = cell(collider.minX)
        val x1 = cell(collider.maxX)
        val z0 = cell(collider.minZ)
        val z1 = cell(collider.maxZ)
        for (cx in x0..x1) {
            for (cz in z0..z1) {
                cells.getOrPut(key(cx, cz)) { ArrayList() }.add(collider)
            }
        }
        count++
    }

    /** Fills [out] (cleared first) with every collider whose cells overlap the query square. */
    fun query(x: Float, z: Float, radius: Float, out: MutableList<Collider>) {
        out.clear()
        stamp++
        for (cx in cell(x - radius)..cell(x + radius)) {
            for (cz in cell(z - radius)..cell(z + radius)) {
                val list = cells[key(cx, cz)] ?: continue
                for (collider in list) {
                    if (collider.queryStamp != stamp) {
                        collider.queryStamp = stamp
                        out.add(collider)
                    }
                }
            }
        }
    }

    private fun cell(v: Float): Int = floor(v / cellSize).toInt()

    private fun key(cx: Int, cz: Int): Long = (cx.toLong() shl 32) xor (cz.toLong() and 0xffffffffL)
}

object CollisionResolver {
    private const val SKIN = 0.002f
    private const val MAX_ITERATIONS = 4
    private const val ALIGN_MIN_COS = 0.4f
    private const val ALIGN_FACTOR = 0.12f
    private const val PI_F = Math.PI.toFloat()
    private const val TWO_PI_F = (2.0 * Math.PI).toFloat()

    /** Radius of a circle that surrounds the whole vehicle footprint; for broad-phase queries. */
    val boundingRadius: Float = hypot(VehicleSpec.HALF_LENGTH, VehicleSpec.HALF_WIDTH)

    /**
     * Pushes [state] out of every overlapping collider and removes the velocity component that
     * points into the obstacle (with a small bounce). Returns the largest impact speed in m/s,
     * or 0 when nothing was hit.
     */
    fun resolve(state: VehicleState, colliders: List<Collider>, restitution: Float = 0.15f): Float {
        val best = Contact()
        val tmp = Contact()
        var maxImpact = 0f

        for (iteration in 0 until MAX_ITERATIONS) {
            var deepest = 0f
            for (collider in colliders) {
                if (contact(state, collider, tmp) && tmp.depth > deepest) {
                    deepest = tmp.depth
                    best.copyFrom(tmp)
                }
            }
            if (deepest <= 0f) return maxImpact

            state.x += best.nx * (best.depth + SKIN)
            state.z += best.nz * (best.depth + SKIN)

            val fx = sin(state.heading)
            val fz = cos(state.heading)
            val vx = fx * state.speed
            val vz = fz * state.speed
            val vn = vx * best.nx + vz * best.nz
            if (vn < 0f) {
                maxImpact = max(maxImpact, -vn)
                val j = (1f + restitution) * vn
                val travelSign = if (state.speed >= 0f) 1f else -1f
                state.speed = (vx - j * best.nx) * fx + (vz - j * best.nz) * fz
                alignWithSurface(state, best, travelSign)
            }
        }
        return maxImpact
    }

    /**
     * A car that scrapes a wall at a shallow angle turns to run along it instead of re-hitting it
     * every step (which would bleed all its speed). Head-on hits (more than ~66 degrees off the
     * surface) are left alone.
     */
    private fun alignWithSurface(state: VehicleState, contact: Contact, travelSign: Float) {
        val tx0 = -contact.nz
        val tz0 = contact.nx
        val fx = sin(state.heading) * travelSign
        val fz = cos(state.heading) * travelSign
        val along = fx * tx0 + fz * tz0
        if (abs(along) < ALIGN_MIN_COS) return
        val side = if (along >= 0f) 1f else -1f
        // Desired direction of travel is the surface tangent, expressed as a heading.
        val desired = atan2(tx0 * side * travelSign, tz0 * side * travelSign)
        var diff = desired - state.heading
        while (diff > PI_F) diff -= TWO_PI_F
        while (diff < -PI_F) diff += TWO_PI_F
        state.heading += diff * ALIGN_FACTOR
    }

    /** Tests the vehicle footprint against [collider]. Fills [out] and returns true on overlap. */
    fun contact(state: VehicleState, collider: Collider, out: Contact): Boolean {
        // Cheap reject using the collider's bounds and the vehicle's bounding circle.
        val r = boundingRadius
        if (state.x + r < collider.minX || state.x - r > collider.maxX ||
            state.z + r < collider.minZ || state.z - r > collider.maxZ
        ) {
            return false
        }
        return when (collider) {
            is BoxCollider -> boxContact(state, collider, out)
            is CircleCollider -> circleContact(state, collider, out)
        }
    }

    private fun boxContact(state: VehicleState, box: BoxCollider, out: Contact): Boolean {
        val fx = sin(state.heading)
        val fz = cos(state.heading)
        val rx = cos(state.heading)
        val rz = -sin(state.heading)

        val bx = (box.minX + box.maxX) * 0.5f
        val bz = (box.minZ + box.maxZ) * 0.5f
        val hx = (box.maxX - box.minX) * 0.5f
        val hz = (box.maxZ - box.minZ) * 0.5f

        var bestOverlap = Float.MAX_VALUE
        var bestNx = 0f
        var bestNz = 0f

        // Separating axis test on the four candidate axes: the box's X and Z, the car's forward and right.
        for (axis in 0 until 4) {
            val ax: Float
            val az: Float
            when (axis) {
                0 -> { ax = 1f; az = 0f }
                1 -> { ax = 0f; az = 1f }
                2 -> { ax = fx; az = fz }
                else -> { ax = rx; az = rz }
            }
            val centreDistance = (state.x - bx) * ax + (state.z - bz) * az
            val carRadius = VehicleSpec.HALF_LENGTH * abs(fx * ax + fz * az) +
                VehicleSpec.HALF_WIDTH * abs(rx * ax + rz * az)
            val boxRadius = hx * abs(ax) + hz * abs(az)
            val overlap = carRadius + boxRadius - abs(centreDistance)
            if (overlap <= 0f) return false
            if (overlap < bestOverlap) {
                bestOverlap = overlap
                val sign = if (centreDistance >= 0f) 1f else -1f
                bestNx = ax * sign
                bestNz = az * sign
            }
        }
        out.set(bestNx, bestNz, bestOverlap)
        return true
    }

    private fun circleContact(state: VehicleState, circle: CircleCollider, out: Contact): Boolean {
        val fx = sin(state.heading)
        val fz = cos(state.heading)
        val rx = cos(state.heading)
        val rz = -sin(state.heading)

        // Circle centre in the car's local frame (f = along the car, r = across it).
        val dx = circle.x - state.x
        val dz = circle.z - state.z
        val lf = dx * fx + dz * fz
        val lr = dx * rx + dz * rz

        val cf = lf.coerceIn(-VehicleSpec.HALF_LENGTH, VehicleSpec.HALF_LENGTH)
        val cr = lr.coerceIn(-VehicleSpec.HALF_WIDTH, VehicleSpec.HALF_WIDTH)
        val df = lf - cf
        val dr = lr - cr
        val dist = hypot(df, dr)

        // Local normal pointing from the car towards the circle.
        val nf: Float
        val nr: Float
        val depth: Float
        if (dist > 1e-5f) {
            if (dist >= circle.radius) return false
            nf = df / dist
            nr = dr / dist
            depth = circle.radius - dist
        } else {
            // Circle centre is inside the footprint: leave through the nearest side.
            val penF = VehicleSpec.HALF_LENGTH - abs(lf)
            val penR = VehicleSpec.HALF_WIDTH - abs(lr)
            if (penF < penR) {
                nf = if (lf >= 0f) 1f else -1f
                nr = 0f
                depth = penF + circle.radius
            } else {
                nf = 0f
                nr = if (lr >= 0f) 1f else -1f
                depth = penR + circle.radius
            }
        }
        // Convert to world space and flip so the normal points from the obstacle to the car.
        out.set(-(fx * nf + rx * nr), -(fz * nf + rz * nr), depth)
        return true
    }
}
