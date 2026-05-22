# Yoko ORB

Yoko is a CORBA 2.3 compliant ORB implementation to support interprocess communication.
It is an open-source project maintained by IBM, forked from Apache Yoko.

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

## Development Setup

### 1. Install SDKMAN (toolchain manager)
```bash
curl -s "https://get.sdkman.io" | bash
```

### 2. Install project toolchain (Java, Gradle)
```bash
sdk env install
```

### 3. Initialize environment (run in each new shell)
```bash
sdk env
```

### 4. Install Python 3 and pip (for pre-commit)
```bash
# macOS: brew install python3
# Ubuntu/Debian: sudo apt install python3-pip
# Fedora/RHEL: sudo dnf install python3-pip
```

### 5. Install pre-commit hooks (code quality checks)
```bash
pip install pre-commit && pre-commit install
```

### 6. Setup shared Bob config (optional, for AI assistance)
```bash
./scripts/setup-bob-worktrees.sh
```

### 7. Build and test
```bash
gradle build
```

## Development Workflow

### 1. Fork and clone
Fork the repository on GitHub, then clone your fork:
```bash
git clone https://github.com/YOUR-USERNAME/yoko.git
cd yoko
```

### 2. Create a feature branch
```bash
git checkout -b feature/your-feature-name
```

### 3. Make your changes
Edit code, following the [coding standards](docs/contributing/CODING-STANDARDS.md).

### 4. Run tests
```bash
gradle test
```

### 5. Commit your changes
Use [Conventional Commits](https://www.conventionalcommits.org/) format:
```bash
git commit -m "feat(orb): add GIOP 1.3 support"
git commit -m "fix(codec): correct UTF-8 surrogate pair handling"
git commit -m "docs(readme): update build instructions"
```

### 6. Push and create pull request
```bash
git push origin feature/your-feature-name
```
Then create a pull request on GitHub.

## Key Development Practices

### Java Language Levels
- **Production code**: Java 8 (for backward compatibility)
- **Test code**: Java 11+ (for modern testing features)
- See `build.gradle` for current version configuration

### Testing Requirements
- **All new or changed behavior must have tests**
- Test both success and failure paths
- Include edge cases and boundary conditions
- Run full test suite before submitting PR: `gradle test`

### Code Standards
- **Indentation**: 4 spaces (no tabs)
- **Line endings**: No trailing whitespace
- **Checkstyle**: Enforced via `gradle check`
- **Imports**: No star imports, organized by package
- See [Coding Standards](https://openliberty.github.io/yoko/contribution/coding-standards) for complete standards

### Commit Message Format
Follow [Conventional Commits](https://www.conventionalcommits.org/): `<type>(<scope>): <description>`

**Types**: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`

See `git log` for examples.

### GenAI Usage
When using AI tools for code contributions:
- **Review and understand** all generated code
- **Ensure code meets** project standards
- **Test thoroughly** - AI-generated code must pass all tests
- **Take responsibility** for the code you submit
- See [Open Liberty GenAI Guidelines](https://github.com/OpenLiberty/open-liberty/blob/release/GENAI_GUIDELINES.md)

## Contributing

### Pull Request Requirements
- ✅ All tests pass (`gradle test`)
- ✅ No checkstyle violations (`gradle check`)
- ✅ Comprehensive tests for new/changed behavior
- ✅ Documentation updated (if applicable)
- ✅ Commit messages follow Conventional Commits format

### Contributor License Agreement
For non-trivial changes, you must sign the [Individual Contributor License Agreement](https://github.com/OpenLiberty/open-liberty/blob/release/cla/open-liberty-cla-individual.pdf). Trivial changes (typos, formatting) don't require a CLA.

### Code Review Process
1. Submit pull request against `main` branch
2. Automated checks run (tests, checkstyle)
3. Team reviews code
4. Address review feedback
5. PR merged when approved

## Testify Testing Framework

Testify is our internal testing framework for JUnit 5, supporting:
- Multi-threaded test execution
- Forked process testing
- Inter-process communication via Bus
- Flexible test configuration

See [Testify documentation](https://openliberty.github.io/yoko/) for usage details.

For creating or simplifying Yoko IIOP testcases, see the
[testify-iiop testcase guide](https://openliberty.github.io/yoko/testify/iiop-testcases/).

## Documentation

### Testing Documentation Locally
To preview documentation changes before publishing:
```bash
# Install MkDocs Material theme
pip install mkdocs-material

# Serve documentation locally (auto-reloads on changes)
mkdocs serve --config-file config/mkdocs.yml

# Open http://127.0.0.1:8000 in your browser
```

### Building Documentation
```bash
# Build static site to site/ directory
mkdocs build --config-file config/mkdocs.yml
```

Documentation is automatically published to GitHub Pages when changes are pushed to `main`.

## Resources

- **[Coding Standards](https://openliberty.github.io/yoko/contribution/coding-standards)** - Complete coding guidelines
- **[Release Process](https://openliberty.github.io/yoko/contribution/release-process)** - For maintainers
- **[Testify Documentation](https://openliberty.github.io/yoko/)** - Testing framework guide
- **[Interop Testing](https://openliberty.github.io/yoko/contribution/interop-testing)** - Version compatibility testing
- **[CORBA Specifications](https://www.omg.org/spec/CORBA/)** - CORBA standards
- **[Issue Tracker](https://github.com/OpenLiberty/yoko/issues)** - Report bugs or request features
- **[Conventional Commits](https://www.conventionalcommits.org/)** - Commit message format

## License

Yoko is licensed under the Apache License 2.0. See [LICENSE](LICENSE) for details.
