# Yoko Interoperability Testing Plan

## Objective
Enable testing of interoperability between the current development version of Yoko and earlier released versions (1.5.0 onwards), specifically focusing on:
- Full Value Descriptor (FVD) compatibility when versions differ between client and server
- Enum serialization/marshalling across versions

## Scope
- Test against released versions: 1.5.0 onwards (including versions not on GitHub releases)
- Use INTER_PROCESS separation only (separate JVM processes)
- Leverage existing `@ConfigureServer` annotation framework in testify-iiop
- Support bidirectional testing: current-as-client/released-as-server AND released-as-client/current-as-server

## Approach: Git Worktree + Build with Local Caching

### Selected Approach
Use git worktree to checkout and build release tags, with results cached outside the build directory to survive `gradle clean`.

### Rationale
1. **Availability**: Can test versions not available on GitHub releases (1.5.0, 1.5.1, 1.5.2)
2. **Flexibility**: Can test any tag/commit, not just releases
3. **Caching**: Build once, reuse many times (cache survives gradle clean)
4. **Debugging**: Full source available if needed
5. **No external dependencies**: No GitHub API rate limits

### Cache and Worktree Locations

**JAR Cache**: `~/.yoko-interop-cache/<version>/`
- Stores built JARs for each version
- Outside project directory
- Survives `gradle clean`
- Shared across all Yoko project clones
- Can be manually cleared if needed

**Worktrees**: `<project-root>/worktrees/<version>/`
- Located in project directory under `worktrees/` (git-ignored)
- Persistent across builds
- Not cleaned by `gradle clean`
- Allows reuse of worktrees (no repeated checkout)
- Add `worktrees/` to `.gitignore`

## Current Architecture Analysis

### Existing Test Framework
The `testify-iiop` module provides:
- `@ConfigureServer` annotation with three separation modes:
  - `COLLOCATED`: Client and server share same ORB
  - `INTER_ORB`: Client and server use different ORBs in same JVM
  - `INTER_PROCESS`: Client and server run in separate JVM processes
- `ServerSteward` class manages server lifecycle
- `ServerComms` handles communication between client and server processes
- `PartRunner` manages forked processes with configurable JVM args

### INTER_PROCESS Mechanism
When `separation = INTER_PROCESS`:
1. `PartRunner.useNewJVMWhenForking(jvmArgs)` is called
2. Server code runs in a separate JVM with specified JVM arguments
3. Client and server communicate via IIOP/RMI-IIOP
4. Each JVM has its own classpath (no classpath isolation needed within JVM)

### Available Yoko Versions
From git tags (need to verify which exist):
- v1.5.0, v1.5.1, v1.5.2 (not on GitHub releases)
- v1.5.3 (2026-01-14)
- v1.6.0 (2026-03-02)
- v1.6.1 (2026-04-16)

## Implementation Plan

### Phase 1: Build Infrastructure

#### 1.1 Create Gradle Task for Version Building
Create task that:
1. Checks if version already cached in `~/.yoko-interop-cache/<version>/`
2. If not cached:
   - Creates git worktree in `worktrees/<version>` (if not exists)
   - Checks out the specified tag
   - Runs `gradle build` in worktree
   - Copies built JARs to cache directory
3. Returns path to cached JARs
4. Worktrees are persistent and reused across builds

```gradle
task buildYokoVersion {
    doLast {
        def version = project.findProperty('yokoVersion')
        def cacheDir = file("${System.getProperty('user.home')}/.yoko-interop-cache/${version}")
        
        if (!cacheDir.exists()) {
            // Create worktree, build, cache, cleanup
        }
    }
}
```

#### 1.2 Extend @ConfigureServer Annotation
Add new parameter to specify Yoko version:
```java
@ConfigureServer(
    separation = INTER_PROCESS,
    yokoVersion = "1.5.3",  // NEW: Specify server Yoko version
    serverOrb = @ConfigureOrb(...)
)
```

#### 1.3 Modify ServerSteward
- Detect `yokoVersion` parameter
- Trigger build if not cached (via Gradle task)
- Locate cached JARs
- Construct classpath for forked JVM
- Pass classpath via JVM args (`-cp` or `-classpath`)

### Phase 2: Test Implementation

#### 2.1 FVD Compatibility Tests
Create test cases that:
- Define value types with different versions
- Test marshalling/unmarshalling across versions
- Verify FVD negotiation works correctly
- Test both directions (current→old, old→current)

Example test structure:
```java
@ConfigureServer(
    separation = INTER_PROCESS,
    yokoVersion = "1.5.3",
    serverOrb = @ConfigureOrb(nameService = READ_WRITE)
)
class FVDInteropTest {
    @RemoteImpl
    static MyValueService serviceImpl = new MyValueServiceImpl();
    
    @Test
    void testValueTypeV1ToV2(MyValueService service) {
        // Test sending V1 value type to V2 server
    }
}
```

#### 2.2 Enum Compatibility Tests
Create test cases that:
- Test enum serialization across versions
- Verify enum ordinal handling
- Test enum value addition/removal scenarios
- Test both directions

### Phase 3: Documentation and CI Integration

#### 3.1 Documentation
- Document how to add new versions
- Document test patterns for interop testing
- Add examples to testify-guide.md
- Document cache management

#### 3.2 Test Execution Strategy
- Interop tests are NOT part of regular test suite
- Tests are excluded from standard `gradle test` runs
- Tests can be run explicitly by developers: `gradle interopTest`
- Tests are run as part of release testing cycle before each release
- This keeps regular CI fast while ensuring interop validation before releases
- Optional: Pre-build and cache common versions in CI for faster execution when needed

## Technical Details

### Worktree Build Process
```bash
# 1. Create worktree (if not exists)
git worktree add worktrees/v1.5.3 v1.5.3

# 2. Build in worktree
cd worktrees/v1.5.3
./gradlew build -x test  # Skip tests for faster build

# 3. Copy JARs to cache
mkdir -p ~/.yoko-interop-cache/1.5.3
cp yoko-core/build/libs/*.jar ~/.yoko-interop-cache/1.5.3/
cp yoko-rmi-impl/build/libs/*.jar ~/.yoko-interop-cache/1.5.3/
# ... copy other required JARs

# 4. Worktree remains for reuse in future builds
cd ../..
```

### Classpath Construction
```java
private String[] buildJvmArgs(String yokoVersion) {
    Path cacheDir = Paths.get(System.getProperty("user.home"), 
                              ".yoko-interop-cache", yokoVersion);
    
    if (!Files.exists(cacheDir)) {
        // Trigger Gradle task to build version
        buildVersion(yokoVersion);
    }
    
    String classpath = Files.list(cacheDir)
        .filter(p -> p.toString().endsWith(".jar"))
        .map(Path::toString)
        .collect(Collectors.joining(File.pathSeparator));
    
    return new String[] {
        "-cp", classpath,
        // ... other JVM args
    };
}
```

### Cache Management
```bash
# View cached versions
ls ~/.yoko-interop-cache/

# Clear specific version
rm -rf ~/.yoko-interop-cache/1.5.3

# Clear all cached versions
rm -rf ~/.yoko-interop-cache/
```

## JAR Collection Strategy

**Approach**: Automatically collect all JARs from the build, including transitive dependencies.

The set of required JARs may vary between versions, so we will:
1. Build the specified version in the worktree
2. Collect all JARs from the build output (including dependencies)
3. Cache the complete set in `~/.yoko-interop-cache/<version>/`
4. Use the entire cached directory as the classpath for the server process

This ensures:
- All required JARs are available (no missing dependencies)
- Version-specific dependencies are included
- No manual maintenance of JAR lists
- Works even if dependency structure changes between versions

### Implementation Approach
Use Gradle to collect runtime classpath:
```gradle
task collectRuntimeJars {
    doLast {
        def cacheDir = file("${System.getProperty('user.home')}/.yoko-interop-cache/${version}")
        cacheDir.mkdirs()
        
        // Copy all project JARs
        subprojects.each { subproject ->
            copy {
                from subproject.tasks.jar.archiveFile
                into cacheDir
            }
        }
        
        // Copy all runtime dependencies
        configurations.runtimeClasspath.files.each { file ->
            copy {
                from file
                into cacheDir
            }
        }
    }
}
```

## Open Questions

1. **Test data management?**
   - **Resolved**: Use existing `yoko-verify/artifact` structure (v0, v1, v2)
   - Extend these modules to include FVD and Enum test cases
   - Each version module contains version-specific implementations
   - Tests reference these artifacts to verify cross-version compatibility

2. **Build compatibility?**
   - Older versions may not build on newer Java/Gradle versions
   - Strategy: Use SDKMAN to install version-specific Java/Gradle if needed
   - Fallback: Document minimum supported version for interop testing
   - Handle build failures gracefully with clear error messages

3. **Performance considerations?**
   - **Resolved**: Version specified in test class annotations
   - First build will be slow (full Gradle build)
   - Subsequent runs will be fast (cached)
   - Tests NOT run as part of regular test suite
   - Tests run explicitly by developers OR as part of release testing cycle
   - This keeps regular CI fast while ensuring interop testing before releases

## Success Criteria

1. ✅ Can specify Yoko version in `@ConfigureServer` annotation
2. ✅ Gradle task builds and caches specified version
3. ✅ Cache survives `gradle clean`
4. ✅ Server process runs with specified Yoko version
5. ✅ Client process runs with current development version
6. ✅ FVD compatibility tests pass for all supported versions
7. ✅ Enum compatibility tests pass for all supported versions
8. ✅ Tests can run in both directions (current→old, old→current)
9. ✅ Documentation is complete and clear
10. ✅ CI pipeline includes interop tests

## Next Steps

1. Verify which git tags exist for versions 1.5.0 onwards
2. Create proof-of-concept Gradle task for building versions
3. Test worktree creation and build process
4. Implement basic version switching in ServerSteward
5. Create first FVD interop test
6. Iterate and expand test coverage
