---
description: Sync the .bob worktree by rebasing, committing, and pushing its changes
---

1. Inspect the `.bob` worktree changes first using:
   - `git -C .bob status --short`
   - `git -C .bob diff --stat`
   - `git -C .bob diff --cached --stat`
2. Summarize what changed in `.bob`.
3. Propose a suitable documentation-style commit message in the form:
   - `doc: ...`
4. Present the proposed commit message to the user for confirmation before running any mutating git command beyond the rebase pull.
5. When approved, run these commands in order:
   - `git -C .bob pull --rebase`
   - `git -C .bob add .`
   - `git -C .bob commit -m "<approved message>"`
   - `git -C .bob push origin`
6. If the rebase pull reports conflicts or fails, stop and report the problem instead of continuing.
7. If there is nothing to commit, report that and do not push.
8. Keep the commit message specific to the actual `.bob` changes.
9. Include AI attribution unless the user explicitly rejects it: "Co-authored-by-AI: IBM Bob version" (Ask user for version.)

Preferred interaction:

```text
- inspect .bob changes
- propose a doc: commit message
- ask for approval
- run git -C .bob pull --rebase
- run git -C .bob add .
- run git -C .bob commit -m "doc: ..."
- run git -C .bob push origin
```