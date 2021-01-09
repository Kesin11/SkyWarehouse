import com.google.cloud.storage.Blob
import com.google.cloud.storage.Bucket
import com.google.cloud.storage.StorageOptions
import kotlinx.coroutines.*
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
        return runBlocking {
            val blobs = storeBlobs(pathsOrGlob, prefixPath)
            storeIndex(blobs, key, tags)
            blobs
        }
    }

    fun download(localPath: String, key: String, tag: String): List<Path> {
        return runBlocking {
            val content = fetchIndex(key, tag)
            val remotePaths = content.split("\n")

            downloadBlobs(remotePaths, localPath)
        }
    }

    private suspend fun storeIndex(blobs: List<Blob>, key: String, tags: List<String>) {
        val content = blobs.joinToString(separator = "\n") { it.name }
        val indexPaths = tags.map { "skw_index/$key/$it" }
        return coroutineScope {
            async(Dispatchers.IO) {
                indexPaths.forEach {
                    bucket.create(it, content.toByteArray())
                }
            }
        }
    }

    private suspend fun fetchIndex(key: String, tag: String): String {
        val blob: Blob = withContext(Dispatchers.IO) { bucket.get("skw_index/$key/$tag") }
            ?: throw IllegalArgumentException("Index file not found. key: $key, tag: $tag")
        return String(blob.getContent())
    }

    private suspend fun storeBlobs(pathsOrGlob: List<String>, prefixPath: String?): List<Blob> {
        val paths = resolvePathsOrGlob(pathsOrGlob)
            .filter { p -> p.toFile().isFile }
        if (paths.isEmpty()) {
            throw IllegalArgumentException("None of files are matched by paths or glob")
        }
        println("Try to upload local files: ${paths.map { it.toString() }}")

        return coroutineScope {
            paths.map { localPath ->
                async(Dispatchers.IO) {
                    val blobName = toBlobName(localPath, prefixPath)
                    val blobInputStream = Files.newInputStream(localPath)

                    println("$blobName : Working in thread ${Thread.currentThread().name}")

                    bucket.create(blobName, blobInputStream)
                }
            }.awaitAll()
        }
    }

    // ./sample/text.txt -> sample/text.txtのようにGCS上のパスに合うように変換する
    private fun toBlobName(localPath: Path, prefixPath: String?): String {
        val prefix = Paths.get(prefixPath ?: ".")
        val remotePath = prefix.resolve(localPath).normalize()
        // NOTE: WindowsでパスがバックスラッシュにならないようにStringでjoinする
        return remotePath.toList().joinToString(separator = "/")
    }

    private suspend fun downloadBlobs(remotePaths: List<String>, localPathPrefix: String): List<Path> {
        return coroutineScope {
            remotePaths.map { remotePath ->
                async(Dispatchers.IO) {
                    val blob = bucket.get(remotePath)
                    val localPath = Paths.get(localPathPrefix, remotePath).normalize()

                    println("$remotePath : Working in thread ${Thread.currentThread().name}")

                    Files.createDirectories(localPath.parent)
                    blob.downloadTo(localPath)
                    localPath
                }
            }.awaitAll()
        }
    }
}
