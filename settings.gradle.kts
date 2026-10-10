pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/") { name = "FabricMC" }
        maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.8"
    // Selects the correct Fabric Loom variant per MC version (remapping below 26.1, unobfuscated from 26.1).
    id("dev.kikugie.loom-back-compat") version "0.4.2"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
    create(rootProject) {
        /**
         * Creates `versions/{project}-{loader}` nodes sharing the root `src/` tree.
         * Each node uses the `build.{loader}.gradle.kts` build script.
         * A node targets the newest MC version of the range it covers (see `stonecutter.properties.toml`).
         */
        fun match(project: String, vararg loaders: String, version: String = project) {
            for (loader in loaders) version("$project-$loader", version).buildscript("build.$loader.gradle.kts")
        }

        match("1.21.1", "fabric", "neoforge")  // 1.21 - 1.21.1
        match("1.21.3", "fabric", "neoforge")  // 1.21.2 - 1.21.3
        match("1.21.4", "fabric", "neoforge")
        match("1.21.5", "fabric", "neoforge")
        match("1.21.8", "fabric", "neoforge")  // 1.21.6 - 1.21.8
        match("1.21.10", "fabric", "neoforge") // 1.21.9 - 1.21.10
        match("1.21.11", "fabric", "neoforge")
        match("26.1.2", "fabric", "neoforge")  // 26.1 - 26.1.2
        match("26.2", "fabric", "neoforge")
        match("26.3", "fabric", "neoforge")
        vcsVersion = "26.3-fabric"
    }
}

rootProject.name = "SprocketDyes"
