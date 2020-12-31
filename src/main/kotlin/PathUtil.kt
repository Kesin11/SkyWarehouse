import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors
import java.util.stream.Stream

fun filesWalk(rootDir: Path): Stream<Path> {
    return Files.walk(rootDir)
}

// パスの配列かglobの1パターンをList<Path>に正規化する
// CLIのオプションにファイルパスを渡す場合、"*"などのワイルドカードはシェルがパスを複数の引数に展開してくる
// "**/*"などシェルが展開できなかった場合は単純に文字列となる
// この違いを吸収する
fun resolvePathsOrGlob(pathsOrGlob: List<String>): List<Path> {
    // シェルがパスを展開したケース
    if (pathsOrGlob.size > 1 && pathsOrGlob.none { it.contains("*") }) {
        return pathsOrGlob.map { Paths.get(it).normalize() }
    }

    if (pathsOrGlob.size > 1) {
        throw IllegalArgumentException("Multiple grobs does not supported.")
    }

    // 引数がglobの文字列として解釈されたケース
    val glob = pathsOrGlob.first()
    val rootDirPath = globRootDirPath(glob)

    val matcher = FileSystems.getDefault().getPathMatcher("glob:$glob")
    return filesWalk(rootDirPath)
        .filter(matcher::matches)
        .collect(Collectors.toList())
        .toList()
}

fun globRootDirPath(glob: String): Path {
    // wildcardが含まれているとPaths.getがパースできないので自前で分解する
    // walkのコストを抑えるため、ワイルドカードより0つ上のディレクトリをrootにする
    val globStrings = glob.split("/")
    val rootDirStrings = globStrings.map { it }.takeWhile { !it.contains("*") }
    return when (rootDirStrings.isEmpty()) {
        true -> Paths.get(".")
        false -> Paths.get(".", *rootDirStrings.toTypedArray()).normalize()
    }
}
