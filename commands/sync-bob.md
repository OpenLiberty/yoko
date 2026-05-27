---
description: Sync the .bob worktree by committing, rebasing, and pushing its changes
---

0. Re-read this command definition in case it changed.
1. Inspect the `.bob` worktree changes first using:
   - `git -C .bob status --short`
   - `git -C .bob diff --stat`
   - `git -C .bob diff --cached --stat`
2. Summarize what changed in `.bob`.
3. Propose a suitable documentation-style commit message in the form:
   - `doc: ...`
4. Present the proposed commit message to the user for confirmation before running any mutating git command.
5. When approved:
   - if there are changes to commit, run these commands in order:
     - `git -C .bob add .`
     - `git -C .bob commit -m "<approved message>"`
   - if there is nothing to commit, report that there is nothing to commit and skip add/commit
6. Regardless of whether there were local changes to commit, run:
   - `git -C .bob pull --rebase`
7. If the rebase pull reports conflicts or fails after a local commit, stop and report the problem instead of continuing to push.
8. If the rebase pull succeeds, only run `git -C .bob push origin` when there was a successful local commit to push.
9. Keep the commit message specific to the actual `.bob` changes.
10. Include AI attribution unless the user explicitly rejects it: `Co-authored-by-AI: IBM Bob version` (Ask user for version.)

Preferred interaction:

```text
- inspect .bob changes
- propose a doc: commit message
- ask for approval
- if needed, run git -C .bob add .
- if needed, run git -C .bob commit -m "doc: ..."
- run git -C .bob pull --rebase
- if a local commit was created, run git -C .bob push origin
```
