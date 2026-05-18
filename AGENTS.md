# Bob Shell Configuration for Yoko Project

This file provides guidance for developers using Bob Shell with the Yoko project.

## Quick Start

If you're new to this repository and want to use Bob Shell:

1. **Set up Bob worktree** (one-time setup per clone):
   ```bash
   chmod +x scripts/setup-bob-worktrees.sh
   ./scripts/setup-bob-worktrees.sh
   ```
   
   **Note:** If you already have a `.bob/` directory that isn't a worktree (e.g., from Bob Shell creating it automatically), the setup script will:
   - Detect this situation and migrate it to a proper worktree
   - Back up all existing content to `.bob-migration-backup/`
   - Create the worktree from the `bob` branch
   - Restore all your backed up content
   - Keep the backup for you to verify and remove manually

2. **Start using Bob Shell** - Bob will automatically load team preferences from `.bob/memory/AGENTS.md`

## Bob Worktree Structure

This project uses a Git worktree to manage Bob-related artifacts separately from the main codebase:

- **`.bob/`** (bob branch) - Single worktree for all Bob artifacts
  - `config/` - Bob configuration (custom modes, MCP servers, team settings)
  - `docs/` - Shared documentation (test plans, technical analysis, design documents)
  - `memory/` - Team knowledge (`AGENTS.md` with conventions and preferences)
  - `notes/` - Personal notes (not tracked in git)

## Workflow

### Daily Use
```bash
# Pull latest team knowledge before starting work
cd .bob && git pull && cd ..

# Work with Bob Shell normally
# Bob automatically loads .bob/memory/AGENTS.md

# Keep personal notes in .bob/notes/ (not tracked)
# Move to .bob/docs/ when ready to share with team
```

### Sharing Documentation
```bash
# When your analysis/test plan is ready to share:
cd .bob
git add docs/my-feature-analysis.md
git commit -m "docs: add analysis for feature X"
git push
cd ..
```

### Updating Team Conventions
```bash
# When team agrees on a new convention:
cd .bob
# Edit memory/AGENTS.md
git add memory/AGENTS.md
git commit -m "docs: add convention for error handling"
git push
cd ..
```

## Cleanup

To remove Bob worktree (e.g., when switching to a different clone):
```bash
./scripts/cleanup-bob-worktrees.sh
```


## Benefits

- **Clean History**: Bob artifacts don't pollute main branch history
- **Selective Sharing**: Choose what to share with the team
- **Privacy**: Personal notes stay local in `.bob/notes/`
- **Collaboration**: Team can review and improve shared documentation
- **Versioning**: Full Git history for Bob artifacts
- **Simplicity**: Single worktree instead of three

## Team Conventions

After setting up the worktree, Bob Shell will automatically load team conventions from:
- **`.bob/memory/AGENTS.md`** - Main team memory file with project conventions
- **`.bob/config/modes/`** - Custom Bob Shell modes (if any)
- **`.bob/config/mcp-servers/`** - MCP server configurations (if any)

To view or edit team conventions:
```bash
# View current conventions
cat .bob/memory/AGENTS.md

# Edit conventions (then commit and push to share)
cd .bob
# Edit memory/AGENTS.md
git add memory/AGENTS.md
git commit -m "docs: update team conventions"
git push
cd ..
```

See `.bob/memory/AGENTS.md` for current team conventions and preferences.