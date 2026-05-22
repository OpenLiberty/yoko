---
description: Conventionally commit the changes with optional AI attribution
---

1. Figure out what changed using `git status` and `git diff`.
2. Figure out the scope for the changes. Verify it with the user and allow them to select an alternative, or no component at all.
3. Figure out whether the changes included anything created or substantially modified by you (AI).
4. Figure out whether there were any human co-authors. Ask the user to identify them, and offer likely suggestions from recent git logs when available.
5. Produce a Conventional Commit with:
   - a subject line in the form `type(scope): description`, or `type: description` if there is no component
   - a detailed body explaining the substantive changes
   - a short summary of lines added and removed
6. If AI contributed to the change, add a footer using:
   `Co-authored-by-AI: IBM Bob <bob version>`
7. If the Bob version is not already known, ask the user for it explicitly before finalizing the commit message.
8. Before running `git commit`, present the full proposed commit message to the user for confirmation.
9. When approved, run the commit command.
10. If there are human co-authors, include standard git footers such as:
    `Co-authored-by: Neil Richards <neil_richards@uk.ibm.com>`
11. Do not claim tests or verification that were not actually run.
12. Keep the body specific to the actual files and behaviors changed, not generic boilerplate.

Preferred body shape:

```text
type(scope): short description

- first concrete change
- second concrete change
- third concrete change
- lines added: <n>, lines removed: <n>

Co-authored-by-AI: IBM Bob <bob version> (GPT-5)
