# Configuration Guide

This guide covers all configuration options for the Central Portal Publisher plugin.

## Basic Configuration

The plugin uses the `centralPublisher` extension for configuration:

```kotlin
centralPublisher {
    credentials {
        username = project.findProperty("SONATYPE_USERNAME")?.toString() ?: ""
        password = project.findProperty("SONATYPE_PASSWORD")?.toString() ?: ""
    }
    
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
    
    signing {
        keyId = project.findProperty("SIGNING_KEY_ID")?.toString() ?: ""
        password = project.findProperty("SIGNING_PASSWORD")?.toString() ?: ""
        secretKeyRingFile = project.findProperty("SIGNING_SECRET_KEY_RING_FILE")?.toString() ?: ""
    }
    
    publishing {
        autoPublish = false
        aggregation = true
        dryRun = false
    }
}
```

## Configuration Sections

### Credentials

Configure your Sonatype Central Portal credentials:

```kotlin
credentials {
    username = "your-username"           // Your Sonatype account username
    password = "your-token"              // Your Sonatype account token (not password!)
}
```

!!! tip "Security Best Practice"
    Always use environment variables or gradle.properties for credentials. Never hardcode them in your build script.

### Project Information

Basic project metadata required for Maven Central:

```kotlin
projectInfo {
    name = "my-library"                                    // Project name
    description = "A useful library for doing things"     // Project description
    url = "https://github.com/myorg/my-library"          // Project homepage
    
    license {
        name = "Apache License 2.0"                       // License name
        url = "https://www.apache.org/licenses/LICENSE-2.0.txt"  // License URL
    }
    
    developer {
        id = "myid"                                        // Developer ID
        name = "My Name"                                   // Developer name
        email = "me@example.com"                          // Developer email
        organization = "My Organization"                   // Optional: organization
        organizationUrl = "https://myorg.com"             // Optional: org URL
    }
    
    scm {
        url = "https://github.com/myorg/my-library"
        connection = "scm:git:git://github.com/myorg/my-library.git"
        developerConnection = "scm:git:ssh://github.com/myorg/my-library.git"
    }
}
```

!!! note "Auto-Detection"
    The setup wizard can auto-detect many of these fields from your git repository and existing configuration.

### GPG Signing

Configure GPG signing for your artifacts:

#### Option 1: In-Memory Key (Recommended)

```kotlin
signing {
    key = "-----BEGIN PGP PRIVATE KEY BLOCK-----\n...\n-----END PGP PRIVATE KEY BLOCK-----"
    password = "your-gpg-password"
}
```

#### Option 2: Key File

```kotlin
signing {
    keyId = "your-key-id"                    // GPG key ID (last 8 characters)
    password = "your-gpg-password"           // GPG key password  
    secretKeyRingFile = "/path/to/secring.gpg"  // Path to secret keyring file
}
```

### Publishing Options

Configure publishing behavior:

```kotlin
publishing {
    autoPublish = false      // Auto-publish after upload (default: false, manual approval)
    aggregation = true       // Bundle multi-module projects (default: true)
    dryRun = false          // Test mode without actual publishing (default: false)
}
```

#### Publishing Options Explained

- **`autoPublish`**: If `true`, automatically publishes artifacts after successful upload. If `false` (recommended), artifacts are uploaded but require manual approval in the Central Portal.

- **`aggregation`**: For multi-module projects:
  - `true` (default): All modules are bundled into a single deployment
  - `false`: Each module is published separately

- **`dryRun`**: If `true`, validates configuration and creates bundles but doesn't upload to Central Portal.

## Configuration Sources

The plugin supports multiple configuration sources with the following precedence (highest to lowest):

1. **Environment Variables** (highest priority, most secure)
2. **Global gradle.properties** (`~/.gradle/gradle.properties`)  
3. **Local gradle.properties** (project root)
4. **Build script DSL** (lowest priority)

### Environment Variables

```bash
# Credentials
export SONATYPE_USERNAME=your-username
export SONATYPE_PASSWORD=your-password

# GPG Signing
export SIGNING_KEY="-----BEGIN PGP PRIVATE KEY BLOCK-----..."
export SIGNING_PASSWORD=your-gpg-password

# Or traditional GPG setup
export SIGNING_KEY_ID=your-key-id
export SIGNING_SECRET_KEY_RING_FILE=/path/to/secring.gpg
```

### Global gradle.properties

Add to `~/.gradle/gradle.properties` (applies to all projects):

```properties
SONATYPE_USERNAME=your-username
SONATYPE_PASSWORD=your-password

SIGNING_KEY=-----BEGIN PGP PRIVATE KEY BLOCK-----\n...\n-----END PGP PRIVATE KEY BLOCK-----
SIGNING_PASSWORD=your-gpg-password
```

### Local gradle.properties

Add to your project's `gradle.properties` (⚠️ **never commit to version control**):

```properties
# WARNING: Do not commit this file!
SONATYPE_USERNAME=your-username
SONATYPE_PASSWORD=your-password

SIGNING_KEY=-----BEGIN PGP PRIVATE KEY BLOCK-----
# ... your full PGP private key here ...
-----END PGP PRIVATE KEY BLOCK-----
SIGNING_PASSWORD=your-gpg-password
```

## Multi-Module Configuration

For multi-module projects, apply the plugin to the root project:

```kotlin
// root build.gradle.kts
plugins {
    id("com.tddworks.central-publisher") version "0.2.1-alpha"
}

centralPublisher {
    // Configuration as shown above
    
    publishing {
        aggregation = true  // Recommended: bundle all modules together
    }
}

// subproject build.gradle.kts files
plugins {
    `java-library`  // or kotlin-jvm, etc.
    `maven-publish` // Required for publication
}
```

### Individual Module Publishing

If you prefer to publish modules separately:

```kotlin
centralPublisher {
    publishing {
        aggregation = false  // Each module publishes independently
    }
}
```

With this configuration, you can publish specific modules:
```bash
./gradlew :module-a:publishToCentral
./gradlew :module-b:publishToCentral
```

## Kotlin Multiplatform Configuration

For Kotlin Multiplatform projects, the plugin automatically detects all targets:

```kotlin
// No special configuration needed!
kotlin {
    jvm()
    js(IR) { browser(); nodejs() }
    linuxX64()
    macosX64() 
    macosArm64()
    // ... other targets
}

centralPublisher {
    // Standard configuration
}
```

The plugin will automatically include all platform publications in the deployment bundle.

## Validation

The plugin validates your configuration before publishing. Common validation errors:

- **Missing required fields**: Ensure all required projectInfo fields are set
- **Invalid credentials**: Check your Sonatype username and token
- **GPG signing issues**: Verify your GPG key format and password
- **No publications found**: Ensure you've applied appropriate plugins (`java-library`, `kotlin-jvm`, etc.)

Run validation manually:
```bash
./gradlew validatePublishing
```

## Legacy Configuration Migration

If you're migrating from the old `sonatypePortalPublisher` DSL:

```kotlin
// Old (deprecated)
sonatypePortalPublisher {
    // ...
}

// New (recommended)  
centralPublisher {
    // Same configuration structure
}
```

The setup wizard can help migrate your configuration:
```bash
./gradlew setupPublishing
```