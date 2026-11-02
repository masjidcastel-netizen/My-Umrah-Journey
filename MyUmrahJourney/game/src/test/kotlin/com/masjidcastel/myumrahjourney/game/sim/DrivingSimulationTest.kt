package com.masjidcastel.myumrahjourney.game.sim

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class DrivingSimulationTest {

    private val dt = 1f / 120f

    private fun run(
        sim: DrivingSimulation,
        seconds: Float,
        input: VehicleInput,
        world: ColliderGrid? = null,
    ): Float {
        var impact = 0f
        repeat((seconds / dt).toInt()) { impact = maxOf(impact, sim.step(input, dt, world)) }
        return impact
    }

    @Test
    fun throttleAcceleratesAlongForwardAxis() {
        val sim = DrivingSimulation()
        run(sim, 3f, VehicleInput(throttle = 1f))
        assertTrue("speed ${sim.state.speed}", sim.state.speed in 10f..sim.params.maxSpeed)
        assertTrue(sim.state.z > 10f)
        assertEquals(0f, sim.state.x, 1e-3f)
        assertEquals(0f, sim.state.heading, 1e-6f)
    }

    @Test
    fun topSpeedIsNeverExceeded() {
        val sim = DrivingSimulation()
        run(sim, 60f, VehicleInput(throttle = 1f))
        assertTrue("speed ${sim.state.speed}", sim.state.speed > 25f)
        assertTrue(sim.state.speed <= sim.params.maxSpeed)
    }

    @Test
    fun brakeStopsTheCar() {
        val sim = DrivingSimulation()
        run(sim, 5f, VehicleInput(throttle = 1f))
        val before = sim.state.speed
        assertTrue(before > 15f)
        run(sim, 2.5f, VehicleInput(brake = 1f))
        assertTrue("speed ${sim.state.speed}", sim.state.speed < 1.0f)
    }

    @Test
    fun holdingBrakeFromStandstillReverses() {
        val sim = DrivingSimulation()
        run(sim, 2.5f, VehicleInput(brake = 1f))
        assertTrue("speed ${sim.state.speed}", sim.state.speed < -3f)
        assertTrue(sim.state.z < -3f)
        assertTrue(sim.state.speed >= -sim.params.reverseMaxSpeed)
    }

    @Test
    fun coastingSlowsDownAndStopsWithoutReversing() {
        val sim = DrivingSimulation()
        run(sim, 4f, VehicleInput(throttle = 1f))
        run(sim, 60f, VehicleInput())
        assertEquals(0f, sim.state.speed, 1e-4f)
    }

    @Test
    fun carCannotSteerWhileStationary() {
        val sim = DrivingSimulation()
        run(sim, 2f, VehicleInput(steer = 1f))
        assertEquals(0f, sim.state.heading, 1e-6f)
    }

    @Test
    fun steeringLeftIncreasesHeadingAndRightDecreasesIt() {
        val left = DrivingSimulation()
        run(left, 4f, VehicleInput(steer = 1f, throttle = 1f))
        assertTrue(left.state.heading > 0.3f)
        assertTrue("left means +X", left.state.x > 1f)

        val right = DrivingSimulation()
        run(right, 4f, VehicleInput(steer = -1f, throttle = 1f))
        assertTrue(right.state.heading < -0.3f)
        assertTrue("right means -X", right.state.x < -1f)
    }

    @Test
    fun steeringAngleShrinksAtSpeed() {
        val sim = DrivingSimulation()
        assertTrue(sim.maxSteerAt(30f) < sim.maxSteerAt(0f))
        assertTrue(sim.maxSteerAt(30f) > 0f)
    }

    @Test
    fun steeringWheelReturnsToCentreWhenReleased() {
        val sim = DrivingSimulation()
        run(sim, 2f, VehicleInput(steer = 1f, throttle = 1f))
        assertTrue(sim.state.steerAngle > 0.1f)
        run(sim, 1f, VehicleInput(throttle = 1f))
        assertEquals(0f, sim.state.steerAngle, 1e-4f)
    }

    @Test
    fun wallStopsTheCar() {
        val world = ColliderGrid()
        world.add(BoxCollider(-20f, 20f, 20f, 22f))
        val sim = DrivingSimulation()
        val impact = run(sim, 6f, VehicleInput(throttle = 1f), world)
        assertTrue("impact $impact", impact > 5f)
        assertTrue("front must stay behind the wall, z=${sim.state.z}", sim.state.z + VehicleSpec.HALF_LENGTH <= 20.01f)
        assertTrue("speed ${sim.state.speed}", abs(sim.state.speed) < 2.5f)
    }

    @Test
    fun treeTrunkBlocksTheCar() {
        val world = ColliderGrid()
        world.add(CircleCollider(0f, 15f, 0.5f))
        val sim = DrivingSimulation()
        val impact = run(sim, 6f, VehicleInput(throttle = 1f), world)
        assertTrue("impact $impact", impact > 3f)
        assertTrue("z=${sim.state.z}", sim.state.z + VehicleSpec.HALF_LENGTH <= 14.5f + 0.01f)
    }

    @Test
    fun carSlidesAlongAWallInsteadOfStickingToIt() {
        val world = ColliderGrid()
        world.add(BoxCollider(5f, -100f, 8f, 400f)) // long wall on the car's left (+X side)
        val sim = DrivingSimulation()
        sim.state.heading = 0.2f // angled slightly towards the wall
        run(sim, 8f, VehicleInput(throttle = 1f), world)
        assertTrue("z=${sim.state.z}", sim.state.z > 60f)
        assertTrue("x=${sim.state.x}", sim.state.x + VehicleSpec.HALF_WIDTH <= 5.6f)
    }

    @Test
    fun noImpactWhenNothingIsNear() {
        val world = ColliderGrid()
        world.add(BoxCollider(100f, 100f, 110f, 110f))
        val sim = DrivingSimulation()
        assertEquals(0f, run(sim, 2f, VehicleInput(throttle = 1f), world), 0f)
    }

    @Test
    fun rotatedCarContactsABoxCornerWithDepth() {
        val state = VehicleState(x = 0f, z = 0f, heading = (Math.PI / 4).toFloat())
        val contact = Contact()
        // A box whose corner pokes into the car's nose.
        val hit = CollisionResolver.contact(state, BoxCollider(1.4f, 1.4f, 6f, 6f), contact)
        assertTrue(hit)
        assertTrue(contact.depth > 0f)
        val length = kotlin.math.hypot(contact.nx, contact.nz)
        assertEquals(1f, length, 1e-4f)
        // Normal must point from the box towards the car, i.e. back towards the origin.
        assertTrue(contact.nx < 0.1f && contact.nz < 0.1f)
    }

    @Test
    fun farAwayObstacleIsNotAContact() {
        val state = VehicleState()
        assertFalse(CollisionResolver.contact(state, BoxCollider(30f, 30f, 40f, 40f), Contact()))
        assertFalse(CollisionResolver.contact(state, CircleCollider(10f, 0f, 1f), Contact()))
    }

    @Test
    fun colliderGridReturnsEachNearbyColliderOnce() {
        val grid = ColliderGrid(cellSize = 16f)
        val big = BoxCollider(-100f, -100f, 100f, 100f) // spans many cells
        val tree = CircleCollider(3f, 3f, 0.5f)
        val far = CircleCollider(500f, 500f, 0.5f)
        grid.add(big)
        grid.add(tree)
        grid.add(far)
        val out = ArrayList<Collider>()
        grid.query(0f, 0f, 40f, out)
        assertEquals(2, out.size)
        assertTrue(out.contains(big) && out.contains(tree))
        grid.query(0f, 0f, 40f, out) // a second query must not be polluted by stamps
        assertEquals(2, out.size)
        assertEquals(3, grid.count)
    }

    @Test
    fun forwardVectorMatchesHeading() {
        val s = VehicleState(heading = 0.7f)
        assertEquals(sin(0.7f), s.forwardX, 1e-6f)
        assertEquals(cos(0.7f), s.forwardZ, 1e-6f)
    }

    @Test
    fun missingBoxContactReturnsNullEquivalent() {
        val out = Contact()
        assertNull(if (CollisionResolver.contact(VehicleState(x = 50f), BoxCollider(0f, 0f, 1f, 1f), out)) out else null)
    }
}
