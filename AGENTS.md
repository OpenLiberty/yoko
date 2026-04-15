# AI Agent Guidelines for Apache Yoko

This document provides guidelines for AI agents (such as GitHub Copilot, Claude, ChatGPT, etc.) working on the Apache Yoko project. These guidelines complement the [GENAI_GUIDELINES.md](GENAI_GUIDELINES.md) and [CONTRIBUTING.md](CONTRIBUTING.md) files.

## Table of Contents

- [Project Overview](#project-overview)
- [Code Style and Conventions](#code-style-and-conventions)
- [Testing Requirements](#testing-requirements)
- [Critical Areas](#critical-areas)
- [Do's and Don'ts](#dos-and-donts)
- [Integration with GENAI_GUIDELINES](#integration-with-genai_guidelines)
- [Best Practices](#best-practices)

## Project Overview

Apache Yoko is a CORBA (Common Object Request Broker Architecture) implementation written in Java. The project consists of several key modules:

- **yoko-core** - Core ORB (Object Request Broker) implementation
- **yoko-rmi-impl** - RMI (Remote Method Invocation) implementation
- **yoko-rmi-spec** - RMI specification
- **yoko-spec-corba** - CORBA specification
- **yoko-util** - Utility classes
- **yoko-osgi** - OSGi support
- **testify** - Testing framework

### Build System

- **Build Tool**: Gradle
- **Java Compatibility**: 
  - Production code: Java 8 (sourceCompatibility/targetCompatibility = 1.8)
  - Test code: Java 11+
- **Build Requirements**: Must run with Java 11 or higher

## Code Style and Conventions

### General Guidelines

1. **License Headers**: All files must include Apache License 2.0 header (see existing files for format)
   - Include copyright, license text, and `SPDX-License-Identifier: Apache-2.0`

2. **Checkstyle**: Follow `checkstyle.xml` rules (indentation, naming, method lengths, if/try depths)

3. **Java 8 Compatibility**: Production code MUST be Java 8 compatible:
   - No lambda expressions with type inference improvements from Java 11+
   - No `var` keyword
   - No new APIs introduced after Java 8
   - Use Java 8 compatible APIs only

4. **OSGi Bundle Metadata**: Projects with `bnd.bnd` files require proper OSGi metadata

### Naming Conventions

- **Packages**: Follow existing package structure (e.g., `org.apache.yoko.*`)
- **Classes**: PascalCase
- **Methods**: camelCase
- **Constants**: UPPER_SNAKE_CASE
- **Test Classes**: Should end with `Test` (e.g., `MyFeatureTest`)

## Testing Requirements

### Test Framework

- **Primary**: JUnit 5 (Jupiter)
- **Legacy Support**: JUnit 4 (Vintage) for existing tests
- **Mocking**: Mockito
- **Assertions**: Hamcrest and JUnit assertions

### Test Standards

1. **Coverage**: All AI-generated code must include comprehensive tests
2. **Test Naming**: Use descriptive test method names that explain what is being tested
3. **Test Structure**: Follow Arrange-Act-Assert (AAA) pattern
4. **JVM Arguments**: Tests require specific JVM arguments (already configured):
   ```
   --add-opens=java.base/java.lang=ALL-UNNAMED
   --add-opens=java.base/java.io=ALL-UNNAMED
   --add-opens=java.base/java.util=ALL-UNNAMED
   --add-opens=java.rmi/java.rmi=ALL-UNNAMED
   ```

### Test Location

- Unit tests: `src/test/java`
- Integration tests: `src/test/java-testify` (using Testify framework)
- Test resources: `src/test/resources`

### Running Tests

```bash
# Run all tests
./gradlew test

# Run tests for specific module
./gradlew :yoko-core:test

# Generate test report
./gradlew testReport
```

## Critical Areas

### High-Risk Components (Require Extra Scrutiny)

1. **CORBA Protocol Implementation** (`yoko-core`)
   - Wire protocol handling
   - Marshalling/unmarshalling
   - Type system implementation
   - Security-sensitive operations

2. **RMI Implementation** (`yoko-rmi-impl`)
   - Remote object handling
   - Serialization/deserialization
   - Network communication

3. **OSGi Integration** (`yoko-osgi`)
   - Bundle lifecycle management
   - Service registration/discovery
   - ClassLoader interactions

### YASF - Yoko Auxiliary Stream Format

**YASF** is Yoko's backward compatibility negotiation mechanism. See `org.apache.yoko.util.yasf.Yasf` for:
- Current YASF options and their purposes
- How YASF is communicated between ORBs (IOR profile component tag and GIOP service context)
- Detection mechanism: Non-Yoko ORBs are detected by absence of YASF data; all options assumed ON (spec-compliant)

**Important**: When modifying serialization or marshalling code:
- Check if changes affect wire format (may require new YASF option)
- Test with both YASF-enabled and YASF-disabled scenarios
- See `YasfThreadLocal` for usage patterns

### ROFL - Remote ORB Finessing Logic

**ROFL** encapsulates fixes for interoperability with other ORB implementations. See `org.apache.yoko.util.rofl.Rofl` for:
- Known remote ORB types and their quirks
- How ROFL data is read from IOR profile component tags and GIOP service contexts
- Usage via `RoflThreadLocal` during marshalling

**Important**: ROFL is read-only and applied only when marshalling data to ensure compatibility with other ORBs.

### Security Considerations

- **Input Validation**: Always validate external inputs
- **Serialization**: Be cautious with deserialization (potential security risks)
- **Network Operations**: Validate all network data
- **Resource Management**: Ensure proper cleanup (use try-with-resources)

## Do's and Don'ts

### ✅ DO

- **Read existing code** before making changes to understand patterns
- **Follow existing patterns** in the module you're modifying
- **Write comprehensive tests** for all new functionality
- **Use Java 8 compatible syntax** for production code
- **Include proper error handling** and logging
- **Document complex logic** with clear comments
- **Verify backward compatibility** when modifying public APIs
- **Run the full build** before submitting changes: `./gradlew build`
- **Check test results** using the test report
- **Use proper exception types** (don't catch generic Exception unless necessary)
- **Close resources properly** (use try-with-resources)

### ❌ DON'T

- **Don't use Java 9+ features** in production code (Java 8 only)
- **Don't modify public APIs** without careful consideration
- **Don't skip tests** - all code must be tested
- **Don't ignore Checkstyle warnings** - fix them
- **Don't commit code that doesn't compile**
- **Don't break backward compatibility** without discussion
- **Don't introduce new dependencies** without justification
- **Don't copy code** from external sources without proper licensing
- **Don't modify generated code** (e.g., CORBA stubs) manually
- **Don't use deprecated APIs** unless maintaining legacy code

## Integration with GENAI_GUIDELINES

All AI-generated contributions must comply with [GENAI_GUIDELINES.md](GENAI_GUIDELINES.md):

### Commit Message Format

When committing AI-generated code, use:

```
<commit message>

Co-authored-by-AI: <AI Tool/IDE/Platform> (<Model Name/Version>)
```

**Examples:**
```
feat: add new CORBA type handler

Co-authored-by-AI: GitHub Copilot (gpt-4.1)
```

```
fix: resolve serialization issue in RMI

Co-authored-by-AI: Claude Code (claude-sonnet-4-6)
```

### Quality Requirements

1. **Functionality**: Code must accomplish its intended purpose
2. **Quality**: Must adhere to project standards (Checkstyle, Java 8 compatibility)
3. **Validation**: Must include tests and be verified to work correctly
4. **Confidence**: You must understand and vouch for the code
5. **IP Compliance**: Validate that generated content doesn't violate copyrights

## Best Practices

### Code Generation

1. **Understand Context**: Read related files before generating code
2. **Match Style**: Follow the existing code style in the module
3. **Incremental Changes**: Make small, focused changes rather than large rewrites
4. **Test First**: Consider writing tests before implementation (TDD)
5. **Review Generated Code**: Always review and understand AI-generated code

### Documentation

1. **Javadoc**: Add Javadoc for public APIs
2. **Inline Comments**: Explain complex logic
3. **README Updates**: Update documentation when adding features
4. **CHANGELOG**: Follow the changelog format for notable changes

### Version Control

1. **Atomic Commits**: Each commit should represent one logical change
2. **Clear Messages**: Write descriptive commit messages
3. **Branch Strategy**: Work on feature branches, not directly on main
4. **Clean History**: Squash commits if needed before merging

### Release Process

When contributing to releases:

1. **Version Bumping**: Use `gradle bumpVersion` (don't manually edit)
2. **Changelog**: Update using `gradle updateChangelog`
3. **Testing**: Ensure all tests pass before release
4. **Build Verification**: Run `gradle build` successfully

### Logging

Yoko uses the `yoko.verbose` logger hierarchy. See `org.apache.yoko.logging.VerboseLogging` for:
- Complete list of logger categories (init, connection, giop, request, marshal, etc.)
- Logging levels and their purposes (CONFIG, FINE, FINER, FINEST)
- Directional variants (`.in` and `.out` for many categories)

**In Tests**: Use `@Logging` annotation to enable specific loggers:
```java
@Test
@Logging("yoko.verbose.connection.in")
void testConnection() { ... }
```

**Note**: Replaces legacy `org.apache.yoko.orb.OB.CoreTraceLevels` and `org.apache.yoko.orb.OB.Logger`

### Debugging

1. **Logging**: Use the `yoko.verbose` logger hierarchy as described above
2. **Test Isolation**: Ensure tests don't depend on execution order
3. **Reproducibility**: Make bugs reproducible with tests

## Module-Specific Guidelines

### yoko-core

- Most critical module - extra care required
- Changes may affect all other modules
- Extensive testing required
- Performance considerations important

### yoko-rmi-impl

- Must maintain RMI specification compliance
- Serialization changes require careful testing
- Network protocol changes need thorough validation

### yoko-osgi

- Must work in OSGi environments
- ClassLoader issues are common - test thoroughly
- Bundle metadata must be correct

### testify

- Testing framework used by yoko-verify
- Changes here affect all integration tests
- Must maintain backward compatibility

## Getting Help

If you're unsure about:

- **Architecture decisions**: Review existing code patterns
- **API changes**: Check for similar changes in git history
- **Testing approach**: Look at existing tests in the same module
- **Build issues**: Check `build.gradle` and `build-release.gradle`

## Summary

As an AI agent working on Apache Yoko:

1. **Prioritize quality** over speed
2. **Maintain Java 8 compatibility** for production code
3. **Write comprehensive tests** for all changes
4. **Follow existing patterns** and conventions
5. **Document your changes** appropriately
6. **Comply with licensing** requirements
7. **Mark AI contributions** in commit messages
8. **Understand the code** you generate

Remember: You are a tool to assist developers, not a replacement for developer judgment and responsibility. All AI-generated code must be reviewed, understood, and validated by the contributor before submission.

---

*This document should be updated as the project evolves and new patterns emerge.*