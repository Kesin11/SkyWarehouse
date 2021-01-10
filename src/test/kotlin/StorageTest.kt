import com.google.cloud.storage.Blob
import com.google.cloud.storage.Bucket
import com.google.cloud.storage.StorageException
import io.mockk.*
import org.junit.jupiter.api.*
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

internal class StorageTest {
    lateinit var bucketMock: Bucket
    lateinit var blobMock: Blob

    @BeforeEach
    fun setUp() {
        mockkStatic(Files::class)
        every { Files.newInputStream(any()) } returns mockk()
        every { Files.createDirectories(any()) } returns mockk()

        blobMock = mockk()
        every { blobMock.name } returns ""
        every { blobMock.getContent() } returns "".toByteArray()

        bucketMock = mockk()
    }

    @AfterEach
    fun tearDown() {
    }

    @Test
    fun storeWhenSuccess() {
        val pathsOrGlob = listOf("./sample")
        val prefix = "."
        val key = "test"
        val tags = listOf("latest")
        mockkStatic(::getLocalFilePaths)
        every { bucketMock.create(any<String>(), any<ByteArray>()) } returns blobMock
        every { bucketMock.create(any<String>(), any<InputStream>()) } returns blobMock
        every { getLocalFilePaths(any()) } returns listOf(Paths.get(""))

        val storage = Storage(bucketMock)
        assertDoesNotThrow { storage.store(pathsOrGlob, key, tags, prefix) }
    }

    @Test
    fun storeWhenLocalFileNotFound() {
        val pathsOrGlob = listOf("./sample")
        val prefix = "."
        val key = "test"
        val tags = listOf("latest")
        val dummyPath = Paths.get("nothing.txt")
        mockkStatic(::getLocalFilePaths)
        every { getLocalFilePaths(any()) } returns listOf(dummyPath)
        every { Files.newInputStream(dummyPath) } throws IOException()

        val storage = Storage(bucketMock)
        assertThrows<IOException> {
            storage.store(pathsOrGlob, key, tags, prefix)
        }
    }

    @Test
    fun storeWhenFailStoreBlobs() {
        val pathsOrGlob = listOf("./sample")
        val prefix = "."
        val key = "test"
        val tags = listOf("latest")
        mockkStatic(::getLocalFilePaths)
        every { getLocalFilePaths(any()) } returns listOf(Paths.get("success.txt"), Paths.get("fail.txt"))
        every { bucketMock.create("success.txt", any<InputStream>()) } returns blobMock
        every { bucketMock.create("fail.txt", any<InputStream>()) } throws StorageException(500, "Dummy Error")

        val storage = Storage(bucketMock)
        assertThrows<StorageException> {
            storage.store(pathsOrGlob, key, tags, prefix)
        }
    }

    @Test
    fun downloadWhenSuccess() {
        every { bucketMock.get(any<String>()) } returns blobMock
        justRun { blobMock.downloadTo(any<Path>()) }
        val localPath = "test"
        val key = "test"
        val tag = "latest"

        val storage = Storage(bucketMock)
        assertDoesNotThrow { storage.download(localPath, key, tag) }
    }

    @Test
    fun downloadWhenFailFetchIndex() {
        val localPath = "test"
        val key = "test"
        val tag = "latest"
        every { bucketMock.get("skw_index/$key/$tag") } throws StorageException(500, "Dummy Error")

        val storage = Storage(bucketMock)
        assertThrows<StorageException> { storage.download(localPath, key, tag) }
        verify(inverse = true) { blobMock.downloadTo(any<Path>()) }
    }

    @Test
    fun downloadWhenFailDownloadBlobs() {
        val localPath = "test"
        val key = "test"
        val tag = "latest"
        val indexBlobMock = mockk<Blob>()
        every { bucketMock.get("skw_index/$key/$tag") } returns indexBlobMock
        every { indexBlobMock.getContent() } returns "success.txt\nfail.txt".toByteArray()
        every { bucketMock.get("success.txt") } returns blobMock
        every { bucketMock.get("fail.txt") } throws StorageException(500, "Dummy Error")
        justRun { blobMock.downloadTo(any<Path>()) }

        val storage = Storage(bucketMock)
        assertThrows<StorageException> { storage.download(localPath, key, tag) }
    }
}
