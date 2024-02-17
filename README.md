# sonatype-portal-gradle-plugin
## Usage

### Single module

```kotlin
sonatypePortal {
    authentication {
        username = "your-username"
        password = "your-password"
    }
    
    autoPublish = false
}
```

### Multi-module
```kotlin
sonatypePortal {
    authentication {
        username = "your-username"
        password = "your-password"
    }

    autoPublish = false

    modules {
        project(":module1")
        project(":module2")
    }
}
```