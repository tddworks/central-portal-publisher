# Central Portal Gradle Plugin
## https://central.sonatype.com/

![CI](https://github.com/tddworks/central-portal-publisher/actions/workflows/main.yml/badge.svg)
[![codecov](https://codecov.io/gh/tddworks/central-portal-publisher/graph/badge.svg?token=izDBfMwLY0)](https://codecov.io/gh/tddworks/central-portal-publisher)

A modern Gradle plugin for publishing to Maven Central via the Sonatype Central Portal. Features intelligent auto-detection, type-safe configuration, and an interactive setup wizard.

## Quick Start

### 🧙 Interactive Setup Wizard (Recommended)

The easiest way to get started is with the interactive setup wizard:

```kotlin
plugins {
    id("com.tddworks.central-publisher") version "0.0.6"
}
```

Then run the setup wizard:
```bash
./gradlew setupPublishing
```

The wizard will:
- ✅ **Auto-detect** project information from git
- ✅ **Auto-detect** existing environment variables (recommended for security)
- ✅ **Guide you** through credentials and GPG signing setup
- ✅ **Generate** all necessary configuration files
- ✅ **Explain** security best practices

#### Environment Variable Auto-Detection

For best security, the wizard automatically detects and uses environment variables:

```bash
# Credentials (detected automatically)
export SONATYPE_USERNAME=your-username
export SONATYPE_PASSWORD=your-password

# GPG Signing (detected automatically) 
export SIGNING_KEY="-----BEGIN PGP PRIVATE KEY BLOCK-----..."
export SIGNING_PASSWORD=your-gpg-password
```

If found, the wizard will show:
```
📋 CREDENTIALS SETUP - AUTO-DETECTED!
✅ Found existing environment variables:
• SONATYPE_USERNAME: your-username
• SONATYPE_PASSWORD: ********

Using these existing credentials.
```

## Usage

### Manual Configuration

If you prefer manual setup, you can configure the plugin directly:

#### build.gradle.kts
```kotlin
plugins {
    id("com.tddworks.central-portal-publisher") version "0.2.0-alpha"
}

centralPublisher {
    // Credentials from environment variables (recommended)
    credentials {
        username = project.findProperty("SONATYPE_USERNAME")?.toString() ?: ""
        password = project.findProperty("SONATYPE_PASSWORD")?.toString() ?: ""
    }
    
    // Project info (many fields auto-detected from git)
    projectInfo {
        name = project.name
        description = "Description of your project"
        url = "https://github.com/yourorg/${project.name}"
        
        license {
            name = "Apache License 2.0"
            url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
        }
        
        developer {
            id = "yourid"
            name = "Your Name"
            email = "your.email@example.com"
        }
        
        scm {
            url = "https://github.com/yourorg/${project.name}"
            connection = "scm:git:git://github.com/yourorg/${project.name}.git"
            developerConnection = "scm:git:ssh://github.com/yourorg/${project.name}.git"
        }
    }
    
    // GPG signing (also supports environment variables)
    signing {
        keyId = project.findProperty("SIGNING_KEY_ID")?.toString() ?: ""
        password = project.findProperty("SIGNING_PASSWORD")?.toString() ?: ""
        secretKeyRingFile = project.findProperty("SIGNING_SECRET_KEY_RING_FILE")?.toString() ?: ""
    }
    
    // Publishing options
    publishing {
        autoPublish = false // Manual approval (default, safer)
        aggregation = true  // Bundle multiple modules (default)
        dryRun = false     // Set true to test without publishing
    }
}
```

## Available Tasks

Once configured, the plugin provides these tasks:

- **`setupPublishing`** - Interactive setup wizard (recommended for first-time setup)
- **`publishToCentral`** - Publish all artifacts to Maven Central
- **`bundleArtifacts`** - Create deployment bundle for Maven Central  
- **`validatePublishing`** - Validate configuration without publishing

Example workflow:
```bash
# First-time setup
./gradlew setupPublishing

# Validate your configuration
./gradlew validatePublishing  

# Test bundle creation (recommended)
./gradlew bundleArtifacts

# Publish to Maven Central
./gradlew publishToCentral
```

## Configuration Options

### Environment Variables (Recommended)

The most secure approach uses environment variables (automatically detected by the setup wizard):

```bash
# Sonatype credentials
export SONATYPE_USERNAME=your-username
export SONATYPE_PASSWORD=your-password

# GPG signing (get your private key with: gpg --armor --export-secret-keys email@example.com)
export SIGNING_KEY="-----BEGIN PGP PRIVATE KEY BLOCK-----
...your full private key here...
-----END PGP PRIVATE KEY BLOCK-----"
export SIGNING_PASSWORD=your-gpg-password
```

### Global gradle.properties

Alternative: add to `~/.gradle/gradle.properties` (applies to all projects):

```properties
SONATYPE_USERNAME=your-username
SONATYPE_PASSWORD=your-password

SIGNING_KEY=-----BEGIN PGP PRIVATE KEY BLOCK-----\n...\n-----END PGP PRIVATE KEY BLOCK-----
SIGNING_PASSWORD=your-gpg-password
```

### Local gradle.properties (Not Recommended)

Only use for local development, **never commit to git**:

```properties
# WARNING: Do not commit this file to version control!
SONATYPE_USERNAME=your-username
SONATYPE_PASSWORD=your-password

SIGNING_KEY=-----BEGIN PGP PRIVATE KEY BLOCK-----
# ... your full PGP private key here ...
-----END PGP PRIVATE KEY BLOCK-----
SIGNING_PASSWORD=your-gpg-password
```

## Legacy Configuration (Deprecated)

> **Note:** The old `sonatypePortalPublisher {}` DSL is deprecated. Use `centralPublisher {}` with the setup wizard for the best experience.

For existing projects, you can migrate by running:
```bash
./gradlew setupPublishing
```

## Project Types Supported

### Single Module Projects

The plugin automatically detects single-module projects and configures publishing accordingly:

```kotlin
centralPublisher {
    // Configuration as shown above
    publishing {
        aggregation = true  // Still useful for consistent behavior
    }
}
```

### Multi-Module Projects

The plugin automatically detects and supports multi-module projects with two publishing strategies:

#### Individual Module Publishing

Each module is published separately (useful for independent versioning):

```kotlin
centralPublisher {
    publishing {
        aggregation = false  // Each module publishes independently
        autoPublish = false  // Manual approval for each module
    }
}
```

#### Aggregated Publishing (Recommended)

All modules are bundled into a single deployment (simpler, faster):

```kotlin
centralPublisher {
    publishing {
        aggregation = true   // Bundle all modules together (default)
        autoPublish = false  // Manual approval for entire bundle
    }
}
```

With aggregation enabled, you'll get a single deployment bundle containing all modules:
```
build/central-portal/project-name-1.0.0-bundle.zip
```

### Kotlin Multiplatform (KMP) Projects

Full support for KMP projects with all target publications:

```kotlin
kotlin {
    jvm()
    js(IR) { browser(); nodejs() }
    linuxX64()
    macosX64() 
    macosArm64()
    // ... other targets
}

centralPublisher {
    // Same configuration as above
    // Plugin automatically detects and includes all platform publications
}
```

The generated bundle includes all platform-specific artifacts with proper Maven repository layout.

## Supported Features

- ✅ **Auto-detection**: Project info, git repository, developer information
- ✅ **Multiple project types**: Single-module, multi-module, Kotlin Multiplatform  
- ✅ **Flexible authentication**: Environment variables, gradle.properties, or DSL
- ✅ **GPG signing**: In-memory keys, file-based, or environment variables
- ✅ **Bundle generation**: Proper Maven repository layout with checksums and signatures
- ✅ **Validation**: Configuration validation before publishing
- ✅ **CI/CD friendly**: Works great with GitHub Actions, Jenkins, etc.
- ✅ **Interactive setup**: Guided wizard for first-time setup

## CI/CD Integration

### GitHub Actions

The plugin works seamlessly with GitHub Actions. Here's a complete workflow example:

1. **Add secrets to your GitHub repository**:
   - `SONATYPE_USERNAME` - Your Sonatype username  
   - `SONATYPE_PASSWORD` - Your Sonatype password/token
   - `SIGNING_KEY` - Your GPG private key 
   - `SIGNING_PASSWORD` - Your GPG key password

2. **Create `.github/workflows/publish.yml`**:

```yaml
name: Publish to Maven Central

on:
  push:
    tags:
      - 'v*'  # Trigger on version tags

jobs:
  publish:
    runs-on: ubuntu-latest
    
    environment: publishing  # Optional: use GitHub environments for additional protection
    
    steps:
      - name: Checkout sources
        uses: actions/checkout@v4
        
      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
          
      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v3
        
      - name: Validate configuration  
        run: ./gradlew validatePublishing
        env:
          SONATYPE_USERNAME: ${{ secrets.SONATYPE_USERNAME }}
          SONATYPE_PASSWORD: ${{ secrets.SONATYPE_PASSWORD }}
          SIGNING_KEY: ${{ secrets.SIGNING_KEY }}
          SIGNING_PASSWORD: ${{ secrets.SIGNING_PASSWORD }}
        
      - name: Publish to Maven Central
        run: ./gradlew publishToCentral
        env:
          SONATYPE_USERNAME: ${{ secrets.SONATYPE_USERNAME }}
          SONATYPE_PASSWORD: ${{ secrets.SONATYPE_PASSWORD }}
          SIGNING_KEY: ${{ secrets.SIGNING_KEY }}
          SIGNING_PASSWORD: ${{ secrets.SIGNING_PASSWORD }}
```

The setup wizard can generate this workflow file for you automatically!


# Maven Repository Layout

```shell
$ tree .
└─ com
   └─ sonatype
      └─ central
         └─ example
            └─ example_java_project
               └─ 0.1.0
                   ├── example_java_project-0.1.0-javadoc.jar
                   ├── example_java_project-0.1.0-javadoc.jar.asc
                   ├── example_java_project-0.1.0-javadoc.jar.md5
                   ├── example_java_project-0.1.0-javadoc.jar.sha1
                   ├── example_java_project-0.1.0-sources.jar
                   ├── example_java_project-0.1.0-sources.jar.asc
                   ├── example_java_project-0.1.0-sources.jar.md5
                   ├── example_java_project-0.1.0-sources.jar.sha1
                   ├── example_java_project-0.1.0.jar
                   ├── example_java_project-0.1.0.jar.asc
                   ├── example_java_project-0.1.0.jar.md5
                   ├── example_java_project-0.1.0.jar.sha1
                   ├── example_java_project-0.1.0.pom
                   ├── example_java_project-0.1.0.pom.asc
                   ├── example_java_project-0.1.0.pom.md5
                   └── example_java_project-0.1.0.pom.sha1

```
### Q&A
[Does the Portal support SNAPSHOT releases?](https://central.sonatype.org/faq/snapshot-releases/#releasing-to-central)
Does the Portal support SNAPSHOT releases?⚓︎
Question⚓︎
Does the Central Portal support -SNAPSHOT releases?

Answer⚓︎
Historically, users have been able to publish -SNAPSHOT releases to OSSRH (but not to Maven Central). The intention of this was to allow users to verify their own releases prior to publishing to Maven Central, but it has the side effect of users being able to make their pre-release versions available to their communities during ongoing development.

The Central Publisher Portal does not support -SNAPSHOT releases, and deployments of -SNAPSHOT releases will have a version cannot be a SNAPSHOT error in their validation results. Versions should only be published to the Portal if they are intended to reach Maven Central.

The Portal supports a more limited feature that is intended to fill the original need for publishers to be able to perform manual verification of their builds. Publishers will be able to point their build configurations to either specific deployments or any VALIDATED deployment that they published in order to use the component before publishing. This will be particularly useful for publishers who publish via CI pipelines, but want to verify locally.

Alternative Options⚓︎
If you are looking for a solution that is able to handle -SNAPSHOT releases for your project, consider Sonatype Nexus Repository which supports maven-snapshots as a repository out of the box