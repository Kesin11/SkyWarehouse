import com.google.cloud.storage.Blob
import com.google.cloud.storage.Bucket
import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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
    fun store() {
        val pathsOrGlob = listOf("./sample")
        val prefix = "."
        val key = "test"
        val tags = listOf("latest")
        mockkStatic(::getLocalFilePaths)
        every { bucketMock.create(any<String>(), any<ByteArray>()) } returns blobMock
        every { bucketMock.create(any<String>(), any<InputStream>()) } returns blobMock
        every { getLocalFilePaths(any()) } returns listOf(Paths.get(""))

        val storage = Storage(bucketMock)
        storage.store(pathsOrGlob, key, tags, prefix)
    }

    @Test
    fun download() {
        every { bucketMock.get(any<String>()) } returns blobMock
        justRun { blobMock.downloadTo(any<Path>()) }
        val localPath = "test"
        val key = "test"
        val tag = "latest"

        val storage = Storage(bucketMock)
        storage.download(localPath, key, tag)
    }
}
