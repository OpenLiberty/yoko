# Yoko Interoperability Testing

## Overview

Yoko includes infrastructure for testing interoperability between different versions of Yoko. This allows you to verify that the current development version can communicate correctly with earlier released versions.

## Quick Start

### 1. Build or Fetch a Yoko Version

Before running interop tests, you need to cache the Yoko version you want to test against. You have two options:

**Option A: Fetch from GitHub releases (faster)**
```bash
./gradlew fetchYokoVersion -PyokoVersion=1.5.3
```

This downloads the release JARs from GitHub using the `gh` CLI and caches them in `~/.yoko-interop-cache/<version>/`.

**Option B: Build from source**
```bash
./gradlew buildYokoVersion -PyokoVersion=1.5.3
```

This creates a git worktree, builds the specified version from source, and caches all JARs in `~/.yoko-interop-cache/<version>/`.

If this step is omitted, any tests relying on this version will be skipped.

### 2. Write an Interop Test

Use `@InteropTest` to mark tests that require a specific Yoko version:

```java
@InteropTest(V1_5_3)
public class MyInteropTest {
    public interface MyService extends Remote {
        Object echo(Object o);
    }

    @RemoteImpl
    static MyService serviceImpl = o -> o;

    @Test
    void testCompatibility(MyService stub) {
        // Test logic here
    }
}
```

The `@InteropTest` annotation automatically:
- Tags the test with `@Tag("interop")` for selective execution
- Configures `@ConfigureServer(separation = INTER_PROCESS)`
- Sets up the server to run with the specified Yoko version

## Gradle Tasks

```bash
# Fetch and cache a version from GitHub releases (requires gh CLI)
./gradlew fetchYokoVersion -PyokoVersion=1.5.3

# Build and cache a version from source
./gradlew buildYokoVersion -PyokoVersion=1.5.3

# List cached versions
./gradlew listCachedYokoVersions

# Clean cached JARs only
./gradlew cleanYokoCache

# Clean worktrees only
./gradlew cleanYokoWorktrees

# Clean both JARs and worktrees
./gradlew cleanCachedYokoVersions
```


## Cache Locations

- **JARs**: `~/.yoko-interop-cache/<version>/`
- **Worktrees**: `<project-root>/worktrees/<version>/`

## Key Constraints

- `@InteropTest` automatically configures `separation = INTER_PROCESS`
- Tests are tagged with `@Tag("interop")` and can be excluded in CI builds
- Tests automatically skip if the version isn't cached (using JUnit assumptions)
- Server runs with the specified version; client always uses current development version
