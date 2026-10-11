plugins {
    id("net.neoforged.moddev") version "2.0.147"
    id("neoforge-mutex")
}

version = "${property("mod.version")}+${sc.current.version}"
base.archivesName = "${property("mod.id") as String}-neoforge"

val requiredJava: JavaVersion = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    else -> JavaVersion.VERSION_21
}

val compatibleVersions: List<String> = sc.properties.rawOrNull("mod", "mc_releases")
    ?.asList().orEmpty().map { it.toString() }

repositories {
    fun strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
        forRepository { maven(url) { name = alias } }
        filter { groups.forEach(::includeGroup) }
    }
    strictMaven("https://maven.architectury.dev/", "Architectury", "dev.architectury")
    strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")
    mavenCentral()
}

dependencies {
    implementation("dev.architectury:architectury-neoforge:${property("deps.architectury")}")

    // NeoForge ships MixinExtras jar-in-jar, but on 1.21.x dev runs never start it, which breaks every
    // `@Local`/`@WrapOperation` mixin (Architectury's included). Put it on the dev classpath; our mixin
    // plugin bootstraps it. Production uses NeoForge's own copy.
    implementation("io.github.llamalad7:mixinextras-neoforge:0.5.3")
}


// Data (recipes, models, loot tables, tags, ...) for this MC version, generated from vanilla's own files.
// Not committed: `python3 datagen/generate.py data <mc> <dir>` runs here, before processResources.
val generatedData = layout.buildDirectory.dir("generated/sprocket-data")
val generateData = tasks.register<Exec>("generateData") {
    group = "build"
    description = "Generates this version's data from vanilla templates (datagen/generate.py)"
    val mc = sc.current.version
    val out = generatedData.get().asFile
    inputs.dir(rootProject.file("datagen")).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file(rootProject.file("palette/colors.json"))
    inputs.file(File(System.getProperty("user.home"), ".gradle/caches/fabric-loom/$mc/minecraft-client.jar"))
    outputs.dir(out)
    environment("PYTHONDONTWRITEBYTECODE", "1")
    commandLine("python3", rootProject.file("datagen/generate.py").absolutePath, "data", mc, out.absolutePath)
}

sourceSets.main {
    resources.srcDir(generatedData)
}

neoForge {
    enable {
        version = property("deps.neo_loader") as String
        // Run on vanilla's original bytecode (binary-patched) instead of a recompiled jar: dev then matches
        // production, and mixins that capture locals (e.g. Architectury's) see the same local variable layout.
        isDisableRecompilation = true
    }


    mods {
        register(property("mod.id") as String) {
            sourceSet(sourceSets.main.get())
        }
    }

    runs {
        register("client") {
            gameDirectory = rootProject.file("run")
            client()
            if (project.hasProperty("clienttest")) systemProperty("sprocket_dyes.clienttest", "true")
            // `-Pclientshots`: quick-play into run/saves/sprocket-test and screenshot the creative tab.
            if (project.hasProperty("clientshots")) {
                systemProperty("sprocket_dyes.clientshots", "true")
                programArguments.addAll("--quickPlaySingleplayer", "sprocket-test")
            }
        }

        register("server") {
            // Per-node directory so worlds from newer versions never meet older ones.
            gameDirectory = rootProject.file("run/server-${project.name}")
            server()
            programArgument("--nogui")
            if (project.hasProperty("selftest")) systemProperty("sprocket_dyes.selftest", "true")
        }
    }
}

tasks.matching { it.name == "runServer" }.configureEach {
    doFirst { rootProject.file("run/server-${project.name}/eula.txt").apply { parentFile.mkdirs(); writeText("eula=true\n") } }
}

java {
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava

    toolchain {
        vendor = JvmVendorSpec.ADOPTIUM
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}

tasks {
    processResources {
        fun MutableMap<String, String>.register(key: String, value: String) {
            inputs.property(key, value)
            set(key, value)
        }

        val props = buildMap {
            register("id", sc.properties["mod.id"])
            register("name", sc.properties["mod.name"])
            register("version", sc.properties["mod.version"])
            register("minecraft", sc.properties["mod.mc_compat"])
            register("author", sc.properties["mod.author"])
            register("license", sc.properties["mod.license"])
            register("description", sc.properties["mod.description"])
            register("homepage", sc.properties["mod.homepage"])
            register("architectury", "[" + sc.properties.get<String>("mod.arch_compat") + ",)")
            // NeoForge 26.2 deprecated `logoFile` in favor of `iconFile` (square) / `bannerFile`.
            register("logo_key", if (sc.current.parsed >= "26.2") "iconFile" else "logoFile")
        }

        filesMatching("META-INF/neoforge.mods.toml") { expand(props) }

        val mixinJava = "JAVA_${requiredJava.majorVersion}"
        filesMatching("*.mixins.json") { expand("java" to mixinJava) }

        exclude("fabric.mod.json", "*.ct", "*.classtweaker")
        dependsOn(generateData)
    }

    named("createMinecraftArtifacts") {
        dependsOn("stonecutterGenerate")
    }

    withType<Jar> {
        val name = project.property("mod.id")
        inputs.property("mod_id", name)
        from(rootProject.file("LICENSE")) { rename { "${it}_$name" } }
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds the mod jar and copies it to `build/libs/{mod version}/`"

        inputs.property("version", project.property("mod.version"))
        from(jar.flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
    }
}
