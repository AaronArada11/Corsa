# Corsa project knowledge

## Location and purpose

The shared project knowledge base is `docs/project-knowledge/` in this repository. These files are part of the repository and should be edited there so the team can share them through Git.

Use these notes:

- `Overview.md` — product purpose, intended users, agreed features, and requirements.
- `STATUS.md` — the project's current, verified implementation state and known blockers.
- `decisions.md` — important product, architecture, and technical decisions, with reasons.
- `progress.md` — a dated history of meaningful completed work and its verification.

Do not use or search a teammate's personal Obsidian vault path. Each teammate can open the repository's `docs/project-knowledge/` folder as a vault in Obsidian.

## How to use the notes

Before planning implementation, answering questions about project history, or making product or architecture decisions:

1. Read `Overview.md` and `STATUS.md`.
2. Read relevant entries in `decisions.md` and `progress.md` if the task depends on past decisions or work.
3. Read only notes relevant to the current task. Do not scan unrelated folders or notes.

Treat the repository's code as the source of truth for what is currently implemented. Treat the project knowledge notes as the source of truth for agreed product intent, decisions, research, terminology, and historical context.

If the code and notes disagree, explain the disagreement. Do not change product behavior based on stale or ambiguous notes without asking the user.

## Keeping notes current

After completing and verifying a meaningful project change, update only the affected notes:

- Update `STATUS.md` when the verified current implementation state, features, or blockers change.
- Add an entry to `progress.md` for meaningful completed work. Include the date, a concise description, and how the work was verified. Do not record unfinished work as completed.
- Add an entry to `decisions.md` when a material product or technical decision is made. Record the date, decision, reason, and relevant alternatives or consequences.
- Update `Overview.md` only when the agreed product purpose, requirements, or scope changes.

Preserve useful existing information. Do not rewrite unrelated sections. Do not claim that code, behavior, or verification exists unless you have evidence for it. Do not commit or push changes unless the user asks.

All other project knowledge notes are read-only unless the user explicitly asks to change them.

## Resolving note conflicts

If a Git merge or rebase creates conflicts in `STATUS.md`, `progress.md`, `decisions.md`, or `Overview.md`:

1. Read both sides of each conflict before editing. Do not discard a side without reviewing its content.
2. Review relevant Git history and the changes on both sides. Use recorded dates and commit history to understand sequence; do not assume that push time alone establishes which work is newer.
3. Preserve distinct, relevant information from both sides. Remove only exact duplicates or entries shown to be obsolete.
4. Rebuild `progress.md` as a chronological history, retaining meaningful completed work from both sides and its verification details.
5. Rebuild `STATUS.md` to describe the current state verified from the repository after the merge or rebase. Keep historical details in `progress.md`.
6. Preserve each decision and its rationale in `decisions.md`. If two decisions conflict and the repository or history does not show which one the team accepted, do not choose one; explain the conflict and ask the user.
7. Remove all Git conflict markers only after reconciling the content. If an item cannot be reconciled confidently, leave it clearly identified and ask the user before treating it as settled.

After resolving conflicts, summarize what was combined, what was changed in `STATUS.md`, and anything still needing confirmation.