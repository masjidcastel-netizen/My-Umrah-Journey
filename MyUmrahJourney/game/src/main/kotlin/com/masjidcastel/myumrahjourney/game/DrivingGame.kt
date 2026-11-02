package com.masjidcastel.myumrahjourney.game

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.environment.DirectionalShadowLight
import com.badlogic.gdx.graphics.g3d.utils.DepthShaderProvider
import com.badlogic.gdx.math.Vector3
import com.masjidcastel.myumrahjourney.core.player.GraphicsQuality
import com.masjidcastel.myumrahjourney.core.player.PlayerState
import com.masjidcastel.myumrahjourney.game.render.CameraRig
import com.masjidcastel.myumrahjourney.game.render.CarModel
import com.masjidcastel.myumrahjourney.game.render.ProceduralTextures
import com.masjidcastel.myumrahjourney.game.render.QualityProfile
import com.masjidcastel.myumrahjourney.game.render.WorldChunk
import com.masjidcastel.myumrahjourney.game.render.WorldScene
import com.masjidcastel.myumrahjourney.game.sim.DrivingSimulation
import com.masjidcastel.myumrahjourney.game.sim.VehicleInput
import com.masjidcastel.myumrahjourney.game.ui.GameUi
import com.masjidcastel.myumrahjourney.game.ui.HudData
import com.masjidcastel.myumrahjourney.game.ui.UiActions
import com.masjidcastel.myumrahjourney.game.world.WorldGenerator
import com.masjidcastel.myumrahjourney.game.world.WorldLayout
import kotlin.math.min

/** What the game needs from the Android side. */
interface GameHost {
    /** The player changed the graphics preset in the pause menu; persist it. */
    fun onQualityChanged(quality: GraphicsQuality)
}

/**
 * The 3D driving prototype: a real-time libGDX scene with a perspective camera, lit and
 * shadowed geometry, a drivable car with collision, and touch controls.
 *
 * Frame: poll input -> fixed-step simulation -> camera -> shadow pass -> main 3D pass -> HUD.
 */
class DrivingGame(
    private val player: PlayerState,
    private val host: GameHost,
) : ApplicationAdapter() {

    private lateinit var layout: WorldLayout
    private lateinit var textures: ProceduralTextures
    private lateinit var world: WorldScene
    private lateinit var car: CarModel
    private lateinit var camera: PerspectiveCamera
    private lateinit var rig: CameraRig
    private lateinit var modelBatch: ModelBatch
    private lateinit var shadowBatch: ModelBatch
    private lateinit var ui: GameUi

    private val simulation = DrivingSimulation()
    private val actions = UiActions()
    private val hud = HudData()
    private val environment = Environment()
    private val sun = DirectionalLight()
    private var shadowLight: DirectionalShadowLight? = null

    private var quality: GraphicsQuality = player.graphicsQuality
    private var profile: QualityProfile = QualityProfile.of(quality)

    private var paused = false
    private var accumulator = 0f
    private var lastInput = VehicleInput()
    private var flash = 0f

    private val shadowFocus = Vector3()
    private val sunDirection = Vector3(-0.55f, -1f, -0.35f).nor()
    private val renderList = ArrayList<WorldChunk>(64)

    override fun create() {
        Gdx.input.setCatchKey(Input.Keys.BACK, true)

        layout = WorldGenerator.generate()
        textures = ProceduralTextures()
        world = WorldScene.build(layout, textures)
        car = CarModel()

        camera = PerspectiveCamera(62f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat()).apply {
            near = 0.5f
            far = QualityProfile.FAR_PLANE
        }
        rig = CameraRig(camera)

        modelBatch = ModelBatch()
        shadowBatch = ModelBatch(DepthShaderProvider())
        ui = GameUi()
        ui.resize(Gdx.graphics.width, Gdx.graphics.height)

        environment.set(ColorAttribute(ColorAttribute.AmbientLight, 0.48f, 0.52f, 0.56f, 1f))
        environment.set(ColorAttribute(ColorAttribute.Fog, SKY_R, SKY_G, SKY_B, 1f))
        applyQuality(quality)

        resetCar()
        Gdx.app.log(TAG, "created: ${layout.buildings.size} buildings, ${layout.trees.size} trees, " +
            "${world.chunks.size} chunks, quality=$quality")
    }

    private fun resetCar() {
        val s = layout.spawn
        simulation.state.reset(s.x, s.z, s.heading)
        rig.snapTo(simulation.state)
        car.update(simulation.state, 0f)
    }

    /** Applies draw distance and shadows. Safe to call at any time while running. */
    private fun applyQuality(newQuality: GraphicsQuality) {
        quality = newQuality
        profile = QualityProfile.of(newQuality)
        hud.quality = newQuality

        shadowLight?.let {
            environment.remove(it)
            environment.shadowMap = null
            it.dispose()
        }
        shadowLight = null
        environment.remove(sun)

        if (profile.shadows) {
            try {
                val light = DirectionalShadowLight(profile.shadowMapSize, profile.shadowMapSize, 70f, 70f, 1f, 160f)
                light.set(1f, 0.96f, 0.86f, sunDirection.x, sunDirection.y, sunDirection.z)
                environment.add(light)
                environment.shadowMap = light
                shadowLight = light
                return
            } catch (e: RuntimeException) {
                // Some GPUs cannot create the depth framebuffer. Fall back to plain lighting.
                Gdx.app.error(TAG, "Shadows unavailable, continuing without them", e)
            }
        }
        sun.set(1f, 0.96f, 0.86f, sunDirection.x, sunDirection.y, sunDirection.z)
        environment.add(sun)
    }

    override fun resize(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        camera.viewportWidth = width.toFloat()
        camera.viewportHeight = height.toFloat()
        camera.update()
        ui.resize(width, height)
    }

    override fun render() {
        val frameTime = min(Gdx.graphics.deltaTime, 0.1f)

        lastInput = ui.poll(paused, actions)
        handleActions()

        var impact = 0f
        if (!paused) {
            accumulator += frameTime
            while (accumulator >= STEP) {
                impact = maxOf(impact, simulation.step(lastInput, STEP, layout.colliders))
                accumulator -= STEP
            }
            car.update(simulation.state, frameTime)
            if (impact > IMPACT_MIN) onImpact(impact)
            if (actions.cameraCycled) rig.cycleMode()
            rig.update(simulation.state, frameTime)
        }

        flash = (flash - frameTime * 2.5f).coerceAtLeast(0f)
        hud.impactAge += frameTime
        hud.speedKmh = simulation.state.speedKmh
        hud.maxSpeedKmh = simulation.params.maxSpeed * 3.6f * 0.9f
        hud.cameraMode = rig.mode
        hud.flash = flash
        hud.fps = Gdx.graphics.framesPerSecond

        drawScene()
        ui.render(hud, paused)
    }

    private fun handleActions() {
        if (!paused && actions.pauseToggled) {
            paused = true
        } else if (paused) {
            if (actions.resume) paused = false
            if (actions.cycleQuality) {
                applyQuality(quality.next())
                host.onQualityChanged(quality)
            }
            if (actions.exit) Gdx.app.exit()
        }
    }

    private fun onImpact(speed: Float) {
        hud.collisions++
        hud.impactKmh = speed * 3.6f
        hud.impactAge = 0f
        flash = (speed / 12f).coerceIn(0.25f, 1f)
        rig.kick((speed / 20f).coerceIn(0.15f, 0.9f))
    }

    private fun drawScene() {
        Gdx.gl.glClearColor(SKY_R, SKY_G, SKY_B, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        collectVisibleChunks()

        shadowLight?.let { light ->
            shadowFocus.set(simulation.state.x, 0f, simulation.state.z)
            // Second argument is the camera's look direction: it shifts the shadow box ahead of the view.
            light.begin(shadowFocus, camera.direction)
            shadowBatch.begin(light.camera)
            for (chunk in renderList) {
                if (chunk.centre.dst2(shadowFocus) < (SHADOW_RANGE + chunk.radius) * (SHADOW_RANGE + chunk.radius)) {
                    shadowBatch.render(chunk.instance)
                }
            }
            if (rig.showsCarBody) shadowBatch.render(car.instance)
            shadowBatch.end()
            light.end()
        }

        modelBatch.begin(camera)
        for (hill in world.hills) {
            if (camera.frustum.sphereInFrustum(hill.centre, hill.radius)) modelBatch.render(hill.instance, environment)
        }
        for (chunk in renderList) modelBatch.render(chunk.instance, environment)
        if (rig.showsCarBody) modelBatch.render(car.instance, environment)
        modelBatch.end()
    }

    private fun collectVisibleChunks() {
        renderList.clear()
        val cx = camera.position.x
        val cz = camera.position.z
        val limit = profile.drawDistance
        for (chunk in world.chunks) {
            val dx = chunk.centre.x - cx
            val dz = chunk.centre.z - cz
            val reach = limit + chunk.radius
            if (dx * dx + dz * dz > reach * reach) continue
            if (!camera.frustum.sphereInFrustum(chunk.centre, chunk.radius)) continue
            renderList.add(chunk)
        }
    }

    /** The Activity went to the background: stop the car and show the pause menu on return. */
    override fun pause() {
        paused = true
    }

    override fun dispose() {
        shadowLight?.dispose()
        modelBatch.dispose()
        shadowBatch.dispose()
        ui.dispose()
        car.dispose()
        world.dispose()
        textures.dispose()
        Gdx.app.log(TAG, "disposed")
    }

    private companion object {
        const val TAG = "MyUmrahDriving"
        const val STEP = 1f / 120f
        const val IMPACT_MIN = 1.5f
        const val SHADOW_RANGE = 90f
        const val SKY_R = 0.62f
        const val SKY_G = 0.80f
        const val SKY_B = 0.90f
    }
}
