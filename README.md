# sonatype-portal-gradle-plugin
## Usage

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

#### With project aggregation
This will publish all the subprojects in the root project.
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

# Example


# Maven Repository Layout.
```shell
$ tree
.
`-- com
    `-- sonatype
        `-- central
            `-- example
                `-- example_java_project
                    `-- 0.1.0
                        |-- example_java_project-0.1.0-javadoc.jar
                        |-- example_java_project-0.1.0-javadoc.jar.asc
                        |-- example_java_project-0.1.0-javadoc.jar.md5
                        |-- example_java_project-0.1.0-javadoc.jar.sha1
                        |-- example_java_project-0.1.0-sources.jar
                        |-- example_java_project-0.1.0-sources.jar.asc
                        |-- example_java_project-0.1.0-sources.jar.md5
                        |-- example_java_project-0.1.0-sources.jar.sha1
                        |-- example_java_project-0.1.0.jar
                        |-- example_java_project-0.1.0.jar.asc
                        |-- example_java_project-0.1.0.jar.md5
                        |-- example_java_project-0.1.0.jar.sha1
                        |-- example_java_project-0.1.0.pom
                        |-- example_java_project-0.1.0.pom.asc
                        |-- example_java_project-0.1.0.pom.md5
                        `-- example_java_project-0.1.0.pom.sha1
```