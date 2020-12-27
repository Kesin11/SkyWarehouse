import com.google.cloud.storage.StorageOptions

class Storage(bucketPath: String) {
    private val bucketPath: String = ""

    init {
//        val storage = StorageOptions.getDefaultInstance().service
//        val bucket = storage.get(bucketPath) ?: error("Bucket $bucketPath does not exist.")
    }

    // TODO: result型を返したい
    fun store(localPath: String, key: String, tags: List<String>): Unit {
        val remotePath = storeBlobs(localPath)
        storeIndex(key, tags)
    }

    fun download(): Unit {

    }

    private fun storeIndex(key: String, tags: List<String>): Unit {
    }

    private fun fetchIndex() {
    }

    private fun storeBlobs(localPath: String): String {
        println("store: $localPath")
        return localPath
    }

    private fun downloadBlobs(): Unit {
    }

}