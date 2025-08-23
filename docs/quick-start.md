# Quick Start Guide

Get up and running with the Central Portal Publisher plugin in minutes using our interactive setup wizard.

## Installation

Add the plugin to your `build.gradle.kts`:

```kotlin
plugins {
    id("com.tddworks.central-publisher") version "0.2.1-alpha"
}
```

## Interactive Setup Wizard

The easiest way to configure the plugin is with the interactive setup wizard:

```bash
./gradlew setupPublishing
```

### What the Wizard Does

The wizard will guide you through:

1. **Auto-detection** of project information from your git repository
2. **Credential setup** with automatic detection of existing environment variables
3. **GPG signing configuration** with security best practices
4. **Configuration file generation** with all necessary settings
5. **GitHub Actions workflow** setup (optional)

### Sample Wizard Flow

```
🧙 CENTRAL PORTAL PUBLISHER SETUP WIZARD
===============================================

📋 PROJECT INFORMATION - AUTO-DETECTED!
✅ Project name: my-awesome-library
✅ Description: An awesome Kotlin library
✅ URL: https://github.com/myorg/my-awesome-library
✅ License: Apache License 2.0
✅ Developer info detected from git config

📋 CREDENTIALS SETUP - AUTO-DETECTED!
✅ Found existing environment variables:
• SONATYPE_USERNAME: myusername
• SONATYPE_PASSWORD: ********

📋 GPG SIGNING SETUP - AUTO-DETECTED!
✅ Found existing environment variables:
• SIGNING_KEY: ********
• SIGNING_PASSWORD: ********

✅ Setup complete! Generated files:
• build.gradle.kts (updated)
• .github/workflows/publish.yml (created)

🚀 Ready to publish! Try: ./gradlew validatePublishing
```

## Manual Setup (Alternative)

If you prefer manual configuration, see the [Configuration Guide](configuration.md).

## Next Steps

After running the setup wizard:

### 1. Validate Your Configuration

```bash
./gradlew validatePublishing
```

This checks your configuration without actually publishing anything.

### 2. Create a Test Bundle

```bash
./gradlew bundleArtifacts
```

This creates a deployment bundle in `build/central-portal/` that you can inspect before publishing.

### 3. Publish to Maven Central

```bash
./gradlew publishToCentral
```

This uploads your artifacts to the Sonatype Central Portal for review and publishing.

## Environment Variables Setup

For security, we recommend using environment variables for credentials:

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

The setup wizard will automatically detect these variables if they exist.

## Troubleshooting

### Common Issues

#### "No publications found"
Make sure you have applied the appropriate plugin for your project type:
- Java: `java-library`
- Kotlin/JVM: `org.jetbrains.kotlin.jvm`
- Kotlin Multiplatform: `org.jetbrains.kotlin.multiplatform`

#### "GPG signing failed"
Ensure your GPG key is properly formatted:
- Include the full armored key including headers and footers
- Use `\n` for newlines in gradle.properties
- Or use environment variables (recommended)

#### "Authentication failed"
Verify your Sonatype credentials:
- Username should be your Sonatype account username
- Password should be your account token (not your login password)
- Get your token from: https://central.sonatype.com/account

### Getting Help

- Check the [troubleshooting guide](advanced/troubleshooting.md)
- Review [configuration options](configuration.md)
- See [examples](examples/single-module.md) for your project type