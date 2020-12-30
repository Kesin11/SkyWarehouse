import org.junit.jupiter.api.*

import org.junit.jupiter.api.Assertions.*
import java.nio.file.Paths

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
            assertEquals(listOf(
                Paths.get("sample", "foo.kt"),
                Paths.get("sample", "bar.kt")
            ), actual)
        }

        // TODO: globのケースはFile.walkをmockする
        // TODO: globが複数の場合はエラーにする
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
