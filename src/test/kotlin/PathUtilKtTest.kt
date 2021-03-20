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
import java.util.stream.Stream
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
        private val ktPathsFixture = listOf(
            Paths.get("sample", "foo.kt"),
            Paths.get("sample", "bar.kt"),
        )
        private val nestJavaPathsFixture = listOf(
            Paths.get("sample", "nest", "foo.java"),
            Paths.get("sample", "nest", "bar.java"),
        )
        private fun createPathsFixtureStream(): Stream<Path> {
            return (ktPathsFixture + nestJavaPathsFixture).stream()
        }

        @Test
        fun emptyPath() {
            assertThrows<IllegalArgumentException>("Paths or glob is empty.") {
                resolvePathOrGlobList(listOf())
            }
        }

        @Test
        fun unixPaths() {
            val actual = resolvePathOrGlobList(listOf("sample/foo.kt", "sample/bar.kt"))
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
            val actual = resolvePathOrGlobList(listOf("sample\\foo.kt", "sample\\bar.kt"))
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
            every { filesWalk(rootDirPath) } answers { createPathsFixtureStream() }

            val actual = resolvePathOrGlobList(listOf("**/*.kt"))
            assertEquals(
                ktPathsFixture,
                actual
            )
        }

        @Test
        fun globMatchNoting() {
            val rootDirPath = Paths.get(".")
            mockkStatic(::filesWalk)
            every { filesWalk(rootDirPath) } answers { createPathsFixtureStream() }

            val actual = resolvePathOrGlobList(listOf("**/noting"))
            assertEquals(listOf(), actual)
        }

        @Test
        fun globMulti() {
            val rootDirPath = Paths.get(".")
            mockkStatic(::filesWalk)
            every { filesWalk(rootDirPath) } answers { createPathsFixtureStream() }

            val actual = resolvePathOrGlobList(listOf("**/*.kt", "**/*.java"))
            assertEquals(
                ktPathsFixture + nestJavaPathsFixture,
                actual
            )
        }

        @Test
        fun combinePathsAndGlobs() {
            val rootDirPath = Paths.get(".")
            mockkStatic(::filesWalk)
            every { filesWalk(rootDirPath) } answers { createPathsFixtureStream() }

            val actual = resolvePathOrGlobList(listOf("sample/foo.kt", "sample/bar.kt", "**/*.java"))
            assertEquals(
                ktPathsFixture + nestJavaPathsFixture,
                actual
            )
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

    @Nested
    class TestNormalizeGlob {
        @Test
        @DisplayName("./**/*")
        fun redundantPrefix() {
            val actual = normalizeGlob("./**/*")
            assertEquals("**/*", actual)
        }
    }
}
