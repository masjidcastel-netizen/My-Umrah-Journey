package com.masjidcastel.myumrahjourney.game.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.model.Node
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Disposable
import com.masjidcastel.myumrahjourney.game.sim.VehicleSpec
import com.masjidcastel.myumrahjourney.game.sim.VehicleState

/**
 * The player's car: a body node plus four separate wheel nodes. The wheels spin with distance
 * travelled and the front pair steers, so the motion is visible from every camera.
 * Built from primitives in code (original design, no external assets).
 */
class CarModel : Disposable {
    private val model: Model
    val instance: ModelInstance

    private val wheels: List<Wheel>
    private var spin = 0f

    private val steerRotation = Quaternion()
    private val spinRotation = Quaternion()

    private class Wheel(val node: Node, val steers: Boolean)

    init {
        val builder = ModelBuilder()
        val material = Material("car", ColorAttribute.createDiffuse(Color.WHITE))
        builder.begin()

        // ---- Body (one node, one mesh part) ----
        builder.node().id = "body"
        buildBody(builder.part("body", GL20.GL_TRIANGLES, MeshKit.ATTRIBUTES, material))

        // ---- Wheels (one node each, so they can rotate independently) ----
        val wheelSpecs = listOf(
            Triple("wheel_front_left", 1f, 1f),
            Triple("wheel_front_right", -1f, 1f),
            Triple("wheel_rear_left", 1f, -1f),
            Triple("wheel_rear_right", -1f, -1f),
        )
        for ((id, side, axle) in wheelSpecs) {
            val node = builder.node()
            node.id = id
            node.translation.set(side * VehicleSpec.TRACK_HALF, VehicleSpec.WHEEL_RADIUS, axle * VehicleSpec.AXLE_OFFSET)
            buildWheel(builder.part(id, GL20.GL_TRIANGLES, MeshKit.ATTRIBUTES, material))
        }

        model = builder.end()
        instance = ModelInstance(model)
        wheels = wheelSpecs.map { (id, _, axle) -> Wheel(instance.getNode(id), steers = axle > 0f) }
    }

    /** Places the car in the world and animates wheel spin and steering. */
    fun update(state: VehicleState, dt: Float) {
        spin = (spin + state.speed / VehicleSpec.WHEEL_RADIUS * dt) % MathUtils.PI2
        val spinDegrees = spin * MathUtils.radiansToDegrees
        val steerDegrees = state.steerAngle * MathUtils.radiansToDegrees

        for (wheel in wheels) {
            spinRotation.setFromAxis(Vector3.X, spinDegrees)
            if (wheel.steers) {
                steerRotation.setFromAxis(Vector3.Y, steerDegrees)
                wheel.node.rotation.set(steerRotation).mul(spinRotation)
            } else {
                wheel.node.rotation.set(spinRotation)
            }
        }
        instance.calculateTransforms()
        instance.transform.setToTranslation(state.x, 0f, state.z)
            .rotate(Vector3.Y, state.heading * MathUtils.radiansToDegrees)
    }

    private fun buildBody(p: MeshPartBuilder) {
        p.resetState()
        val paint = floatArrayOf(0.08f, 0.70f, 0.64f) // turquoise paint
        val glass = floatArrayOf(0.08f, 0.17f, 0.21f)
        val dark = floatArrayOf(0.07f, 0.08f, 0.09f)

        // Under-body so no daylight shows between the wheels.
        p.paint(dark[0], dark[1], dark[2])
        p.box(0f, 0.30f, 0f, 1.70f, 0.16f, 4.20f)

        // Lower body.
        p.paint(paint[0], paint[1], paint[2])
        p.box(0f, 0.58f, 0f, 1.88f, 0.52f, 4.50f)

        // Bonnet and boot slope down towards the ends.
        p.taperedBox(0.84f, 0.99f, -0.92f, 0.92f, 0.95f, 2.22f, -0.84f, 0.84f, 1.05f, 2.00f)
        p.taperedBox(0.84f, 0.97f, -0.92f, 0.92f, -2.22f, -1.15f, -0.84f, 0.84f, -2.00f, -1.20f)

        // Greenhouse: glass, with a body-coloured roof panel on top.
        p.paint(glass[0], glass[1], glass[2])
        p.taperedBox(0.84f, 1.42f, -0.85f, 0.85f, -1.20f, 1.05f, -0.70f, 0.70f, -0.88f, 0.38f)
        p.paint(paint[0], paint[1], paint[2])
        p.box(0f, 1.44f, -0.25f, 1.44f, 0.07f, 1.30f)
        // Pillars.
        for (side in floatArrayOf(-1f, 1f)) {
            p.box(side * 0.80f, 1.12f, -1.02f, 0.07f, 0.58f, 0.18f)
        }

        // Bumpers and grille.
        p.paint(dark[0], dark[1], dark[2])
        p.box(0f, 0.40f, 2.27f, 1.92f, 0.22f, 0.16f)
        p.box(0f, 0.40f, -2.27f, 1.92f, 0.22f, 0.16f)
        p.box(0f, 0.66f, 2.27f, 1.00f, 0.16f, 0.06f)

        // Lights.
        p.paint(1.0f, 0.96f, 0.78f)
        for (side in floatArrayOf(-1f, 1f)) p.box(side * 0.64f, 0.74f, 2.28f, 0.40f, 0.14f, 0.07f)
        p.paint(0.80f, 0.05f, 0.05f)
        for (side in floatArrayOf(-1f, 1f)) p.box(side * 0.68f, 0.76f, -2.28f, 0.42f, 0.14f, 0.07f)

        // Mirrors.
        p.paint(dark[0], dark[1], dark[2])
        for (side in floatArrayOf(-1f, 1f)) p.box(side * 1.02f, 1.02f, 0.72f, 0.16f, 0.11f, 0.12f)
    }

    private fun buildWheel(p: MeshPartBuilder) {
        p.resetState()
        val radius = VehicleSpec.WHEEL_RADIUS
        val width = VehicleSpec.WHEEL_WIDTH

        // Cylinders are built along Y, so turn them to lie along the axle (X).
        val onSide = Matrix4().setToRotation(Vector3.Z, 90f)
        p.setVertexTransform(onSide)
        p.paint(0.04f, 0.04f, 0.05f)
        p.cylinder(radius * 2f, width, radius * 2f, 18) // tyre
        p.paint(0.74f, 0.77f, 0.80f)
        p.cylinder(radius * 1.25f, width + 0.03f, radius * 1.25f, 14) // rim
        p.setVertexTransform(null)

        // Spokes: without them a spinning symmetric wheel would look static.
        p.paint(0.30f, 0.32f, 0.35f)
        p.box(0f, 0f, 0f, width + 0.05f, radius * 1.1f, 0.06f)
        p.box(0f, 0f, 0f, width + 0.05f, 0.06f, radius * 1.1f)
    }

    override fun dispose() {
        model.dispose()
    }
}
