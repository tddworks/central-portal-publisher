# Sonatype Portal Gradle Plugin

## Usage
add gradle.properties file in the root project with the following content:
refer to [Signing Plugin](https://docs.gradle.org/current/userguide/signing_plugin.html) for more information.
```properties
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
      - [ ] publish by system environment, e.g. `SONATYPE_USERNAME` and `SONATYPE_PASSWORD`
    - [ ] publish by custom signing
      - [ ] publish by specific username and password
      - [ ] publish by system environment, e.g. `SONATYPE_USERNAME` and `SONATYPE_PASSWORD`
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

- [ ] scm settings
  - Developers information is missing
  - License information is missing
  - Project URL is not defined
  - Project description is missing
  - SCM URL is not defined


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