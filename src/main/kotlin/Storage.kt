import com.google.cloud.storage.Blob
import com.google.cloud.storage.Bucket
import com.google.cloud.storage.StorageOptions
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

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

    fun download(localPath: String, key: String, tag: String): Unit {
        val content = fetchIndex(key, tag)
        val remotePaths = content.split("\n")

        downloadBlobs(remotePaths, localPath)
    }

    private fun storeIndex(blobs: List<Blob>, key: String, tags: List<String>) {
        val content = blobs.map { it.name }.joinToString(separator = "\n")
        val indexPaths = tags.map { "skw_index/$key/$it" }
        indexPaths.forEach {
            bucket.create(it, content.toByteArray())
        }
    }

    private fun fetchIndex(key: String, tag: String): String {
        // TODO: 存在しなかった場合のエラーハンドリング
        val blob = bucket.get("skw_index/$key/$tag")
        return String(blob.getContent())
    }

    private fun storeBlobs(pathsOrGlob: List<String>): List<Blob> {
        val paths = resolvePathsOrGlob(pathsOrGlob)
        println(paths.map { it.toString() })
        // TODO: 1つもマッチしなかった場合はエラーを出しておく
        return paths.map { localPath ->
            val blobName = toBlobName(localPath)
            val blobInputStream = Files.newInputStream(localPath)
            return@map bucket.create(blobName, blobInputStream)
        }
    }

    // ./sample/text.txt -> sample/text.txtのようにGCS上のパスに合うように変換する
    private fun toBlobName(localPath: Path): String {
        val filePath = localPath.normalize()
        // Windowsでのバックスラッシュを変換するために手動でjoinする
        return filePath.toList().joinToString(separator = "/")
    }

    private fun downloadBlobs(remotePaths: List<String>, localPathPrefix: String) {
        return remotePaths.forEach { remotePath ->
            val blob = bucket.get(remotePath)
            val localPath = Paths.get(localPathPrefix, remotePath).normalize()
            println(localPath)

            Files.createDirectories(localPath.parent)
            blob.downloadTo(localPath)
        }
    }
}
