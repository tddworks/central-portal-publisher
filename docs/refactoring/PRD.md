# Product Requirements Document (PRD)
## Central Portal Publisher Plugin Refactoring

### Version: 1.0
### Date: 2024-01-20
### Status: Draft

---

## 1. Executive Summary

The Central Portal Publisher Gradle plugin currently requires extensive configuration and deep understanding of Maven publishing concepts. This refactoring initiative aims to dramatically reduce the cognitive load and learning curve for developers by implementing sensible defaults, auto-detection capabilities, and a streamlined configuration API.

## 2. Problem Statement

### Current Pain Points

1. **Configuration Overhead**: Users must configure 15+ properties in gradle.properties
2. **Multiple Configuration Sources**: Settings scattered across gradle.properties, environment variables, and build.gradle
3. **Manual Signing Setup**: Complex GPG key configuration with multiple formats
4. **Unclear Task Naming**: Long, technical task names like `publishAggregationPublicationsToSonatypePortalRepository`
5. **Lack of Guidance**: No validation or helpful error messages for misconfiguration
6. **Platform Complexity**: Different configurations needed for single vs multi-module projects

### User Impact

- New users spend 2-4 hours understanding configuration
- High error rate during initial setup (60% of users encounter signing issues)
- Frequent support requests about configuration precedence
- Developers often copy-paste configurations without understanding

## 3. Goals and Objectives

### Primary Goals

1. **Zero-to-Publish in < 5 minutes** for standard projects
2. **80% reduction in required configuration** for common use cases
3. **Intelligent defaults** that work for most projects
4. **Progressive disclosure** - simple things simple, complex things possible

### Success Metrics

- Time to first successful publish: < 5 minutes
- Required configuration lines: < 5 for basic setup
- User-reported issues: 70% reduction
- Documentation queries: 50% reduction

## 4. Target Users

### Primary Personas

1. **Solo Developer Sarah**
   - Publishing first open-source library
   - Limited Maven Central experience
   - Wants quick setup, minimal configuration

2. **Team Lead Tom**
   - Managing multi-module enterprise project
   - Needs fine-grained control
   - Values consistency and automation

3. **OSS Maintainer Maria**
   - Multiple projects to publish
   - CI/CD integration critical
   - Needs reliable, repeatable process

## 5. Solution Overview

### Core Principles

1. **Convention over Configuration**: Smart defaults based on project structure
2. **Progressive Enhancement**: Start simple, add complexity as needed
3. **Fail-Fast with Guidance**: Clear error messages with actionable fixes
4. **Auto-Detection**: Discover project information automatically

### Key Features

#### 5.1 Simplified Configuration DSL

```kotlin
// Minimal configuration - everything else auto-detected
centralPublisher {
    // Auto-detects from git, gradle.properties, or prompts
}

// Or with explicit minimal setup
centralPublisher {
    credentials {
        // Auto-loads from env vars or gradle.properties
    }
    projectInfo {
        // Auto-populates from git and project metadata
    }
}
```

#### 5.2 Interactive Setup Wizard

```bash
./gradlew centralPublisherSetup
```
- Guided setup process
- Validates credentials
- Generates required files
- Tests configuration

#### 5.3 Auto-Detection Capabilities

- **Project Info**: From git config, existing POM, or build.gradle
- **Credentials**: From keychain, env vars, or gradle.properties
- **Signing**: From GPG agent or configured keys
- **Module Structure**: Automatic multi-module detection

#### 5.4 Simplified Task Names

Old → New mapping:
- `publishAggregationPublicationsToSonatypePortalRepository` → `publishToCentral`
- `zipAggregationPublications` → `bundleArtifacts`
- `publishMavenPublicationToSonatypePortalRepository` → `publishMaven`

#### 5.5 Smart Validation

- Pre-flight checks before publishing
- Configuration validation with fix suggestions
- Dry-run mode for testing
- Requirements checklist

## 6. Detailed Requirements

### 6.1 Configuration Management

#### Must Have
- Single source of truth for configuration
- Environment variable auto-loading
- Sensible defaults for all settings
- Configuration validation

#### Should Have
- Configuration profiles (dev/staging/prod)
- Configuration import/export
- Team sharing capabilities

#### Nice to Have
- Web-based configuration generator
- IDE plugin for configuration

### 6.2 Developer Experience

#### Must Have
- Clear, actionable error messages
- Progress indicators for long operations
- Verbose mode for debugging
- Dry-run capability

#### Should Have
- Interactive prompts for missing config
- Auto-fix suggestions
- Configuration migration tool

### 6.3 Documentation

#### Must Have
- Quick start guide (< 5 min)
- Migration guide from v0.x
- Troubleshooting guide
- Example projects

#### Should Have
- Video tutorials
- Configuration reference
- Best practices guide

## 7. Technical Architecture

### 7.1 Configuration Layers (Priority Order)

1. Explicit DSL configuration
2. gradle.properties
3. Environment variables
4. Auto-detected values
5. Sensible defaults

### 7.2 Module Structure

```
plugin/
├── core/           # Core publishing logic
├── config/         # Configuration management
├── autodetect/     # Auto-detection logic
├── validation/     # Validation and checks
├── tasks/          # Simplified tasks
└── wizard/         # Interactive setup
```

### 7.3 Backward Compatibility

- Maintain support for existing configuration
- Deprecation warnings for old APIs
- Migration path for existing users
- Feature flags for new behavior

## 8. Implementation Phases

### Phase 1: Foundation (Week 1-2)
- Configuration layer refactoring
- Auto-detection framework
- Validation system

### Phase 2: Simplification (Week 3-4)
- New DSL implementation
- Simplified task structure
- Smart defaults

### Phase 3: Developer Experience (Week 5-6)
- Setup wizard
- Error message improvements
- Progress indicators

### Phase 4: Polish (Week 7-8)
- Documentation
- Examples
- Migration tools
- Testing

## 9. Risks and Mitigation

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| Breaking changes | High | Medium | Feature flags, deprecation period |
| Auto-detection failures | Medium | Low | Fallback to manual config |
| Complexity increase | Medium | Low | Modular design, clear separation |
| User adoption | Medium | Medium | Clear migration benefits, tooling |

## 10. Success Criteria

- [ ] New user can publish in < 5 minutes
- [ ] Configuration reduced by 80% for standard cases
- [ ] All existing features maintained
- [ ] 90% of users prefer new configuration
- [ ] Support tickets reduced by 50%

## 11. Future Considerations

- IDE plugins for IntelliJ and VS Code
- Web dashboard for publication management
- GitHub Actions marketplace action
- Kotlin Multiplatform specific optimizations
- Integration with other repositories (GitHub Packages, etc.)

## 12. Appendix

### A. Current Configuration Example (Before)

```properties
# gradle.properties - 20+ lines
signing.keyId=ABCD1234
signing.password=secret
signing.secretKeyRingFile=/path/to/key
POM_NAME=My Library
POM_DESCRIPTION=A description
POM_URL=https://github.com/user/repo
POM_SCM_URL=https://github.com/user/repo
POM_SCM_CONNECTION=scm:git:git://github.com/user/repo.git
POM_SCM_DEV_CONNECTION=scm:git:ssh://github.com/user/repo.git
POM_LICENCE_NAME=MIT License
POM_LICENCE_URL=https://github.com/user/repo/blob/main/LICENSE
POM_LICENCE_DIST=repo
POM_DEVELOPER_ID=userid
POM_DEVELOPER_NAME=User Name
POM_DEVELOPER_EMAIL=user@example.com
POM_DEVELOPER_ORGANIZATION=Org
POM_DEVELOPER_ORGANIZATION_URL=https://org.com
SONATYPE_USERNAME=username
SONATYPE_PASSWORD=password
```

### B. New Configuration Example (After)

```kotlin
// build.gradle.kts - 3 lines for basic setup
centralPublisher {
    // Everything auto-detected or defaulted
}
```

Or with customization:
```kotlin
centralPublisher {
    credentials {
        // Auto-loads from environment or gradle.properties
    }
    
    projectInfo {
        // Auto-populated from git, can override specific fields
        description = "My awesome library"
    }
    
    publishing {
        autoPublish = true  // Default: false
        dryRun = false      // Default: false in prod
    }
}
```