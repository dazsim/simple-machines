import org.gradle.api.tasks.SourceSet
import java.io.File

// Whole-file Java overlays, highest priority first. A file in an earlier dir hides the same path in later dirs and in main.
// Names ending in "@down" are copied through Downport into the build dir first.
fun SourceSet.applyOverlays(root: File, names: List<String>, build: File? = null) {
    val dirs = names.map { n ->
        if (n.endsWith("@down")) Downport.copy(File(root, "src/legacy-java/${n.removeSuffix("@down")}"), File(build!!, "downport/${n.removeSuffix("@down")}"))
        else File(root, "src/legacy-java/$n")
    }
    dirs.forEach { java.srcDir(it) }
    val rel = dirs.map { d -> if (d.isDirectory) d.walk().filter { it.isFile }.map { it.relativeTo(d).invariantSeparatorsPath }.toSet() else emptySet() }
    java.exclude { e ->
        val idx = dirs.indexOfFirst { e.file.absolutePath.startsWith(it.absolutePath + File.separator) }
        val higher = if (idx < 0) rel else rel.subList(0, idx)
        !e.isDirectory && higher.any { e.relativePath.pathString in it }
    }
}

fun overlayChain(mc: String, loader: String): List<String> = when {
    mc == "1.20.1" -> listOfNotNull("1.20.1-$loader", "1.20.1", if (loader == "fabric") "1.21.1-fabric@down" else null, "1.21.1@down")
    else -> listOf("$mc-$loader", mc)
}
