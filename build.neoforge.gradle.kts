plugins {
    id("net.neoforged.moddev")
    id("neoforge-mutex")
}

val modVersion = providers.gradleProperty("version").orNull ?: sc.properties.get<String>("mod.version")
val mc = sc.current.version
val modern = sc.current.parsed >= "26.1"
version = modVersion
group = property("mod.group") as String
base.archivesName = "${property("mod.id")}-neoforge-$mc"

val requiredJava = when {
    modern -> JavaVersion.VERSION_25
    else -> JavaVersion.VERSION_21
}

repositories {
    exclusiveContent {
        forRepository { maven("https://maven.blamejared.com") { name = "BlameJared" } }
        filter { includeGroup("mezz.jei") }
    }
}

sourceSets.main {
    java.exclude("com/workshopcraft/simplemachines/fabric/**", "com/workshopcraft/simplemachines/forge/**")
    if (!modern) resources.setSrcDirs(listOf(rootProject.file("src/legacy/1.21.1"), rootProject.file("src/main/resources")))
}

neoForge {
    version = property("deps.neo_loader") as String
    if (modern) accessTransformers.files.setFrom(rootProject.file("src/main/resources/META-INF/accesstransformer.cfg"))
    else accessTransformers.files.setFrom()
    mods {
        register(property("mod.id") as String) {
            sourceSet(sourceSets.main.get())
        }
    }
    runs {
        register("client") {
            gameDirectory = rootProject.file("runs/${sc.current.project}")
            client()
            programArguments.addAll(providers.gradleProperty("quickPlay").map { listOf("--quickPlayMultiplayer", it) }.orElse(listOf()))
        }
        register("server") {
            gameDirectory = rootProject.file("runs/${sc.current.project}-server")
            server()
        }
    }
}

dependencies {
    compileOnly("mezz.jei:jei-$mc-common-api:${property("deps.jei")}")
}

java {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava
    toolchain.languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
}

tasks {
    processResources {
        val props = mapOf(
            "id" to sc.properties.get<String>("mod.id"),
            "name" to sc.properties.get<String>("mod.name"),
            "version" to modVersion,
            "author" to sc.properties.get<String>("mod.author"),
            "description" to sc.properties.get<String>("mod.description"),
            "minecraft" to sc.properties.get<String>("mod.mc_compat"),
            "neoforge" to if (modern) "[26.1.0.0-beta,)" else "[21.1,)",
        )
        inputs.properties(props)
        filesMatching(listOf("META-INF/neoforge.mods.toml", "pack.mcmeta")) { expand(props) }
        exclude("fabric.mod.json", "META-INF/mods.toml")
        if (!modern) exclude("pack.mcmeta", "simplemachines.accesswidener", "META-INF/accesstransformer.cfg", "assets/simplemachines/items/**")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    named("createMinecraftArtifacts") {
        dependsOn("stonecutterGenerate")
    }

    jar {
        manifest.attributes(
            "Specification-Title" to sc.properties.get<String>("mod.name"),
            "Specification-Vendor" to sc.properties.get<String>("mod.author"),
            "Specification-Version" to modVersion,
            "Implementation-Title" to "neoforge",
            "Implementation-Version" to modVersion,
            "Implementation-Vendor" to sc.properties.get<String>("mod.author"),
            "Built-On-Minecraft" to mc,
        )
    }

    withType<Jar> {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        val name = project.property("mod.name")
        inputs.property("mod_name", name)
        from(rootProject.file("LICENSE")) { rename { "${it}_$name" } }
    }
}
