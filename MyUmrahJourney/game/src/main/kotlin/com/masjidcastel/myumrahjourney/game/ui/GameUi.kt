package com.masjidcastel.myumrahjourney.game.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.Disposable
import com.masjidcastel.myumrahjourney.core.player.GraphicsQuality
import com.masjidcastel.myumrahjourney.game.render.CameraMode
import com.masjidcastel.myumrahjourney.game.sim.VehicleInput
import kotlin.math.min

/** What the player asked for this frame. */
class UiActions {
    var pauseToggled = false
    var cameraCycled = false
    var resume = false
    var cycleQuality = false
    var exit = false

    fun clear() {
        pauseToggled = false; cameraCycled = false; resume = false; cycleQuality = false; exit = false
    }
}

/** Snapshot of everything the HUD shows. */
class HudData {
    var speedKmh = 0f
    var maxSpeedKmh = 144f
    var cameraMode = CameraMode.CHASE
    var impactKmh = 0f
    var impactAge = 99f
    var collisions = 0
    var flash = 0f
    var fps = 0
    var quality = GraphicsQuality.MEDIUM
}

private class Button(val id: String) {
    var cx = 0f
    var cy = 0f
    var radius = 0f
    var pressed = false
    var wasPressed = false
    val justPressed: Boolean get() = pressed && !wasPressed

    fun contains(x: Float, y: Float, slop: Float = 1.15f): Boolean {
        val dx = x - cx
        val dy = y - cy
        return dx * dx + dy * dy <= (radius * slop) * (radius * slop)
    }
}

private class MenuItem(val id: String) {
    var x = 0f
    var y = 0f
    var w = 0f
    var h = 0f
    var pressed = false
    var wasPressed = false
    val justPressed: Boolean get() = pressed && !wasPressed

    fun contains(px: Float, py: Float) = px >= x && px <= x + w && py >= y && py <= y + h
}

/**
 * On-screen controls, HUD and pause menu, drawn with libGDX's ShapeRenderer and SpriteBatch
 * (no Android views). Touch input is polled every frame, so several fingers can steer and
 * press a pedal at the same time.
 *
 * Text uses libGDX's built-in font, which only has basic Latin glyphs. The game screens
 * therefore show English labels for now; localised text and Arabic shaping arrive with the
 * dedicated language/audio stage.
 */
class GameUi : Disposable {
    private val camera = OrthographicCamera()
    private val shapes = ShapeRenderer()
    private val batch = SpriteBatch()
    private val font = BitmapFont()
    private val layout = GlyphLayout()

    private var width = 1f
    private var height = 1f
    private var dp = 1f

    private val left = Button("left")
    private val right = Button("right")
    private val accelerator = Button("accelerate")
    private val brake = Button("brake")
    private val pause = Button("pause")
    private val cameraButton = Button("camera")
    private val buttons = listOf(left, right, accelerator, brake, pause, cameraButton)

    private val resumeItem = MenuItem("resume")
    private val qualityItem = MenuItem("quality")
    private val exitItem = MenuItem("exit")
    private val menuItems = listOf(resumeItem, qualityItem, exitItem)

    fun resize(w: Int, h: Int) {
        width = w.toFloat()
        height = h.toFloat()
        dp = Gdx.graphics.density.coerceAtLeast(0.75f)
        camera.setToOrtho(false, width, height)
        camera.update()
        font.data.setScale(dp * 1.15f)

        val margin = 20f * dp
        left.radius = 44f * dp
        right.radius = 44f * dp
        brake.radius = 44f * dp
        accelerator.radius = 54f * dp
        pause.radius = 26f * dp
        cameraButton.radius = 26f * dp

        left.cx = margin + left.radius; left.cy = margin + left.radius
        right.cx = left.cx + left.radius + 18f * dp + right.radius; right.cy = left.cy
        accelerator.cx = width - margin - accelerator.radius; accelerator.cy = margin + accelerator.radius
        brake.cx = accelerator.cx - accelerator.radius - 14f * dp - brake.radius; brake.cy = left.cy
        pause.cx = width - margin - pause.radius; pause.cy = height - margin - pause.radius
        cameraButton.cx = pause.cx - pause.radius - 14f * dp - cameraButton.radius; cameraButton.cy = pause.cy

        val menuW = min(width * 0.7f, 300f * dp)
        val menuH = 56f * dp
        val gap = 14f * dp
        val top = height / 2f + (menuH * 3 + gap * 2) / 2f - 10f * dp
        menuItems.forEachIndexed { index, item ->
            item.w = menuW
            item.h = menuH
            item.x = (width - menuW) / 2f
            item.y = top - menuH - index * (menuH + gap)
        }
    }

    /** Reads touches and keys. Returns driving input while playing; fills [actions] either way. */
    fun poll(paused: Boolean, actions: UiActions): VehicleInput {
        buttons.forEach { it.wasPressed = it.pressed; it.pressed = false }
        menuItems.forEach { it.wasPressed = it.pressed; it.pressed = false }

        for (pointer in 0 until MAX_POINTERS) {
            if (!Gdx.input.isTouched(pointer)) continue
            val x = Gdx.input.getX(pointer).toFloat()
            val y = height - Gdx.input.getY(pointer).toFloat()
            if (paused) {
                menuItems.forEach { if (it.contains(x, y)) it.pressed = true }
            } else {
                buttons.forEach { if (it.contains(x, y)) it.pressed = true }
            }
        }

        actions.clear()
        if (paused) {
            actions.resume = resumeItem.justPressed || Gdx.input.isKeyJustPressed(Input.Keys.P)
            actions.cycleQuality = qualityItem.justPressed || Gdx.input.isKeyJustPressed(Input.Keys.Q)
            actions.exit = exitItem.justPressed || Gdx.input.isKeyJustPressed(Input.Keys.BACK) ||
                Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
            return VehicleInput()
        }

        actions.pauseToggled = pause.justPressed || Gdx.input.isKeyJustPressed(Input.Keys.P) ||
            Gdx.input.isKeyJustPressed(Input.Keys.BACK) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
        actions.cameraCycled = cameraButton.justPressed || Gdx.input.isKeyJustPressed(Input.Keys.C)

        val steerLeft = left.pressed || Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)
        val steerRight = right.pressed || Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)
        val gas = accelerator.pressed || Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W)
        val stop = brake.pressed || Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S)

        val steer = (if (steerLeft) 1f else 0f) - (if (steerRight) 1f else 0f)
        return VehicleInput(steer = steer, throttle = if (gas) 1f else 0f, brake = if (stop) 1f else 0f)
    }

    fun render(data: HudData, paused: Boolean) {
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST)
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        shapes.projectionMatrix = camera.combined
        batch.projectionMatrix = camera.combined

        if (data.flash > 0.01f) {
            shapes.begin(ShapeRenderer.ShapeType.Filled)
            shapes.setColor(0.85f, 0.1f, 0.1f, min(0.4f, data.flash * 0.4f))
            shapes.rect(0f, 0f, width, height)
            shapes.end()
        }

        if (!paused) {
            drawControls(data)
        } else {
            drawPauseMenu(data)
        }

        Gdx.gl.glDisable(GL20.GL_BLEND)
    }

    private fun drawControls(data: HudData) {
        shapes.begin(ShapeRenderer.ShapeType.Filled)

        // Button discs.
        for (b in buttons) {
            val base = when (b) {
                accelerator -> Color(0.10f, 0.75f, 0.45f, 1f)
                brake -> Color(0.85f, 0.25f, 0.25f, 1f)
                else -> Color(0.10f, 0.70f, 0.66f, 1f)
            }
            shapes.setColor(base.r, base.g, base.b, if (b.pressed) 0.75f else 0.38f)
            shapes.circle(b.cx, b.cy, b.radius, 40)
        }

        // Icons.
        shapes.setColor(1f, 1f, 1f, 0.95f)
        val s = left.radius * 0.45f
        shapes.triangle(left.cx - s, left.cy, left.cx + s * 0.7f, left.cy + s, left.cx + s * 0.7f, left.cy - s)
        shapes.triangle(right.cx + s, right.cy, right.cx - s * 0.7f, right.cy + s, right.cx - s * 0.7f, right.cy - s)
        val a = accelerator.radius * 0.5f
        shapes.triangle(accelerator.cx, accelerator.cy + a, accelerator.cx - a, accelerator.cy - a * 0.2f, accelerator.cx + a, accelerator.cy - a * 0.2f)
        shapes.rect(accelerator.cx - a * 0.35f, accelerator.cy - a * 0.9f, a * 0.7f, a * 0.8f)
        val q = brake.radius * 0.38f
        shapes.rect(brake.cx - q, brake.cy - q, q * 2f, q * 2f)
        val bar = pause.radius * 0.28f
        shapes.rect(pause.cx - bar * 1.5f, pause.cy - bar * 1.6f, bar, bar * 3.2f)
        shapes.rect(pause.cx + bar * 0.5f, pause.cy - bar * 1.6f, bar, bar * 3.2f)
        val c = cameraButton.radius * 0.5f
        shapes.rect(cameraButton.cx - c, cameraButton.cy - c * 0.55f, c * 2f, c * 1.1f)
        shapes.setColor(0.1f, 0.35f, 0.33f, 1f)
        shapes.circle(cameraButton.cx, cameraButton.cy, c * 0.38f, 20)

        // Speed bar.
        val barW = min(width * 0.3f, 320f * dp)
        val barH = 10f * dp
        val barX = (width - barW) / 2f
        val barY = 24f * dp
        shapes.setColor(0f, 0f, 0f, 0.4f)
        shapes.rect(barX, barY, barW, barH)
        val fraction = (data.speedKmh / data.maxSpeedKmh).coerceIn(0f, 1f)
        shapes.setColor(0.18f + 0.7f * fraction, 0.85f - 0.4f * fraction, 0.65f - 0.45f * fraction, 0.95f)
        shapes.rect(barX, barY, barW * fraction, barH)
        shapes.end()

        batch.begin()
        // Speed readout.
        font.color = Color.WHITE
        font.data.setScale(dp * 2.6f)
        drawCentred(data.speedKmh.toInt().toString(), width / 2f, 24f * dp + barH + 58f * dp)
        font.data.setScale(dp * 1.1f)
        drawCentred("km/h", width / 2f, 24f * dp + barH + 58f * dp - 44f * dp)

        font.data.setScale(dp * 1.0f)
        drawCentred(data.cameraMode.label, cameraButton.cx, cameraButton.cy - cameraButton.radius - 10f * dp)

        font.color = Color(1f, 1f, 1f, 0.7f)
        font.draw(batch, "FPS ${data.fps}", 14f * dp, height - 14f * dp)
        font.draw(batch, "HITS ${data.collisions}", 14f * dp, height - 36f * dp)
        if (data.impactAge < 1.8f) {
            font.color = Color(1f, 0.55f, 0.5f, 1f - data.impactAge / 1.8f)
            font.data.setScale(dp * 1.5f)
            drawCentred("IMPACT ${data.impactKmh.toInt()} km/h", width / 2f, height - 40f * dp)
        }
        font.data.setScale(dp * 1.15f)
        batch.end()
    }

    private fun drawPauseMenu(data: HudData) {
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.setColor(0.01f, 0.09f, 0.07f, 0.78f)
        shapes.rect(0f, 0f, width, height)
        for (item in menuItems) {
            val fill = if (item.pressed) Color(0.18f, 0.83f, 0.75f, 1f) else Color(0.06f, 0.30f, 0.26f, 1f)
            shapes.setColor(fill.r, fill.g, fill.b, 0.95f)
            shapes.rect(item.x, item.y, item.w, item.h)
        }
        shapes.end()

        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.setColor(0.18f, 0.83f, 0.75f, 1f)
        for (item in menuItems) shapes.rect(item.x, item.y, item.w, item.h)
        shapes.end()

        batch.begin()
        font.color = Color.WHITE
        font.data.setScale(dp * 1.8f)
        drawCentred("PAUSED", width / 2f, resumeItem.y + resumeItem.h + 46f * dp)
        font.data.setScale(dp * 1.2f)
        drawCentred("RESUME", resumeItem.x + resumeItem.w / 2f, resumeItem.y + resumeItem.h / 2f)
        drawCentred("GRAPHICS: ${data.quality.name}", qualityItem.x + qualityItem.w / 2f, qualityItem.y + qualityItem.h / 2f)
        drawCentred("EXIT TO APP", exitItem.x + exitItem.w / 2f, exitItem.y + exitItem.h / 2f)
        font.data.setScale(dp * 0.95f)
        font.color = Color(1f, 1f, 1f, 0.65f)
        drawCentred("Smoother edges apply next launch", width / 2f, exitItem.y - 22f * dp)
        font.data.setScale(dp * 1.15f)
        batch.end()
    }

    private fun drawCentred(text: String, x: Float, y: Float) {
        layout.setText(font, text)
        font.draw(batch, layout, x - layout.width / 2f, y + layout.height / 2f)
    }

    override fun dispose() {
        shapes.dispose()
        batch.dispose()
        font.dispose()
    }

    private companion object {
        const val MAX_POINTERS = 5
    }

}
