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

- **TDD Methodology**: Always write tests first, then implement to make them pass
- Unit tests use JUnit 5 Platform with AssertJ for assertions  
- Test utilities in `src/test/kotlin/` include builders and test fixtures
- System environment stubbing via `system-stubs-jupiter` for testing environment-based configuration
- Coverage tracked via Kover plugin with reports uploaded to Codecov
- **Test Structure**: Organize tests by feature/component, use descriptive test names

## Dependencies

- **HTTP Client**: OkHttp 4.12.0 for REST API communication
- **File I/O**: Okio 3.9.0 for efficient file operations
- **Testing**: JUnit 5, Mockito, AssertJ
- **Build**: Kotlin 1.9.23, Gradle Kotlin DSL

## Development Practices

### TDD Process
1. **Red**: Write a failing test that describes the desired functionality
2. **Green**: Write minimal code to make the test pass
3. **Refactor**: Improve code quality while keeping tests green
4. Always run tests before committing changes

### Task Management
- **Always** update task status in `docs/refactoring/implementation-tasks.md` when completing work
- Use the TodoWrite tool to track progress within Claude Code sessions
- Mark tasks as `in_progress` when starting, `completed` when finished
- Update time tracking with actual vs. estimated hours

### Code Quality
- Write descriptive, meaningful test names using backticks: `` `should validate empty configuration and return errors` ``
- Use AssertJ assertions for readable test failures
- Follow existing naming conventions and code style
- Add KDoc/JavaDoc for public APIs and DSL components

## Current Refactoring Status

**Project is undergoing major refactoring** - see `docs/refactoring/implementation-tasks.md` for details.

**Completed Components:**
- ✅ Configuration Model with builders and validation
- ✅ Configuration Source Manager with multi-source loading
- ✅ Auto-Detection Framework (GitInfoDetector, ProjectInfoDetector)
- ✅ Validation Engine with structured error reporting
- ✅ Type-Safe DSL Structure with comprehensive test coverage
- ✅ Task Simplification and error messaging improvements

**Current Status:** Phase 1 (Configuration Layer) complete, Phase 2 (DSL) in progress

### New Architecture (Post-Refactoring)

1. **Configuration Layer** (`src/main/kotlin/.../config/`)
   - `CentralPublisherConfig` - Immutable configuration model with serialization
   - `CentralPublisherConfigBuilder` - Builder pattern for configuration construction
   - `ConfigurationSourceManager` - Multi-source configuration loading with precedence

2. **Auto-Detection** (`src/main/kotlin/.../autodetection/`)
   - `AutoDetector` interface - Contract for auto-detection components
   - `GitInfoDetector` - Automatically detects git repository information
   - `ProjectInfoDetector` - Detects project names and descriptions

3. **Validation** (`src/main/kotlin/.../validation/`)
   - `ValidationEngine` - Orchestrates validation rules
   - `ValidationViolation` - Structured validation errors with suggestions
   - Extensible validator system with severity levels (ERROR/WARNING/INFO)

4. **DSL** (`src/main/kotlin/.../dsl/`)
   - `CentralPublisherExtension` - Main Gradle extension for type-safe DSL
   - Type-safe builder classes for each configuration section
   - Compile-time validation and IDE auto-completion support