import com.google.cloud.storage.Blob
import com.google.cloud.storage.Bucket
import com.google.cloud.storage.StorageOptions
import java.nio.file.Files
import java.nio.file.Paths

class Storage(bucketPath: String) {
    private var bucket: Bucket

    init {
        val storage = StorageOptions.getDefaultInstance().service
        bucket = storage.get(bucketPath) ?: error("Bucket $bucketPath does not exist.")
    }

    // TODO: result型を返したい
    fun store(localPath: String, key: String, tags: List<String>) {
        val blob = storeBlobs(localPath)
        storeIndex(blob.name, key, tags)
    }

    fun download(): Unit {

    }

    private fun storeIndex(remotePath: String, key: String, tags: List<String>) {
        val indexPaths = tags.map { "skw_index/$key/$it" }
        indexPaths.forEach {
            bucket.create(it, remotePath.toByteArray())
        }
    }

    private fun fetchIndex() {
    }

    private fun storeBlobs(localPath: String): Blob {
        val filePath = Paths.get(localPath)
        val blobName = toBlobName(localPath)
        val blobInputStream = Files.newInputStream(filePath)

        return bucket.create(blobName, blobInputStream)
    }

    // ./sample/text.txt -> sample/text.txtのようにGCS上のパスに合うように変換する
    private fun toBlobName(localPath: String): String {
        val filePath = Paths.get(localPath).normalize()
        // Windowsでのバックスラッシュを変換するために手動でjoinする
        return filePath.toList().joinToString(separator = "/")
    }

    private fun downloadBlobs(): Unit {
    }

}