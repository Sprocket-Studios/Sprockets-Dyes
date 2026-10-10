plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "26.3-fabric"

// See https://stonecutter.kikugie.dev/wiki/config/params
stonecutter parameters {
    val (version, loader) = current.project.split('-', limit = 2)

    // Makes version- and loader-specific tables from `stonecutter.properties.toml` apply.
    properties {
        tags(version, loader)
    }

    // Constants usable in Stonecutter comments, i.e. `//? if fabric {`
    constants {
        match(loader, "fabric", "neoforge")
    }

    // Token swaps, i.e. `public static final String VERSION = /*$ mod_version*/ "0.0.0";`
    swaps["mod_version"] = "\"${properties.get<String>("mod.version")}\";"
    swaps["minecraft"] = "\"${node.metadata.version}\";"
    dependencies["fapi"] = properties.getOrNull<String>("deps.fabric_api") ?: "0"

    /**
     * Mechanical renames that keep the same argument lists, so they need no `//? if` block.
     *
     * Shared `src/` holds the active node's dialect (26.3); every other node gets a processed copy.
     * Anything whose parameters or semantics changed stays a real `//? if` block instead.
     */
    replacements {
        string(current.parsed >= "1.21.11") {
            replace("ResourceLocation", "Identifier")
        }

        // The class tweaker header differs between obfuscated (<=1.21.11) and unobfuscated (26.1+) runs.
        string(current.parsed >= "26.1") {
            replace("classTweaker v2 named", "classTweaker v2 official")
        }

        // 26.1 replaced immediate-mode `GuiGraphics` with the render state extractor.
        regex(current.parsed >= "26.1") {
            replace("\\bGuiGraphics\\b", "GuiGraphicsExtractor", "\\bGuiGraphicsExtractor\\b", "GuiGraphics")
        }
        regex(current.parsed >= "26.1") {
            replace("\\.drawString\\(", ".text(", "\\.text\\(", ".drawString(")
        }
        regex(current.parsed >= "26.1") {
            replace("\\.drawCenteredString\\(", ".centeredText(", "\\.centeredText\\(", ".drawCenteredString(")
        }
    }
}

/**
 * Builds every version/loader node and collects the jars into `build/libs/<mod version>/`.
 * `./gradlew chiseledBuild`
 */
tasks.register("chiseledBuild") {
    group = "project"
    description = "Builds all Stonecutter version/loader nodes"
    dependsOn(stonecutter.tasks.named("buildAndCollect").map { it.values })
}
