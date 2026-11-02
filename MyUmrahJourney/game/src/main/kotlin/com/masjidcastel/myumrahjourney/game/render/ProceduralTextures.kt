package com.masjidcastel.myumrahjourney.game.render

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.glutils.PixmapTextureData
import com.badlogic.gdx.utils.Disposable
import java.util.Random

/**
 * Tileable textures generated in code, so the prototype ships no binary art assets.
 * The pixmaps are kept alive and the textures are "managed", so libGDX can re-upload them if
 * Android destroys the OpenGL context (for example when the app is sent to the background).
 */
class ProceduralTextures : Disposable {
    private val pixmaps = ArrayList<Pixmap>()

    val grass: Texture = make(64, seed = 11) { pm, rnd ->
        for (y in 0 until 64) for (x in 0 until 64) {
            val n = (rnd.nextFloat() - 0.5f) * 0.12f
            val patch = if (((x / 8) + (y / 8)) % 3 == 0) 0.03f else 0f
            pm.setColor(0.30f + n + patch, 0.52f + n + patch, 0.24f + n * 0.6f, 1f)
            pm.drawPixel(x, y)
        }
        repeat(70) {
            pm.setColor(0.42f, 0.66f, 0.30f, 1f)
            pm.drawPixel(rnd.nextInt(64), rnd.nextInt(64))
        }
    }

    val asphalt: Texture = make(64, seed = 23) { pm, rnd ->
        for (y in 0 until 64) for (x in 0 until 64) {
            val g = 0.23f + (rnd.nextFloat() - 0.5f) * 0.07f
            pm.setColor(g, g, g * 1.03f, 1f)
            pm.drawPixel(x, y)
        }
        repeat(40) {
            pm.setColor(0.36f, 0.36f, 0.37f, 1f)
            pm.drawPixel(rnd.nextInt(64), rnd.nextInt(64))
        }
    }

    /** Pale wall with a 4x4 grid of windows. Tinted per building through vertex colours. */
    val facade: Texture = make(64, seed = 37) { pm, rnd ->
        pm.setColor(0.96f, 0.96f, 0.95f, 1f)
        pm.fill()
        for (row in 0 until 4) for (col in 0 until 4) {
            val x = col * 16 + 3
            val y = row * 16 + 3
            pm.setColor(0.28f, 0.40f, 0.50f, 1f)
            pm.fillRectangle(x, y, 10, 10)
            pm.setColor(0.55f, 0.70f, 0.78f, 1f)
            pm.fillRectangle(x + 1, y + 1, 4, 3) // glint
            pm.setColor(0.82f, 0.82f, 0.80f, 1f)
            pm.drawRectangle(x - 1, y - 1, 12, 12) // sill / frame
            if (rnd.nextInt(5) == 0) {
                pm.setColor(0.95f, 0.88f, 0.62f, 1f) // a lit window
                pm.fillRectangle(x + 2, y + 2, 6, 6)
            }
        }
    }

    private fun make(size: Int, seed: Long, paint: (Pixmap, Random) -> Unit): Texture {
        val pixmap = Pixmap(size, size, Pixmap.Format.RGBA8888)
        paint(pixmap, Random(seed))
        pixmaps.add(pixmap)
        val data = PixmapTextureData(pixmap, Pixmap.Format.RGBA8888, true, false, true)
        return Texture(data).apply {
            setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear)
            setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat)
        }
    }

    override fun dispose() {
        grass.dispose()
        asphalt.dispose()
        facade.dispose()
        pixmaps.forEach { it.dispose() }
        pixmaps.clear()
    }
}
