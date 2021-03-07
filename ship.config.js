module.exports = {
  // prepare phase
  installCommand: ({ isYarn }) => 'true', // disable
  versionUpdated: ({ version, releaseType, dir, exec }) => {
    // Update version var inside gradle script
    exec(`sed -i -E 's/version = .+/version = "${version}"/' build.gradle.kts`)
  },
  // trigger phase
  buildCommand: () => 'true', // disable
  publishCommand: () => 'true', // disable
};
