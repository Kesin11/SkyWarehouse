import io.mockk.every
import io.mockk.mockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.IllegalArgumentException
import kotlin.test.assertEquals

internal class PathUtilKtTest {

    @BeforeEach
    fun setUp() {
    }

    @AfterEach
    fun tearDown() {
    }

    @Nested
    class TestResolvePathsOrGlob {
        @Test
        fun unixPaths() {
            val actual = resolvePathsOrGlob(listOf("sample/foo.kt", "sample/bar.kt"))
            assertEquals(
                listOf(
                    Paths.get("sample", "foo.kt"),
                    Paths.get("sample", "bar.kt")
                ),
                actual
            )
        }

        @Test
        @EnabledOnOs(OS.WINDOWS)
        fun windowsPaths() {
            val actual = resolvePathsOrGlob(listOf("sample\\foo.kt", "sample\\bar.kt"))
            assertEquals(
                listOf(
                    Paths.get("sample", "foo.kt"),
                    Paths.get("sample", "bar.kt")
                ),
                actual
            )
        }

        @Test
        fun globMatch() {
            val rootDirPath = Paths.get(".")
            mockkStatic(::filesWalk)
            every { filesWalk(rootDirPath) } returns listOf(
                Paths.get("sample", "foo.kt"),
                Paths.get("sample", "bar.kt"),
                Paths.get("sample", "bar.java")
            ).stream()

            val actual = resolvePathsOrGlob(listOf("**/*.kt"))
            assertEquals(
                listOf(
                    Paths.get("sample", "foo.kt"),
                    Paths.get("sample", "bar.kt")
                ),
                actual
            )
        }

        @Test
        fun globMatchNoting() {
            val rootDirPath = Paths.get(".")
            mockkStatic(::filesWalk)
            every { filesWalk(rootDirPath) } returns listOf(
                Paths.get("sample", "foo.kt"),
                Paths.get("sample", "bar.kt"),
                Paths.get("sample", "bar.java")
            ).stream()

            val actual = resolvePathsOrGlob(listOf("**/noting"))
            assertEquals(listOf<Path>(), actual)
        }

        @Test
        fun globMultiThrowsError() {
            val rootDirPath = Paths.get(".")
            mockkStatic(::filesWalk)
            every { filesWalk(rootDirPath) } returns listOf(
                Paths.get("sample", "foo.kt"),
                Paths.get("sample", "bar.kt"),
                Paths.get("sample", "bar.java")
            ).stream()

            assertThrows<IllegalArgumentException>("Multiple globs does not supported") {
                resolvePathsOrGlob(listOf("**/first", "**/second"))
            }
        }
    }

    @Nested
    class TestGlobRootDirPath {
        @Test
        @DisplayName("**/*.kt")
        fun noPrefix() {
            val actual = globRootDirPath("**/*.kt")
            assertEquals(Paths.get("."), actual)
        }

        @Test
        @DisplayName("sample/**/*.kt")
        fun onePrefix() {
            val actual = globRootDirPath("sample/**/*.kt")
            assertEquals(Paths.get("sample"), actual)
        }

        @Test
        @DisplayName("sample/foo/**/*.kt")
        fun multiPrefix() {
            val actual = globRootDirPath("sample/foo/**/*.kt")
            assertEquals(Paths.get("sample", "foo"), actual)
        }
    }
}
