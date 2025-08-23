# Multi-Module Project Example

This guide shows how to set up the Central Portal Publisher plugin for multi-module Java or Kotlin projects.

## Project Structure

```
my-multi-module-project/
├── build.gradle.kts                 # Root build script
├── settings.gradle.kts
├── module-a/
│   ├── build.gradle.kts
│   └── src/main/kotlin/...
├── module-b/
│   ├── build.gradle.kts
│   └── src/main/kotlin/...
└── shared/
    ├── build.gradle.kts
    └── src/main/kotlin/...
```

## Root Configuration

### build.gradle.kts (Root)

```kotlin
plugins {
    id("com.tddworks.central-publisher") version "0.2.1-alpha"
}

group = "com.example"
version = "1.0.0"

centralPublisher {
    // Credentials from environment variables
    credentials {
        username = project.findProperty("SONATYPE_USERNAME")?.toString() ?: ""
        password = project.findProperty("SONATYPE_PASSWORD")?.toString() ?: ""
    }
    
    // Project information
    projectInfo {
        name = "my-multi-module-project"
        description = "A multi-module project demonstrating the Central Portal Publisher"
        url = "https://github.com/myorg/my-multi-module-project"
        
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
            url = "https://github.com/myorg/my-multi-module-project"
            connection = "scm:git:git://github.com/myorg/my-multi-module-project.git"
            developerConnection = "scm:git:ssh://github.com/myorg/my-multi-module-project.git"
        }
    }
    
    // GPG signing
    signing {
        key = project.findProperty("SIGNING_KEY")?.toString() ?: ""
        password = project.findProperty("SIGNING_PASSWORD")?.toString() ?: ""
    }
    
    // Publishing options - aggregation is recommended for multi-module
    publishing {
        aggregation = true   // Bundle all modules together (recommended)
        autoPublish = false  // Manual approval
        dryRun = false
    }
}
```

## Module Configuration

### module-a/build.gradle.kts

```kotlin
plugins {
    `kotlin-jvm`
    `java-library`
    `maven-publish`
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
}

java {
    withJavadocJar()
    withSourcesJar()
}

tasks.test {
    useJUnitPlatform()
}
```

### module-b/build.gradle.kts

```kotlin
plugins {
    `kotlin-jvm` 
    `java-library`
    `maven-publish`
}

dependencies {
    implementation(project(":module-a"))  // Inter-module dependency
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
}

java {
    withJavadocJar()
    withSourcesJar()
}

tasks.test {
    useJUnitPlatform()
}
```

## Publishing Strategies

### Strategy 1: Aggregated Publishing (Recommended)

With aggregation enabled (default), all modules are bundled into a single deployment:

```kotlin
centralPublisher {
    publishing {
        aggregation = true  // Default behavior
    }
}
```

**Benefits:**
- Single deployment bundle
- Consistent versioning across modules
- Simpler review process
- Faster deployment

**Publishing commands:**
```bash
./gradlew validatePublishing     # Validates all modules
./gradlew bundleArtifacts       # Creates single bundle with all modules
./gradlew publishToCentral      # Uploads single deployment
```

### Strategy 2: Individual Module Publishing

For independent module versioning:

```kotlin
centralPublisher {
    publishing {
        aggregation = false  // Each module publishes separately
    }
}
```

**Benefits:**
- Independent versioning
- Selective publishing
- Module-specific metadata

**Publishing commands:**
```bash
./gradlew :module-a:validatePublishing
./gradlew :module-a:publishToCentral

./gradlew :module-b:validatePublishing  
./gradlew :module-b:publishToCentral
```

## Setup with Interactive Wizard

Run the setup wizard from the root project:

```bash
./gradlew setupPublishing
```

The wizard will:
1. Auto-detect your multi-module structure
2. Configure all modules for publishing
3. Set up aggregated publishing (recommended)
4. Generate GitHub Actions workflow

## Generated Bundle Structure (Aggregated)

With aggregation enabled, you get a single bundle containing all modules:

```
build/central-portal/my-multi-module-project-1.0.0-bundle.zip
├── com/example/module-a/1.0.0/
│   ├── module-a-1.0.0.jar
│   ├── module-a-1.0.0-sources.jar
│   ├── module-a-1.0.0-javadoc.jar
│   ├── module-a-1.0.0.pom
│   └── signatures and checksums...
├── com/example/module-b/1.0.0/
│   ├── module-b-1.0.0.jar
│   ├── module-b-1.0.0-sources.jar
│   ├── module-b-1.0.0-javadoc.jar
│   ├── module-b-1.0.0.pom
│   └── signatures and checksums...
└── com/example/shared/1.0.0/
    └── ... (similar structure)
```

## Module Dependencies

Inter-module dependencies are automatically handled:

```kotlin
// In module-b/build.gradle.kts
dependencies {
    implementation(project(":module-a"))    // Internal dependency
    implementation(project(":shared"))      // Another internal dependency
    api("external:library:1.0")           // External dependency
}
```

The generated POM files will correctly reference published modules and external dependencies.

## Common Issues

### "Module not found in publications"

Ensure each module has the required plugins:

```kotlin
plugins {
    `java-library`    // or kotlin-jvm
    `maven-publish`   // Required for publishing
}
```

### "Inter-module dependency not resolved"

For aggregated publishing, inter-module dependencies are handled automatically. For individual publishing, ensure dependent modules are published first or use version ranges.

### "Different versions across modules"

With aggregation, all modules inherit the root project version. For different versions:

```kotlin
// In individual module build.gradle.kts
version = "1.1.0"  // Override root version
```

## CI/CD Integration

The plugin works great with CI/CD for multi-module projects:

```yaml
# .github/workflows/publish.yml
- name: Publish all modules
  run: ./gradlew publishToCentral
  env:
    SONATYPE_USERNAME: ${{ secrets.SONATYPE_USERNAME }}
    SONATYPE_PASSWORD: ${{ secrets.SONATYPE_PASSWORD }}
    SIGNING_KEY: ${{ secrets.SIGNING_KEY }}
    SIGNING_PASSWORD: ${{ secrets.SIGNING_PASSWORD }}
```

## Next Steps

- Learn about [Kotlin Multiplatform setup](kmp.md)
- Explore [task reference](../advanced/tasks.md)
- See [troubleshooting guide](../advanced/troubleshooting.md)