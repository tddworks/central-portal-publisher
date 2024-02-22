# Sonatype Portal Gradle Plugin

## Usage
add gradle.properties file in the root project with the following content:
refer to [Signing Plugin](https://docs.gradle.org/current/userguide/signing_plugin.html) for more information.
```properties
signing.keyId=[your-key-id]
signing.password=[your-key-password]
signing.secretKeyRingFile=[your-key-file]
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
- [ ] publish different publications (maven, kotlinMultiplatform, etc.)
  - [x] publishMavenPublicationToSonatypePortalRepository
    - [ ] publish by specific username and password
    - [ ] publish by system environment, e.g. `SONATYPE_USERNAME` and `SONATYPE_PASSWORD`
  - [ ] publishKotlinMultiplatformPublicationToSonatypeRepository
- [ ] publish aggregation publications
  - [ ] publishAggregationPublicationToSonatypeRepository

- [ ] zip different publications
  - [x] zipMavenPublication
  - [ ] zipKotlinMultiplatformPublication
  - [ ] zipAllPublications
- [x] zip aggregation
  - [x] zipAggregationPublication

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