# Multi-Module Kotlin Example

This example shows how to use the Central Portal Publisher plugin with a multi-module Kotlin project. All modules are bundled together and published as one deployment to Maven Central.

## Project Structure

```
multi-module-kotlin/
├── build.gradle.kts           # Root project with Central Publisher plugin
├── core/                      # Core module 
│   └── build.gradle.kts       # Applies maven-publish (opt-in)
├── api/                       # API module (depends on core)
│   └── build.gradle.kts       # Applies maven-publish (opt-in)  
└── client/                    # Client module (depends on api)
    └── build.gradle.kts       # Applies maven-publish (opt-in)
```

**Dependencies:** `client → api → core`

## Quick Start

### 1. Run Setup Wizard (Recommended)

```bash
./gradlew setupPublishing --console=plain
```

The wizard will guide you through:
- Setting up your Sonatype credentials
- Configuring project information
- Setting up GPG signing (optional)

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
# Build all modules
./gradlew build

# Test configuration
./gradlew validatePublishing

# Create bundle (test deployment)
./gradlew bundleArtifacts
```

### 4. Publish to Maven Central

```bash
./gradlew publishToCentral
```

## How It Works

### Zero-Boilerplate Configuration

The plugin automatically detects modules that apply `maven-publish` and configures them for publishing:

**Root project configures the plugin once:**
```kotlin
plugins {
    id("com.tddworks.central-publisher")
}

centralPublisher {
    credentials {
        username = project.findProperty("SONATYPE_USERNAME")?.toString() ?: ""
        password = project.findProperty("SONATYPE_PASSWORD")?.toString() ?: ""
    }
    
    projectInfo {
        name = "multi-module-example"
        description = "Example multi-module project"
        url = "https://github.com/tddworks/central-portal-publisher"
        // ... license, developer, SCM info
    }
}
```

**Submodules just opt-in to publishing:**
```kotlin
plugins {
    kotlin("jvm")
    `maven-publish` // This is all you need!
}
```

### Aggregated Publishing

With `aggregation = true` (the default), all modules are bundled into a single deployment:
- Simplifies the publishing process
- All modules are published together
- Single deployment bundle contains all artifacts

### What Gets Auto-Configured

For each module with `maven-publish` plugin:
- ✅ Maven publication with JAR, sources, and javadoc
- ✅ POM configuration inherited from root project  
- ✅ GPG signing (if credentials provided)
- ✅ Integration with bundle creation

## Available Tasks

- `./gradlew setupPublishing --console=plain` - Interactive setup wizard
- `./gradlew validatePublishing` - Validate configuration  
- `./gradlew bundleArtifacts` - Create deployment bundle
- `./gradlew publishToCentral` - Publish to Maven Central

## Configuration

The root project's `centralPublisher` configuration applies to all modules. Key settings:

```kotlin
centralPublisher {
    publishing {
        aggregation = true   // Bundle all modules (default)
        autoPublish = false  // Manual approval (safer)
        dryRun = false      // Set true to test
    }
}
```

## Excluding Modules

To exclude a module from publishing, simply don't apply the `maven-publish` plugin:

```kotlin
// This module won't be published
plugins {
    kotlin("jvm")
    // No maven-publish = not included in bundle
}
```

## Troubleshooting

### Module Not Getting Published

Check that the module:
1. Has `maven-publish` plugin in its `build.gradle.kts`
2. Builds successfully: `./gradlew :module:build`

### Invalid Group ID

Update the group in root `build.gradle.kts`:
```kotlin
allprojects {
    group = "com.yourcompany.yourproject"  // Use your domain
    version = "1.0.0"
}
```

### Bundle Creation Fails

The bundle creation will fail if modules haven't been built. Run:
```bash
./gradlew publishToLocalRepo
```

This publishes all modules to a local repository that's used for bundle creation.

## CI/CD

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