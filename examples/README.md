# Central Portal Publisher Examples

This directory contains example projects demonstrating how to use the Central Portal Publisher Gradle plugin in different scenarios.

## Available Examples

### 1. [Single-Module Kotlin](./single-module-kotlin)

A simple single-module Kotlin library project that demonstrates:
- Basic plugin configuration
- Auto-detection features
- Smart defaults
- Credential management
- Simple publishing workflow

**Best for**: Getting started with the plugin, simple library projects

### 2. [Multi-Module Kotlin](./multi-module-kotlin)

A complex multi-module Kotlin project that demonstrates:
- Multi-module configuration
- Selective publishing
- Configuration inheritance
- Module groups and filtering
- Mixed publishable/non-publishable modules

**Best for**: Enterprise projects, complex library structures, selective publishing needs

## Quick Start

Each example is a standalone Gradle project. To run an example:

1. Navigate to the example directory:
   ```bash
   cd single-module-kotlin
   # or
   cd multi-module-kotlin
   ```

2. **For local plugin testing**: Uncomment the `includeBuild("../..")` line in `settings.gradle.kts`

3. Build the project:
   ```bash
   ./gradlew build
   ```

4. Run tests:
   ```bash
   ./gradlew test
   ```

5. Check publishing configuration:
   ```bash
   ./gradlew validatePublishing
   ```

## Common Features

All examples demonstrate:

### Configuration Methods
- **DSL Configuration**: Type-safe Kotlin DSL for plugin configuration
- **Properties Files**: Using gradle.properties for credentials
- **Environment Variables**: CI/CD-friendly credential management
- **Auto-Detection**: Automatic detection of project information

### Publishing Features
- **Simplified Tasks**: User-friendly task names
- **Validation**: Configuration validation before publishing
- **Dry-Run Mode**: Test publishing without uploading
- **Progress Tracking**: Visual feedback during publishing

### Best Practices
- **Security**: Safe credential handling
- **Modularity**: Clean module separation
- **Testing**: Comprehensive test coverage
- **Documentation**: Clear, actionable documentation

## Choosing an Example

| Use Case | Recommended Example |
|----------|-------------------|
| Simple library | Single-Module |
| Microservices | Multi-Module |
| SDK with multiple artifacts | Multi-Module |
| Learning the plugin | Single-Module first, then Multi-Module |
| CI/CD setup | Both examples show CI/CD configuration |

## Setting Up Credentials

All examples require Sonatype credentials. Add to `~/.gradle/gradle.properties`:

```properties
SONATYPE_USERNAME=your-username
SONATYPE_PASSWORD=your-password
SIGNING_KEY_ID=your-key-id
SIGNING_PASSWORD=your-signing-password
SIGNING_SECRET_KEY_RING_FILE=/path/to/secring.gpg
```

**Never commit credentials to version control!**

## Advanced Topics

### Custom Configuration

Examples show how to:
- Override smart defaults
- Customize per-module settings
- Define custom publishing logic

### Integration

Examples include:
- GitHub Actions workflows
- Maven publication configuration
- Signing setup

### Troubleshooting

Each example includes:
- Common error solutions
- Validation commands
- Debug output options

## Contributing

To add a new example:

1. Create a new directory under `examples/`
2. Include a complete, working Gradle project
3. Add comprehensive README.md
4. Test all workflows
5. Submit a pull request

## Support

For issues or questions:
- Check example README files
- Review [plugin documentation](https://github.com/tddworks/central-portal-publisher)
- Open an issue on GitHub

## License

Examples are provided under the same license as the Central Portal Publisher plugin.