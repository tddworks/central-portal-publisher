# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Gradle plugin that helps developers publish artifacts to the Sonatype Central Portal (https://central.sonatype.org/). The plugin simplifies the publishing process for both single-module and multi-module Kotlin/Java projects, handling signing, aggregation, and deployment bundle creation.

## Key Commands

### Build and Test
- `./gradlew clean build` - Clean build the project
- `./gradlew test` - Run all tests using JUnit Platform
- `./gradlew koverVerify koverXmlReport` - Run code coverage verification and generate reports
- `./gradlew check` - Run all code quality checks

### Publishing Tasks
- `./gradlew publishToMavenLocal` - Publish to local Maven repository for testing
- `./gradlew publishPlugins` - Publish the plugin to the Gradle Plugin Portal

### Example Projects
The repository includes two example projects demonstrating plugin usage:
- `example-single-module/` - Single module project example
- `example-multi-modules/` - Multi-module project with aggregation support

To run examples, navigate to the example directory and use `../gradlew` (examples don't have their own wrapper).

## Architecture

### Core Components

1. **Plugin Entry Point**: `SonatypePortalPublisherPlugin` - Main plugin class that configures the publishing tasks and extensions.

2. **API Layer** (`src/main/kotlin/.../portal/api/`)
   - `SonatypePortalPublisher` - Core publisher implementation handling authentication and deployment
   - `Authentication` - Manages Sonatype credentials
   - `DeploymentBundle` - Represents deployment artifacts
   - `FileUploader` - Handles file uploads via OkHttp

3. **Plugin Configuration** (`src/main/kotlin/.../portal/plugin/`)
   - `SonatypePortalPublisherExtension` - DSL for plugin configuration in build scripts
   - `PublishingTaskManager` - Orchestrates publishing tasks across modules
   - `ZipPublicationTaskFactory` - Creates ZIP bundle tasks for deployments

4. **Task Factories** (`src/main/kotlin/.../portal/plugin/tasks/`)
   - `PublishTask` - Main publishing task implementation
   - `DevelopmentBundlePublishTaskFactory` - Creates development bundle publishing tasks
   - `PublishPublicationToMavenRepositoryTaskFactory` - Creates Maven repository publishing tasks

### Key Design Patterns

- **Extension-based Configuration**: Uses Gradle's extension mechanism for clean DSL (`sonatypePortalPublisher {}`)
- **Task Factory Pattern**: Separate factories for different publication types (Maven, KMP, aggregated)
- **Provider Pattern**: Lazy configuration resolution using Gradle's Provider API
- **Repository Manager**: Handles both staging and release repositories for the publishing workflow

### Publishing Flow

1. Plugin reads configuration from `gradle.properties` or environment variables
2. Creates appropriate publication tasks based on project type (single/multi-module)
3. For aggregation mode, collects artifacts from all subprojects
4. Signs artifacts using GPG keys (via Gradle's signing plugin)
5. Creates deployment bundles (ZIP files) in Maven repository layout
6. Uploads to Sonatype Central Portal via REST API

### Configuration Priority

The plugin uses configuration in this order:
1. Environment variables (`SIGNING_KEY`, `SIGNING_PASSWORD`)
2. Gradle properties (`signing.keyId`, `signing.password`, `signing.secretKeyRingFile`)
3. Default signing configuration

## Testing Approach

- Unit tests use JUnit 5 Platform with Mockito for mocking
- Test utilities in `src/test/kotlin/` include builders and test fixtures
- System environment stubbing via `system-stubs-jupiter` for testing environment-based configuration
- Coverage tracked via Kover plugin with reports uploaded to Codecov

## Dependencies

- **HTTP Client**: OkHttp 4.12.0 for REST API communication
- **File I/O**: Okio 3.9.0 for efficient file operations
- **Testing**: JUnit 5, Mockito, AssertJ
- **Build**: Kotlin 1.9.23, Gradle Kotlin DSL