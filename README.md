# Sonatype Portal Gradle Plugin

## Usage

### Using the plugins DSL:
```kotlin
plugins {
  id("com.tddworks.sonatype-portal-publisher") version "0.0.1"
}
```

add gradle.properties file in the root project with the following content:
refer to [Signing Plugin](https://docs.gradle.org/current/userguide/signing_plugin.html) for more information.
```properties
## Provide signing information required by Maven Central
signing.keyId=[your-key-id]
signing.password=[your-key-password]
signing.secretKeyRingFile=[your-key-file]

## Provide artifacts information required by Maven Central
POM_NAME=openai-kotlin
POM_DESCRIPTION=OpenAI API KMP Client
POM_URL=https://github.com/tddworks/openai-kotlin
POM_SCM_URL=https://github.com/tddworks/openai-kotlin
POM_SCM_CONNECTION=scm:git:git://github.com/tddworks/openai-kotlin.git
POM_SCM_DEV_CONNECTION=scm:git:ssh://github.com/tddworks/openai-kotlin.git
POM_LICENCE_NAME=MIT License
POM_LICENCE_URL=https://github.com/tddworks/openai-kotlin/blob/main/LICENSE

POM_LICENCE_DIST=repo
POM_DEVELOPER_ID=tddworks
POM_DEVELOPER_NAME=itshan
POM_DEVELOPER_EMAIL=itshan@ttdworks.com
POM_DEVELOPER_ORGANIZATION=tddworks
POM_DEVELOPER_ORGANIZATION_URL=https://tddworks.com
POM_ISSUE_SYSTEM=github
POM_ISSUE_URL=https://github.com/tddworks/openai-kotlin/issues

## Provide Sonatype Portal credentials
SONATYPE_USERNAME=[your-sonatype-username]
SONATYPE_PASSWORD=[your-sonatype-password]
```

Or you can provide the credentials in the system environment, e.g. `SONATYPE_USERNAME` and `SONATYPE_PASSWORD` and `SIGNING_KEY` and `SIGNING_PASSWORD` for the signing key.

```shell
export SONATYPE_USERNAME=your-sonatype-username
export SONATYPE_PASSWORD=your-sonatype-password

export SIGNING_KEY=your-key
export SIGNING_PASSWORD=your-key-password
```
SIGING_KEY can be get from 
```shell
gpg --armor --export-secret-keys foobar@example.com \
    | awk 'NR == 1 { print "SIGNING_KEY=" } 1' ORS='\\n' \
    >> gradle.properties
```

## About signing
The plugin will use the signing information from the gradle.properties file first, and then the system environment. If you don't provide the signing information, the plugin will use the default signing configuration.
### example
```properties
## Provide signing information required by Maven Central - default  - order(3)
signing.keyId=[your-key-id]
signing.password=[your-key-password]
signing.secretKeyRingFile=[your-key-file]


## Provide signing information required by Maven Central - order(1)
SIGNING_KEY=\n-----BEGIN PGP PRIVATE KEY BLOCK-----END PGP PRIVATE KEY BLOCK-----\n
SIGNING_PASSWORD=your-key-password
````

###
```shell
## Provide signing information required by Maven Central - order(2)
export SIGNING_KEY=your-key
export SIGNING_PASSWORD=your-key-password
```


For those two configurations, the plugin will use the credentials from the gradle.properties file first, and then the system environment. and configure the plugin in the build.gradle file as follows:

```kotlin
sonatypePortal {
    settings {
        autoPublish = false
    }
}
```


### Single module

```kotlin
sonatypePortal {
    authentication {
        username = "your-username"
        password = "your-password"
    }

    settings {
        autoPublish = false
    }
}
```

### Multi-modules

Supported Features:
- [x] publish different publications (maven, kotlinMultiplatform, etc.)
  - [x] publishMavenPublicationToSonatypePortalRepository
    - [x] publish by signing from gradle.properties
      - [x] publish by specific username and password
      - [x] publish by system environment, e.g. `SONATYPE_USERNAME` and `SONATYPE_PASSWORD`
    - [x] publish by custom signing
      - [x] publish by specific signing.keyId, signing.password and signing.secretKeyRingFile
      - [x] publish by system environment, e.g. `SIGNING_KEY` and `SIGNING_PASSWORD`
  - [x] publishKotlinMultiplatformPublicationToSonatypeRepository
    - [x] publishMacosX64PublicationToSonatypePortalRepository
    
- [x] publish aggregation publications
  - [x] publish by signing from gradle.properties
    - [x] publishAggregationPublicationsToSonatypePortalRepository
      - kmp aggregated deployment bundle zip layout
        <img src="./docs/images/kmp-aggregated-deployment-bundle.png">

- [x] zip different publications
  - [x] zipMavenPublication
  - [x] zipKotlinMultiplatformPublication
  - [x] zipAllPublications
    - Generated zip files:
      - [x] jvm-deployment-bundle.zip
      - [x] kotlinMultiplatform-deployment-bundle.zip
- [x] zip aggregation
  - [x] zipAggregationPublications

- [x] SCM settings
  - [x] Developers information
  - [x] License information
  - [x] Project URL
  - [x] Project description
  - [x] SCM URL


#### With project isolation
This will publish all the subprojects in its own module.

```kotlin
sonatypePortal {
    authentication {
        username = "your-username"
        password = "your-password"
    }

    settings {
        autoPublish = false
        aggregation = false
    }
}
```

`aggregation = false` will disable the task `zipAggregationPublication`.

#### With project aggregation
```kotlin
sonatypePortal {
    authentication {
        username = "your-username"
        password = "your-password"
    }

    settings {
        autoPublish = false
        aggregation = true
    }
}
```

This will publish all the subprojects in the root project.

`aggregation = true` will enable the task `zipAggregationPublication`, which will generate a zip file containing all the subprojects' artifacts. You can find the zip file in the `build/sonatype/zip` directory.

```shell
example-multi-modules/build/sonatype/zip/aggregated-deployment-bundle.zip
```

You can run the following command to generate the zip file:

```shell
gradle clean zipAggregationPublication
```

# Publish to Sonatype Portal
## kmp deployment info
[<img src="./docs/images/kmp-deployment-Info.png">](https://central.sonatype.com/publishing/deployments)


# CI Pipeline on Github Actions
## Config the secrets first
[<img src="./docs/images/github-action-secrets.png">](https://docs.github.com/en/actions/reference/encrypted-secrets)

```yaml
name: build and publish a release to Central Sonatype

on:
  workflow_dispatch:

jobs:
  build:
    runs-on: macOS-latest
    env:
      SONATYPE_USERNAME: ${{ secrets.SONATYPE_USERNAME }}
      SONATYPE_PASSWORD: ${{ secrets.SONATYPE_PASSWORD }}
      SIGNING_KEY: ${{ secrets.SIGNING_KEY }}
      SIGNING_PASSWORD: ${{ secrets.SIGNING_PASSWORD }}
    steps:
      - name: Checkout sources
        uses: actions/checkout@v4
      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 21
      - name: Setup Gradle
        uses: gradle/gradle-build-action@v3
      - name: Release to Central Sonatype with Gradle
        run: ./gradlew build publishAggregationPublicationsToSonatypePortalRepository
```

# Maven Repository Layout

```shell
$ tree .
└─ com
   └─ sonatype
      └─ central
         └─ example
            └─ example_java_project
               └─ 0.1.0
                   ├── example_java_project-0.1.0-javadoc.jar
                   ├── example_java_project-0.1.0-javadoc.jar.asc
                   ├── example_java_project-0.1.0-javadoc.jar.md5
                   ├── example_java_project-0.1.0-javadoc.jar.sha1
                   ├── example_java_project-0.1.0-sources.jar
                   ├── example_java_project-0.1.0-sources.jar.asc
                   ├── example_java_project-0.1.0-sources.jar.md5
                   ├── example_java_project-0.1.0-sources.jar.sha1
                   ├── example_java_project-0.1.0.jar
                   ├── example_java_project-0.1.0.jar.asc
                   ├── example_java_project-0.1.0.jar.md5
                   ├── example_java_project-0.1.0.jar.sha1
                   ├── example_java_project-0.1.0.pom
                   ├── example_java_project-0.1.0.pom.asc
                   ├── example_java_project-0.1.0.pom.md5
                   └── example_java_project-0.1.0.pom.sha1

```
### Q&A
[Does the Portal support SNAPSHOT releases?](https://central.sonatype.org/faq/snapshot-releases/#releasing-to-central)
Does the Portal support SNAPSHOT releases?⚓︎
Question⚓︎
Does the Central Portal support -SNAPSHOT releases?

Answer⚓︎
Historically, users have been able to publish -SNAPSHOT releases to OSSRH (but not to Maven Central). The intention of this was to allow users to verify their own releases prior to publishing to Maven Central, but it has the side effect of users being able to make their pre-release versions available to their communities during ongoing development.

The Central Publisher Portal does not support -SNAPSHOT releases, and deployments of -SNAPSHOT releases will have a version cannot be a SNAPSHOT error in their validation results. Versions should only be published to the Portal if they are intended to reach Maven Central.

The Portal supports a more limited feature that is intended to fill the original need for publishers to be able to perform manual verification of their builds. Publishers will be able to point their build configurations to either specific deployments or any VALIDATED deployment that they published in order to use the component before publishing. This will be particularly useful for publishers who publish via CI pipelines, but want to verify locally.

Alternative Options⚓︎
If you are looking for a solution that is able to handle -SNAPSHOT releases for your project, consider Sonatype Nexus Repository which supports maven-snapshots as a repository out of the box.