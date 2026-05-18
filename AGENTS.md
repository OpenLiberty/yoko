## Bob-Shell Team Memories

This file contains shared team knowledge and preferences for the Yoko project.

### Git Commit Conventions
- All git commits should follow the Conventional Commits specification (https://www.conventionalcommits.org/)
- Commit message format: `type(scope): description`
- Common types: feat, fix, docs, style, refactor, test, chore, build, ci

### Git Commit Attribution for AI-Generated Content
- When committing content generated or significantly modified by AI tools, add attribution at the end of the commit message body
- Format: `Co-authored-by-AI: <Tool Name> <Version> (<LLM and Version>)`
- Version number is **mandatory** - developer must supply it if not automatically discoverable
- Include LLM and version in parentheses when known
- Examples:
  - `Co-authored-by-AI: IBM Bob 1.0.1 (Claude 3.5 Sonnet)`
  - `Co-authored-by-AI: IBM Bob 1.0.1 (GPT-4)`
  - `Co-authored-by-AI: GitHub Copilot 1.2.3 (GPT-4)`
- Note: Both Bob Shell and Bob IDE can be attributed as "IBM Bob"

### Project-Specific Notes
- Add project-specific conventions and preferences here as the team discovers them
