# Single-Module Kotlin Example

This example shows how to use the Central Portal Publisher plugin with a simple Kotlin library. The plugin provides zero-configuration publishing to Maven Central with an interactive setup wizard.

## Project Structure

```
single-module-kotlin/
├── build.gradle.kts          # Build configuration with Central Publisher plugin
├── settings.gradle.kts       # Project settings
├── src/
│   ├── main/kotlin/          # Library source code
│   │   └── com/example/StringUtils.kt
│   └── test/kotlin/          # Unit tests
│       └── com/example/StringUtilsTest.kt
└── README.md
```

## Quick Start

### 1. Run Setup Wizard (Recommended)

```bash
./gradlew setupPublishing --console=plain
```

The wizard will guide you through:
- Setting up your Sonatype credentials 
- Configuring project information (auto-detected from git)
- Setting up GPG signing (optional)
- Generating configuration files

### 2. Manual Setup

If you prefer manual setup, add your credentials to `~/.gradle/gradle.properties`:

```properties
SONATYPE_USERNAME=your-username
SONATYPE_PASSWORD=your-token
```

For signed releases (optional):
```properties
SIGNING_KEY=-----BEGIN PGP PRIVATE KEY BLOCK-----
...your key here...
-----END PGP PRIVATE KEY BLOCK-----
SIGNING_PASSWORD=your-signing-password
```

### 3. Build and Test

```bash
# Build the library
./gradlew build

# Run tests
./gradlew test

# Validate configuration
./gradlew validatePublishing
```

### 4. Publish to Maven Central

```bash
# Test bundle creation
./gradlew bundleArtifacts

# Publish to Maven Central
./gradlew publishToCentral
```

## How It Works

### Zero-Configuration Publishing

The plugin automatically configures everything needed for Maven Central:

```kotlin
plugins {
    kotlin("jvm")
    id("com.tddworks.central-publisher")
}

centralPublisher {
    credentials {
        username = project.findProperty("SONATYPE_USERNAME")?.toString() ?: ""
        password = project.findProperty("SONATYPE_PASSWORD")?.toString() ?: ""
    }
    
    projectInfo {
        name = project.name                    // Auto-detected
        description = "A useful Kotlin library"
        url = "https://github.com/yourorg/yourproject"  // Auto-detected from git
        
        license {
            name = "Apache License 2.0"
            url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
        }
        
        developer {
            id = "yourid"
            name = "Your Name"              // Auto-detected from git
            email = "your.email@example.com"
        }
        
        scm {
            url = "https://github.com/yourorg/yourproject"  // Auto-detected
            connection = "scm:git:git://github.com/yourorg/yourproject.git"
            developerConnection = "scm:git:ssh://github.com/yourorg/yourproject.git"
        }
    }
}
```

### What Gets Auto-Configured

The plugin automatically sets up:
- ✅ Maven publication with JAR, sources, and javadoc
- ✅ POM file with all required Maven Central metadata
- ✅ GPG signing (if credentials provided)
- ✅ Deployment bundle creation
- ✅ Integration with Sonatype Central Portal

### Auto-Detection Features

The setup wizard automatically detects:
- Project name from `settings.gradle.kts`
- Git repository URLs for SCM configuration
- Developer name and email from git config
- Existing environment variables for credentials

## Available Tasks

- `./gradlew setupPublishing --console=plain` - Interactive setup wizard
- `./gradlew validatePublishing` - Validate configuration
- `./gradlew bundleArtifacts` - Create deployment bundle
- `./gradlew publishToCentral` - Publish to Maven Central

## Configuration Options

### Security Best Practices

The setup wizard prefers environment variables for credentials:

```bash
export SONATYPE_USERNAME=your-username
export SONATYPE_PASSWORD=your-token
export SIGNING_KEY="-----BEGIN PGP PRIVATE KEY BLOCK-----..."
export SIGNING_PASSWORD=your-signing-password
```

### Publishing Options

```kotlin
centralPublisher {
    publishing {
        autoPublish = false  // Manual approval (safer)
        dryRun = false      // Set true to test without publishing
    }
}
```

## Troubleshooting

### Invalid Group ID

Update your group ID to use your own domain:

```kotlin
group = "com.yourcompany.yourproject"  // Not com.example
version = "1.0.0"
```

### Missing Credentials

If you see credential errors:
1. Run the setup wizard: `./gradlew setupPublishing --console=plain`
2. Ensure credentials are in `~/.gradle/gradle.properties` or environment variables
3. Never commit credentials to git

### Validation Errors

Run validation to see detailed error messages:
```bash
./gradlew validatePublishing
```

The plugin provides actionable feedback for any configuration issues.

### Bundle Creation Issues

Test bundle creation before publishing:
```bash
./gradlew bundleArtifacts
```

This creates a ZIP file in `build/central-portal/` that would be uploaded to Maven Central.

## CI/CD Integration

For GitHub Actions:

```yaml
- name: Publish to Maven Central
  env:
    SONATYPE_USERNAME: ${{ secrets.SONATYPE_USERNAME }}
    SONATYPE_PASSWORD: ${{ secrets.SONATYPE_PASSWORD }}
    SIGNING_KEY: ${{ secrets.SIGNING_KEY }}
    SIGNING_PASSWORD: ${{ secrets.SIGNING_PASSWORD }}
  run: ./gradlew publishToCentral
```

## Learn More

- [Plugin Documentation](https://github.com/tddworks/central-portal-publisher)
- [Maven Central Guide](https://central.sonatype.org/publish/publish-guide/)
- [Setting up GPG](https://central.sonatype.org/publish/requirements/gpg/)