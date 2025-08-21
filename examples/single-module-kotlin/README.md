# Single-Module Kotlin Example

This example demonstrates how to use the Central Portal Publisher plugin with a simple single-module Kotlin library project.

## Project Structure

```
single-module-kotlin/
├── build.gradle.kts          # Build configuration with Central Publisher plugin
├── settings.gradle.kts       # Project settings
├── gradle.properties         # Properties for credentials (git-ignored)
├── src/
│   ├── main/
│   │   └── kotlin/
│   │       └── com/example/
│   │           └── StringUtils.kt    # Example library code
│   └── test/
│       └── kotlin/
│           └── com/example/
│               └── StringUtilsTest.kt # Unit tests
└── README.md
```

## Features Demonstrated

1. **Simple DSL Configuration** - Shows the new simplified DSL for configuring publishing
2. **Auto-Detection** - Many fields can be auto-detected from git and project settings
3. **Smart Defaults** - Sensible defaults reduce configuration boilerplate
4. **Credential Management** - Safe handling of credentials via properties or environment variables
5. **Task Aliases** - Simplified task names for better discoverability

## Setup

### 1. Local Plugin Testing (Optional)

If you want to test with the local plugin development version:

1. Uncomment the line in `settings.gradle.kts`:
   ```kotlin
   includeBuild("../..")
   ```

2. Build the main plugin first:
   ```bash
   cd ../..
   ./gradlew publishToMavenLocal
   cd examples/single-module-kotlin
   ```

### 2. Configure Credentials

Add your Sonatype credentials to `~/.gradle/gradle.properties` (NOT in the project):

```properties
SONATYPE_USERNAME=your-username
SONATYPE_PASSWORD=your-password
SIGNING_KEY_ID=your-key-id
SIGNING_PASSWORD=your-signing-password
SIGNING_SECRET_KEY_RING_FILE=/path/to/secring.gpg
```

Or set them as environment variables:

```bash
export SONATYPE_USERNAME=your-username
export SONATYPE_PASSWORD=your-password
export SIGNING_KEY_ID=your-key-id
export SIGNING_PASSWORD=your-signing-password
```

### 3. Build the Project

```bash
./gradlew build
```

### 4. Run Tests

```bash
./gradlew test
```

### 5. Validate Publishing Configuration

```bash
./gradlew validatePublishing
```

### 6. Publish to Central Portal

#### Dry Run (Test without publishing)
```bash
./gradlew publishDryRun
```

#### Actual Publishing
```bash
./gradlew publishToCentral
```

## Key Configuration Points

### Minimal Configuration

The plugin uses smart defaults and auto-detection to minimize configuration:

```kotlin
centralPublisher {
    // Most fields are auto-detected or use smart defaults
    projectInfo {
        description = "Your library description"
        // name, url, scm info can be auto-detected
    }
    
    // Credentials from properties/environment
    credentials {
        username = project.findProperty("SONATYPE_USERNAME")?.toString() ?: ""
        password = project.findProperty("SONATYPE_PASSWORD")?.toString() ?: ""
    }
}
```

### Auto-Detection Features

The plugin automatically detects:
- Project name from `settings.gradle.kts`
- SCM URLs from git remote configuration
- Developer information from git config
- License from LICENSE file (if present)

### Smart Defaults

The plugin provides sensible defaults for:
- License (Apache 2.0)
- Publishing options (manual approval, aggregation enabled)
- Signing configuration (standard GPG paths)

## Troubleshooting

### Missing Credentials

If you see errors about missing credentials:
1. Check that credentials are in `~/.gradle/gradle.properties` or environment variables
2. Never commit credentials to the project repository
3. Use the setup wizard: `./gradlew setupPublishing`

### Validation Errors

Run validation to check configuration:
```bash
./gradlew validatePublishing
```

The plugin provides detailed error messages with suggested fixes.

### Dry Run Mode

Always test with dry run first:
```bash
./gradlew publishDryRun
```

This simulates the publishing process without actually uploading artifacts.

## Additional Resources

- [Central Portal Publisher Documentation](https://github.com/tddworks/central-portal-publisher)
- [Maven Central Portal](https://central.sonatype.com/)
- [Gradle Publishing Documentation](https://docs.gradle.org/current/userguide/publishing_maven.html)