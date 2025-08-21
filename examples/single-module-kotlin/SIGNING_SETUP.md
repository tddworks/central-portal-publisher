# Setting Up Signing for Maven Central

To publish to Maven Central, you need to sign your artifacts. Here's how to set up signing:

## Option 1: In-Memory GPG Keys (Recommended for CI/CD)

Set these environment variables:

```bash
export SIGNING_KEY="-----BEGIN PGP PRIVATE KEY BLOCK-----
...your private key content...
-----END PGP PRIVATE KEY BLOCK-----"

export SIGNING_PASSWORD="your-gpg-password"
```

## Option 2: File-based Signing (for local development)

Add to your `gradle.properties`:

```properties
signing.keyId=your-key-id
signing.password=your-gpg-password
signing.secretKeyRingFile=/path/to/secring.gpg
```

## Test Current Bundle Creation

Without signing (current state):
```bash
./gradlew bundleArtifacts
```

The bundle will contain:
- ✅ Main artifacts (JAR, POM, sources, javadoc, module.json)
- ✅ All checksums (MD5, SHA1, SHA256, SHA512) - generated automatically by Gradle
- ❌ Missing signatures (.asc files) - need signing setup

With signing configured, the bundle will also include `.asc` signature files for each artifact.

## Verification

After setting up signing, verify by checking the bundle contents or deploying to Central Portal.