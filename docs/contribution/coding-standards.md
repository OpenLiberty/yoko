# Yoko Coding Standards

This document defines the coding standards for the OpenLiberty Yoko project, a Java implementation of CORBA (Common Object Request Broker Architecture) forked from Apache Yoko.

## Table of Contents

1. [General Principles](#general-principles)
2. [Java Language Standards](#java-language-standards)
3. [Code Formatting](#code-formatting)
4. [Naming Conventions](#naming-conventions)
5. [Documentation Standards](#documentation-standards)
6. [Error Handling and Logging](#error-handling-and-logging)
7. [CORBA-Specific Guidelines](#corba-specific-guidelines)
8. [Testing Standards](#testing-standards)
9. [Build and Dependencies](#build-and-dependencies)
10. [Version Control](#version-control)

---

## General Principles

### Code Quality
- Write clear, maintainable, and self-documenting code
- Favor readability over cleverness
- Follow the principle of least surprise
- Keep methods focused and cohesive (single responsibility)
- Avoid premature optimization

### Project Philosophy
- Maintain backward compatibility where possible (see Yasf and Rofl in the code for compatibility mechanisms)
- Prioritize correctness and reliability over performance
- Document deviations from CORBA specifications
- Consider thread safety in all concurrent code

---

## Java Language Standards

### Java Version
- **Target Version**: Java 8+ (check `gradle.properties` for current versions: development code tends to be an earlier language level than test code)
- **Language Features**: Use modern Java features appropriately
  - Prefer lambdas and streams for functional operations
  - Use try-with-resources for AutoCloseable resources
  - Leverage Optional for nullable return values where appropriate
  - Use method references when they improve readability

### Forbidden Practices
- **No raw types**: Always use generics with type parameters
- **No star imports**: no matter what your tools try to do
- **Never import `org.omg.CORBA.Object`**: this is too confusing because it replaces the implicit import of `java.lang.Object`
- **No sun.* packages**: Never import from internal JDK packages
- **No assertions with side effects**: Assert statements must not modify state

---

## Code Formatting

### File Structure
1. License header (Apache 2.0)
2. Package declaration
3. Import statements (organized and sorted)
4. Class/interface declaration
5. Class documentation
6. Class body

### License Header
All source files must include the Apache License 2.0 header:

```java
/*
 * Copyright 2026 IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
```

### Import Organization
Imports must be organized in the following order:
1. `java.*` packages
2. `javax.*` packages
3. `org.w3c.*` packages
4. `org.xml.*` packages
5. `w3c.*` packages
6. All other imports

Within each group, imports should be alphabetically sorted.

### Line Length and Wrapping
- **Maximum line length**: 110 characters
- Break lines at logical points (after commas, before operators)
- Indent continuation lines appropriately

### Whitespace
- **Indentation**: 4 spaces - **NO TABS**
- **Case indentation**: 0 spaces (case labels align with switch)
- Use spaces after commas and semicolons
- Use spaces around operators (`=`, `+`, `-`, `*`, `/`, `==`, `!=`, etc.)
- **NO TRAILING WHITESPACE** - Lines must not end with spaces or tabs
- **Pure ASCII only** - No non-ASCII characters in Java source files (scripts may use emoji/Unicode)
- Files must end with a newline

**Critical Requirements for Java Source Files**:
- Tab characters are strictly prohibited - use 4 spaces for indentation
- Trailing whitespace is strictly prohibited - lines must not end with spaces or tabs
- Only ASCII characters are allowed in Java source files (`.java` files)
- Configure your editor to remove trailing whitespace on save
- Run `./gradlew check` to detect violations before committing

Note: Scripts (e.g., shell scripts) may use emoji and Unicode characters as appropriate.

### Braces
- **Opening brace**: Same line as declaration/statement
- **Closing brace**: On its own line, aligned with the start of the statement
- Always use braces for `if`, `else`, `for`, `while`, and `do` statements, even for single-line blocks

```java
// Good
if (condition) {
    doSomething();
}

// Bad
if (condition)
    doSomething();
```

### Method and Class Size Limits
- **Maximum method length**: 150 lines (excluding blank lines)
- **Maximum executable statements**: 50 per method
- **Maximum parameters**: 7 per method
- **Maximum anonymous inner class length**: 40 lines
- **Maximum cyclomatic complexity**: Keep methods simple and focused

---

## Naming Conventions

### Classes and Interfaces
- **Classes**: PascalCase (e.g., `ORBInstance`, `ClientManager`)
- **Interfaces**: PascalCase (e.g., `Client`, `Connector`)
- **Abstract classes**: Must match pattern `^Abstract.*$|^.*Factory$|^.*Bus$|^.*ConfigurationRepository$|^.*Base$|^Exception$|^.*Builder$`
- **Test classes**: Must end with `Test` (e.g., `Utf8Test`, `CorbanamesTest`)

### Methods
- **Method names**: camelCase, starting with a verb (e.g., `getORB()`, `startDowncall()`, `destroy()`)
- **Boolean methods**: Prefix with `is`, `has`, `can`, or `should` (e.g., `isSupplementaryCodePoint()`, `hasNext()`)
- **Factory methods**: Use `create`, `build`, or `new` prefix (e.g., `createWriteBuffer()`)

### Variables
- **Local variables**: camelCase (e.g., `clientManager`, `codecFactory`)
- **Instance variables**: camelCase (e.g., `orbId`, `nativeCodeSet`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `MIN_1_BYTE`, `MAX_LOW_SURROGATE`)
- **Static final fields**: UPPER_SNAKE_CASE for true constants
- **Parameters**: camelCase (e.g., `concModel`, `codecs`)

### Packages
- All lowercase, no underscores (e.g., `org.apache.yoko.orb`)
- Use meaningful, hierarchical names

### Acronyms in Names
- Treat acronyms as words: `ORB` → `Orb` in camelCase contexts
- Exception: When the acronym is the entire name (e.g., `IOR`, `POA`)

```java
// Good
getOrbId()
createIor()

// Avoid
getORBID()
createIOR()
```

---

## Documentation Standards

### JavaDoc Requirements
- **Public APIs**: Must have comprehensive JavaDoc
- **Package-private/protected**: Should have JavaDoc for non-obvious functionality
- **Private methods**: JavaDoc optional but encouraged for complex logic

### JavaDoc Format
```java
/**
 * Brief one-line description ending with a period.
 * 
 * More detailed description if needed. Explain the purpose,
 * behavior, and any important considerations.
 *
 * @param paramName description of parameter
 * @param anotherParam description of another parameter
 * @return description of return value
 * @throws ExceptionType when and why this exception is thrown
 * @see RelatedClass
 * @since 1.0
 */
public ReturnType methodName(ParamType paramName, AnotherType anotherParam) 
    throws ExceptionType {
    // implementation
}
```

### Comments
- **Inline comments**: Use `//` for single-line comments
- **Block comments**: Use `/* */` for multi-line explanations
- **TODO comments**: Use `// TODO: description` (will trigger checkstyle warning with `WARNING` tag)
- Explain *why*, not *what* (the code should be self-explanatory for *what*)
- Keep comments up-to-date with code changes

### CORBA Specification References
When implementing CORBA specifications, include references:

```java
/**
 * Implements the CORBA 3.0 specification section 11.3.4.
 * 
 * @see <a href="https://www.omg.org/spec/CORBA/3.0">CORBA 3.0 Specification</a>
 */
```

---

## Error Handling and Logging

### Exception Handling
- **Never catch and ignore exceptions** without logging or re-throwing
- **Catch specific exceptions** rather than generic `Exception` or `Throwable`
- **Don't throw `Error` or `RuntimeException`** from methods (checkstyle enforced)
- **Document all checked exceptions** in JavaDoc with `@throws`
- **Use try-with-resources** for AutoCloseable resources

```java
// Good
try (InputStream in = new FileInputStream(file)) {
    // process stream
} catch (IOException e) {
    logger.error("Failed to read file: " + file, e);
    throw new ProcessingException("Cannot process file", e);
}

// Bad
try {
    InputStream in = new FileInputStream(file);
    // process stream
    in.close();
} catch (Exception e) {
    // ignore
}
```

### CORBA Exception Patterns
- Use appropriate CORBA system exceptions (e.g., `INTERNAL`, `BAD_PARAM`, `NO_RESOURCES`)
- Include minor codes for detailed error information
- Preserve exception chains when wrapping exceptions

```java
// Example from codebase
throw new INTERNAL("Could not find PolicyManager");
```

### Logging
- Use Java Util Logging (JUL) or the project's logging facade
- Follow existing logging patterns in the codebase
- Log levels:
  - **SEVERE**: Critical errors requiring immediate attention
  - **WARNING**: Potential problems or deprecated usage
  - **INFO**: Important runtime events
  - **FINE/FINER/FINEST**: Debug information

```java
// Example logging pattern
if (logger.isLoggable(Level.FINE)) {
    logger.fine("Processing request: " + requestId);
}
```

### Assertions
- Use `Assert.ensure()` for internal invariants (from `org.apache.yoko.util.Assert`)
- Assertions should never have side effects
- Use for conditions that should never occur in correct code

```java
Assert.ensure(count >= 0);
Assert.ensure(destroyCalled.get());
```

---

## CORBA-Specific Guidelines

### IDL to Java Mapping
- Follow OMG IDL-to-Java mapping specifications
- Generated code should not be manually modified
- Use appropriate holders and helpers for IDL types

### Object References and Lifecycle
- Always properly manage object reference lifecycles
- Use reference counting where appropriate (see `Client.obtain()` and `Client.release()`)
- Implement proper cleanup in `destroy()` methods

```java
public final void obtain() {
    int count = users.incrementAndGet();
    Assert.ensure(count > 0);
}

public final boolean release() {
    int count = users.decrementAndGet();
    Assert.ensure(count >= 0);
    return count == 0;
}
```

### Thread Safety
- Document thread-safety guarantees in class JavaDoc
- Use appropriate synchronization mechanisms:
  - `RecursiveMutex` for reentrant locking
  - `AtomicInteger`, `AtomicBoolean` for simple atomic operations
  - `synchronized` blocks for critical sections
- Prefer immutable objects where possible
- Use `final` for fields that should not change after construction

### POA (Portable Object Adapter) Patterns
- Follow standard POA lifecycle: create → activate → process requests → deactivate → destroy
- Properly implement servant locators and activators
- Handle POA policies correctly

### Codec and Marshaling
- Use appropriate codecs for character encoding (UTF-8, ISO-8859-1, etc.)
- Handle byte order (endianness) correctly
- Validate input data during unmarshaling

---

## Testing Standards

### Test Organization
- **Location**: Tests in `src/test/java` mirroring `src/main/java` structure
- **Naming**: Test classes must end with `Test` (e.g., `Utf8Test`)
- **Framework**: Use JUnit 5 (Jupiter) for new tests

### Test Structure
```java
class MyClassTest {
    // Test fixtures
    private MyClass instance;
    
    @BeforeEach
    void setUp() {
        instance = new MyClass();
    }
    
    @Test
    void testMethodName_shouldDoSomething_whenCondition() {
        // Arrange
        String input = "test";
        
        // Act
        String result = instance.methodName(input);
        
        // Assert
        assertEquals("expected", result);
    }
    
    @AfterEach
    void tearDown() {
        instance.destroy();
    }
}
```

### Test Naming
- Use descriptive test method names: `test<MethodName>_should<ExpectedBehavior>_when<Condition>`
- Or use `@DisplayName` for readable test names
- Parameterized tests should have descriptive names in `@ParameterizedTest(name = "...")`

```java
@ParameterizedTest(name = "Decode 1-byte UTF-8 char: {0} ({2})")
@MethodSource("_1_ByteChars")
void testDecode1ByteChar(String hex, int codepoint, String c) {
    checkDecoding(codepoint, c);
}
```

### Test Coverage
- **All public APIs must have tests**
- Test both success and failure paths
- Test boundary conditions and edge cases
- Test thread safety for concurrent code
- Include integration tests for CORBA interactions

### Assertions
- Use JUnit 5 assertions: `assertEquals()`, `assertTrue()`, `assertThrows()`, etc.
- Provide meaningful assertion messages
- Use `assertAll()` for multiple related assertions

```java
assertAll("codec state",
    () -> assertTrue(codec.readFinished(), "should have no outstanding low surrogate"),
    () -> assertEquals(0, in.available(), "should read all encoded bytes")
);
```

### Test Data
- Use `@MethodSource` for parameterized test data
- Keep test data organized and maintainable
- Use meaningful test data that covers edge cases

---

## Build and Dependencies

### Gradle
- Follow existing Gradle conventions in `build.gradle`
- Keep dependencies up-to-date and minimal
- **Avoid introducing new dependencies for production code** - use existing libraries where possible
- Document any non-obvious dependency requirements
- Use BOM (Bill of Materials) for version management where appropriate

### Dependency Guidelines
- **Production code**: Minimize external dependencies; prefer standard Java libraries


### Checkstyle
- All code must pass Checkstyle validation
- Configuration in `checkstyle.xml`
- Run `./gradlew check` before committing
- Do not suppress Checkstyle warnings without good reason and documentation

### Build Requirements
- All PRs must have a passing build
- Run full test suite: `./gradlew clean build`
- Verify no Checkstyle violations
- Ensure all tests pass

---

## Version Control

### Git Commits
All commit messages **must** follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:

```
<type>(<scope>): <description>

[optional body]

[optional footer(s)]
```

#### Commit Types
- **feat**: A new feature
- **fix**: A bug fix
- **docs**: Documentation only changes
- **style**: Changes that don't affect code meaning (formatting, whitespace)
- **refactor**: Code change that neither fixes a bug nor adds a feature
- **test**: Adding or correcting tests
- **chore**: Changes to build process, dependencies, or auxiliary tools

#### Examples
```
feat(orb): add support for GIOP 1.3 protocol

fix(codec): correct UTF-8 surrogate pair handling

docs(readme): update build instructions for Java 11

test(corba): add integration tests for POA lifecycle
```

### Pull Requests
- Create feature branches from `main`
- Keep PRs focused and reasonably sized
- Include tests for all changes
- Update documentation as needed
- Ensure CI passes before requesting review
- Respond to review comments promptly

### Branch Naming
- Use descriptive branch names: `feature/add-giop-13`, `fix/utf8-encoding`, `docs/update-standards`
- Avoid generic names like `fix`, `update`, or `patch`

---

## Code Review Checklist

Before submitting code for review, verify:

- [ ] Code follows all formatting standards
- [ ] All public APIs have JavaDoc
- [ ] Appropriate error handling is in place
- [ ] Tests are included and passing
- [ ] No Checkstyle violations
- [ ] Commit messages follow Conventional Commits
- [ ] License headers are present
- [ ] No TODO comments without tracking issues
- [ ] Thread safety is considered and documented
- [ ] CORBA specifications are followed
- [ ] Build passes locally: `./gradlew clean build`

---

## Additional Resources

- [Apache Yoko GitHub Repository](https://github.com/yoko-tool/yoko)
- [CORBA Specifications](https://www.omg.org/spec/CORBA/)
- [Conventional Commits](https://www.conventionalcommits.org/)
- [Java Code Conventions](https://www.oracle.com/java/technologies/javase/codeconventions-contents.html)
- [Effective Java (Joshua Bloch)](https://www.oreilly.com/library/view/effective-java/9780134686097/)

---

## Questions or Clarifications

If you have questions about these standards or need clarification on specific cases, please:

1. Check existing code for patterns and examples
2. Consult the project maintainers
3. Open a discussion on GitHub

Remember: These standards exist to maintain code quality and consistency. When in doubt, favor readability and maintainability.
