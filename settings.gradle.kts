
rootProject.name = "skw"

plugins {
    id("io.perezalcolea.gcs-build-cache") version "0.1.0"
}

buildCache {
    val isCi = System.getenv().containsKey("CI")
    local {
        isEnabled = !isCi
    }

    val enableGoogleApplicationCredentials = System.getenv().containsKey("GOOGLE_APPLICATION_CREDENTIALS")
    remote<io.perezalcolea.gradle.caching.gcs.GCSBuildCache> {
        isEnabled = enableGoogleApplicationCredentials
        applicationName = "skw"
        bucket = "kesin11_bazel_cache"
        path = "gradle_cache"
        isPush = isCi
    }
}
