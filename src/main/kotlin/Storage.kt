import com.google.cloud.storage.Blob
import com.google.cloud.storage.Bucket
import com.google.cloud.storage.StorageOptions
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class Storage(BucketName: String) {
    private var bucket: Bucket

    init {
        val storage = StorageOptions.getDefaultInstance().service
        bucket = storage.get(BucketName) ?: error("Bucket $BucketName does not exist.")
    }

    fun store(pathsOrGlob: List<String>, key: String, tags: List<String>, prefixPath: String?): List<Blob> {
        val blobs = storeBlobs(pathsOrGlob, prefixPath)
        storeIndex(blobs, key, tags)
        return blobs
    }

    fun download(localPath: String, key: String, tag: String): List<Path> {
        val content = fetchIndex(key, tag)
        val remotePaths = content.split("\n")

        return downloadBlobs(remotePaths, localPath)
    }

    private fun storeIndex(blobs: List<Blob>, key: String, tags: List<String>) {
        val content = blobs.map { it.name }.joinToString(separator = "\n")
        val indexPaths = tags.map { "skw_index/$key/$it" }
        indexPaths.forEach {
            bucket.create(it, content.toByteArray())
        }
    }

    private fun fetchIndex(key: String, tag: String): String {
        val blob: Blob = bucket.get("skw_index/$key/$tag")
            ?: throw IllegalArgumentException("Index file not found. key: $key, tag: $tag")
        return String(blob.getContent())
    }

    private fun storeBlobs(pathsOrGlob: List<String>, prefixPath: String?): List<Blob> {
        val paths = resolvePathsOrGlob(pathsOrGlob)
            .filter { p -> p.toFile().isFile }
        if (paths.isEmpty()) {
            throw IllegalArgumentException("None of files are matched by paths or glob")
        }
        println("Try to upload local files: ${paths.map { it.toString() }}")

        return paths.map { localPath ->
            val blobName = toBlobName(localPath, prefixPath)
            val blobInputStream = Files.newInputStream(localPath)
            return@map bucket.create(blobName, blobInputStream)
        }
    }

    // ./sample/text.txt -> sample/text.txtのようにGCS上のパスに合うように変換する
    private fun toBlobName(localPath: Path, prefixPath: String?): String {
        val prefix = Paths.get(prefixPath ?: ".")
        val remotePath = prefix.resolve(localPath).normalize()
        // NOTE: WindowsでパスがバックスラッシュにならないようにStringでjoinする
        return remotePath.toList().joinToString(separator = "/")
    }

    private fun downloadBlobs(remotePaths: List<String>, localPathPrefix: String): List<Path> {
        return remotePaths.map { remotePath ->
            val blob = bucket.get(remotePath)
            val localPath = Paths.get(localPathPrefix, remotePath).normalize()

            Files.createDirectories(localPath.parent)
            blob.downloadTo(localPath)
            return@map localPath
        }
    }
}
