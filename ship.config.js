module.exports = {
  // prepare phase
  installCommand: ({ isYarn }) => undefined, // disable
  versionUpdated: ({ version, releaseType, dir, exec }) => {
    // Update version var inside gradle script
    exec(`sed -iE "s/version = .+/version = \"${version}\"/" build.gradle.kts`)
  },
  // trigger phase
  buildCommand: () => './gradlew assembleArchive',
  publishCommand: () => '', // disable
  releases: [
    'build/libs/skw.jar',
    'build/distributions/skw.tar',
    'build/distributions/skw.zip'
  ]
};
