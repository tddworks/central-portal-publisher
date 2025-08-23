# Task Reference

This page provides a comprehensive reference for all tasks provided by the Central Portal Publisher plugin.

## Main Tasks

### setupPublishing

**Interactive setup wizard for first-time configuration.**

```bash
./gradlew setupPublishing
```

**What it does:**
- Auto-detects project information from git repository
- Detects existing environment variables for credentials and signing
- Guides you through configuration setup
- Generates necessary configuration files
- Optionally creates GitHub Actions workflow

**Options:**
- `--non-interactive` - Run in batch mode with defaults
- `--force` - Overwrite existing configuration

**Example:**
```bash
# Interactive mode (default)
./gradlew setupPublishing

# Non-interactive with defaults
./gradlew setupPublishing --non-interactive

# Force overwrite existing config
./gradlew setupPublishing --force
```

### publishToCentral

**Publish all artifacts to the Sonatype Central Portal.**

```bash
./gradlew publishToCentral
```

**What it does:**
- Creates deployment bundle with all artifacts
- Signs all artifacts with GPG
- Uploads bundle to Central Portal
- Returns deployment ID for tracking

**Prerequisites:**
- Valid configuration (run `validatePublishing` first)
- Credentials configured
- GPG signing configured

**Example output:**
```
> Task :publishToCentral
📦 Creating deployment bundle...
✅ Bundle created: my-project-1.0.0-bundle.zip
🔐 Signing artifacts...
🚀 Uploading to Central Portal...
✅ Upload successful!
📋 Deployment ID: 12345678-abcd-ef90-1234-567890abcdef

Visit https://central.sonatype.com/publishing/deployments to review and publish.
```

### validatePublishing

**Validate configuration without publishing.**

```bash
./gradlew validatePublishing
```

**What it does:**
- Validates all configuration settings
- Checks credentials (without uploading)
- Verifies GPG signing setup
- Confirms publications are available
- Reports any configuration issues

**Example output:**
```
> Task :validatePublishing
✅ Configuration validation successful
✅ Credentials verified
✅ GPG signing key validated
✅ Publications found: maven, jvm, js, linuxX64
✅ Ready to publish!
```

### bundleArtifacts

**Create deployment bundle without uploading.**

```bash
./gradlew bundleArtifacts
```

**What it does:**
- Generates all artifacts (JARs, sources, javadoc)
- Signs artifacts with GPG
- Creates Maven repository layout
- Packages everything into deployment ZIP
- Stores bundle in `build/central-portal/`

**Useful for:**
- Testing bundle creation
- Inspecting artifacts before publishing
- Manual upload to Central Portal

**Output location:**
```
build/central-portal/
├── project-name-1.0.0-bundle.zip
└── repository/                    # Unpacked Maven layout
    └── com/example/project/1.0.0/
        ├── project-1.0.0.jar
        ├── project-1.0.0-sources.jar
        ├── project-1.0.0-javadoc.jar
        ├── project-1.0.0.pom
        └── signatures and checksums...
```

## Module-Specific Tasks

For multi-module projects with aggregation disabled, you can run tasks on individual modules:

### Module Validation

```bash
./gradlew :module-name:validatePublishing
```

### Module Publishing

```bash
./gradlew :module-name:publishToCentral
```

### Module Bundle Creation

```bash
./gradlew :module-name:bundleArtifacts
```

## Configuration Tasks

### showConfiguration

**Display current plugin configuration.**

```bash
./gradlew showConfiguration
```

**What it shows:**
- Resolved credentials (masked)
- Project information
- Signing configuration
- Publishing options
- Auto-detected values

### detectConfiguration

**Run auto-detection and display results.**

```bash
./gradlew detectConfiguration
```

**What it does:**
- Runs git repository detection
- Detects project information
- Shows what would be auto-configured
- Useful for troubleshooting detection issues

## Dry Run Mode

All publishing tasks support dry run mode through configuration:

```kotlin
centralPublisher {
    publishing {
        dryRun = true
    }
}
```

Or via command line property:
```bash
./gradlew publishToCentral -PdryRun=true
```

**In dry run mode:**
- Bundles are created but not uploaded
- Configuration is validated
- Credentials are checked (no upload)
- Perfect for testing CI/CD pipelines

## Task Dependencies

Understanding task dependencies helps with troubleshooting:

```
publishToCentral
├── validatePublishing
├── bundleArtifacts
│   ├── jar (or equivalent publication tasks)
│   ├── sourcesJar
│   ├── javadocJar
│   └── signMavenPublication
└── uploadToPortal
```

## Error Handling and Debugging

### Verbose Output

Add `--info` or `--debug` for detailed logging:

```bash
./gradlew publishToCentral --info
```

### Common Task Failures

#### validatePublishing fails

**Symptoms:**
```
> Configuration validation failed
> Missing required field: projectInfo.description
```

**Solutions:**
1. Check your `centralPublisher` configuration
2. Ensure all required fields are set
3. Verify environment variables are available

#### bundleArtifacts fails

**Symptoms:**
```
> No publications found for project
```

**Solutions:**
1. Ensure you have `maven-publish` plugin applied
2. For Java projects, apply `java-library` plugin
3. For Kotlin projects, apply appropriate Kotlin plugin

#### publishToCentral fails

**Symptoms:**
```
> Upload failed: Authentication failed (401)
```

**Solutions:**
1. Verify `SONATYPE_USERNAME` and `SONATYPE_PASSWORD`
2. Ensure you're using a Central Portal token, not OSSRH password
3. Check that your Central Portal account is verified

**Symptoms:**
```
> GPG signing failed
```

**Solutions:**
1. Verify `SIGNING_KEY` contains complete private key
2. Check `SIGNING_PASSWORD` is correct
3. Ensure key is not expired: `gpg --list-keys`

## Performance Optimization

### Parallel Execution

Enable parallel builds for faster execution:

```properties
# gradle.properties
org.gradle.parallel=true
org.gradle.caching=true
```

### Build Cache

The plugin supports Gradle build cache:

```bash
./gradlew publishToCentral --build-cache
```

### Task Output Caching

Most plugin tasks support incremental builds and output caching automatically.

## Integration with Other Plugins

### Version Catalog Updates

```bash
# Update dependencies first
./gradlew versionCatalogUpdate

# Then publish with latest versions
./gradlew publishToCentral
```

### Code Quality Checks

```bash
# Run quality checks before publishing
./gradlew check publishToCentral
```

### Documentation Generation

```bash
# Generate docs and publish together
./gradlew dokkaHtml publishToCentral
```

## Troubleshooting Task Issues

### Task Not Found

If tasks are not available:

1. Ensure plugin is applied correctly:
   ```kotlin
   plugins {
       id("com.tddworks.central-publisher") version "0.2.1-alpha"
   }
   ```

2. Check plugin application in multi-module projects (apply to root)

3. Verify Gradle version compatibility (minimum 7.0)

### Task Skipped

If tasks show as `UP-TO-DATE` or `SKIPPED`:

1. Use `--rerun-tasks` to force execution:
   ```bash
   ./gradlew publishToCentral --rerun-tasks
   ```

2. Check if configuration has changed

3. Verify input/output dependencies

### Task Timeout

For long-running uploads:

```kotlin
// In build.gradle.kts
tasks.withType<com.tddworks.sonatype.publish.portal.plugin.tasks.PublishTask> {
    timeout.set(Duration.ofMinutes(30)) // Extend timeout
}
```

## Task Customization

### Custom Task Configuration

```kotlin
// Customize bundleArtifacts task
tasks.named("bundleArtifacts") {
    doLast {
        println("Bundle created at: ${project.buildDir}/central-portal/")
    }
}

// Customize publishToCentral task
tasks.named("publishToCentral") {
    doLast {
        println("Published successfully!")
    }
}
```

### Task Ordering

```kotlin
// Ensure tests run before publishing
tasks.named("publishToCentral") {
    dependsOn("test")
}
```

## Next Steps

- Learn about [troubleshooting common issues](troubleshooting.md)
- See [migration guide](migration.md) for upgrading
- Check [API reference](../api/index.md) for programmatic usage