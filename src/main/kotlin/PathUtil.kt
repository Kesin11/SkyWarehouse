import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors
import java.util.stream.Stream

fun filesWalk(rootDir: Path): Stream<Path> {
    return Files.walk(rootDir)
}

fun resolvePathOrGlobList(pathOrGlobList: List<String>): List<Path> {
    if (pathOrGlobList.isEmpty()) {
        throw IllegalArgumentException("Path or glob is empty.")
    }
    // Paths that already expanded by shell
    val paths = pathOrGlobList
        .filterNot { it.contains("*") }
        .map { Paths.get(it).normalize() }

    // Paths that contain double wildcard (ex: **/*.java)
    val expandedPaths = pathOrGlobList
        .filter { it.contains("*") }
        .flatMap { it ->
            val glob = normalizeGlob(it)
            val rootDirPath = globRootDirPath(glob)
            val matcher = FileSystems.getDefault().getPathMatcher("glob:$glob")
            filesWalk(rootDirPath)
                .filter(matcher::matches)
                .collect(Collectors.toList())
                .toList()
        }

    return paths.union(expandedPaths).toList()
}

// Paths.get() can not parse when path has wildcard, so this func parse it using own process
fun globRootDirPath(glob: String): Path {
    val globStrings = glob.split("/")
    val rootDirStrings = globStrings.map { it }.takeWhile { !it.contains("*") }
    return when (rootDirStrings.isEmpty()) {
        true -> Paths.get(".")
        false -> Paths.get(".", *rootDirStrings.toTypedArray()).normalize()
    }
}

fun getLocalFilePaths(pathOrGlobList: List<String>): List<Path> {
    return resolvePathOrGlobList(pathOrGlobList).filter { p -> p.toFile().isFile }
}

fun normalizeGlob(glob: String): String {
    return glob.replaceFirst(Regex("^\\./"), "")
}
