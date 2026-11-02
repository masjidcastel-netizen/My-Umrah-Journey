package com.masjidcastel.myumrahjourney.game.world

import com.masjidcastel.myumrahjourney.game.sim.Collider
import com.masjidcastel.myumrahjourney.game.sim.CollisionResolver
import com.masjidcastel.myumrahjourney.game.sim.Contact
import com.masjidcastel.myumrahjourney.game.sim.DrivingSimulation
import com.masjidcastel.myumrahjourney.game.sim.VehicleInput
import com.masjidcastel.myumrahjourney.game.sim.VehicleState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class WorldLayoutTest {

    private val world = WorldGenerator.generate()

    @Test
    fun layoutIsDeterministicForAGivenSeed() {
        val again = WorldGenerator.generate()
        assertEquals(world.buildings.size, again.buildings.size)
        assertEquals(world.trees.size, again.trees.size)
        assertEquals(world.props.size, again.props.size)
        assertEquals(world.colliders.count, again.colliders.count)
        assertEquals(world.buildings.first().height, again.buildings.first().height, 0f)
    }

    @Test
    fun levelHasEnoughContent() {
        assertTrue("buildings ${world.buildings.size}", world.buildings.size >= 25)
        assertTrue("trees ${world.trees.size}", world.trees.size >= 100)
        assertTrue("lamps", world.props.count { it.kind == PropKind.LAMP } >= 20)
        assertTrue("barriers", world.props.count { it.kind == PropKind.BARRIER } >= 3)
        assertTrue("road pieces", world.roads.size > 50)
        assertTrue("markings", world.markings.size > 200)
        assertTrue("hills", world.hills.size >= 10)
        assertTrue("towers", world.buildings.any { it.kind == BuildingKind.TOWER })
        assertTrue("palms", world.trees.any { it.kind == TreeKind.PALM })
    }

    @Test
    fun spawnPointIsOnTheRoadAndClearOfObstacles() {
        val spawn = world.spawn
        assertTrue(abs(spawn.x) < WorldConstants.ROAD_HALF)
        val state = VehicleState(spawn.x, spawn.z, spawn.heading)
        val nearby = ArrayList<Collider>()
        world.colliders.query(spawn.x, spawn.z, 12f, nearby)
        val contact = Contact()
        assertFalse(nearby.any { CollisionResolver.contact(state, it, contact) })
    }

    @Test
    fun buildingsNeverSitOnARoad() {
        val half = WorldConstants.ROAD_HALF
        for (b in world.buildings) {
            for (line in WorldConstants.LINES) {
                val overlapsX = b.cx + b.width / 2 > line - half && b.cx - b.width / 2 < line + half
                val overlapsZ = b.cz + b.depth / 2 > line - half && b.cz - b.depth / 2 < line + half
                assertFalse("building at ${b.cx},${b.cz} overlaps road x=$line", overlapsX)
                assertFalse("building at ${b.cx},${b.cz} overlaps road z=$line", overlapsZ)
            }
        }
    }

    @Test
    fun buildingsStayInsideTheBoundary() {
        for (b in world.buildings) {
            assertTrue(abs(b.cx) + b.width / 2 < WorldConstants.HALF)
            assertTrue(abs(b.cz) + b.depth / 2 < WorldConstants.HALF)
        }
    }

    @Test
    fun noTreeOrLampIsStandingInTheRoad() {
        val half = WorldConstants.ROAD_HALF
        for (t in world.trees) {
            for (line in WorldConstants.LINES) {
                assertFalse("tree at ${t.x},${t.z}", abs(t.x - line) < half)
                assertFalse("tree at ${t.x},${t.z}", abs(t.z - line) < half)
            }
        }
    }

    @Test
    fun noRoadPieceIsLongerThanTheFogFriendlyLimit() {
        for (r in world.roads) {
            assertTrue(r.sizeX <= WorldConstants.MAX_PIECE + 0.1f)
            assertTrue(r.sizeZ <= WorldConstants.MAX_PIECE + 0.1f)
        }
    }

    @Test
    fun carCannotLeaveTheWorld() {
        val sim = DrivingSimulation()
        sim.state.reset(world.spawn.x, world.spawn.z, world.spawn.heading)
        // Drive straight along the main road for a long time: the barrier chicane and the wall must stop it.
        repeat(120 * 90) { sim.step(VehicleInput(throttle = 1f), 1f / 120f, world.colliders) }
        assertTrue("x=${sim.state.x} z=${sim.state.z}", abs(sim.state.x) <= WorldConstants.HALF)
        assertTrue("x=${sim.state.x} z=${sim.state.z}", abs(sim.state.z) <= WorldConstants.HALF)
    }

    @Test
    fun drivingIntoTheChicaneCausesACollision() {
        val sim = DrivingSimulation()
        sim.state.reset(world.spawn.x, world.spawn.z, world.spawn.heading)
        var impact = 0f
        repeat(120 * 20) { impact = maxOf(impact, sim.step(VehicleInput(throttle = 1f), 1f / 120f, world.colliders)) }
        assertTrue("impact $impact", impact > 1f)
    }

    @Test
    fun flatSurfacesAreOnTheGroundPlaneArea() {
        for (m in world.markings) {
            assertTrue(abs(m.cx) <= WorldConstants.HALF + 1f && abs(m.cz) <= WorldConstants.HALF + 1f)
        }
    }
}
