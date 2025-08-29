# Central Portal Publisher Plugin

A modern Gradle plugin for publishing to Maven Central via the Sonatype Central Portal. Features intelligent auto-detection, type-safe configuration, and an interactive setup wizard.

![CI](https://github.com/tddworks/central-portal-publisher/actions/workflows/main.yml/badge.svg)
[![codecov](https://codecov.io/gh/tddworks/central-portal-publisher/graph/badge.svg?token=izDBfMwLY0)](https://codecov.io/gh/tddworks/central-portal-publisher)

## Key Features

- ✅ **Auto-detection**: Project info, git repository, developer information
- ✅ **Multiple project types**: Single-module, multi-module, Kotlin Multiplatform  
- ✅ **Flexible authentication**: Environment variables, gradle.properties, or DSL
- ✅ **GPG signing**: In-memory keys, file-based, or environment variables
- ✅ **Bundle generation**: Proper Maven repository layout with checksums and signatures
- ✅ **Validation**: Configuration validation before publishing
- ✅ **CI/CD friendly**: Works great with GitHub Actions, Jenkins, etc.
- ✅ **Interactive setup**: Guided wizard for first-time setup

## Quick Start

### 🧙 Interactive Setup Wizard (Recommended)

The easiest way to get started is with the interactive setup wizard:

```kotlin
plugins {
    id("com.tddworks.central-publisher") version "0.2.1-alpha"
}
```

Then run the setup wizard:
```bash
./gradlew setupPublishing --console=plain
```

The wizard will:
- ✅ **Auto-detect** project information from git
- ✅ **Auto-detect** existing environment variables (recommended for security)
- ✅ **Guide you** through credentials and GPG signing setup
- ✅ **Generate** all necessary configuration files
- ✅ **Explain** security best practices

## Environment Variable Auto-Detection

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

## Supported Project Types

### Single Module Projects
The plugin automatically detects single-module projects and configures publishing accordingly.

### Multi-Module Projects
Support for both individual module publishing and aggregated publishing (bundling all modules together).

### Kotlin Multiplatform (KMP) Projects
Full support for KMP projects with all target publications, automatically detecting and including all platform-specific artifacts.

## Available Tasks

Once configured, the plugin provides these tasks:

- **`setupPublishing`** - Interactive setup wizard (recommended for first-time setup)
- **`publishToCentral`** - Publish all artifacts to Maven Central
- **`bundleArtifacts`** - Create deployment bundle for Maven Central  
- **`validatePublishing`** - Validate configuration without publishing

## Next Steps

- [Get started with the Quick Start guide](quick-start.md)
- [Learn about configuration options](configuration.md)
- [See examples for your project type](examples/single-module.md)
- [Set up CI/CD integration](advanced/troubleshooting.md)