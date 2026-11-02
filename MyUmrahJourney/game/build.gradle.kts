plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

// libGDX ships its native engine (libgdx.so) as per-ABI jars. They are resolved through this
// configuration and unpacked into a generated jniLibs folder so they are packaged into the APK.
val natives: Configuration by configurations.creating

val gdxNativesDir: File = layout.buildDirectory.dir("generated/gdxNatives").get().asFile

android {
    namespace = "com.masjidcastel.myumrahjourney.game"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDir(gdxNativesDir)
        }
    }
}

dependencies {
    implementation(project(":core"))
    api(libs.gdx)
    api(libs.gdx.backend.android)

    testImplementation(libs.junit)

    val gdxVersion = libs.versions.gdx.get()
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86_64")
}

val copyGdxNatives by tasks.registering {
    group = "libgdx"
    description = "Unpacks libGDX native libraries (libgdx.so) for every Android ABI."
    inputs.files(natives)
    outputs.dir(gdxNativesDir)
    doLast {
        gdxNativesDir.deleteRecursively()
        natives.resolve().forEach { jar ->
            val abi = Regex("natives-([A-Za-z0-9_-]+)\\.jar$").find(jar.name)?.groupValues?.get(1)
            if (abi != null) {
                copy {
                    from(zipTree(jar))
                    into(File(gdxNativesDir, abi))
                    include("*.so")
                }
            }
        }
    }
}

tasks.matching { it.name == "preBuild" || Regex("merge.*JniLibFolders").matches(it.name) }
    .configureEach { dependsOn(copyGdxNatives) }
