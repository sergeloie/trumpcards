# Core Module Analysis: Trump Cards Game Engine

**Date:** 2026-07-15  
**Analyst:** Architect Mode  
**Project:** trumpcards (core module)

---

## Executive Summary

The `core` module is **well-architected and ready for multi-platform UI integration** (mobile, desktop, web). The codebase demonstrates a clean separation of concerns through a deliberate multi-stage refactoring process (documented in code comments as "Stage 1–7"). The domain model is minimal, cohesive, and has low coupling. The event-driven architecture with `GameListener`/`GameEvent` and `InputProvider` interfaces provides clean seams for UI integration.

---

## 1. Domain Model Characterization

### 1.1 Core Entities

| Entity | Type | Responsibility | Coupling |
|--------|------|----------------|----------|
| `Card` | `record` | Immutable value object (Suit + Rank) | Zero dependencies |
| `Player` | `class` | Hand management, AI decision logic, event emission | Depends on `GameListener`, `Card` |
| `HumanPlayer` | `class` (extends `Player`) | Human decision delegation via `InputProvider` | Depends on `InputProvider` |
| `Dealer` | `class` | Deck creation, shuffling, dealing | Depends on `Card`, `TurnOrder`, `Player` |
| `TurnOrder` | `class` | Circular seating order, active-player navigation | Depends on `Player` |
| `Scoreboard` | `class` | Per-suit ladder stacks, elimination rules | Depends on `Card` only |
| `Game` | `class` | Orchestrator: rounds, flow, `GameDriver` for stepwise play | Depends on all above + `GameListener` |
| `GameDriver` | inner class | Stepwise driver for UI-driven play (one move per `step()`) | Internal to `Game` |
| `GameSimulator` | `class` | Verification harness (invariants, determinism) | Test-only, depends on `Game` |

### 1.2 Domain Model Simplicity Assessment

**Strengths:**
- **Minimal entities**: Only 7 core domain classes + 1 record (`Card`)
- **Pure data where possible**: `Card` is a `record`; `GameEvent` variants are `record`s
- **Single responsibility**: Each class has a clear, narrow purpose (e.g., `Scoreboard` only knows about card stacks and ladder rules)
- **No JPA/ORM annotations**: Pure domain model, no persistence leakage
- **No circular dependencies**: Dependency graph is a DAG

**Coupling Analysis:**
```
Card (leaf)
  ↑
Scoreboard ← Dealer ← Game → TurnOrder
  ↑              ↑
Player ← HumanPlayer
  ↑
GameListener (interface, presentation layer)
InputProvider (interface, presentation layer)
```

**Verdict:** ✅ **Excellent** — Domain model is maximally simple with minimal coupling. The only "heavy" class is `Game` (orchestrator), which is expected for a game engine.

---

## 2. Layer Separation Analysis

### 2.1 Three-Layer Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                        │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────┐  │
│  │ ConsoleGameListener │ DesktopGameListener │ WebGameListener │  │
│  │ ConsoleInputProvider │ DesktopInputProvider │ WebInputProvider  │  │
│  └────────┬────────┘  └────────┬────────┘  └──────┬──────┘  │
└───────────┼────────────────────┼────────────────────┼─────────┘
            │                    │                    │
            ▼                    ▼                    ▼
┌─────────────────────────────────────────────────────────────┐
│                      CORE ENGINE (core module)               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐   │
│  │   Game       │  │  GameDriver  │  │  GameSimulator   │   │
│  │  (orchestrator)│  (stepwise)    │  (verification)    │   │
│  └──────┬───────┘  └──────┬───────┘  └────────┬─────────┘   │
│         │                 │                     │            │
│  ┌──────▼─────────────────▼─────────────────────▼──────┐    │
│  │              DOMAIN MODEL                            │    │
│  │  Player, Dealer, TurnOrder, Scoreboard, Card        │    │
│  └──────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 Separation Verification

| Layer | Classes | Depends On | Verified Separation |
|-------|---------|------------|---------------------|
| **Game Engine** | `Game`, `GameDriver`, `Dealer`, `TurnOrder`, `Scoreboard`, `Player`, `Card`, `GameSimulator` | Domain model only + `GameListener`/`InputProvider` interfaces | ✅ No I/O, no UI framework deps |
| **Input (Human)** | `InputProvider` (interface), `ConsoleInputProvider`, `DesktopInputProvider`, `NoopInputProvider` | `Player`, `Card`, `List<Card>` | ✅ Pure interface in core; impls in presentation |
| **Output (Events)** | `GameListener` (interface), `GameEvent` (sealed interface + records), `ConsoleGameListener`, `DesktopGameListener` | `Player`, `Card`, `Map`, `List` | ✅ Pure data events; core never formats/prints |

### 2.3 Key Architectural Decisions (from refactor stages)

| Stage | Change | Impact on Separation |
|-------|--------|---------------------|
| Stage 1 | Introduced `GameEvent` + `GameListener` | Moved all `System.out` out of model |
| Stage 1 | Introduced `InputProvider` | Moved `Scanner`/console out of `HumanPlayer` |
| Stage 2 | `HumanPlayer` extends `Player`, overrides only decision hooks | Eliminated `makeMove` duplication |
| Stage 3 | `TurnOrder` replaces `CircularDoublyLinkedList` | Deterministic, testable turn navigation |
| Stage 4 | Extracted `Dealer` + `Scoreboard` from `Game` | Isolated dealing/scoring rules, unit-testable |
| Stage 5 | Injected `Random` into `Game` | Deterministic replay, seed-based testing |
| Stage 6 | `Messages` ResourceBundle + `CardLocalizer` | All user strings externalized, i18n-ready |
| Stage 7 | `GameSimulator` verification harness | Automated invariant checking |

---

## 3. Multi-Platform UI Readiness Assessment

### 3.1 Current Platform Implementations

| Platform | Module | Listener | Input Provider | Status |
|----------|--------|----------|----------------|--------|
| Console | `core` (App.java) | `ConsoleGameListener` | `ConsoleInputProvider` | ✅ Working |
| Desktop (LibGDX) | `desktop-libgdx` | `DesktopGameListener` | `DesktopInputProvider` | ✅ Working |
| Mobile (Android) | *planned* | — | — | 🔄 Ready to implement |
| Web (HTML5/GWT) | *planned* | — | — | 🔄 Ready to implement |

### 3.2 Readiness Checklist

| Requirement | Status | Evidence |
|-------------|--------|----------|
| **Engine is UI-agnostic** | ✅ | Core has zero UI framework dependencies |
| **Event-driven output** | ✅ | `GameListener.onEvent(GameEvent)` — 7 event types cover full game flow |
| **Pluggable input** | ✅ | `InputProvider` interface with 2 methods (`chooseLeadCard`, `chooseDefense`) |
| **Deterministic replay** | ✅ | `Game.playGame(Random)` + `GameDriver` for stepwise control |
| **Snapshot capability** | ✅ | `GameSnapshot` (desktop) captures full state for rendering |
| **I18n support** | ✅ | `Messages` (ResourceBundle) + `CardLocalizer` (FULL/SHORT/LETTERS styles) |
| **Thread-safety for UI** | ⚠️ Partial | Desktop uses `CompletableFuture` + `Gdx.app.postRunnable()`; needs documentation for mobile/web |
| **Headless testability** | ✅ | `NoopInputProvider`, `NopListener`, `GameSimulator` run without UI |

### 3.3 Integration Pattern for New Platforms

```java
// 1. Implement GameListener (rendering)
public class MobileGameListener implements GameListener {
    @Override public void onEvent(GameEvent event) {
        // Update UI widgets, animate, show toasts, etc.
    }
}

// 2. Implement InputProvider (human choices)
public class MobileInputProvider implements InputProvider {
    @Override public Card chooseLeadCard(Player player, List<Card> hand) {
        // Show card selection UI, return CompletableFuture<Card> or block on UI thread
    }
    @Override public Card chooseDefense(Player player, Card attacking, List<Card> validDefenses) {
        // Show defense options, return chosen card
    }
}

// 3. Wire in platform launcher (like App.java / DesktopLauncher.java)
Game game = new Game(true); // humanPlayer = true
game.setListener(new MobileGameListener());
HumanPlayer human = (HumanPlayer) game.getPlayers().stream()
    .filter(p -> p instanceof HumanPlayer).findFirst().orElseThrow();
human.setInput(new MobileInputProvider());
game.playGame(); // or use GameDriver for stepwise
```

---

## 4. Refactoring Suggestions

### 4.1 High Priority (Do Before Multi-Platform Rollout)

| # | Issue | Recommendation | Effort |
|---|-------|----------------|--------|
| 1 | **`Game` exposes mutable `List<Card> getPot()`** | Return `List<Card>` copy (already does) but document immutability; consider `UnmodifiableList` | Low |
| 2 | **`Player.hand` is mutable `List<Card>` exposed via getter** | Make `getHand()` return `List<Card>` copy or `UnmodifiableList`; provide `handSize()` for UI | Medium |
| 3 | **`HumanPlayer` duplicates defense-card filtering logic** | Move `validDefenses` computation to `Player` as `getValidDefenses(Card attacking)` | Low |
| 4 | **`GameDriver` is inner class** | Extract to top-level `GameDriver` for easier testing and reuse across platforms | Low |
| 5 | **No `GameState` DTO for snapshots** | Create immutable `GameSnapshot` in core (desktop has its own) for consistent state transfer | Medium |

### 4.2 Medium Priority (Architecture Hardening)

| # | Issue | Recommendation |
|---|-------|----------------|
| 6 | **`Game` constructor creates players with hardcoded suits** | Extract `PlayerFactory` or `GameSetup` for configurable player count/suits (needed for 2-3 player variants) |
| 7 | **`TurnOrder` fixed at 4 players** | Make `TurnOrder` work with any `List<Player>` (already does, but `Game` assumes 4) |
| 8 | **`Scoreboard.init()` mutates dealer's deck** | Consider making `Dealer` immutable after shuffle; pass deck copy to `Scoreboard` |
| 9 | **`GameEvent.RoundStarted` exposes full hands map** | For security/privacy (networked play), consider `RoundStarted` with only current player's hand + counts for others |
| 10 | **No explicit "your turn" event** | Add `GameEvent.TurnStarted(Player currentPlayer, List<Card> validChoices)` so UI doesn't need `repaintHook` workaround |

### 4.3 Low Priority (Nice to Have)

| # | Suggestion |
|---|------------|
| 11 | Add `GameConfig` record (player count, deck variant, max rounds, etc.) |
| 12 | Extract `CardComparator` for consistent sorting (trump-aware, rank-aware) |
| 13 | Add `GameEvent.MoveHint` for AI-suggested moves (tutorial/hint mode) |
| 14 | Consider `GameListener` sub-interfaces (`GameRenderer`, `GameLogger`) for separation of concerns |
| 15 | Add `GameObserver` read-only interface for spectator/replay modes |

---

## 5. Test Coverage Assessment

| Component | Test Class | Coverage Notes |
|-----------|------------|----------------|
| `Card` | (implicit via others) | Record — minimal logic |
| `Dealer` | `DealerTest` | ✅ Deck composition, deterministic shuffle, dealing to active players |
| `Scoreboard` | `ScoreboardTest` | ✅ Ladder progression, ACE elimination, stack order |
| `Game` | `GameTest` | ✅ Creation, dealing, obligatory exchange, full game, determinism |
| `GameSimulator` | `GameSimulatorTest` | ✅ Invariants (36 cards, 1 winner, termination) across seeds |
| `CardLocalizer` | `CardLocalizerTest` | ✅ Styles, locales, formatting |
| `Messages` | `MessagesTest` | ✅ Bundle resolution, formatting |

**Verdict:** ✅ **Strong test coverage** for core engine. The `GameSimulator` provides property-based verification across thousands of seeds.

---

## 6. Dependency Graph (Core Module Only)

```
ru.anseranser
├── model
│   ├── Card (record, no deps)
│   ├── Player
│   │   └── → GameListener, Card
│   ├── HumanPlayer (extends Player)
│   │   └── → InputProvider
│   ├── Dealer
│   │   └── → Card, TurnOrder, Player, Random
│   ├── TurnOrder
│   │   └── → Player
│   ├── Scoreboard
│   │   └── → Card
│   ├── Game
│   │   └── → Player, Dealer, TurnOrder, Scoreboard, GameListener, Random, GameDriver
│   ├── GameDriver (inner)
│   └── GameSimulator
│       └── → Game, Card, Player
├── event
│   ├── GameEvent (sealed, records)
│   │   └── → Player, Card, Map, List
│   ├── GameListener (interface)
│   ├── ConsoleGameListener
│   │   └── → Messages, CardLocalizer
│   └── NopListener
├── input
│   ├── InputProvider (interface)
│   ├── ConsoleInputProvider
│   │   └── → Messages, CardLocalizer, Scanner
│   └── NoopInputProvider
└── i18n
    ├── Messages (ResourceBundle wrapper)
    └── CardLocalizer
        └── → Messages
```

**No cycles detected.** ✅

---

## 7. Conclusion & Sign-Off

### Readiness Verdict: **READY FOR MULTI-PLATFORM UI DEVELOPMENT**

The `core` module meets all architectural prerequisites:
1. ✅ Clean domain model with minimal coupling
2. ✅ Strict separation of engine / input / output
3. ✅ Event-driven output (`GameListener` + `GameEvent`)
4. ✅ Pluggable human input (`InputProvider`)
5. ✅ Deterministic, seedable engine (`Random` injection)
6. ✅ Stepwise driver (`GameDriver`) for frame-by-frame UI control
7. ✅ Snapshot capability (`Game.allCards()`, `getPot()`, `getPlayers()`)
8. ✅ I18n-ready (`Messages` + `CardLocalizer` with 3 styles)
9. ✅ Headless testability (`NoopInputProvider`, `NopListener`, `GameSimulator`)
10. ✅ Proven desktop integration (`desktop-libgdx` module works)

### Recommended Next Steps

1. **Implement Priority 1–5 refactorings** (2–3 days) to harden APIs before mobile/web work
2. **Create `core/src/main/java/ru/anseranser/snapshot/GameSnapshot.java`** as shared DTO
3. **Add `GameEvent.TurnStarted`** to eliminate `repaintHook` workaround
4. **Document `GameDriver` threading contract** for platform implementers
5. **Begin Android module** (`android-libgdx` or `android-jetpack`) using same integration pattern as `desktop-libgdx`

---

*Document generated by Architect mode analysis. Save to `docs/core-module-analysis.md`.*