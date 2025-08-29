# Kotlin Multiplatform Example

This guide shows how to set up the Central Portal Publisher plugin for Kotlin Multiplatform (KMP) projects.

## Project Structure

```
my-kmp-library/
├── build.gradle.kts
├── settings.gradle.kts
└── src/
    ├── commonMain/kotlin/
    ├── commonTest/kotlin/
    ├── jvmMain/kotlin/
    ├── jvmTest/kotlin/
    ├── jsMain/kotlin/
    ├── jsTest/kotlin/
    ├── nativeMain/kotlin/
    └── nativeTest/kotlin/
```

## Complete Example

### build.gradle.kts

```kotlin
plugins {
    kotlin("multiplatform") version "1.9.22"
    `maven-publish`
    id("com.tddworks.central-publisher") version "0.2.1-alpha"
}

group = "com.example"
version = "1.0.0"

kotlin {
    // JVM target
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
        withJava() // Enable Java sources
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    
    // JavaScript targets
    js(IR) {
        browser {
            commonWebpackConfig {
                cssSupport {
                    enabled.set(true)
                }
            }
        }
        nodejs()
    }
    
    // Native targets
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    
    when {
        hostOs == "Mac OS X" -> {
            macosX64()
            macosArm64()
        }
        hostOs == "Linux" -> {
            linuxX64()
        }
        isMingwX64 -> {
            mingwX64()
        }
    }
    
    // iOS targets (if on macOS)
    if (hostOs == "Mac OS X") {
        iosX64()
        iosArm64()
        iosSimulatorArm64()
    }
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
            }
        }
        
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        
        val jvmMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.7.3")
            }
        }
        
        val jvmTest by getting
        
        val jsMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:1.7.3")
            }
        }
        
        val jsTest by getting
        
        val nativeMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
            }
        }
        
        val nativeTest by getting
    }
}

centralPublisher {
    // Credentials from environment variables
    credentials {
        username = project.findProperty("SONATYPE_USERNAME")?.toString() ?: ""
        password = project.findProperty("SONATYPE_PASSWORD")?.toString() ?: ""
    }
    
    // Project information
    projectInfo {
        name = "my-kmp-library"
        description = "A Kotlin Multiplatform library for all platforms"
        url = "https://github.com/myorg/my-kmp-library"
        
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
            url = "https://github.com/myorg/my-kmp-library"
            connection = "scm:git:git://github.com/myorg/my-kmp-library.git"
            developerConnection = "scm:git:ssh://github.com/myorg/my-kmp-library.git"
        }
    }
    
    // GPG signing
    signing {
        key = project.findProperty("SIGNING_KEY")?.toString() ?: ""
        password = project.findProperty("SIGNING_PASSWORD")?.toString() ?: ""
    }
    
    // Publishing options
    publishing {
        autoPublish = false // Manual approval
        dryRun = false
    }
}
```

## Generated Publications

The plugin automatically detects and configures all Kotlin Multiplatform targets. For the example above, the following publications are generated:

### Main Publications
- **`kotlinMultiplatform`** - Root metadata publication
- **`jvm`** - JVM target artifacts
- **`js`** - JavaScript target artifacts  
- **`linuxX64`** - Linux native target
- **`macosX64`** - macOS Intel target
- **`macosArm64`** - macOS Apple Silicon target
- **`iosX64`** - iOS Simulator target
- **`iosArm64`** - iOS Device target
- **`iosSimulatorArm64`** - iOS Simulator ARM target

## Bundle Structure

The generated deployment bundle contains all platform-specific artifacts:

```
build/central-portal/my-kmp-library-1.0.0-bundle.zip
├── com/example/my-kmp-library/1.0.0/
│   ├── my-kmp-library-1.0.0.module          # Gradle metadata
│   ├── my-kmp-library-1.0.0.pom             # Root POM
│   └── signatures and checksums...
├── com/example/my-kmp-library-jvm/1.0.0/
│   ├── my-kmp-library-jvm-1.0.0.jar
│   ├── my-kmp-library-jvm-1.0.0-sources.jar
│   ├── my-kmp-library-jvm-1.0.0-javadoc.jar
│   ├── my-kmp-library-jvm-1.0.0.module
│   ├── my-kmp-library-jvm-1.0.0.pom
│   └── signatures and checksums...
├── com/example/my-kmp-library-js/1.0.0/
│   ├── my-kmp-library-js-1.0.0.jar
│   ├── my-kmp-library-js-1.0.0-sources.jar
│   ├── my-kmp-library-js-1.0.0.module
│   ├── my-kmp-library-js-1.0.0.pom
│   └── signatures and checksums...
└── com/example/my-kmp-library-{platform}/1.0.0/
    └── ... (similar structure for each native target)
```

## Platform-Specific Configuration

### JVM with Java Interop

```kotlin
jvm {
    withJava() // Enable Java sources
    compilations.all {
        kotlinOptions {
            jvmTarget = "1.8"
            freeCompilerArgs += listOf("-Xjsr305=strict")
        }
    }
}

// Generate JVM-specific Javadoc
tasks.named<Javadoc>("javadoc") {
    if (JavaVersion.current().isJava9Compatible) {
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }
}
```

### JavaScript with Node.js and Browser

```kotlin
js(IR) {
    browser {
        commonWebpackConfig {
            cssSupport {
                enabled.set(true)
            }
        }
        testTask {
            useKarma {
                useChromeHeadless()
                useFirefox()
            }
        }
    }
    nodejs {
        testTask {
            useMocha {
                timeout = "60s"
            }
        }
    }
    binaries.executable() // Generate executable JS
}
```

### Native Targets with Conditional Compilation

```kotlin
// Conditional compilation based on host OS
val hostOs = System.getProperty("os.name")

when {
    hostOs == "Mac OS X" -> {
        macosX64("native") {
            binaries {
                framework {
                    baseName = "MyKMPLibrary"
                }
            }
        }
        macosArm64()
        
        // iOS targets
        iosX64()
        iosArm64()
        iosSimulatorArm64()
    }
    hostOs == "Linux" -> {
        linuxX64("native")
        linuxArm64()
    }
    hostOs.startsWith("Windows") -> {
        mingwX64("native")
    }
}
```

## Source Set Configuration

### Hierarchical Source Sets

```kotlin
sourceSets {
    val commonMain by getting {
        dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
        }
    }
    
    val commonTest by getting {
        dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
        }
    }
    
    // JVM-specific sources
    val jvmMain by getting {
        dependencies {
            implementation("ch.qos.logback:logback-classic:1.4.11")
        }
    }
    
    // Native-specific sources  
    val nativeMain by creating {
        dependsOn(commonMain)
    }
    
    val linuxX64Main by getting {
        dependsOn(nativeMain)
    }
    
    val macosX64Main by getting {
        dependsOn(nativeMain) 
    }
    
    // iOS-specific sources
    val iosMain by creating {
        dependsOn(commonMain)
    }
    
    val iosX64Main by getting {
        dependsOn(iosMain)
    }
}
```

## Publishing Workflow

### 1. Setup with Interactive Wizard

```bash
./gradlew setupPublishing --console=plain
```

The wizard automatically detects your KMP setup and configures all targets.

### 2. Validate All Targets

```bash
./gradlew validatePublishing
```

This validates publications for all configured targets.

### 3. Build All Targets

```bash
./gradlew build
```

Builds and tests all targets (may require different host OS for some native targets).

### 4. Create Deployment Bundle

```bash
./gradlew bundleArtifacts
```

Creates a single bundle with all platform artifacts.

### 5. Publish to Central Portal

```bash
./gradlew publishToCentral
```

Uploads all platform artifacts in a single deployment.

## CI/CD for KMP Projects

### GitHub Actions Matrix Strategy

```yaml
name: Build and Publish KMP

on:
  push:
    tags: ['v*']

jobs:
  build:
    strategy:
      matrix:
        os: [ubuntu-latest, macos-latest, windows-latest]
    runs-on: ${{ matrix.os }}
    
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
          
      - name: Build on ${{ matrix.os }}
        run: ./gradlew build
        
  publish:
    needs: build
    runs-on: ubuntu-latest # Can publish from any OS
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
          
      - name: Publish to Central Portal
        run: ./gradlew publishToCentral
        env:
          SONATYPE_USERNAME: ${{ secrets.SONATYPE_USERNAME }}
          SONATYPE_PASSWORD: ${{ secrets.SONATYPE_PASSWORD }}
          SIGNING_KEY: ${{ secrets.SIGNING_KEY }}
          SIGNING_PASSWORD: ${{ secrets.SIGNING_PASSWORD }}
```

## Common Issues and Solutions

### "Target not supported on this host"

Some native targets can only be built on specific host OS:
- **iOS targets**: Require macOS
- **Windows targets**: Require Windows  
- **Linux targets**: Can be built on Linux or macOS with cross-compilation

Solution: Use CI/CD with matrix builds or configure only supported targets.

### "Missing platform-specific dependencies"

Ensure platform-specific dependencies are declared in the correct source sets:

```kotlin
val jvmMain by getting {
    dependencies {
        implementation("platform:specific:dependency") // JVM only
    }
}
```

### "Publication conflicts"

If you have custom `publishing` configuration, ensure it doesn't conflict with the plugin:

```kotlin
// Remove or comment out custom publishing configuration
// publishing {
//     publications { ... }
// }
```

The plugin handles all publication setup automatically.

## Advanced Configuration

### Custom Target Names

```kotlin
kotlin {
    jvm("desktop") // Custom name for JVM target
    js("web", IR)  // Custom name for JS target
    
    linuxX64("linux")
    macosX64("macos")
}
```

### Experimental Targets

```kotlin
kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }
    
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyHierarchyTemplate(default) {
        group("common") {
            group("jvmCommon") {
                withJvm()
                withAndroid()
            }
            group("nativeCommon") {
                withNative()
            }
        }
    }
}
```

## Next Steps

- Learn about [multi-module KMP projects](multi-module.md)
- Explore [advanced publishing tasks](../advanced/tasks.md)  
- See [troubleshooting guide](../advanced/troubleshooting.md)