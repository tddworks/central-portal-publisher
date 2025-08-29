# Migration Guide

This guide helps you migrate to the Central Portal Publisher plugin from other publishing solutions or upgrade between plugin versions.

## Migrating from OSSRH/Nexus Publishing

If you're currently using the traditional OSSRH (OSS Repository Hosting) with `maven-publish` and `signing` plugins:

### Before (Traditional OSSRH)

```kotlin
plugins {
    `maven-publish`
    signing
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            
            pom {
                name.set("My Library")
                description.set("A useful library")
                url.set("https://github.com/myorg/my-library")
                
                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                
                developers {
                    developer {
                        id.set("myid")
                        name.set("My Name")
                        email.set("me@example.com")
                    }
                }
                
                scm {
                    connection.set("scm:git:git://github.com/myorg/my-library.git")
                    developerConnection.set("scm:git:ssh://github.com/myorg/my-library.git")
                    url.set("https://github.com/myorg/my-library")
                }
            }
        }
    }
    
    repositories {
        maven {
            name = "OSSRH"
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = project.findProperty("ossrhUsername")?.toString()
                password = project.findProperty("ossrhPassword")?.toString()
            }
        }
    }
}

signing {
    sign(publishing.publications["maven"])
}
```

### After (Central Portal Publisher)

```kotlin
plugins {
    id("com.tddworks.central-publisher") version "0.2.1-alpha"
}

centralPublisher {
    credentials {
        username = project.findProperty("SONATYPE_USERNAME")?.toString() ?: ""
        password = project.findProperty("SONATYPE_PASSWORD")?.toString() ?: ""
    }
    
    projectInfo {
        name = "My Library"
        description = "A useful library"
        url = "https://github.com/myorg/my-library"
        
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
            url = "https://github.com/myorg/my-library"
            connection = "scm:git:git://github.com/myorg/my-library.git"
            developerConnection = "scm:git:ssh://github.com/myorg/my-library.git"
        }
    }
    
    signing {
        key = project.findProperty("SIGNING_KEY")?.toString() ?: ""
        password = project.findProperty("SIGNING_PASSWORD")?.toString() ?: ""
    }
}
```

### Key Differences

1. **Simpler configuration** - No need to manually configure `publishing` and `signing`
2. **Auto-detection** - Many fields can be auto-detected from git
3. **New credentials** - Use Central Portal token instead of OSSRH password
4. **Single bundle** - All artifacts uploaded in one deployment
5. **Manual approval** - Review in Central Portal web interface before publishing

### Migration Steps

1. **Get Central Portal account** at [central.sonatype.com](https://central.sonatype.com/)
2. **Verify your namespace** (same as your group ID)
3. **Generate user token** for authentication
4. **Run setup wizard**: `./gradlew setupPublishing --console=plain`
5. **Remove old configuration** (publishing, signing blocks)
6. **Update CI/CD** with new credentials and tasks

## Migrating from Gradle Maven Publish Plugin

If you're using `com.vanniktech.maven.publish`:

### Before

```kotlin
plugins {
    id("com.vanniktech.maven.publish") version "0.25.3"
}

mavenPublishing {
    coordinates("com.example", "my-library", "1.0.0")
    
    pom {
        name.set("My Library")
        description.set("A useful library")
        inceptionYear.set("2023")
        url.set("https://github.com/myorg/my-library")
        
        licenses {
            license {
                name.set("Apache License 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        
        developers {
            developer {
                id.set("myid")
                name.set("My Name") 
                email.set("me@example.com")
            }
        }
        
        scm {
            url.set("https://github.com/myorg/my-library")
            connection.set("scm:git:git://github.com/myorg/my-library.git")
            developerConnection.set("scm:git:ssh://github.com/myorg/my-library.git")
        }
    }
    
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()
}
```

### After

```kotlin
plugins {
    id("com.tddworks.central-publisher") version "0.2.1-alpha"
}

group = "com.example"
version = "1.0.0"

centralPublisher {
    // Same configuration as shown above
}
```

### Key Changes

- Replace `mavenPublishing` with `centralPublisher`
- Move `coordinates()` to root `group` and `version`
- Simpler signing configuration
- Interactive setup wizard available

## Upgrading Plugin Versions

### From 0.1.x to 0.2.x

**Breaking Changes:**

1. **DSL Name Change:**
   ```kotlin
   // Old (deprecated)
   sonatypePortalPublisher {
       // config
   }
   
   // New
   centralPublisher {
       // same config structure
   }
   ```

2. **Task Name Changes:**
   - `publishToSonatypePortal` → `publishToCentral`
   - `bundleForSonatypePortal` → `bundleArtifacts`

3. **Configuration Structure:**
   Most configuration remains the same, but some field names changed:
   ```kotlin
   // Old
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

**Migration Steps:**

1. **Update plugin version:**
   ```kotlin
   plugins {
       id("com.tddworks.central-publisher") version "0.2.1-alpha"
   }
   ```

2. **Run migration wizard:**
   ```bash
   ./gradlew setupPublishing --console=plain
   ```
   This will automatically migrate your old configuration.

3. **Update CI/CD scripts:**
   ```yaml
   # Old
   - run: ./gradlew publishToSonatypePortal
   
   # New
   - run: ./gradlew publishToCentral
   ```

4. **Test the migration:**
   ```bash
   ./gradlew validatePublishing
   ```

### From 0.2.x to 0.3.x (Future)

When available, upgrade steps will be documented here.

## Migrating Multi-Module Projects

### Before (Manual Configuration)

```kotlin
// Root build.gradle.kts
subprojects {
    apply(plugin = "maven-publish")
    apply(plugin = "signing")
    
    publishing {
        // Duplicate configuration in each module
    }
    
    signing {
        // Duplicate signing configuration
    }
}
```

### After (Simplified)

```kotlin
// Root build.gradle.kts
plugins {
    id("com.tddworks.central-publisher") version "0.2.1-alpha"
}

// Single configuration applies to all modules
centralPublisher {
    // Configuration here
}

// Each module build.gradle.kts
plugins {
    `maven-publish`  // Still required
}
```

## Environment Variable Migration

### Old OSSRH Variables

```bash
# OSSRH (old)
export OSSRH_USERNAME=your-username
export OSSRH_PASSWORD=your-password
export SIGNING_KEY_ID=your-key-id
export SIGNING_PASSWORD=your-gpg-password
export SIGNING_SECRET_KEY_RING_FILE=/path/to/secring.gpg
```

### New Central Portal Variables

```bash
# Central Portal (new)
export SONATYPE_USERNAME=your-username
export SONATYPE_PASSWORD=your-central-token
export SIGNING_KEY="-----BEGIN PGP PRIVATE KEY BLOCK-----..."
export SIGNING_PASSWORD=your-gpg-password
```

## CI/CD Migration

### GitHub Actions

**Before (OSSRH):**
```yaml
- name: Publish to Maven Central
  run: ./gradlew publishToSonatype closeAndReleaseSonatypeStagingRepository
  env:
    OSSRH_USERNAME: ${{ secrets.OSSRH_USERNAME }}
    OSSRH_PASSWORD: ${{ secrets.OSSRH_PASSWORD }}
    SIGNING_KEY_ID: ${{ secrets.SIGNING_KEY_ID }}
    SIGNING_PASSWORD: ${{ secrets.SIGNING_PASSWORD }}
    SIGNING_SECRET_KEY_RING_FILE: ${{ secrets.SIGNING_SECRET_KEY_RING_FILE }}
```

**After (Central Portal):**
```yaml
- name: Publish to Maven Central
  run: ./gradlew publishToCentral
  env:
    SONATYPE_USERNAME: ${{ secrets.SONATYPE_USERNAME }}
    SONATYPE_PASSWORD: ${{ secrets.SONATYPE_PASSWORD }}
    SIGNING_KEY: ${{ secrets.SIGNING_KEY }}
    SIGNING_PASSWORD: ${{ secrets.SIGNING_PASSWORD }}
```

**Key Changes:**
- Simpler single-step publishing
- No need for staging repository management
- Updated secret names
- Reduced complexity

## Common Migration Issues

### "Namespace not verified"

When migrating from OSSRH to Central Portal:

1. **Verify your namespace** in Central Portal
2. **Same group ID** should work if you had OSSRH access
3. **Complete verification process** if needed

### "Authentication failed"

1. **Use Central Portal token**, not OSSRH password
2. **Generate token** at central.sonatype.com
3. **Update CI/CD secrets** with new credentials

### "GPG key issues"

1. **Export full armored key**:
   ```bash
   gpg --armor --export-secret-keys your-email@example.com
   ```

2. **Include complete headers/footers**
3. **Test signing** before migration:
   ```bash
   echo "test" | gpg --armor --detach-sign --local-user your-email@example.com
   ```

### "Publication not found"

Ensure you still have necessary plugins:
```kotlin
plugins {
    `maven-publish`  // Still required
    // Remove `signing` plugin - handled by central-publisher
}
```

## Rollback Strategy

If you need to rollback to your previous setup:

1. **Keep backup** of your old build.gradle.kts
2. **Preserve old CI/CD scripts**
3. **Keep old credentials** as backup
4. **Test rollback** in a separate branch first

## Validation After Migration

After migration, validate everything works:

```bash
# 1. Validate configuration
./gradlew validatePublishing

# 2. Test bundle creation
./gradlew bundleArtifacts

# 3. Dry run publish
./gradlew publishToCentral -PdryRun=true

# 4. Full publish (when ready)
./gradlew publishToCentral
```

## Getting Help

If you encounter issues during migration:

1. **Run the setup wizard**: `./gradlew setupPublishing --console=plain`
2. **Check troubleshooting guide**: [troubleshooting.md](troubleshooting.md)
3. **Search existing issues**: [GitHub Issues](https://github.com/tddworks/central-portal-publisher/issues)
4. **Ask for help**: Create a new issue with migration details

The setup wizard can automatically detect and migrate most configurations, making the migration process smoother.