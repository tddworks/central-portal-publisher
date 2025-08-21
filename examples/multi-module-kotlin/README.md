# Multi-Module Kotlin Example

This example demonstrates how to use the Central Portal Publisher plugin with a multi-module Kotlin project, showcasing advanced features like selective publishing and module configuration inheritance.

## Project Structure

```
multi-module-kotlin/
├── build.gradle.kts           # Root build configuration with Central Publisher
├── settings.gradle.kts        # Multi-module project settings
├── gradle.properties          # Properties file for credentials
├── core/                      # Core module (publishable)
│   ├── build.gradle.kts
│   └── src/main/kotlin/...   # Core data models
├── api/                       # API module (publishable)
│   ├── build.gradle.kts
│   └── src/main/kotlin/...   # Service interfaces
├── client/                    # Client module (publishable)
│   ├── build.gradle.kts
│   └── src/main/kotlin/...   # High-level client
├── test-utils/                # Test utilities (NOT publishable)
│   ├── build.gradle.kts
│   └── src/main/kotlin/...   # Test helpers
└── README.md
```

## Module Dependencies

```
client --> api --> core
           ^
           |
      test-utils (internal only)
```

## Features Demonstrated

### 1. Multi-Module Configuration

The root `build.gradle.kts` configures the Central Publisher plugin for all modules:

```kotlin
centralPublisher {
    projectInfo {
        // Configuration inherited by all modules
        name = rootProject.name
        description = "Multi-module project description"
        // ...
    }
}
```

### 2. Selective Publishing

The example shows how to control which modules get published:

```kotlin
val moduleSelection = ModuleSelectionConfig(
    includePatterns = listOf("core", "api", "client"),
    excludePatterns = listOf("*test*"),
    filters = listOf(
        ModuleFilter.PublishableOnly(true)
    )
)
```

Key points:
- `core`, `api`, and `client` modules are publishable (have `maven-publish` plugin)
- `test-utils` is excluded from publishing (internal testing module)

### 3. Configuration Inheritance

Submodules inherit configuration from the root project:
- Common group and version
- Shared credentials and signing configuration
- Base project information (can be overridden per module)

### 4. Module Detection

The plugin automatically detects:
- Multi-module structure from `settings.gradle.kts`
- Which modules are publishable (have `maven-publish` plugin)
- Module dependencies and relationships

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
   cd examples/multi-module-kotlin
   ```

### 2. Configure Credentials

Add to `~/.gradle/gradle.properties` (NOT in the project):

```properties
SONATYPE_USERNAME=your-username
SONATYPE_PASSWORD=your-password
SIGNING_KEY_ID=your-key-id
SIGNING_PASSWORD=your-signing-password
SIGNING_SECRET_KEY_RING_FILE=/path/to/secring.gpg
```

### 3. Build All Modules

```bash
./gradlew build
```

### 4. Run Tests

```bash
./gradlew test
```

### 5. Detect Module Structure

```bash
./gradlew detectModules
```

Output:
```
🔍 Detecting module structure...
Root project: multi-module-example

Submodules:
  - core: ✓ Publishable
  - api: ✓ Publishable
  - client: ✓ Publishable
  - test-utils: ✗ Non-publishable
```

### 6. Validate Publishing Configuration

```bash
./gradlew validateMultiModulePublishing
```

### 7. Publish Modules

#### Publish All Modules
```bash
./gradlew publishAllModules
```

#### Publish Specific Module
```bash
./gradlew :core:publishAllPublicationsToCentralPortal
./gradlew :api:publishAllPublicationsToCentralPortal
./gradlew :client:publishAllPublicationsToCentralPortal
```

## Advanced Configuration

### Per-Module Configuration

Each module can override the root configuration:

```kotlin
// In api/build.gradle.kts
publishing {
    publications {
        create<MavenPublication>("maven") {
            pom {
                name.set("Custom API Module Name")
                description.set("Custom description for API")
            }
        }
    }
}
```

### Module Groups

Organize modules into logical groups:

```kotlin
groups = mapOf(
    "public-api" to listOf("api", "client"),
    "internals" to listOf("core"),
    "testing" to listOf("test-utils")
)
```

### Filtering Strategies

Use different filters for module selection:

```kotlin
filters = listOf(
    ModuleFilter.PublishableOnly(true),          // Only publishable modules
    ModuleFilter.HasDependencies(false),         // Only leaf modules
    ModuleFilter.PathPattern(listOf(":api:*"))  // Modules under :api
)
```

## Module Descriptions

### Core Module
- **Purpose**: Provides core data models and domain objects
- **Published**: Yes
- **Dependencies**: None (foundation module)

### API Module
- **Purpose**: Defines service interfaces and contracts
- **Published**: Yes
- **Dependencies**: Core module

### Client Module
- **Purpose**: High-level client library with convenience methods
- **Published**: Yes
- **Dependencies**: API module (transitively includes Core)

### Test-Utils Module
- **Purpose**: Internal testing utilities and test data generators
- **Published**: No (internal use only)
- **Dependencies**: Core and API modules

## Best Practices

### 1. Module Organization
- Keep core domain models in a separate module
- Define clear API boundaries
- Separate internal utilities from published artifacts

### 2. Configuration Management
- Use root configuration for common settings
- Override only when necessary in submodules
- Keep credentials in user-level gradle.properties

### 3. Selective Publishing
- Explicitly mark which modules should be published
- Use exclude patterns for internal modules
- Validate configuration before publishing

### 4. Testing Strategy
- Keep test utilities in a separate non-published module
- Share test data generators across modules
- Test module interactions in integration tests

## Troubleshooting

### Module Not Publishing

Check that the module:
1. Has the `maven-publish` plugin applied
2. Is included in the module selection configuration
3. Has valid publishing configuration

### Configuration Not Inherited

Ensure:
1. The root project applies the Central Publisher plugin
2. Subprojects properly reference root configuration
3. No conflicting configuration in submodules

### Dependency Issues

Verify:
1. Module dependencies are correctly declared
2. Publishing order respects dependencies
3. All transitive dependencies are available

## CI/CD Integration

For CI/CD pipelines, use environment variables for credentials:

```yaml
# GitHub Actions example
env:
  SONATYPE_USERNAME: ${{ secrets.SONATYPE_USERNAME }}
  SONATYPE_PASSWORD: ${{ secrets.SONATYPE_PASSWORD }}
  SIGNING_KEY_ID: ${{ secrets.SIGNING_KEY_ID }}
  SIGNING_PASSWORD: ${{ secrets.SIGNING_PASSWORD }}
```

Then publish:
```bash
./gradlew publishAllModules
```

## Additional Resources

- [Central Portal Publisher Documentation](https://github.com/tddworks/central-portal-publisher)
- [Gradle Multi-Project Builds](https://docs.gradle.org/current/userguide/multi_project_builds.html)
- [Maven Central Requirements](https://central.sonatype.org/publish/requirements/)