import com.google.cloud.storage.Blob
import com.google.cloud.storage.Bucket
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageException
import com.google.cloud.storage.StorageOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class Storage {
    private var bucket: Bucket
    private val logger: Logger
    private val indexPrefix = "skw_index"

    constructor(bucket: Bucket, logger: Logger) {
        this.bucket = bucket
        this.logger = logger
    }

    constructor(bucketPath: String, logger: Logger) {
        if (!this.verifyBucketPath(bucketPath)) throw IllegalArgumentException("bucketPath must be start with gs://")
        val bucketName = bucketPath.removePrefix("gs://")

        this.logger = logger
        val storage = StorageOptions.getDefaultInstance().service
        try {
            this.bucket = storage.get(bucketName) ?: error("Bucket $bucketName does not exist.")
        } catch (e: StorageException) {
            when (e.code) {
                401 -> error("Must need authorization to access GCS. Set service account path to GOOGLE_APPLICATION_CREDENTIALS or execute application-default login (https://cloud.google.com/sdk/gcloud/reference/auth/application-default/login).")
                else -> throw e
            }
        }
    }

     internal fun verifyBucketPath(bucketPath: String): Boolean {
        return (bucketPath.startsWith("gs://"))
    }

    fun upload(pathsOrGlob: List<String>, key: String, tags: List<String>, prefixPath: String?): List<Blob> {
        return runBlocking {
            val blobs = uploadBlobs(pathsOrGlob, prefixPath)
            uploadIndex(blobs, key, tags)
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

    fun listKeys(keyPrefix: String?): List<String> {
        val prefix = keyPrefix ?: ""
        val pager = bucket.list(
            Storage.BlobListOption.pageSize(100),
            Storage.BlobListOption.prefix("$indexPrefix/$prefix"),
            Storage.BlobListOption.currentDirectory(),
        )
        val iterator = pager.iterateAll()
        return iterator.map { blob ->
            val name = blob.name
            name.substringAfter('/').trimEnd('/')
        }
    }

    private suspend fun uploadIndex(blobs: List<Blob>, key: String, tags: List<String>) {
        val content = blobs.joinToString(separator = "\n") { it.name }
        val indexPaths = tags.map { "$indexPrefix/$key/$it" }
        return coroutineScope {
            async(Dispatchers.IO) {
                indexPaths.forEach {
                    bucket.create(it, content.toByteArray())
                }
            }
        }
    }

    fun listTags(key: String): List<String> {
        val pager = bucket.list(
            Storage.BlobListOption.pageSize(100),
            Storage.BlobListOption.prefix("$indexPrefix/$key/"),
        )
        val iterator = pager.iterateAll()
        if (!iterator.iterator().hasNext()) {
            throw RuntimeException("key:$key has not any tags!")
        }

        return iterator
            .sortedBy { -it.updateTime }
            .map { blob ->
                val name = blob.name
                name.substringAfterLast('/').trimEnd('/')
            }
    }

    private suspend fun fetchIndex(key: String, tag: String): String {
        val blob: Blob = withContext(Dispatchers.IO) { bucket.get("$indexPrefix/$key/$tag") }
            ?: throw IllegalArgumentException("Index file not found. key: $key, tag: $tag")
        return String(blob.getContent())
    }

    private suspend fun uploadBlobs(pathsOrGlob: List<String>, prefixPath: String?): List<Blob> {
        val paths = getLocalFilePaths(pathsOrGlob)
        if (paths.isEmpty()) {
            throw IllegalArgumentException("None of files are matched by paths or glob")
        }
        logger.debug("Try to upload local files: ${paths.map { it.toString() }}")

        return coroutineScope {
            paths.map { localPath ->
                async(Dispatchers.IO) {
                    val blobName = toBlobName(localPath, prefixPath)
                    val blobInputStream = Files.newInputStream(localPath)

                    logger.debug("$blobName : Working in thread ${Thread.currentThread().name}")

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

                    logger.debug("$remotePath : Working in thread ${Thread.currentThread().name}")

                    Files.createDirectories(localPath.parent)
                    blob.downloadTo(localPath)
                    localPath
                }
            }.awaitAll()
        }
    }
}
