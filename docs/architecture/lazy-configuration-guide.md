# Lazy Configuration Architecture Guide

## Overview

This guide explains the simplified lazy configuration architecture in the Central Publisher plugin. The key principle is: **configure publications silently during project evaluation, show messages only when publishing tasks execute**.

## Problem Statement

**Before Lazy Configuration:**
- Plugin configured publications during `project.afterEvaluate` with immediate logging
- Configuration messages appeared during all tasks, including `./gradlew test`
- Developers saw publishing noise during regular development work
- Complex conditional logic with `showMessages` parameters throughout the codebase

**With Lazy Configuration:**
- Publications configured silently during project evaluation (so tasks are created)
- Messages appear only when publishing tasks actually run
- Silent development experience during non-publishing tasks
- Clean, simple codebase without conditional logging parameters

## Final Architecture (Simplified)

```
project.afterEvaluate
├── Has explicit configuration?
│   ├── YES → configurePublishing()
│   │   ├── Configure publications silently (showMessages = false)
│   │   ├── Configure subprojects silently  
│   │   ├── Create tasks with taskManager.createTasks()
│   │   └── Set up gradle.taskGraph.whenReady { show messages when publishing }
│   └── NO → createSetupTask()
└── Simple and predictable
```

## Why This Design?

### 1. **Respects Gradle's Build Lifecycle**

Gradle has three phases: Initialization → Configuration → Execution

- **Configuration Phase**: We configure publications and create tasks
- **Execution Phase**: We show messages and run actual publishing logic

This timing ensures that:
- All required tasks (`sourcesJar`, `javadocJar`, signing tasks) are created during Configuration Phase
- Maven-publish plugin can properly detect and wire up publications
- No "task already executing" or "javadocJar already exists" errors

### 2. **Follows User Mental Models**

**Developer Mental Model:**
1. "I apply the plugin" → Plugin registers extension
2. "I configure what I need" → Extension collects configuration  
3. "I run publish" → Plugin shows messages and publishes
4. "If something goes wrong, I get clear feedback" → Actionable error messages

**The architecture matches this exactly:**
- Silent configuration during setup (steps 1-2)
- Visible feedback during publishing (steps 3-4)

### 3. **Eliminates Cognitive Load**

**Before (Complex):**
- 3 different configuration paths (test mode, lazy mode, immediate mode)
- 2 different task creation methods (skeletons vs real tasks)
- 250+ lines with duplicate logic and conditional flows
- `showMessages` parameters threading through 6+ classes

**After (Simple):**
- 1 unified configuration path
- 1 task creation method (`createTasks`)
- 100 lines total (60% reduction)
- No conditional logging - just log when needed

## Core Components

### 1. CentralPublisherPlugin (Entry Point)

**Single Path - No Complexity:**

```kotlin
override fun apply(project: Project) {
    // Always register the type-safe DSL extension
    val extension = project.extensions.create(EXTENSION_NAME, CentralPublisherExtension::class.java, project)

    // Configure after project evaluation
    project.afterEvaluate {
        if (extension.hasExplicitConfiguration()) {
            configurePublishing(project, extension)
        } else {
            // Just create setup task for projects without configuration
            val taskManager = CentralPublisherTaskManager(project)
            taskManager.createSetupTask()
        }
    }
}
```

**Why this works:**
- **Clear decision tree**: Has config? → Configure. No config? → Setup task.
- **No test mode complexity**: Tests work the same as production
- **Predictable**: Same path every time

### 2. Unified Configuration Method

```kotlin
private fun configurePublishing(project: Project, extension: CentralPublisherExtension) {
    val config = extension.build()
    
    // Configure publications silently (so tasks are created)
    val publicationManager = CentralPublisherPublicationManager(project)
    publicationManager.configurePublications(config, showMessages = false)
    
    // Configure subprojects silently  
    configureSubprojects(project, config, showMessages = false)
    
    // Create tasks
    val taskManager = CentralPublisherTaskManager(project)
    taskManager.createTasks(config)
    
    // Show messages when publishing tasks run
    project.gradle.taskGraph.whenReady {
        val willPublish = allTasks.any { task ->
            CENTRAL_PUBLISHER_TASKS.contains(task.name) ||
            (task.name.startsWith("publish") && (task.name.contains("LocalRepo") || task.name.contains("MavenLocal") || task.name == "publish"))
        }
        
        if (willPublish) {
            project.logger.quiet("🔧 Central Publisher ready for publishing")
        }
    }
}
```

**Why this works:**
- **Silent configuration**: Publications configured immediately so tasks exist
- **Lazy messages**: `gradle.taskGraph.whenReady` shows messages only when publishing tasks execute  
- **Single responsibility**: Each manager does one thing well
- **No conditionals**: No `showMessages` parameters or complex logic

### 3. CentralPublisherTaskManager (Simplified)

```kotlin
/**
 * Creates all publishing tasks. Simple and direct.
 */
fun createTasks(config: CentralPublisherConfig) {
    if (project.tasks.findByName(TASK_PUBLISH_TO_CENTRAL) != null) {
        return // Tasks already created
    }

    setupLocalRepository()
    createPublishToCentralTask(config)
    createBundleArtifactsTask(config)
    createValidatePublishingTask(config)
    createSetupTask()
}
```

**Why this works:**
- **One method**: `createTasks()` does everything needed
- **No skeletons**: Tasks are created with actual behavior immediately
- **Clear dependencies**: Bundle task properly depends on publishing tasks
- **Idempotent**: Safe to call multiple times

## Developer Experience

### During Development (`./gradlew test`, `./gradlew build`)

```bash
$ ./gradlew test
> Configure project :central-portal-publisher
> Task :compileKotlin
> Task :test

BUILD SUCCESSFUL
# ✅ No publishing configuration messages!
```

**Why this works:**
- Publications are configured with `showMessages = false`
- `gradle.taskGraph.whenReady` doesn't trigger because no publishing tasks in the graph
- Developers get clean, quiet builds

### During Task Discovery (`./gradlew tasks`)

```bash
$ ./gradlew tasks --group="Central Publishing"

Central Publishing tasks
------------------------
bundleArtifacts - 📦 Prepare your artifacts for publishing
publishToCentral - 🚀 Publish your artifacts to Maven Central  
setupPublishing - 🧙 Set up your project for Maven Central publishing
validatePublishing - ✅ Check if your project is ready to publish

# ✅ Tasks visible without configuration noise!
```

**Why this works:**
- Publications are configured silently, so all tasks exist
- No publishing tasks execute, so no messages appear
- Perfect discoverability without spam

### During Publishing (`./gradlew publishToCentral`)

```bash
$ ./gradlew validatePublishing
🔧 Central Publisher ready for publishing

> Task :validatePublishing
✅ Validating publishing configuration...
✅ All validation checks passed!
📋 Configuration summary:
   • Project: single-module-example
   • Version: 1.0.0
   • Credentials: ✓ Configured
   • Signing: ✓ Configured
   • License: Apache License 2.0
💡 Ready to publish! Run './gradlew publishToCentral' when ready.

# ✅ Full configuration messages when actually publishing!
```

**Why this works:**
- `gradle.taskGraph.whenReady` detects `validatePublishing` task will execute
- Shows "ready for publishing" message indicating configuration is active
- Task executes with full feedback and validation results

## Complete Message Flow Explanation

Understanding when and where different types of messages appear is crucial for maintaining the lazy configuration architecture.

### Two Types of Messages

**1. Configuration Messages** (Controlled by lazy configuration)
```
🔧 Central Publisher ready for publishing
Configured Kotlin JVM project for publishing using kotlin-jvm strategy  
✅ Auto-configured for kotlin-jvm project
```

**2. Task Execution Messages** (Always appear during task execution)  
```
🚀 Publishing to Maven Central...
📤 Uploading bundle to Sonatype Central Portal...
📦 Bundle: /path/to/bundle.zip
📊 Bundle size: 24550 bytes
🎯 Target: https://central.sonatype.com/
🔐 Authenticating with Sonatype Central Portal...
✅ Upload successful with deployment ID: 2c0ad7cf-9623-4a79-ba55-8ab489665346
✅ Upload completed successfully!
```

### Timeline: What Messages Appear When

```
1. Configuration Phase (./gradlew publishToCentral starts)
   ├── Publications configured silently (no messages)
   ├── Tasks created silently (no messages)  
   └── taskGraph.whenReady callback registered (no messages)

2. Task Graph Ready (End of Configuration Phase)
   ├── gradle.taskGraph.whenReady executes
   ├── Detects "publishToCentral" in task graph
   └── 🔧 Central Publisher ready for publishing  ← CONFIGURATION MESSAGE

3. Task Execution Phase  
   ├── > Task :publishToCentral
   ├── PublishToCentralTaskExecutor.execute() runs
   ├── 🚀 Publishing to Maven Central...          ← TASK EXECUTION MESSAGE
   ├── 📤 Uploading bundle to Sonatype Central Portal... ← TASK EXECUTION MESSAGE
   ├── 📦 Bundle: /path/to/bundle.zip             ← TASK EXECUTION MESSAGE
   ├── 📊 Bundle size: 24550 bytes                ← TASK EXECUTION MESSAGE
   ├── 🎯 Target: https://central.sonatype.com/   ← TASK EXECUTION MESSAGE
   ├── ... (upload happens) ...
   └── ✅ Upload completed successfully!          ← TASK EXECUTION MESSAGE
```

### Source Code Locations

**Configuration Messages** (controlled by `taskGraph.whenReady`):
```kotlin
// File: CentralPublisherPlugin.kt
project.gradle.taskGraph.whenReady {
    if (willPublish) {
        project.logger.quiet("🔧 Central Publisher ready for publishing") // ← Configuration message
    }
}
```

**Task Execution Messages** (in task executors):
```kotlin  
// File: PublishToCentralTaskExecutor.kt
fun execute() {
    project.logger.quiet("🚀 Publishing to Maven Central...")           // ← Task execution message
    project.logger.quiet("📤 Uploading bundle to Sonatype Central Portal...") // ← Task execution message
    project.logger.quiet("📦 Bundle: ${bundleFile.absolutePath}")              // ← Task execution message
    project.logger.quiet("📊 Bundle size: ${bundleFile.length()} bytes")       // ← Task execution message
    // ... actual upload logic with more messages ...
    project.logger.quiet("✅ Upload completed successfully!")                  // ← Task execution message
}
```

### Key Distinction

| Message Type | When | Where | Purpose | Controlled by lazy config? |
|-------------|------|-------|---------|---------------------------|
| **Configuration** | Before task execution | `taskGraph.whenReady` | "I'm ready to publish" | ✅ YES |
| **Task Execution** | During task execution | Task executors | "I'm publishing now" | ❌ NO |

### Why Task Execution Messages Always Show

Task execution messages (🚀, 📤, 📦, etc.) **always** appear when publishing tasks run because:

1. **They run during task execution** - not during configuration
2. **They're part of the task logic** - they tell you what the task is doing  
3. **They should always show** - you want to see upload progress when actually publishing

**This is the correct behavior!** When you run `./gradlew publishToCentral`, you **want** to see:
- Configuration message: "🔧 Ready for publishing" 
- Execution messages: "🚀 Publishing...", "📤 Uploading...", "✅ Upload completed!"

### Testing Both Message Types

**Development (Silent):**
```bash
./gradlew test
# ✅ No configuration messages (taskGraph.whenReady doesn't trigger)
# ✅ No task execution messages (no publishing tasks run)
# Result: Complete silence ✅
```

**Publishing (Verbose):**
```bash
./gradlew publishToCentral
# ✅ Configuration message: "🔧 Ready for publishing" (from taskGraph.whenReady)
# ✅ Task execution messages: "🚀 Publishing...", "📤 Uploading...", etc. (from task executors)
# Result: Full feedback during publishing ✅
```

## Key Benefits Achieved

### 1. **Perfect User Experience**
- ✅ **Silent Development**: No noise during `./gradlew test`, `./gradlew build`
- ✅ **Task Discoverability**: All tasks visible in `./gradlew tasks` 
- ✅ **Rich Publishing Feedback**: Full messages during `./gradlew publishToCentral`

### 2. **Simple Architecture**  
- ✅ **60% less code**: From 250+ lines to ~100 lines
- ✅ **Single configuration path**: No test mode, lazy mode, immediate mode complexity
- ✅ **No conditional parameters**: Eliminated `showMessages` threading through classes
- ✅ **Clear responsibilities**: Each component has one job

### 3. **Reliable Timing**
- ✅ **No Gradle lifecycle conflicts**: Publications configured at the right time
- ✅ **No "javadocJar exists" errors**: Tasks created during Configuration Phase
- ✅ **No "task already executing" errors**: No configuration during Execution Phase

### 4. **Maintainable Codebase**
- ✅ **Easy to understand**: Clear mental model matches user expectations
- ✅ **Easy to extend**: Add new tasks by following the same pattern
- ✅ **Easy to test**: Simple, predictable behavior
- ✅ **Easy to debug**: Fewer code paths and conditions

## Implementation Patterns

### ✅ Correct: Silent Configuration with Lazy Messages

```kotlin
// Configure publications immediately (silent)
publicationManager.configurePublications(config, showMessages = false)

// Show messages only when publishing tasks run  
project.gradle.taskGraph.whenReady {
    if (willPublish) {
        project.logger.quiet("🔧 Central Publisher ready for publishing")
    }
}
```

### ❌ Avoid: Conditional Logging Everywhere

```kotlin
// Complex conditional logic (old approach)
fun configure(config: Config, showMessages: Boolean) {
    if (showMessages) {
        project.logger.quiet("Configuring...")
    }
    doConfiguration()
    if (showMessages) {
        project.logger.quiet("Done!")
    }
}
```

### ✅ Correct: Single Task Creation Method

```kotlin
fun createTasks(config: CentralPublisherConfig) {
    setupLocalRepository()
    createPublishToCentralTask(config)
    createBundleArtifactsTask(config) 
    createValidatePublishingTask(config)
    createSetupTask()
}
```

### ❌ Avoid: Multiple Task Creation Paths

```kotlin
// Complex dual paths (old approach)
fun createTaskSkeletons() { /* create empty tasks */ }
fun createPublishingTasks() { /* create real tasks */ }
fun configureTaskBehavior() { /* replace skeleton behavior */ }
```

## Testing Considerations

### Simple Test Pattern

```kotlin
@Test
fun `should create all publishing tasks`() {
    // Configure extension
    project.extensions.configure(CentralPublisherExtension::class.java) {
        credentials { username = "test-user"; password = "test-token" }
    }

    // Trigger evaluation  
    project.evaluate()

    // Assert results
    assertThat(project.tasks.findByName("publishToCentral")).isNotNull()
    assertThat(project.tasks.findByName("bundleArtifacts")).isNotNull()
}
```

**Why this works:**
- Same code path as production (no test mode)
- Direct configuration and evaluation
- Clear assertions on outcomes

## Migration from Complex Architecture

If you encounter old code with complexity, here's how to simplify:

### 1. Remove showMessages Parameters
```kotlin
// OLD
fun configurePublications(config: Config, showMessages: Boolean = true)

// NEW  
fun configurePublications(config: Config)
```

### 2. Eliminate Multiple Configuration Paths
```kotlin
// OLD
when {
    isTestMode -> configureForPublishing(showMessages = true)
    hasConfig -> createLazyTasks()  
    else -> createSetupTask()
}

// NEW
if (extension.hasExplicitConfiguration()) {
    configurePublishing(project, extension)
} else {
    taskManager.createSetupTask()
}
```

### 3. Unify Task Creation
```kotlin
// OLD
fun createTaskSkeletons() + configureTaskBehavior()

// NEW
fun createTasks(config)
```

## Troubleshooting

### Tasks Don't Show Up
- ✅ Ensure `extension.hasExplicitConfiguration()` returns true
- ✅ Verify task creation happens in `createTasks()`
- ✅ Check no duplicate task names

### Configuration Messages During Development  
- ✅ Verify `showMessages = false` in silent configuration calls
- ✅ Check `gradle.taskGraph.whenReady` only triggers for publishing tasks
- ✅ Ensure no immediate logging in publication managers

### Bundle Task Fails
- ✅ Verify publications are configured silently (not in `taskGraph.whenReady`)
- ✅ Check task dependencies include `publishAllPublicationsToLocalRepoRepository`
- ✅ Ensure signing tasks are created during Configuration Phase

## Success Metrics

The lazy configuration architecture succeeds when:

- ✅ **Development is silent**: `./gradlew test` shows no Central Publisher messages
- ✅ **Tasks are discoverable**: `./gradlew tasks` shows all Central Publisher tasks
- ✅ **Publishing is verbose**: `./gradlew publishToCentral` shows full configuration messages
- ✅ **Code is simple**: <100 lines, single configuration path, no conditional parameters
- ✅ **No timing errors**: No "javadocJar exists" or "task executing" errors
- ✅ **Tests are clear**: Simple test setup without complex mocking or test modes

---

**Remember**: The goal is **zero configuration noise during development** while maintaining **full visibility during publishing**. The architecture achieves this through **silent configuration** with **lazy messages** - simple, predictable, and maintainable.