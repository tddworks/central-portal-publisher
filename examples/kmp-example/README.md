# KMP Example Project

This is a simple Kotlin Multiplatform (KMP) project that demonstrates publishing to Maven Central using the `central-publisher` plugin.

## Project Structure

```
examples/kmp-example/
├── src/
│   ├── commonMain/kotlin/com/tddworks/example/kmp/
│   │   └── Platform.kt                    # Shared code with expect/actual declarations
│   ├── jvmMain/kotlin/com/tddworks/example/kmp/
│   │   └── Platform.jvm.kt                # JVM-specific implementations
│   ├── jsMain/kotlin/com/tddworks/example/kmp/
│   │   └── Platform.js.kt                 # JavaScript-specific implementations
│   ├── nativeMain/kotlin/com/tddworks/example/kmp/
│   │   └── Platform.native.kt             # Native platform implementations
│   └── commonTest/kotlin/com/tddworks/example/kmp/
│       └── KmpUtilsTest.kt                # Cross-platform tests
├── build.gradle.kts                       # KMP + central-publisher configuration
├── settings.gradle.kts                    # Plugin management and included builds
├── gradle.properties                      # Project configuration
├── gradle/wrapper/                        # Gradle wrapper files
├── gradlew, gradlew.bat                   # Gradle wrapper scripts
└── README.md                              # This documentation
```

### Source Sets
- **Common code** (`src/commonMain`): Shared code across all platforms using expect/actual mechanism
- **JVM code** (`src/jvmMain`): JVM-specific implementations with access to Java APIs
- **JavaScript code** (`src/jsMain`): JS-specific implementations for browser and Node.js
- **Native code** (`src/nativeMain`): Native platform implementations for Linux and macOS
- **Tests** (`src/commonTest`): Shared tests that run on all platforms

## Targets

This project supports the following targets:
- JVM
- JavaScript (Browser & Node.js)
- Linux x64
- macOS x64
- macOS ARM64

## Building

```bash
# Build all targets
./gradlew build

# Run tests
./gradlew allTests

# Build specific target
./gradlew jvmJar
./gradlew jsJar
```

## Publishing to Maven Central

1. Configure credentials in `~/.gradle/gradle.properties`:
```properties
SONATYPE_USERNAME=your-username
SONATYPE_PASSWORD=your-token
SIGNING_KEY=-----BEGIN PGP PRIVATE KEY BLOCK-----...
SIGNING_PASSWORD=your-gpg-password
```

2. Validate configuration:
```bash
./gradlew validatePublishing
```

3. Create deployment bundle:
```bash
./gradlew bundleArtifacts
```

4. Publish to Maven Central:
```bash
./gradlew publishToCentral
```

## Features Demonstrated

- **Multiplatform expect/actual mechanism**: Platform-specific implementations
- **Shared business logic**: Common utilities across platforms
- **Platform-specific APIs**: Accessing platform-specific functionality
- **Cross-platform testing**: Tests that run on all targets
- **Maven Central publishing**: Automated publication with proper metadata

## Dependencies

- Kotlin Multiplatform: 2.2.0
- Coroutines: 1.9.0 (for async operations)
- Kotlin Test: Built-in testing framework

The project uses the `central-publisher` plugin to simplify Maven Central publishing for KMP projects.