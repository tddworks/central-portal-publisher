# Changelog

All notable changes to the Central Portal Publisher plugin will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Material for MkDocs documentation site
- Comprehensive API reference documentation
- Advanced troubleshooting guide
- Migration guide from OSSRH and other plugins

## [0.2.1-alpha] - 2024-XX-XX

### Added
- Interactive setup wizard (`setupPublishing` task)
- Auto-detection of project information from git repository
- Auto-detection of environment variables for credentials
- Type-safe configuration DSL with `centralPublisher` extension
- Support for Kotlin Multiplatform projects
- Multi-module project aggregation support
- Comprehensive validation engine with structured error reporting
- GitHub Actions workflow generation
- Dry run mode for testing configurations
- Enhanced error messages and suggestions

### Changed
- **BREAKING**: Renamed DSL from `sonatypePortalPublisher` to `centralPublisher`
- **BREAKING**: Renamed task from `publishToSonatypePortal` to `publishToCentral`
- **BREAKING**: Restructured configuration format for better type safety
- Improved bundle generation with proper Maven repository layout
- Enhanced GPG signing with support for in-memory keys
- Better CI/CD integration with automatic credential detection

### Deprecated
- `sonatypePortalPublisher` DSL (use `centralPublisher` instead)
- `publishToSonatypePortal` task (use `publishToCentral` instead)

### Fixed
- Bundle generation for multi-module projects
- GPG signing with environment variable keys
- Publication detection for Kotlin Multiplatform targets
- Gradle configuration cache compatibility

### Security
- Enhanced credential handling with secure defaults
- Automatic masking of sensitive information in logs
- Support for environment variable-based authentication

## [0.1.2] - 2024-XX-XX

### Fixed
- Bundle upload issues with large artifacts
- GPG signing compatibility with newer GPG versions
- Multi-module dependency resolution

### Changed
- Improved error messages for authentication failures
- Better handling of network timeouts during upload

## [0.1.1] - 2024-XX-XX

### Added
- Support for custom POM configurations
- Validation task (`validatePublishing`)
- Bundle creation task (`bundleArtifacts`)

### Fixed
- Issue with missing sources and javadoc JARs
- Gradle configuration issues in multi-module projects
- Signing configuration not being applied correctly

## [0.1.0] - 2024-XX-XX

### Added
- Initial release of the Central Portal Publisher plugin
- Basic publishing support to Sonatype Central Portal
- GPG signing for artifacts
- Single-module project support
- Basic configuration via `sonatypePortalPublisher` DSL
- Maven POM generation with project metadata
- Bundle creation and upload functionality

### Features
- Gradle 7.0+ compatibility
- Support for Java and Kotlin JVM projects
- Configurable credentials and signing
- Automatic artifact collection and signing
- ZIP bundle generation for Central Portal upload

## Migration Notes

### Upgrading from 0.1.x to 0.2.x

The 0.2.x release introduces breaking changes for improved usability and type safety:

1. **Update DSL configuration:**
   ```kotlin
   // Old (deprecated)
   sonatypePortalPublisher {
       username = "..."
       password = "..."
   }
   
   // New
   centralPublisher {
       credentials {
           username = "..."
           password = "..."
       }
   }
   ```

2. **Update task names:**
   ```bash
   # Old
   ./gradlew publishToSonatypePortal
   
   # New  
   ./gradlew publishToCentral
   ```

3. **Run the migration wizard:**
   ```bash
   ./gradlew setupPublishing --console=plain
   ```
   
   This will automatically detect and migrate your existing configuration.

4. **Update CI/CD scripts** to use new task names and credential format.

See the [Migration Guide](advanced/migration.md) for detailed upgrade instructions.

## Upcoming Features

### Version 0.3.0 (Planned)

- **Plugin Portal publishing** - Publish Gradle plugins to both Central Portal and Plugin Portal
- **Advanced validation rules** - More comprehensive pre-publish validation
- **Custom artifact support** - Support for additional artifact types
- **Integration testing framework** - Built-in support for testing publications
- **Performance optimizations** - Faster bundle generation and upload
- **Plugin extensions API** - Allow third-party plugins to extend functionality

### Version 0.4.0 (Planned)

- **Web dashboard** - Local web interface for managing publications
- **Publication templates** - Reusable configuration templates
- **Batch operations** - Publish multiple projects in one operation
- **Advanced retry logic** - Smart retry with exponential backoff
- **Metrics and monitoring** - Publication success metrics and monitoring

## Contributing

We welcome contributions! Please see our [Contributing Guide](https://github.com/tddworks/central-portal-publisher/blob/main/CONTRIBUTING.md) for details.

### Reporting Issues

- **Bug reports**: Use the [bug report template](https://github.com/tddworks/central-portal-publisher/issues/new?template=bug_report.md)
- **Feature requests**: Use the [feature request template](https://github.com/tddworks/central-portal-publisher/issues/new?template=feature_request.md)
- **Questions**: Check [Discussions](https://github.com/tddworks/central-portal-publisher/discussions) first

### Development

The project follows Test-Driven Development (TDD) practices:

1. Write tests first
2. Implement minimal code to make tests pass  
3. Refactor while keeping tests green
4. All PRs require tests and documentation updates

## Acknowledgments

Special thanks to:

- The Sonatype team for creating the Central Portal
- The Gradle team for the excellent plugin development framework
- Contributors and early adopters who provided valuable feedback
- The Kotlin and Java communities for their continued support

## Links

- **Plugin Portal**: [plugins.gradle.org/plugin/com.tddworks.central-publisher](https://plugins.gradle.org/plugin/com.tddworks.central-publisher)
- **GitHub Repository**: [github.com/tddworks/central-portal-publisher](https://github.com/tddworks/central-portal-publisher)
- **Documentation**: [tddworks.github.io/central-portal-publisher](https://tddworks.github.io/central-portal-publisher)
- **Central Portal**: [central.sonatype.com](https://central.sonatype.com)
- **Issue Tracker**: [github.com/tddworks/central-portal-publisher/issues](https://github.com/tddworks/central-portal-publisher/issues)