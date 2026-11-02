package com.masjidcastel.myumrahjourney.game.world

import com.masjidcastel.myumrahjourney.game.sim.BoxCollider
import com.masjidcastel.myumrahjourney.game.sim.CircleCollider
import com.masjidcastel.myumrahjourney.game.sim.ColliderGrid
import java.util.Random
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Plain RGB colour (0..1) so the layout has no dependency on libGDX. */
class Rgb(val r: Float, val g: Float, val b: Float) {
    companion object {
        fun hex(value: Int) = Rgb(
            ((value shr 16) and 0xFF) / 255f,
            ((value shr 8) and 0xFF) / 255f,
            (value and 0xFF) / 255f,
        )
    }
}

/** A flat rectangle lying on the ground: a road piece or a paint marking. */
class FlatRect(
    val cx: Float,
    val cz: Float,
    val sizeX: Float,
    val sizeZ: Float,
    val color: Rgb = Rgb(1f, 1f, 1f),
)

enum class BuildingKind { HOUSE, SHOP, TOWER }

class BuildingSpec(
    val cx: Float,
    val cz: Float,
    val width: Float,
    val depth: Float,
    val height: Float,
    val wall: Rgb,
    val roof: Rgb,
    val kind: BuildingKind,
)

enum class TreeKind { ROUND, PALM }

class TreeSpec(
    val x: Float,
    val z: Float,
    val height: Float,
    val kind: TreeKind,
    val foliage: Rgb,
    val rotation: Float,
)

enum class PropKind { LAMP, BARRIER, BOULDER }

class PropSpec(
    val kind: PropKind,
    val x: Float,
    val z: Float,
    val sizeX: Float = 1f,
    val sizeZ: Float = 1f,
    val height: Float = 1f,
    val rotation: Float = 0f,
)

/** Low boundary wall, drawn on the edge of the playable area. */
class WallSpec(val cx: Float, val cz: Float, val sizeX: Float, val sizeZ: Float, val height: Float)

/** Decorative backdrop hill outside the playable area (no collision). */
class HillSpec(val x: Float, val z: Float, val radius: Float, val height: Float, val color: Rgb)

class SpawnPoint(val x: Float, val z: Float, val heading: Float)

/**
 * Everything about the prototype level except the 3D meshes: where each road, building and tree
 * is, and the matching collision shapes. Built from a fixed seed, so every launch is identical.
 */
class WorldLayout(
    val halfSize: Float,
    val groundHalf: Float,
    val roads: List<FlatRect>,
    val markings: List<FlatRect>,
    val buildings: List<BuildingSpec>,
    val trees: List<TreeSpec>,
    val props: List<PropSpec>,
    val walls: List<WallSpec>,
    val hills: List<HillSpec>,
    val colliders: ColliderGrid,
    val spawn: SpawnPoint,
)

object WorldConstants {
    /** The car can drive anywhere inside +-HALF on both axes. */
    const val HALF = 240f
    const val GROUND_HALF = 480f
    const val ROAD_WIDTH = 14f
    const val ROAD_HALF = ROAD_WIDTH / 2f

    /** Road centre lines, used for both axes (a 3x3 grid of roads). */
    val LINES = floatArrayOf(-150f, 0f, 150f)

    /** Longest single piece of road/ground geometry; keeps per-vertex fog and lighting accurate. */
    const val MAX_PIECE = 40f
}

object WorldGenerator {
    private val WHITE = Rgb(0.95f, 0.95f, 0.93f)
    private val YELLOW = Rgb(0.95f, 0.78f, 0.25f)

    private val WALLS = listOf(
        Rgb.hex(0xE8D8B0), Rgb.hex(0xF1EFE6), Rgb.hex(0xD9A07A), Rgb.hex(0xBFE3DD),
        Rgb.hex(0xF3E5C0), Rgb.hex(0xD0D3D1), Rgb.hex(0xA9D4C0),
    )
    private val ROOFS = listOf(Rgb.hex(0x8C5A3C), Rgb.hex(0x5C6B66), Rgb.hex(0x2F6F66), Rgb.hex(0x7A4A38))
    private val FOLIAGE = listOf(Rgb.hex(0x2E7D32), Rgb.hex(0x388E3C), Rgb.hex(0x1B6B3A), Rgb.hex(0x4C9A2A))
    private val HILLS = listOf(Rgb.hex(0x2F6B45), Rgb.hex(0x3C7A4B), Rgb.hex(0x5A7A3A), Rgb.hex(0x7B7A4A))

    fun generate(seed: Long = 20260101L): WorldLayout {
        val rnd = Random(seed)
        val half = WorldConstants.HALF
        val roadHalf = WorldConstants.ROAD_HALF
        val lines = WorldConstants.LINES

        val roads = ArrayList<FlatRect>()
        val markings = ArrayList<FlatRect>()
        val buildings = ArrayList<BuildingSpec>()
        val trees = ArrayList<TreeSpec>()
        val props = ArrayList<PropSpec>()
        val walls = ArrayList<WallSpec>()
        val hills = ArrayList<HillSpec>()
        val colliders = ColliderGrid(32f)
        val buildingBoxes = ArrayList<BoxCollider>()

        // Free stretches of road between intersections along one axis: [-240,-157], [-143,-7], ...
        val stretches = ArrayList<Pair<Float, Float>>()
        var start = -half
        for (line in lines) {
            stretches.add(start to (line - roadHalf))
            start = line + roadHalf
        }
        stretches.add(start to half)

        // ---- Roads -------------------------------------------------------------------------
        for (line in lines) {
            for ((a, b) in stretches) {
                pieces(a, b, WorldConstants.MAX_PIECE) { centre, length ->
                    roads.add(FlatRect(line, centre, WorldConstants.ROAD_WIDTH, length + 0.04f)) // runs along Z
                    roads.add(FlatRect(centre, line, length + 0.04f, WorldConstants.ROAD_WIDTH)) // runs along X
                }
            }
        }
        for (lx in lines) for (lz in lines) {
            roads.add(FlatRect(lx, lz, WorldConstants.ROAD_WIDTH, WorldConstants.ROAD_WIDTH))
        }

        // ---- Road markings -------------------------------------------------------------------
        val edge = roadHalf - 0.6f
        for (line in lines) {
            for ((a, b) in stretches) {
                var p = a + 2f
                while (p + 3.5f <= b - 1f) {
                    markings.add(FlatRect(line, p + 1.75f, 0.25f, 3.5f, YELLOW))
                    markings.add(FlatRect(p + 1.75f, line, 3.5f, 0.25f, YELLOW))
                    p += 9f
                }
                pieces(a, b, 20f) { centre, length ->
                    for (side in floatArrayOf(-1f, 1f)) {
                        markings.add(FlatRect(line + side * edge, centre, 0.2f, length, WHITE))
                        markings.add(FlatRect(centre, line + side * edge, length, 0.2f, WHITE))
                    }
                }
            }
        }
        // Zebra crossings on all four arms of every intersection.
        for (lx in lines) for (lz in lines) {
            var s = -5.4f
            while (s <= 5.4f) {
                val arm = roadHalf + 2.2f
                markings.add(FlatRect(lx + s, lz + arm, 0.6f, 3f, WHITE))
                markings.add(FlatRect(lx + s, lz - arm, 0.6f, 3f, WHITE))
                markings.add(FlatRect(lx + arm, lz + s, 3f, 0.6f, WHITE))
                markings.add(FlatRect(lx - arm, lz + s, 3f, 0.6f, WHITE))
                s += 1.2f
            }
        }

        // ---- Buildings and green lots --------------------------------------------------------
        for ((ix, sx) in stretches.withIndex()) {
            for ((iz, sz) in stretches.withIndex()) {
                val x0 = sx.first + (if (sx.first <= -half) 9f else 6f)
                val x1 = sx.second - (if (sx.second >= half) 9f else 6f)
                val z0 = sz.first + (if (sz.first <= -half) 9f else 6f)
                val z1 = sz.second - (if (sz.second >= half) 9f else 6f)
                if (x1 - x0 < 12f || z1 - z0 < 12f) continue

                val central = ix in 1..2 && iz in 1..2
                val lot = if (central) 34f else 38f
                val countX = max(1, floor((x1 - x0) / lot).toInt())
                val countZ = max(1, floor((z1 - z0) / lot).toInt())
                val lotW = (x1 - x0) / countX
                val lotD = (z1 - z0) / countZ

                for (i in 0 until countX) for (j in 0 until countZ) {
                    val lcx = x0 + lotW * (i + 0.5f)
                    val lcz = z0 + lotD * (j + 0.5f)
                    val chance = if (central) 0.82f else 0.38f
                    if (rnd.nextFloat() > chance) {
                        greenLot(rnd, lcx, lcz, lotW, lotD, trees, props, colliders)
                        continue
                    }
                    val spec = makeBuilding(rnd, central, lcx, lcz, lotW, lotD)
                    buildings.add(spec)
                    val box = BoxCollider(
                        spec.cx - spec.width / 2, spec.cz - spec.depth / 2,
                        spec.cx + spec.width / 2, spec.cz + spec.depth / 2,
                    )
                    buildingBoxes.add(box)
                    colliders.add(box)
                }
            }
        }

        // ---- Roadside trees and lamp posts ----------------------------------------------------
        val verge = roadHalf + 3.2f
        val lampOffset = roadHalf + 1.2f
        var treeIndex = 0
        var lampIndex = 0
        for (line in lines) {
            for ((a, b) in stretches) {
                val lo = max(a, -half + 6f)
                val hi = min(b, half - 6f)
                var t = lo + 6f
                while (t < hi - 4f) {
                    if (!nearCrossLine(t, lines, 11f)) {
                        for (side in floatArrayOf(-1f, 1f)) {
                            val kind = if (treeIndex % 2 == 0) TreeKind.PALM else TreeKind.ROUND
                            treeIndex++
                            addTree(rnd, line + side * verge, t, kind, trees, buildingBoxes, colliders)
                            addTree(rnd, t, line + side * verge, kind, trees, buildingBoxes, colliders)
                        }
                    }
                    t += 18f
                }
                var l = lo + 14f
                while (l < hi - 8f) {
                    if (!nearCrossLine(l, lines, 11f)) {
                        val side = if (lampIndex % 2 == 0) 1f else -1f
                        lampIndex++
                        // Arm rotation: yaw theta turns local +Z into (sin theta, cos theta); the arm must reach over the road.
                        props.add(PropSpec(PropKind.LAMP, line + side * lampOffset, l, height = 7f, rotation = -side * 90f))
                        colliders.add(CircleCollider(line + side * lampOffset, l, 0.25f))
                        props.add(PropSpec(PropKind.LAMP, l, line - side * lampOffset, height = 7f, rotation = if (side > 0f) 0f else 180f))
                        colliders.add(CircleCollider(l, line - side * lampOffset, 0.25f))
                    }
                    l += 36f
                }
            }
        }

        // ---- A short chicane on the main road: something to steer around and bump into -------
        val chicane = floatArrayOf(-3f, 3f, -3f, 3f)
        for ((index, bx) in chicane.withIndex()) {
            val bz = -52f + index * 11f
            props.add(PropSpec(PropKind.BARRIER, bx, bz, sizeX = 4.2f, sizeZ = 0.9f, height = 0.9f))
            colliders.add(BoxCollider(bx - 2.1f, bz - 0.45f, bx + 2.1f, bz + 0.45f))
        }

        // ---- Boundary wall and collision --------------------------------------------------------
        pieces(-half - 2f, half + 2f, 20f) { centre, length ->
            walls.add(WallSpec(centre, half + 0.75f, length + 0.02f, 1.5f, 1.4f))
            walls.add(WallSpec(centre, -half - 0.75f, length + 0.02f, 1.5f, 1.4f))
            walls.add(WallSpec(half + 0.75f, centre, 1.5f, length + 0.02f, 1.4f))
            walls.add(WallSpec(-half - 0.75f, centre, 1.5f, length + 0.02f, 1.4f))
        }
        val thick = 40f
        colliders.add(BoxCollider(-half - thick, half, half + thick, half + thick)) // north
        colliders.add(BoxCollider(-half - thick, -half - thick, half + thick, -half)) // south
        colliders.add(BoxCollider(half, -half - thick, half + thick, half + thick)) // east
        colliders.add(BoxCollider(-half - thick, -half - thick, -half, half + thick)) // west

        // ---- Backdrop hills, well outside the wall -----------------------------------------------
        val hillCount = 30
        for (i in 0 until hillCount) {
            val angle = (2.0 * PI * i / hillCount).toFloat() + rnd.nextFloat() * 0.1f
            val distance = 330f + rnd.nextFloat() * 90f
            hills.add(
                HillSpec(
                    x = cos(angle) * distance,
                    z = sin(angle) * distance,
                    radius = 55f + rnd.nextFloat() * 55f,
                    height = 28f + rnd.nextFloat() * 52f,
                    color = HILLS[rnd.nextInt(HILLS.size)],
                ),
            )
        }

        return WorldLayout(
            halfSize = half,
            groundHalf = WorldConstants.GROUND_HALF,
            roads = roads,
            markings = markings,
            buildings = buildings,
            trees = trees,
            props = props,
            walls = walls,
            hills = hills,
            colliders = colliders,
            spawn = SpawnPoint(x = 0f, z = -90f, heading = 0f),
        )
    }

    private fun makeBuilding(rnd: Random, central: Boolean, lcx: Float, lcz: Float, lotW: Float, lotD: Float): BuildingSpec {
        val kind = if (!central) {
            BuildingKind.HOUSE
        } else {
            when (rnd.nextInt(10)) {
                0, 1, 2 -> BuildingKind.TOWER
                3, 4, 5 -> BuildingKind.SHOP
                else -> BuildingKind.HOUSE
            }
        }
        val maxW = lotW - 6f
        val maxD = lotD - 6f
        val width: Float
        val depth: Float
        val height: Float
        when (kind) {
            BuildingKind.TOWER -> {
                width = min(maxW, range(rnd, 14f, 22f)); depth = min(maxD, range(rnd, 14f, 22f)); height = range(rnd, 22f, 46f)
            }
            BuildingKind.SHOP -> {
                width = min(maxW, range(rnd, 18f, 26f)); depth = min(maxD, range(rnd, 12f, 18f)); height = range(rnd, 5f, 8f)
            }
            BuildingKind.HOUSE -> {
                val small = !central
                width = min(maxW, if (small) range(rnd, 9f, 14f) else range(rnd, 12f, 20f))
                depth = min(maxD, if (small) range(rnd, 9f, 13f) else range(rnd, 12f, 18f))
                height = if (small) range(rnd, 4f, 6.5f) else range(rnd, 6f, 10f)
            }
        }
        val jitterX = (lotW - width - 4f).coerceAtLeast(0f) * 0.5f * (rnd.nextFloat() * 2f - 1f) * 0.6f
        val jitterZ = (lotD - depth - 4f).coerceAtLeast(0f) * 0.5f * (rnd.nextFloat() * 2f - 1f) * 0.6f
        return BuildingSpec(
            cx = lcx + jitterX,
            cz = lcz + jitterZ,
            width = width,
            depth = depth,
            height = height,
            wall = WALLS[rnd.nextInt(WALLS.size)],
            roof = ROOFS[rnd.nextInt(ROOFS.size)],
            kind = kind,
        )
    }

    private fun greenLot(
        rnd: Random, lcx: Float, lcz: Float, lotW: Float, lotD: Float,
        trees: MutableList<TreeSpec>, props: MutableList<PropSpec>, colliders: ColliderGrid,
    ) {
        val count = 2 + rnd.nextInt(5)
        repeat(count) {
            val x = lcx + (rnd.nextFloat() - 0.5f) * (lotW - 6f)
            val z = lcz + (rnd.nextFloat() - 0.5f) * (lotD - 6f)
            val kind = if (rnd.nextFloat() < 0.5f) TreeKind.PALM else TreeKind.ROUND
            trees.add(newTree(rnd, x, z, kind))
            colliders.add(CircleCollider(x, z, 0.45f))
        }
        if (rnd.nextFloat() < 0.25f) {
            val x = lcx + (rnd.nextFloat() - 0.5f) * (lotW - 8f)
            val z = lcz + (rnd.nextFloat() - 0.5f) * (lotD - 8f)
            val size = range(rnd, 1.4f, 2.6f)
            props.add(PropSpec(PropKind.BOULDER, x, z, sizeX = size, sizeZ = size * 0.9f, height = size * 0.7f, rotation = rnd.nextFloat() * 360f))
            colliders.add(CircleCollider(x, z, size * 0.5f))
        }
    }

    private fun addTree(
        rnd: Random, x: Float, z: Float, kind: TreeKind,
        trees: MutableList<TreeSpec>, buildingBoxes: List<BoxCollider>, colliders: ColliderGrid,
    ) {
        if (abs(x) > WorldConstants.HALF - 5f || abs(z) > WorldConstants.HALF - 5f) return
        if (buildingBoxes.any { x > it.minX - 2.5f && x < it.maxX + 2.5f && z > it.minZ - 2.5f && z < it.maxZ + 2.5f }) return
        trees.add(newTree(rnd, x, z, kind))
        colliders.add(CircleCollider(x, z, 0.45f))
    }

    private fun newTree(rnd: Random, x: Float, z: Float, kind: TreeKind): TreeSpec =
        TreeSpec(
            x = x,
            z = z,
            height = if (kind == TreeKind.PALM) range(rnd, 7f, 11f) else range(rnd, 5f, 8f),
            kind = kind,
            foliage = FOLIAGE[rnd.nextInt(FOLIAGE.size)],
            rotation = rnd.nextFloat() * 360f,
        )

    private fun nearCrossLine(value: Float, lines: FloatArray, margin: Float): Boolean =
        lines.any { abs(value - it) < margin }

    private fun range(rnd: Random, min: Float, max: Float): Float = min + rnd.nextFloat() * (max - min)

    /** Splits [a, b] into equal pieces no longer than [maxLength] and reports each piece's centre and length. */
    private inline fun pieces(a: Float, b: Float, maxLength: Float, action: (centre: Float, length: Float) -> Unit) {
        val count = max(1, ceil((b - a) / maxLength).toInt())
        val step = (b - a) / count
        for (i in 0 until count) {
            action(a + step * (i + 0.5f), step)
        }
    }
}
