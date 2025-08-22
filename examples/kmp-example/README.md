# Kotlin Multiplatform (KMP) Example

This example shows how to use the Central Portal Publisher plugin with a Kotlin Multiplatform project. The plugin automatically configures publishing for all KMP targets to Maven Central.

## Project Structure

```
kmp-example/
├── src/
│   ├── commonMain/kotlin/        # Shared code across all platforms
│   │   └── com/tddworks/example/kmp/Platform.kt
│   ├── jvmMain/kotlin/           # JVM-specific implementations
│   │   └── com/tddworks/example/kmp/Platform.jvm.kt
│   ├── jsMain/kotlin/            # JavaScript implementations  
│   │   └── com/tddworks/example/kmp/Platform.js.kt
│   ├── nativeMain/kotlin/        # Native platform implementations
│   │   └── com/tddworks/example/kmp/Platform.native.kt
│   └── commonTest/kotlin/        # Cross-platform tests
│       └── com/tddworks/example/kmp/KmpUtilsTest.kt
├── build.gradle.kts              # KMP + Central Publisher configuration
└── README.md
```

## Supported Targets

This project publishes artifacts for:
- **JVM** - Java Virtual Machine
- **JavaScript** - Browser and Node.js  
- **Linux x64** - Native Linux
- **macOS x64** - Intel Mac
- **macOS ARM64** - Apple Silicon Mac

## Quick Start

### 1. Run Setup Wizard (Recommended)

```bash
./gradlew setupPublishing
```

The wizard will:
- Set up your Sonatype credentials
- Configure project information (auto-detected from git)
- Set up GPG signing (optional)
- Handle KMP-specific publication configuration

### 2. Manual Setup

Add credentials to `~/.gradle/gradle.properties`:

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
# Build all targets
./gradlew build

# Run tests on all platforms
./gradlew allTests

# Validate publishing setup
./gradlew validatePublishing
```

### 4. Publish to Maven Central

```bash
# Test bundle creation
./gradlew bundleArtifacts

# Publish all targets to Maven Central
./gradlew publishToCentral
```

## How It Works

### Automatic KMP Configuration

The plugin automatically detects Kotlin Multiplatform projects and configures publications for all targets:

```kotlin
plugins {
    kotlin("multiplatform")
    id("com.tddworks.central-publisher")
}

kotlin {
    jvm()
    js(IR) {
        browser()
        nodejs()
    }
    
    linuxX64()
    macosX64() 
    macosArm64()
    
    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

centralPublisher {
    credentials {
        username = project.findProperty("SONATYPE_USERNAME")?.toString() ?: ""
        password = project.findProperty("SONATYPE_PASSWORD")?.toString() ?: ""
    }
    
    projectInfo {
        name = "kmp-example"
        description = "Kotlin Multiplatform library example"
        url = "https://github.com/tddworks/central-portal-publisher"
        // ... license, developer, SCM info
    }
}
```

### What Gets Published

For each target, the plugin automatically publishes:
- **Main artifact** - Compiled code for the target platform
- **Sources JAR** - Source code for debugging
- **Javadoc JAR** - API documentation (empty for KMP)
- **Gradle metadata** - For dependency resolution
- **POM file** - Maven metadata with dependencies

### Multi-Target Bundle

All KMP artifacts are bundled together for deployment:
```
build/central-portal/kmp-example-1.0.0-bundle.zip
├── com/tddworks/example/kmp-example/1.0.0/
│   ├── kmp-example-1.0.0.jar                    # Metadata artifact
│   ├── kmp-example-jvm-1.0.0.jar               # JVM target
│   ├── kmp-example-js-1.0.0.klib               # JS target  
│   ├── kmp-example-linuxx64-1.0.0.klib         # Linux native
│   ├── kmp-example-macosx64-1.0.0.klib         # macOS Intel
│   ├── kmp-example-macosarm64-1.0.0.klib       # macOS ARM
│   └── ... (sources, javadoc, checksums for each)
```

## Available Tasks

- `./gradlew setupPublishing` - Interactive setup wizard
- `./gradlew validatePublishing` - Validate configuration
- `./gradlew bundleArtifacts` - Create deployment bundle 
- `./gradlew publishToCentral` - Publish all targets to Maven Central

### KMP-Specific Tasks

- `./gradlew allTests` - Run tests on all platforms
- `./gradlew jvmTest` - Run JVM tests only
- `./gradlew jsTest` - Run JavaScript tests only

## Example Code

### Shared Code (commonMain)

```kotlin
// Platform.kt - expect/actual declarations
expect fun getPlatform(): Platform

data class Platform(val name: String)

fun greeting(): String {
    return "Hello ${getPlatform().name}!"
}
```

### Platform-Specific Implementation (jvmMain)

```kotlin
// Platform.jvm.kt - JVM implementation
actual fun getPlatform(): Platform = Platform("JVM")
```

### Cross-Platform Tests (commonTest)

```kotlin
// KmpUtilsTest.kt - Tests that run on all platforms
class KmpUtilsTest {
    @Test
    fun testExample() {
        assertTrue(greeting().contains("Hello"))
    }
}
```

## Troubleshooting

### Build Issues

If builds fail for specific targets:
```bash
# Check which targets are available
./gradlew tasks --group="build"

# Build specific target
./gradlew jvmJar
./gradlew compileKotlinJs
```

### Missing Native Toolchain

For native targets, ensure you have the required toolchain:
- **Linux**: GCC toolchain
- **macOS**: Xcode command line tools

### Publication Issues

All KMP targets must build successfully for publication:
```bash
# Validate all targets build
./gradlew build

# Check publication configuration
./gradlew validatePublishing
```

## CI/CD Integration

For GitHub Actions with multiple targets:

```yaml
- name: Build and Publish KMP
  env:
    SONATYPE_USERNAME: ${{ secrets.SONATYPE_USERNAME }}
    SONATYPE_PASSWORD: ${{ secrets.SONATYPE_PASSWORD }}
    SIGNING_KEY: ${{ secrets.SIGNING_KEY }}
    SIGNING_PASSWORD: ${{ secrets.SIGNING_PASSWORD }}
  run: |
    ./gradlew build
    ./gradlew publishToCentral
```

## Learn More

- [Plugin Documentation](https://github.com/tddworks/central-portal-publisher)
- [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html)
- [Maven Central Guide](https://central.sonatype.org/publish/publish-guide/)