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
    private lateinit var bucketMock: Bucket
    private lateinit var blobMock: Blob
    private lateinit var storage: Storage

    @BeforeEach
    fun setUp() {
        mockkStatic(Files::class)
        every { Files.newInputStream(any()) } returns mockk()
        every { Files.createDirectories(any()) } returns mockk()

        blobMock = mockk()
        every { blobMock.name } returns ""
        every { blobMock.getContent() } returns "".toByteArray()

        bucketMock = mockk()
        storage = Storage(bucketMock)
    }

    @AfterEach
    fun tearDown() {
    }

    @Nested
    inner class TestStore {
        private val pathsOrGlob = listOf("./sample")
        private val prefix = "."
        private val key = "test"
        private val tags = listOf("latest")

        @BeforeEach
        fun setUp() {
            mockkStatic(::getLocalFilePaths)
        }

        @Test
        fun whenSuccess() {
            every { bucketMock.create(any<String>(), any<ByteArray>()) } returns blobMock
            every { bucketMock.create(any<String>(), any<InputStream>()) } returns blobMock
            every { getLocalFilePaths(any()) } returns listOf(Paths.get(""))

            assertDoesNotThrow { storage.store(pathsOrGlob, key, tags, prefix) }
        }

        @Test
        fun whenLocalFileNotFound() {
            val dummyPath = Paths.get("nothing.txt")
            every { getLocalFilePaths(any()) } returns listOf(dummyPath)
            every { Files.newInputStream(dummyPath) } throws IOException()

            assertThrows<IOException> {
                storage.store(pathsOrGlob, key, tags, prefix)
            }
        }

        @Test
        fun whenFailUploadToRemote() {
            every { getLocalFilePaths(any()) } returns listOf(Paths.get("success.txt"), Paths.get("fail.txt"))
            every { bucketMock.create("success.txt", any<InputStream>()) } returns blobMock
            every { bucketMock.create("fail.txt", any<InputStream>()) } throws StorageException(500, "Dummy Error")

            assertThrows<StorageException> {
                storage.store(pathsOrGlob, key, tags, prefix)
            }
        }
    }

    @Nested
    inner class TestDownload {
        private val localPath = "test"
        private val key = "test"
        private val tag = "latest"

        @Test
        fun whenSuccess() {
            every { bucketMock.get(any<String>()) } returns blobMock
            justRun { blobMock.downloadTo(any<Path>()) }

            assertDoesNotThrow { storage.download(localPath, key, tag) }
        }

        @Test
        fun whenFailFetchIndex() {
            every { bucketMock.get("skw_index/$key/$tag") } throws StorageException(500, "Dummy Error")

            assertThrows<StorageException> { storage.download(localPath, key, tag) }
            // Assert does not download blob files when failed to fetch index.
            verify(inverse = true) { blobMock.downloadTo(any<Path>()) }
        }

        @Test
        fun whenFailDownloadFromRemote() {
            val indexBlobMock = mockk<Blob>()
            every { bucketMock.get("skw_index/$key/$tag") } returns indexBlobMock
            every { indexBlobMock.getContent() } returns "success.txt\nfail.txt".toByteArray()
            every { bucketMock.get("success.txt") } returns blobMock
            every { bucketMock.get("fail.txt") } throws StorageException(500, "Dummy Error")
            justRun { blobMock.downloadTo(any<Path>()) }

            assertThrows<StorageException> { storage.download(localPath, key, tag) }
        }
    }
}
