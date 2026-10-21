# Saving a game is saving its action log

A game is stored as its setup plus the list of legal moves chosen so far. Resuming is
replaying that list. There is no separate saved-game format, no mapper, and no
serialisation layer in the rules.

## Considered options

- **Snapshot the state** (the previous `SavedGame` + `SavedGameMapper`, with its own
  versioned layout and a Gson dependency inside the rules). Saving is O(1) and the
  file is compact, but it duplicates the layout of the state in a second place, and
  every field added to the state has to be added to the snapshot format too.
- **Both.** A log as the source of truth plus periodic snapshots for speed.

## Consequences

- One artefact serves three purposes: resuming a game, reproducing a reported bug, and
  producing training data.
- Resuming does not need the policy that played the game — the moves are recorded, and
  the legal moves are a pure function of the state.
- The cost is O(moves) to resume, which for a card game is a few hundred entries.
- The rules module has no serialisation dependency at all.
- The format of the log belongs to whoever writes it (a front end or `tools/exporter`),
  not to the rules.
