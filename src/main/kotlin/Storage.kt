import com.google.cloud.storage.Blob
import com.google.cloud.storage.Bucket
import com.google.cloud.storage.StorageOptions
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors.toList

class Storage(bucketPath: String) {
    private var bucket: Bucket

    init {
        val storage = StorageOptions.getDefaultInstance().service
        bucket = storage.get(bucketPath) ?: error("Bucket $bucketPath does not exist.")
    }

    // TODO: result型を返したい
    fun store(pathsOrGlob: List<String>, key: String, tags: List<String>) {
        val blobs = storeBlobs(pathsOrGlob)
        storeIndex(blobs, key, tags)
    }

    // TODO: 複数ファイル対応
    fun download(localPath: String, key: String, tag: String): Unit {
        val remotePath = fetchIndex(key, tag)
        downloadBlobs(remotePath, localPath)
    }

    private fun storeIndex(blobs: List<Blob>, key: String, tags: List<String>) {
        val remotePath = blobs.map { it.name }.joinToString(separator = "\n")
        val indexPaths = tags.map { "skw_index/$key/$it" }
        indexPaths.forEach {
            bucket.create(it, remotePath.toByteArray())
        }
    }

    private fun fetchIndex(key: String, tag: String): String {
        // TODO: 存在しなかった場合のエラーハンドリング
        val blob = bucket.get("skw_index/$key/$tag")
        return String(blob.getContent())
    }

    private fun storeBlobs(pathsOrGlob: List<String>): List<Blob> {
        val paths = globMatchedPaths(pathsOrGlob)
        println(paths.map { it.toString() })
        // TODO: 1つもマッチしなかった場合はエラーを出しておく
        return paths.map { localPath ->
            val blobName = toBlobName(localPath)
            val blobInputStream = Files.newInputStream(localPath)
            return@map bucket.create(blobName, blobInputStream)
        }
    }

    // パスの配列かglobの2パターンをList<Path>に正規化する
    // CLIのオプションにファイルパスを渡す場合、"*"などのワイルドカードはシェルがパスを複数の引数に展開してくる
    // "**/*"などシェルが展開できなかった場合は単純に文字列となる
    // この違いを吸収する
    private fun globMatchedPaths(pathsOrGlob: List<String>): List<Path> {
        // シェルがパスを展開したケース
        if (pathsOrGlob.size > 1) {
           return pathsOrGlob.map { Paths.get(it).normalize() }
        }

        // 引数がglobの文字列として解釈されたケース
        val glob = pathsOrGlob.first()
        val fileSystems = FileSystems.getDefault()
        // wildcardが含まれているとPaths.getがパースできないので独自にLinux, Windowsのパスに対応するglobに書き換える
        val globStrings = glob.split("/")

        // walkのコストを抑えるため、ワイルドカードより1つ上のディレクトリをrootにする
        val rootDirStrings = globStrings.map { it }.takeWhile { !it.contains("*") }
        val rootDirPath = Paths.get(".", *rootDirStrings.toTypedArray()).normalize()

        val matcher = fileSystems.getPathMatcher("glob:$glob")
        return Files.walk(rootDirPath)
            .filter(matcher::matches)
            .collect(toList())
            .toList()
    }

    // ./sample/text.txt -> sample/text.txtのようにGCS上のパスに合うように変換する
    private fun toBlobName(localPath: Path): String {
        val filePath = localPath.normalize()
        // Windowsでのバックスラッシュを変換するために手動でjoinする
        return filePath.toList().joinToString(separator = "/")
    }

    private fun downloadBlobs(remotePath: String, localPathPrefix: String): Unit {
        val blob = bucket.get(remotePath)
        val localPath = Paths.get(localPathPrefix, remotePath).normalize()

        Files.createDirectories(localPath.parent)
        blob.downloadTo(localPath)
    }
}
