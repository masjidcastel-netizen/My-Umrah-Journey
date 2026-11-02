package com.masjidcastel.myumrahjourney.game.render

import com.badlogic.gdx.graphics.VertexAttributes.Usage
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.masjidcastel.myumrahjourney.game.world.Rgb

/**
 * Small helpers on top of libGDX's [MeshPartBuilder]. Every mesh in the game uses the same
 * vertex layout: position, normal, per-vertex colour and one texture coordinate set. Colour
 * comes from vertex colours (multiplied with an optional texture), which lets hundreds of
 * differently coloured buildings and trees share a few meshes and draw calls.
 */
internal object MeshKit {
    val ATTRIBUTES: Long =
        (Usage.Position or Usage.Normal or Usage.ColorUnpacked or Usage.TextureCoordinates).toLong()

    private val scratch = Matrix4()
}

internal fun MeshPartBuilder.paint(color: Rgb, shade: Float = 1f) {
    setColor(color.r * shade, color.g * shade, color.b * shade, 1f)
}

internal fun MeshPartBuilder.paint(r: Float, g: Float, b: Float) {
    setColor(r, g, b, 1f)
}

/** Restores the builder state that earlier shapes may have changed (no transform, full 0..1 UVs). */
internal fun MeshPartBuilder.resetState() {
    setVertexTransform(null)
    setUVRange(0f, 0f, 1f, 1f)
}

/** Vertical cylinder (axis = Y) centred at ([x], [y], [z]). */
internal fun MeshPartBuilder.cylinderAt(x: Float, y: Float, z: Float, width: Float, height: Float, depth: Float, divisions: Int) {
    setVertexTransform(Matrix4().setToTranslation(x, y, z))
    cylinder(width, height, depth, divisions)
    setVertexTransform(null)
}

/** Ellipsoid of the given overall size, centred at ([x], [y], [z]). */
internal fun MeshPartBuilder.sphereAt(x: Float, y: Float, z: Float, width: Float, height: Float, depth: Float, divU: Int, divV: Int) {
    setVertexTransform(Matrix4().setToTranslation(x, y, z))
    sphere(width, height, depth, divU, divV)
    setVertexTransform(null)
}

/** Box centred at the local origin of a frame placed at ([x], [y], [z]) and turned [yawDegrees] about Y. */
internal inline fun MeshPartBuilder.inFrame(x: Float, y: Float, z: Float, yawDegrees: Float, block: MeshPartBuilder.() -> Unit) {
    setVertexTransform(Matrix4().setToTranslation(x, y, z).rotate(Vector3.Y, yawDegrees))
    block()
    setVertexTransform(null)
}

/**
 * Box whose top rectangle differs from its bottom one (car bonnet, windscreen, hip roofs).
 * Bottom: x [bx0, bx1], z [bz0, bz1] at [y0]. Top: x [tx0, tx1], z [tz0, tz1] at [y1].
 * Corner names follow libGDX: cornerXYZ, 0 = minimum, 1 = maximum.
 */
internal fun MeshPartBuilder.taperedBox(
    y0: Float, y1: Float,
    bx0: Float, bx1: Float, bz0: Float, bz1: Float,
    tx0: Float, tx1: Float, tz0: Float, tz1: Float,
) {
    box(
        Vector3(bx0, y0, bz0), Vector3(tx0, y1, tz0), Vector3(bx1, y0, bz0), Vector3(tx1, y1, tz0),
        Vector3(bx0, y0, bz1), Vector3(tx0, y1, tz1), Vector3(bx1, y0, bz1), Vector3(tx1, y1, tz1),
    )
}
