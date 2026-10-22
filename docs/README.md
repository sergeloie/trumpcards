# Documentation

This folder holds the project's engineering documents.

| Folder or file | What it holds |
|---|---|
| [`adr/`](./adr) | The architecture decision records: why the rules module is shaped the way it is, one decision per file. |
| [`plan.md`](./plan.md) | The plan of the migration to the current engine: the steps, the gates that kept it safe, and what each task established. |
| [`archive/`](./archive) | The records of the previous engine and of its review and refactoring cycle. |

The two documents that describe the repository as it is now live outside this
folder: [`../CONTEXT.md`](../CONTEXT.md) is the game's rules and vocabulary, and
[`../CONTEXT-MAP.md`](../CONTEXT-MAP.md) is the architectural map of the modules.

## The archive

`archive/` holds the documents of the engine that was replaced:

- `review.md` — the review of the old engine and of the front ends;
- `determinism-audit.md` — the audit of the old engine's determinism;
- `refactoring-plan.md` — the plan and the record of the refactoring cycle;
- `testing.md` — the test strategy of the old engine;
- `game_state_machine.yaml` — the state machine of the old engine.

They are written in Russian and are kept as they were: they are the record of a
finished cycle, and translating them would rewrite the author's text for no
reader. They describe a codebase that no longer exists — the modules and the
classes they name (`Game`, `GameDriver`, `Player`, `Scoreboard`, `Dealer`) are
gone. Read them for the history of the project, not for the current design.
