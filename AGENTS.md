## Bob-Shell Bootstrap Instructions

### Bob Worktree Setup (REQUIRED FIRST STEP)
Before starting any work, Bob MUST ensure the .bob worktree is properly set up:

1. **Check if .bob is a worktree**: Run `git worktree list | grep "\.bob"`
2. **If NOT a worktree**: Run `./scripts/setup-bob-worktrees.sh` to create it
3. **If IS a worktree**: Run `git -C .bob pull --rebase` to update it

This ensures Bob has access to the latest team configuration, documentation, and shared memories in the .bob worktree.

After setup, refer to `.bob/memory/AGENTS.md` for full team conventions and guidelines.
