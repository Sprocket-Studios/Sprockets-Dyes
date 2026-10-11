plugins {
    // Applies the correct Fabric Loom variant based on the Minecraft version
    // (handles unobfuscated 26.1+ where no mappings exist).
    id("dev.kikugie.loom-back-compat")
}

// DO NOT set group = ...! Loom/Stonecutter derive it.
version = "${property("mod.version")}+${sc.current.version}"
base.archivesName = "${property("mod.id") as String}-fabric"

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
    minecraft("com.mojang:minecraft:${sc.current.version}")
    // No-op on unobfuscated (26.1+) versions, applies Mojang mappings below that.
    loomx.applyMojangMappings()

    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    // The whole of Fabric API: we touch registries, datagen, rendering, item groups and lifecycle events.
    modImplementation("net.fabricmc.fabric-api:fabric-api:${sc.properties.get<String>("deps.fabric_api")}")

    modImplementation("dev.architectury:architectury-fabric:${property("deps.architectury")}") {
        exclude(group = "net.fabricmc.fabric-api")
    }
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

loom {
    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json")
    accessWidenerPath = sc.process(
        rootProject.file("src/main/resources/sprocket_dyes.ct"),
        "build/processed.ct"
    )

    decompilerOptions.named("vineflower") {
        options.put("mark-corresponding-synthetics", "1") // Adds names to lambdas - useful for mixins
    }

    runConfigs.all {
        preferGradleTask = true
        generateRunConfig = true
        runDirectory = rootProject.file("run")
        jvmArguments.add("-Dmixin.debug.export=true")
    }

    // Servers get a per-node directory so worlds from newer versions never meet older ones.
    // `./gradlew :<node>:runServer -Pselftest` runs the self-test and exits.
    runConfigs.named("client") {
        // `./gradlew :<node>:runClient -Pclienttest` loads resources, then exits (scripts/clienttest.sh).
        if (project.hasProperty("clienttest")) jvmArguments.add("-Dsprocket_dyes.clienttest=true")
        // `-Pclientshots`: quick-play into run/saves/sprocket-test and screenshot the creative tab.
        if (project.hasProperty("clientshots")) {
            jvmArguments.add("-Dsprocket_dyes.clientshots=true")
            programArgs("--quickPlaySingleplayer", "sprocket-test")
        }
    }

    runConfigs.named("server") {
        runDirectory = rootProject.file("run/server-${project.name}")
        programArgs("--nogui")
        if (project.hasProperty("selftest")) jvmArguments.add("-Dsprocket_dyes.selftest=true")
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
        // Adding our own resource srcDir drops Stonecutter's implicit task wiring for its processed resources.
        dependsOn("stonecutterGenerate", generateData)

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
            register("architectury", ">=" + sc.properties.get<String>("mod.arch_compat"))
            register("fabric_loader", ">=" + sc.properties.get<String>("deps.fabric_loader"))
        }

        filesMatching("fabric.mod.json") { expand(props) }

        val mixinJava = "JAVA_${requiredJava.majorVersion}"
        filesMatching("*.mixins.json") { expand("java" to mixinJava) }

        // 26.1+ is unobfuscated and the class tweaker is copied verbatim, so the namespace must be right up front.
        val ctNamespace = if (sc.current.parsed >= "26.1") "official" else "named"
        inputs.property("ctNamespace", ctNamespace)
        filesMatching("*.ct") {
            filter { it.replace("classTweaker v2 named", "classTweaker v2 $ctNamespace") }
        }

        exclude("META-INF/neoforge.mods.toml")
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
        from(loomx.modJar.flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
    }
}
