package com.masjidcastel.myumrahjourney.game.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.utils.Disposable
import com.masjidcastel.myumrahjourney.game.world.BuildingKind
import com.masjidcastel.myumrahjourney.game.world.BuildingSpec
import com.masjidcastel.myumrahjourney.game.world.PropKind
import com.masjidcastel.myumrahjourney.game.world.PropSpec
import com.masjidcastel.myumrahjourney.game.world.Rgb
import com.masjidcastel.myumrahjourney.game.world.TreeKind
import com.masjidcastel.myumrahjourney.game.world.TreeSpec
import com.masjidcastel.myumrahjourney.game.world.WorldConstants
import com.masjidcastel.myumrahjourney.game.world.WorldLayout
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

private typealias Op = (MeshPartBuilder) -> Unit

/** One renderable piece of the world with a bounding sphere for frustum and distance culling. */
class WorldChunk(val instance: ModelInstance, val centre: Vector3, val radius: Float)

/** The 3D meshes of the whole prototype level. Built once from a [WorldLayout]. */
class WorldScene private constructor(
    /** Ground, roads, buildings, trees, props. Culled by quality-dependent draw distance. */
    val chunks: List<WorldChunk>,
    /** Backdrop hills beyond the boundary wall. Always drawn when in view. */
    val hills: List<WorldChunk>,
    private val models: List<Model>,
) : Disposable {

    override fun dispose() {
        models.forEach { it.dispose() }
    }

    companion object {
        private const val CHUNK_SIZE = 80f
        private const val OPS_PER_PART = 100
        private const val GROUND_TILE = 40f

        private enum class Surface { GROUND, ROAD, FACADE, PLAIN }

        private class ChunkOps {
            val ops: Map<Surface, ArrayList<Op>> = Surface.entries.associateWith { ArrayList<Op>() }
        }

        fun build(layout: WorldLayout, textures: ProceduralTextures): WorldScene {
            val materials: Map<Surface, Material> = mapOf(
                Surface.GROUND to Material("ground", ColorAttribute.createDiffuse(Color.WHITE), TextureAttribute.createDiffuse(textures.grass)),
                Surface.ROAD to Material("road", ColorAttribute.createDiffuse(Color.WHITE), TextureAttribute.createDiffuse(textures.asphalt)),
                Surface.FACADE to Material("facade", ColorAttribute.createDiffuse(Color.WHITE), TextureAttribute.createDiffuse(textures.facade)),
                Surface.PLAIN to Material("plain", ColorAttribute.createDiffuse(Color.WHITE)),
            )

            val chunkOps = HashMap<Long, ChunkOps>()
            fun cellOf(v: Float): Int = floor(v / CHUNK_SIZE).toInt()
            fun at(x: Float, z: Float): ChunkOps =
                chunkOps.getOrPut((cellOf(x).toLong() shl 32) xor (cellOf(z).toLong() and 0xffffffffL)) { ChunkOps() }

            // ---- Ground: tiles of 40 m so per-vertex fog and lighting stay accurate ----------------
            val tiles = (2 * layout.groundHalf / GROUND_TILE).toInt()
            for (i in 0 until tiles) for (j in 0 until tiles) {
                val x = -layout.groundHalf + GROUND_TILE * (i + 0.5f)
                val z = -layout.groundHalf + GROUND_TILE * (j + 0.5f)
                at(x, z).ops.getValue(Surface.GROUND).add { p ->
                    p.resetState()
                    p.paint(1f, 1f, 1f)
                    p.setUVRange(0f, 0f, GROUND_TILE / 8f, GROUND_TILE / 8f)
                    p.box(x, -0.15f, z, GROUND_TILE + 0.02f, 0.3f, GROUND_TILE + 0.02f)
                }
            }

            // ---- Roads and markings -------------------------------------------------------------------
            for (r in layout.roads) {
                at(r.cx, r.cz).ops.getValue(Surface.ROAD).add { p ->
                    p.resetState()
                    p.paint(1f, 1f, 1f)
                    p.setUVRange(0f, 0f, r.sizeX / 7f, r.sizeZ / 7f)
                    p.box(r.cx, 0f, r.cz, r.sizeX, 0.12f, r.sizeZ)
                }
            }
            for (m in layout.markings) {
                at(m.cx, m.cz).ops.getValue(Surface.PLAIN).add { p ->
                    p.resetState()
                    p.paint(m.color)
                    p.box(m.cx, 0.05f, m.cz, m.sizeX, 0.1f, m.sizeZ)
                }
            }

            // ---- Buildings ----------------------------------------------------------------------------
            for (b in layout.buildings) {
                val c = at(b.cx, b.cz)
                c.ops.getValue(Surface.FACADE).add { p ->
                    p.resetState()
                    p.paint(b.wall)
                    p.setUVRange(0f, 0f, (b.width + b.depth) / 2f / 12f, b.height / 12f)
                    p.box(b.cx, b.height / 2f, b.cz, b.width, b.height, b.depth)
                }
                c.ops.getValue(Surface.PLAIN).add { p -> p.resetState(); addRoof(p, b) }
            }

            // ---- Trees, props, walls ---------------------------------------------------------------------
            for (t in layout.trees) at(t.x, t.z).ops.getValue(Surface.PLAIN).add { p -> p.resetState(); addTree(p, t) }
            for (prop in layout.props) at(prop.x, prop.z).ops.getValue(Surface.PLAIN).add { p -> p.resetState(); addProp(p, prop) }
            for (w in layout.walls) {
                at(w.cx, w.cz).ops.getValue(Surface.PLAIN).add { p ->
                    p.resetState()
                    p.paint(0.78f, 0.75f, 0.68f)
                    p.box(w.cx, w.height / 2f, w.cz, w.sizeX, w.height, w.sizeZ)
                }
            }

            val models = ArrayList<Model>()
            val chunks = ArrayList<WorldChunk>()
            val builder = ModelBuilder()

            for (ops in chunkOps.values) {
                builder.begin()
                builder.node().id = "chunk"
                for (surface in Surface.entries) {
                    val list = ops.ops.getValue(surface)
                    list.chunked(OPS_PER_PART).forEachIndexed { index, group ->
                        val part = builder.part("${surface.name}_$index", GL20.GL_TRIANGLES, MeshKit.ATTRIBUTES, materials.getValue(surface))
                        group.forEach { it(part) }
                    }
                }
                val model = builder.end()
                models.add(model)
                chunks.add(chunkFor(model))
            }

            // ---- Backdrop hills: one small model each so they can be culled individually -----------------
            val hills = ArrayList<WorldChunk>()
            for (h in layout.hills) {
                builder.begin()
                builder.node().id = "hill"
                val part = builder.part("hill", GL20.GL_TRIANGLES, MeshKit.ATTRIBUTES, materials.getValue(Surface.PLAIN))
                part.resetState()
                part.paint(h.color)
                part.sphereAt(h.x, 0f, h.z, h.radius * 2f, h.height * 2f, h.radius * 2f, 14, 8)
                val model = builder.end()
                models.add(model)
                hills.add(chunkFor(model))
            }

            return WorldScene(chunks, hills, models)
        }

        private fun chunkFor(model: Model): WorldChunk {
            val box = BoundingBox()
            model.calculateBoundingBox(box)
            val centre = box.getCenter(Vector3())
            val radius = box.getDimensions(Vector3()).len() / 2f
            return WorldChunk(ModelInstance(model), centre, radius)
        }

        private fun addRoof(p: MeshPartBuilder, b: BuildingSpec) {
            p.paint(b.roof)
            when (b.kind) {
                BuildingKind.HOUSE -> {
                    // Hipped roof: wide at the eaves, a short ridge along the longer side.
                    val rise = min(b.width, b.depth) * 0.28f
                    val x0 = b.cx - b.width / 2f - 0.3f
                    val x1 = b.cx + b.width / 2f + 0.3f
                    val z0 = b.cz - b.depth / 2f - 0.3f
                    val z1 = b.cz + b.depth / 2f + 0.3f
                    if (b.width >= b.depth) {
                        p.taperedBox(b.height, b.height + rise, x0, x1, z0, z1, x0 + 2f, x1 - 2f, b.cz - 0.2f, b.cz + 0.2f)
                    } else {
                        p.taperedBox(b.height, b.height + rise, x0, x1, z0, z1, b.cx - 0.2f, b.cx + 0.2f, z0 + 2f, z1 - 2f)
                    }
                    p.paint(0.55f, 0.52f, 0.50f)
                    p.box(b.cx + b.width * 0.25f, b.height + rise * 0.6f, b.cz, 0.9f, rise * 1.1f, 0.9f) // chimney
                }
                BuildingKind.SHOP -> {
                    p.box(b.cx, b.height + 0.2f, b.cz, b.width + 0.4f, 0.4f, b.depth + 0.4f)
                    // Awning over the front (+Z) pavement.
                    p.paint(0.10f, 0.62f, 0.58f)
                    p.box(b.cx, b.height * 0.55f, b.cz + b.depth / 2f + 1.1f, b.width * 0.9f, 0.25f, 2.2f)
                }
                BuildingKind.TOWER -> {
                    p.box(b.cx, b.height + 0.2f, b.cz, b.width + 0.4f, 0.4f, b.depth + 0.4f)
                    p.paint(0.60f, 0.64f, 0.66f)
                    p.box(b.cx, b.height + 1.9f, b.cz, b.width * 0.55f, 3f, b.depth * 0.55f)
                    p.paint(0.35f, 0.38f, 0.40f)
                    p.cylinderAt(b.cx, b.height + 6.4f, b.cz, 0.3f, 6f, 0.3f, 6)
                }
            }
        }

        private fun addTree(p: MeshPartBuilder, t: TreeSpec) {
            when (t.kind) {
                TreeKind.ROUND -> {
                    val trunk = t.height * 0.45f
                    p.paint(0.42f, 0.29f, 0.17f)
                    p.cylinderAt(t.x, trunk / 2f, t.z, 0.5f, trunk, 0.5f, 6)
                    p.paint(t.foliage)
                    val crown = t.height * 0.62f
                    p.sphereAt(t.x, trunk + crown * 0.35f, t.z, crown * 0.95f, crown, crown * 0.95f, 8, 6)
                }
                TreeKind.PALM -> {
                    p.paint(0.55f, 0.43f, 0.28f)
                    p.cylinderAt(t.x, t.height / 2f, t.z, 0.45f, t.height, 0.45f, 6)
                    p.paint(t.foliage)
                    val fronds = 7
                    val frame = Matrix4()
                    for (i in 0 until fronds) {
                        val yaw = t.rotation + i * 360f / fronds
                        // Turn to face outwards, tilt about X so the frond droops, then lay it along +Z.
                        frame.setToTranslation(t.x, t.height, t.z).rotate(Vector3.Y, yaw).rotate(Vector3.X, 24f)
                        p.setVertexTransform(frame)
                        p.box(0f, 0f, 2.1f, 0.9f, 0.08f, 4.2f)
                        p.setVertexTransform(null)
                    }
                    p.paint(0.38f, 0.28f, 0.12f)
                    p.sphereAt(t.x, t.height - 0.3f, t.z, 0.7f, 0.7f, 0.7f, 6, 4) // coconut cluster
                }
            }
        }

        private fun addProp(p: MeshPartBuilder, prop: PropSpec) {
            when (prop.kind) {
                PropKind.LAMP -> {
                    val h = prop.height
                    p.paint(0.29f, 0.33f, 0.38f)
                    p.cylinderAt(prop.x, h / 2f, prop.z, 0.22f, h, 0.22f, 6)
                    p.inFrame(prop.x, 0f, prop.z, prop.rotation) {
                        paint(0.29f, 0.33f, 0.38f)
                        box(0f, h, 0.8f, 0.15f, 0.15f, 1.6f) // arm reaching over the road
                        paint(0.98f, 0.95f, 0.78f)
                        box(0f, h - 0.12f, 1.55f, 0.5f, 0.18f, 0.9f) // lamp head
                    }
                }
                PropKind.BARRIER -> {
                    p.paint(0.72f, 0.72f, 0.69f)
                    p.box(prop.x, prop.height / 2f, prop.z, prop.sizeX, prop.height, prop.sizeZ)
                    p.paint(0.92f, 0.45f, 0.10f)
                    p.box(prop.x - prop.sizeX * 0.28f, prop.height + 0.02f, prop.z, prop.sizeX * 0.16f, 0.06f, prop.sizeZ * 1.02f)
                    p.box(prop.x + prop.sizeX * 0.28f, prop.height + 0.02f, prop.z, prop.sizeX * 0.16f, 0.06f, prop.sizeZ * 1.02f)
                }
                PropKind.BOULDER -> {
                    p.paint(0.48f, 0.50f, 0.49f)
                    p.inFrame(prop.x, 0f, prop.z, prop.rotation) {
                        sphere(prop.sizeX, max(prop.height * 2f, 0.1f), prop.sizeZ, 8, 6)
                    }
                }
            }
        }
    }
}
