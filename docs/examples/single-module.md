# Single Module Project Example

This guide shows how to set up the Central Portal Publisher plugin for a single-module Java or Kotlin project.

## Project Structure

```
my-library/
├── build.gradle.kts
├── gradle.properties
├── settings.gradle.kts
└── src/
    ├── main/
    │   └── kotlin/
    │       └── com/example/MyLibrary.kt
    └── test/
        └── kotlin/
            └── com/example/MyLibraryTest.kt
```

## Complete Example

### build.gradle.kts

```kotlin
plugins {
    `kotlin-jvm`
    `java-library`
    `maven-publish`
    id("com.tddworks.central-publisher") version "0.2.1-alpha"
}

group = "com.example"
version = "1.0.0"

java {
    withJavadocJar()
    withSourcesJar()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
}

centralPublisher {
    // Credentials from environment variables (recommended)
    credentials {
        username = project.findProperty("SONATYPE_USERNAME")?.toString() ?: ""
        password = project.findProperty("SONATYPE_PASSWORD")?.toString() ?: ""
    }
    
    // Project information
    projectInfo {
        name = "my-awesome-library"
        description = "An awesome Kotlin library that does amazing things"
        url = "https://github.com/myorg/my-awesome-library"
        
        license {
            name = "Apache License 2.0"
            url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
        }
        
        developer {
            id = "myusername"
            name = "My Name"
            email = "me@example.com"
        }
        
        scm {
            url = "https://github.com/myorg/my-awesome-library"
            connection = "scm:git:git://github.com/myorg/my-awesome-library.git"
            developerConnection = "scm:git:ssh://github.com/myorg/my-awesome-library.git"
        }
    }
    
    // GPG signing
    signing {
        key = project.findProperty("SIGNING_KEY")?.toString() ?: ""
        password = project.findProperty("SIGNING_PASSWORD")?.toString() ?: ""
    }
    
    // Publishing options
    publishing {
        autoPublish = false // Manual approval (safer)
        dryRun = false     // Set to true for testing
    }
}

tasks.test {
    useJUnitPlatform()
}
```

### gradle.properties

```properties
# Project metadata
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
org.gradle.caching=true
org.gradle.parallel=true

kotlin.code.style=official

# Note: Never commit sensitive credentials to version control!
# Instead, use environment variables or global ~/.gradle/gradle.properties
```

### settings.gradle.kts

```kotlin
rootProject.name = "my-awesome-library"
```

## Setup with Interactive Wizard

The easiest way to configure everything is with the setup wizard:

```bash
./gradlew setupPublishing
```

This will:
1. Auto-detect your project information
2. Guide you through credential setup
3. Generate the configuration automatically
4. Create GitHub Actions workflow (optional)

## Environment Variables Setup

Set up your credentials securely using environment variables:

```bash
# Sonatype credentials
export SONATYPE_USERNAME=your-username
export SONATYPE_PASSWORD=your-central-token

# GPG signing key (export with: gpg --armor --export-secret-keys your-email@example.com)
export SIGNING_KEY="-----BEGIN PGP PRIVATE KEY BLOCK-----
Version: GnuPG v2

lQOYBGE...your-full-private-key-here...
-----END PGP PRIVATE KEY BLOCK-----"

export SIGNING_PASSWORD=your-gpg-password
```

## Publishing Workflow

### 1. Validate Configuration

First, validate your setup without publishing:

```bash
./gradlew validatePublishing
```

Expected output:
```
> Task :validatePublishing
✅ Configuration validation successful
✅ Credentials verified
✅ GPG signing key validated
✅ Publications found: maven
✅ Ready to publish!
```

### 2. Create Bundle (Optional)

Create a deployment bundle to inspect before publishing:

```bash
./gradlew bundleArtifacts
```

This creates a ZIP file in `build/central-portal/` containing all your artifacts in Maven repository format.

### 3. Publish to Central Portal

Upload your artifacts:

```bash
./gradlew publishToCentral
```

Expected output:
```
> Task :publishToCentral
📦 Creating deployment bundle...
✅ Bundle created: my-awesome-library-1.0.0-bundle.zip
🚀 Uploading to Central Portal...
✅ Upload successful!
📋 Deployment ID: 12345678-abcd-ef90-1234-567890abcdef

Visit https://central.sonatype.com/publishing/deployments to review and publish.
```

### 4. Manual Review and Publish

1. Go to [Central Portal](https://central.sonatype.com/publishing/deployments)
2. Find your deployment by ID
3. Review the artifacts
4. Click "Publish" to release to Maven Central

## Artifacts Generated

For a typical single-module project, the following artifacts are generated:

```
my-awesome-library-1.0.0.jar           # Main JAR
my-awesome-library-1.0.0-sources.jar   # Sources JAR
my-awesome-library-1.0.0-javadoc.jar   # Javadoc JAR
my-awesome-library-1.0.0.pom           # POM file
```

Each artifact includes:
- GPG signature (`.asc` file)
- MD5 checksum (`.md5` file)  
- SHA1 checksum (`.sha1` file)

## Common Issues and Solutions

### "No publications found"

Make sure you have the required plugins:
```kotlin
plugins {
    `java-library`    // Required for Java publications
    `maven-publish`   // Required for publishing
}
```

### "Sources JAR missing"

Ensure you generate source and javadoc JARs:
```kotlin
java {
    withJavadocJar()
    withSourcesJar()
}
```

### "Invalid POM"

The plugin automatically generates a valid POM, but if you have custom `maven-publish` configuration, ensure it doesn't conflict:

```kotlin
publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            
            // Don't override pom {} here - let the plugin handle it
        }
    }
}
```

### "GPG signing failed"

Common GPG issues:
- Ensure your key includes full headers/footers
- Check that the key is not expired
- Verify the password is correct
- For gradle.properties, use `\n` for newlines:

```properties
SIGNING_KEY=-----BEGIN PGP PRIVATE KEY BLOCK-----\nVersion: GnuPG v2\n\nlQOYBGE...\n-----END PGP PRIVATE KEY BLOCK-----
```

## Testing Before Publishing

Use dry run mode to test your configuration:

```kotlin
centralPublisher {
    publishing {
        dryRun = true  // Creates bundle but doesn't upload
    }
}
```

Then run:
```bash
./gradlew publishToCentral
```

This will create the deployment bundle in `build/central-portal/` without uploading to the portal.

## Next Steps

- Set up [CI/CD integration](../advanced/troubleshooting.md#cicd-integration)
- Learn about [multi-module projects](multi-module.md)
- Explore [Kotlin Multiplatform setup](kmp.md)