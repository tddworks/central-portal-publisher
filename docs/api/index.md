# API Reference

This page provides programmatic API reference for the Central Portal Publisher plugin.

## Plugin Extension

The main configuration DSL is provided through the `CentralPublisherExtension`:

```kotlin
centralPublisher {
    // Configuration here
}
```

### CentralPublisherExtension

Main extension class that provides the configuration DSL.

**Package:** `com.tddworks.sonatype.publish.portal.plugin.dsl`

#### Properties

| Property | Type | Description |
|----------|------|-------------|
| `credentials` | `CredentialsSpec` | Sonatype Central Portal credentials |
| `projectInfo` | `ProjectInfoSpec` | Project metadata for Maven POM |
| `signing` | `SigningSpec` | GPG signing configuration |
| `publishing` | `PublishingSpec` | Publishing behavior options |

#### Methods

```kotlin
fun credentials(action: Action<CredentialsSpec>)
fun projectInfo(action: Action<ProjectInfoSpec>)
fun signing(action: Action<SigningSpec>)
fun publishing(action: Action<PublishingSpec>)
```

## Configuration Specifications

### CredentialsSpec

Configure Sonatype Central Portal authentication.

```kotlin
credentials {
    username = "your-username"
    password = "your-token"
}
```

#### Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `username` | `String` | `""` | Sonatype username |
| `password` | `String` | `""` | Sonatype token/password |

### ProjectInfoSpec

Configure project metadata for Maven POM generation.

```kotlin
projectInfo {
    name = "My Project"
    description = "Project description"
    url = "https://github.com/me/project"
    
    license {
        name = "Apache License 2.0"
        url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
    }
    
    developer {
        id = "myid"
        name = "My Name"
        email = "me@example.com"
    }
    
    scm {
        url = "https://github.com/me/project"
        connection = "scm:git:git://github.com/me/project.git"
        developerConnection = "scm:git:ssh://github.com/me/project.git"
    }
}
```

#### Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `name` | `String` | Auto-detected | Project name |
| `description` | `String` | Auto-detected | Project description |
| `url` | `String` | Auto-detected | Project homepage URL |
| `license` | `LicenseSpec` | - | License information |
| `developer` | `DeveloperSpec` | Auto-detected | Developer information |
| `scm` | `ScmSpec` | Auto-detected | Source control information |

#### Methods

```kotlin
fun license(action: Action<LicenseSpec>)
fun developer(action: Action<DeveloperSpec>)
fun scm(action: Action<ScmSpec>)
```

### LicenseSpec

Configure license information.

#### Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `name` | `String` | - | License name |
| `url` | `String` | - | License URL |
| `distribution` | `String` | `"repo"` | License distribution |
| `comments` | `String?` | `null` | License comments |

### DeveloperSpec

Configure developer information.

#### Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `id` | `String` | Auto-detected | Developer ID |
| `name` | `String` | Auto-detected | Developer name |
| `email` | `String` | Auto-detected | Developer email |
| `organization` | `String?` | `null` | Developer organization |
| `organizationUrl` | `String?` | `null` | Organization URL |
| `roles` | `List<String>` | `[]` | Developer roles |
| `timezone` | `String?` | `null` | Developer timezone |

### ScmSpec

Configure source control management information.

#### Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `url` | `String` | Auto-detected | Repository browser URL |
| `connection` | `String` | Auto-detected | SCM connection URL |
| `developerConnection` | `String` | Auto-detected | SCM developer connection |
| `tag` | `String?` | `null` | SCM tag |

### SigningSpec

Configure GPG signing for artifacts.

```kotlin
signing {
    // Option 1: In-memory key (recommended)
    key = "-----BEGIN PGP PRIVATE KEY BLOCK-----..."
    password = "gpg-password"
    
    // Option 2: Key file
    keyId = "key-id"
    password = "gpg-password"
    secretKeyRingFile = "/path/to/secring.gpg"
}
```

#### Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `key` | `String?` | `null` | Armored private key content |
| `keyId` | `String?` | `null` | GPG key ID |
| `password` | `String` | `""` | GPG key password |
| `secretKeyRingFile` | `String?` | `null` | Path to secret keyring file |

### PublishingSpec

Configure publishing behavior.

```kotlin
publishing {
    autoPublish = false  // Manual approval (default)
    aggregation = true   // Bundle multi-module projects
    dryRun = false      // Actually publish (default)
}
```

#### Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `autoPublish` | `Boolean` | `false` | Auto-publish after upload |
| `aggregation` | `Boolean` | `true` | Bundle multi-module projects |
| `dryRun` | `Boolean` | `false` | Test mode without publishing |

## Tasks

### PublishTask

Main task for publishing to Central Portal.

**Task Name:** `publishToCentral`

```kotlin
tasks.named<PublishTask>("publishToCentral") {
    // Task configuration
}
```

#### Properties

| Property | Type | Description |
|----------|------|-------------|
| `deploymentId` | `Property<String>` | Output: Central Portal deployment ID |
| `bundlePath` | `Property<File>` | Output: Path to generated bundle |

### BundleArtifactsTask

Task for creating deployment bundle without uploading.

**Task Name:** `bundleArtifacts`

#### Properties

| Property | Type | Description |
|----------|------|-------------|
| `outputDirectory` | `DirectoryProperty` | Bundle output directory |
| `bundleName` | `Property<String>` | Generated bundle filename |

### ValidatePublishingTask

Task for validating configuration without publishing.

**Task Name:** `validatePublishing`

#### Properties

| Property | Type | Description |
|----------|------|-------------|
| `validationResult` | `Property<ValidationResult>` | Validation outcome |

## Auto-Detection API

### AutoDetector

Interface for auto-detection components.

```kotlin
interface AutoDetector<T> {
    fun detect(project: Project): T?
    fun isApplicable(project: Project): Boolean
}
```

### GitInfoDetector

Auto-detects information from Git repository.

**Package:** `com.tddworks.sonatype.publish.portal.plugin.autodetection`

#### Detected Information

- Repository URL
- Developer name and email (from git config)
- Project name (from repository name)
- SCM connections

#### Usage

```kotlin
val detector = GitInfoDetector()
val gitInfo = detector.detect(project)
```

### ProjectInfoDetector

Auto-detects project information from various sources.

#### Detected Information

- Project name (from settings.gradle.kts)
- Description (from build script or README)
- License information (from LICENSE files)

## Configuration Sources

### ConfigurationSourceManager

Manages loading configuration from multiple sources with precedence.

**Package:** `com.tddworks.sonatype.publish.portal.plugin.config`

#### Source Precedence (highest to lowest)

1. Environment variables
2. System properties
3. Global gradle.properties
4. Local gradle.properties
5. Build script DSL

#### Usage

```kotlin
val sourceManager = ConfigurationSourceManager(project)
val config = sourceManager.loadConfiguration()
```

## Validation API

### ValidationEngine

Validates configuration before publishing.

**Package:** `com.tddworks.sonatype.publish.portal.plugin.validation`

#### Methods

```kotlin
fun validate(config: CentralPublisherConfig): ValidationResult
```

### ValidationResult

Contains validation outcome.

#### Properties

| Property | Type | Description |
|----------|------|-------------|
| `isValid` | `Boolean` | Overall validation result |
| `violations` | `List<ValidationViolation>` | List of validation errors |

### ValidationViolation

Individual validation error.

#### Properties

| Property | Type | Description |
|----------|------|-------------|
| `severity` | `Severity` | ERROR, WARNING, or INFO |
| `field` | `String` | Configuration field name |
| `message` | `String` | Error message |
| `suggestion` | `String?` | Suggested fix |

## Publisher API

### SonatypePortalPublisher

Core publisher implementation.

**Package:** `com.tddworks.sonatype.publish.portal.api`

#### Methods

```kotlin
suspend fun upload(bundle: DeploymentBundle): DeploymentResult
suspend fun checkStatus(deploymentId: String): DeploymentStatus
```

### DeploymentBundle

Represents a deployment bundle.

#### Properties

| Property | Type | Description |
|----------|------|-------------|
| `artifacts` | `List<Artifact>` | Bundle artifacts |
| `metadata` | `BundleMetadata` | Bundle metadata |
| `checksums` | `Map<String, String>` | File checksums |

## Extension Functions

### Project Extensions

Utility extensions for Gradle Project.

```kotlin
// Check if project has publications
val hasPublications: Boolean = project.hasPublications()

// Get all publications
val publications: List<Publication> = project.getPublications()

// Check if multi-module project
val isMultiModule: Boolean = project.isMultiModule()
```

### Configuration Extensions

Extensions for working with configuration.

```kotlin
// Resolve configuration with defaults
val config = project.resolveCentralPublisherConfig()

// Check if configuration is complete
val isComplete = config.isComplete()

// Get missing required fields
val missing = config.getMissingRequiredFields()
```

## Example: Programmatic Usage

```kotlin
// In a custom plugin or build script
import com.tddworks.sonatype.publish.portal.plugin.dsl.CentralPublisherExtension

// Get extension
val extension = project.extensions.getByType<CentralPublisherExtension>()

// Configure programmatically
extension.apply {
    credentials {
        username = "auto-detected-username"
        password = System.getenv("SONATYPE_PASSWORD")
    }
    
    projectInfo {
        name = project.name
        description = "Auto-generated description"
        // ... other fields
    }
}

// Access resolved configuration
val config = extension.buildConfiguration()

// Validate
val validation = ValidationEngine().validate(config)
if (!validation.isValid) {
    validation.violations.forEach { violation ->
        logger.error("${violation.field}: ${violation.message}")
    }
}
```

## Custom Tasks

You can create custom tasks that integrate with the plugin:

```kotlin
abstract class CustomPublishTask : DefaultTask() {
    @get:Nested
    abstract val publisherConfig: CentralPublisherExtension
    
    @TaskAction
    fun execute() {
        val config = publisherConfig.buildConfiguration()
        // Custom logic here
    }
}

tasks.register<CustomPublishTask>("customPublish") {
    publisherConfig.set(extensions.getByType<CentralPublisherExtension>())
}
```

## Error Handling

The plugin provides structured error handling:

```kotlin
try {
    project.tasks.named("publishToCentral").get().execute()
} catch (e: PublishingException) {
    logger.error("Publishing failed: ${e.message}")
    e.validationErrors?.forEach { error ->
        logger.error("- ${error.field}: ${error.message}")
    }
} catch (e: AuthenticationException) {
    logger.error("Authentication failed: ${e.message}")
} catch (e: NetworkException) {
    logger.error("Network error: ${e.message}")
}
```

This API reference covers the main programmatic interfaces. For more examples and usage patterns, see the other documentation pages.