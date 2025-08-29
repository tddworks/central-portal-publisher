# Troubleshooting Guide

Common issues and solutions when using the Central Portal Publisher plugin.

## Configuration Issues

### "Configuration validation failed"

**Symptoms:**
```
> Task :validatePublishing FAILED
Configuration validation failed:
- Missing required field: projectInfo.description
- Missing required field: credentials.username
```

**Solutions:**

1. **Check required fields:**
   ```kotlin
   centralPublisher {
       projectInfo {
           name = "my-project"           // Required
           description = "My awesome project"  // Required
           url = "https://github.com/me/my-project"  // Required
           // ... other required fields
       }
   }
   ```

2. **Verify credentials are set:**
   ```bash
   echo $SONATYPE_USERNAME
   echo $SONATYPE_PASSWORD
   ```

3. **Run the setup wizard:**
   ```bash
   ./gradlew setupPublishing --console=plain
   ```

### "No publications found"

**Symptoms:**
```
> No publications found for project ':myproject'
```

**Solutions:**

1. **For Java projects:**
   ```kotlin
   plugins {
       `java-library`
       `maven-publish`
   }
   
   java {
       withJavadocJar()
       withSourcesJar()
   }
   ```

2. **For Kotlin JVM projects:**
   ```kotlin
   plugins {
       kotlin("jvm")
       `maven-publish`
   }
   
   java {
       withJavadocJar()
       withSourcesJar()
   }
   ```

3. **For Kotlin Multiplatform:**
   ```kotlin
   plugins {
       kotlin("multiplatform")
       `maven-publish`
   }
   ```

## Authentication Issues

### "Authentication failed (401)"

**Symptoms:**
```
> Upload failed: Authentication failed (401)
> Invalid credentials
```

**Solutions:**

1. **Verify you're using Central Portal credentials** (not OSSRH):
   - Username: Your Sonatype Central Portal account username
   - Password: Your Central Portal token (not your login password)

2. **Get your Central Portal token:**
   - Visit [central.sonatype.com](https://central.sonatype.com/)
   - Go to Account → Generate User Token
   - Use the generated token as your password

3. **Test credentials manually:**
   ```bash
   curl -u "$SONATYPE_USERNAME:$SONATYPE_PASSWORD" \
        https://central.sonatype.com/api/v1/publisher/status
   ```

4. **Check environment variables:**
   ```bash
   ./gradlew validatePublishing --info
   ```

### "Access denied (403)"

**Symptoms:**
```
> Upload failed: Access denied (403)
> Namespace not verified
```

**Solutions:**

1. **Verify your namespace** in Central Portal:
   - Go to [central.sonatype.com](https://central.sonatype.com/publishing/namespaces)
   - Ensure your group ID namespace is verified
   - Complete domain or GitHub verification if needed

2. **Check group ID matches namespace:**
   ```kotlin
   group = "com.example"  // Must match verified namespace
   ```

## GPG Signing Issues

### "GPG signing failed"

**Symptoms:**
```
> Execution failed for task ':signMavenPublication'
> Unable to read secret key
```

**Solutions:**

1. **Check key format** - ensure you have the complete armored key:
   ```bash
   gpg --armor --export-secret-keys your-email@example.com
   ```
   
   The key should include headers and footers:
   ```
   -----BEGIN PGP PRIVATE KEY BLOCK-----
   Version: GnuPG v2
   
   [key content]
   -----END PGP PRIVATE KEY BLOCK-----
   ```

2. **For gradle.properties**, escape newlines:
   ```properties
   SIGNING_KEY=-----BEGIN PGP PRIVATE KEY BLOCK-----\nVersion: GnuPG v2\n\n[content]\n-----END PGP PRIVATE KEY BLOCK-----
   ```

3. **For environment variables**, use literal newlines:
   ```bash
   export SIGNING_KEY="-----BEGIN PGP PRIVATE KEY BLOCK-----
   Version: GnuPG v2
   
   [key content]
   -----END PGP PRIVATE KEY BLOCK-----"
   ```

4. **Verify key is not expired:**
   ```bash
   gpg --list-keys
   gpg --list-secret-keys
   ```

5. **Test signing manually:**
   ```bash
   echo "test" | gpg --armor --detach-sign --local-user YOUR_EMAIL
   ```

### "Invalid signature"

**Solutions:**

1. **Check key ID matches:**
   ```bash
   gpg --list-secret-keys --keyid-format SHORT
   ```

2. **Ensure password is correct:**
   ```bash
   echo "test" | gpg --armor --detach-sign --passphrase "$SIGNING_PASSWORD" --batch --local-user YOUR_EMAIL
   ```

## Upload Issues

### "Bundle too large"

**Symptoms:**
```
> Upload failed: Request entity too large (413)
```

**Solutions:**

1. **Check bundle size:**
   ```bash
   ls -lh build/central-portal/*.zip
   ```

2. **For large multi-module projects**, consider individual publishing:
   ```kotlin
   centralPublisher {
       publishing {
           aggregation = false  // Publish modules individually
       }
   }
   ```

3. **Exclude unnecessary files:**
   ```kotlin
   tasks.jar {
       exclude("**/test/**")
       exclude("**/*Test.class")
   }
   ```

### "Network timeout"

**Symptoms:**
```
> Upload failed: Read timeout
> Connection timeout
```

**Solutions:**

1. **Increase timeout:**
   ```kotlin
   // In build.gradle.kts
   tasks.withType<com.tddworks.sonatype.publish.portal.plugin.tasks.PublishTask> {
       timeout.set(Duration.ofMinutes(30))
   }
   ```

2. **Check network connectivity:**
   ```bash
   curl -I https://central.sonatype.com/api/v1/publisher/status
   ```

3. **Retry with exponential backoff** (automatic in plugin)

## Build Issues

### "Task not found"

**Symptoms:**
```
> Task 'publishToCentral' not found in project
```

**Solutions:**

1. **Ensure plugin is applied correctly:**
   ```kotlin
   plugins {
       id("com.tddworks.central-publisher") version "0.2.1-alpha"
   }
   ```

2. **For multi-module projects**, apply to root project only

3. **Check Gradle version** (minimum 7.0 required):
   ```bash
   ./gradlew --version
   ```

### "Plugin not found"

**Symptoms:**
```
> Plugin [id: 'com.tddworks.central-publisher'] was not found
```

**Solutions:**

1. **Check plugin version** exists:
   - Visit [plugins.gradle.org](https://plugins.gradle.org/plugin/com.tddworks.central-publisher)

2. **Update to latest version:**
   ```kotlin
   plugins {
       id("com.tddworks.central-publisher") version "0.2.1-alpha"
   }
   ```

3. **Check internet connection** for plugin download

## Multi-Module Issues

### "Module dependencies not resolved"

**Solutions:**

1. **For aggregated publishing** (default), dependencies are handled automatically

2. **For individual publishing**, ensure correct order:
   ```bash
   ./gradlew :shared:publishToCentral
   ./gradlew :api:publishToCentral      # depends on shared
   ./gradlew :client:publishToCentral   # depends on api
   ```

3. **Use version ranges** for individual publishing:
   ```kotlin
   dependencies {
       implementation("com.example:shared:[1.0,2.0)")
   }
   ```

### "Different versions across modules"

**Solutions:**

1. **Use root project version** (default):
   ```kotlin
   // Root build.gradle.kts
   allprojects {
       version = "1.0.0"
   }
   ```

2. **Or enable individual versioning:**
   ```kotlin
   // In each module
   version = "1.1.0"  // Override root version
   ```

## CI/CD Issues

### GitHub Actions failures

**Common issues:**

1. **Missing secrets:**
   ```yaml
   env:
     SONATYPE_USERNAME: ${{ secrets.SONATYPE_USERNAME }}
     SONATYPE_PASSWORD: ${{ secrets.SONATYPE_PASSWORD }}
     SIGNING_KEY: ${{ secrets.SIGNING_KEY }}
     SIGNING_PASSWORD: ${{ secrets.SIGNING_PASSWORD }}
   ```

2. **Multi-line secrets** - ensure proper YAML formatting:
   ```yaml
   SIGNING_KEY: ${{ secrets.SIGNING_KEY }}  # Not SIGNING_KEY: '${{ secrets.SIGNING_KEY }}'
   ```

3. **Timeout in CI:**
   ```yaml
   - name: Publish to Central
     run: ./gradlew publishToCentral --no-daemon
     timeout-minutes: 30
   ```

### Environment detection issues

**Solutions:**

1. **Debug environment variables:**
   ```bash
   ./gradlew validatePublishing --info
   ```

2. **Check variable names** (case sensitive):
   ```bash
   env | grep SONATYPE
   env | grep SIGNING
   ```

## Performance Issues

### "Build is slow"

**Solutions:**

1. **Enable Gradle optimizations:**
   ```properties
   # gradle.properties
   org.gradle.parallel=true
   org.gradle.caching=true
   org.gradle.configureondemand=true
   ```

2. **Use build cache:**
   ```bash
   ./gradlew publishToCentral --build-cache
   ```

3. **Increase memory:**
   ```properties
   org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g
   ```

## Debugging

### Enable verbose logging

```bash
./gradlew publishToCentral --info --stacktrace
```

### Check configuration

```bash
./gradlew showConfiguration
```

### Dry run mode

```bash
./gradlew publishToCentral -PdryRun=true
```

## Getting Help

If you're still having issues:

1. **Check the logs** with `--info` and `--stacktrace`
2. **Validate your setup** with `./gradlew validatePublishing`
3. **Try dry run mode** to test without uploading
4. **Check Central Portal status** at [status.central.sonatype.com](https://status.central.sonatype.com/)
5. **Search existing issues** on [GitHub](https://github.com/tddworks/central-portal-publisher/issues)
6. **Create a new issue** with full logs and configuration (remove sensitive data)

## Central Portal Web Interface

You can also manage deployments manually:

1. **Visit** [central.sonatype.com/publishing/deployments](https://central.sonatype.com/publishing/deployments)
2. **Upload bundle** manually if automated upload fails
3. **Review and publish** deployments
4. **Check validation results** and error messages
5. **Monitor publication status**

Remember: Even after successful upload, you need to manually review and publish your deployment in the Central Portal web interface (unless `autoPublish = true`).