# Анализ модуля core — Доменная модель, разделение слоёв, готовность к мультиплатформенности

**Дата:** 2026-07-15
**Модуль:** `core/` (чистая логика, без UI-зависимостей)

---

## 1. Структура модуля core

```
core/src/main/java/ru/anseranser/
├── model/
│   ├── Card.java              # record(Suit, Rank) — иммутабельная值型
│   ├── Player.java            # AI-игрок, Template Method (makeMove)
│   ├── HumanPlayer.java       # Человеческий игрок, переопределяет决策 hooks
│   ├── Game.java              # Оркестрация + внутренний GameDriver (417 строк)
│   ├── Dealer.java            # Колода 36 карт, тасовка, раздача
│   ├── Scoreboard.java        # Лесенки по мастям (SIX→ACE), элиминация
│   ├── TurnOrder.java         # Циклический порядок хода
│   └── GameSimulator.java     # Массовая проверка инвариантов (5000 seed'ов)
├── event/
│   ├── GameEvent.java         # Sealed interface + 7 record-подтипов
│   ├── GameListener.java      # Интерфейс приёмника событий
│   ├── NopListener.java       # Заглушка для headless-тестов
│   └── ConsoleGameListener.java # Консольный рендеринг
├── input/
│   ├── InputProvider.java     # Абстракция ввода (выбор карты)
│   ├── NoopInputProvider.java # Заглушка (выбрасывает)
│   └── ConsoleInputProvider.java # Консольный ввод (Scanner)
└── i18n/
    ├── Messages.java          # ResourceBundle + MessageFormat
    └── CardLocalizer.java     # Локализация имён карт (FULL/SHORT/LETTERS)
```

---

## 2. Доменная модель — Характеристика

### 2.1 Сущности и их Responsibilities

| Сущность | Тип | Responsibility | Строк |
|----------|-----|----------------|-------|
| `Card` | record | Иммутабельная值型: масть + ранг | 26 |
| `Card.Suit` | enum | 4 масти: SPADES, CLUBS, DIAMONDS, HEARTS | — |
| `Card.Rank` | enum | 9 рангов: SIX(6)→ACE(14), с числовым value | — |
| `Player` | class | AI-игрок: логика хода, сравнение карт, выбор защиты | 123 |
| `HumanPlayer` | class (extends Player) | Человек: переопределяет выбор карты через InputProvider | 54 |
| `Dealer` | class | Колода 36 карт, тасовка, раздача по кругу | 52 |
| `Scoreboard` | class | Лесенки по мастям: SIX→SEVEN→...→ACE, проверка элиминации | 80 |
| `TurnOrder` | class | Циклический порядок: next(), nextActive() | 75 |
| `Game` | class | Оркестрация: раунды, обмен, определение проигравшего | 417 |
| `Game.GameDriver` | inner class | Пошаговый драйвер для UI (step/finishRound) | ~80 |
| `GameSimulator` | class | Bulk-проверка: 36 карт, 1 победитель, terminate | 101 |

### 2.2 Дизайн-паттерны

| Паттерн | Применение | Где |
|---------|-----------|-----|
| **Record (value object)** | `Card` — иммутабельность, автоматический equals/hashCode | `Card.java` |
| **Template Method** | `Player.makeMove()` — общая логика, hooks `playLeadCard`/`chooseDefenseCard` | `Player.java:61-85` |
| **Strategy** | `InputProvider` — абстракция выбора карты | `InputProvider.java` |
| **Sealed interface + Pattern matching** | `GameEvent` — 7 типов событий | `GameEvent.java` |
| **Observer** | `GameListener` — уведомление UI о ходах | `GameListener.java` |
| **State machine** | `GameDriver.phase` — INITIAL→ROUND_ACTIVE→ROUND_ENDED→GAME_ENDED | `Game.java:316` |
| **Dependency injection** | `Random`, `GameListener`, `InputProvider` — все инжектируются | constructors |

### 2.3 Правила игры (Kozel / Козёл)

Игра на 4 игрока, каждый с自己 мастью. 36 карт (9 рангов × 4 масти). Правила:
1. Каждый раунд: тасовка → раздача → обязательный обмен (SEVEN→KING.owner)
2. Ходы: ведущий кладёт карту, следующий бьёт или берёт банк
3. Проигравший раунд кладёт свойlowest trump на лесенку
4. ACE на лесенке = элиминация (игрок выбывает)
5. Последний оставшийся = победитель

---

## 3. Разделение слоёв — Аудит

### 3.1 Архитектура слоёв

```
┌─────────────────────────────────────────────────┐
│  Presentation Layer (UI)                        │
│  ConsoleGameListener, ConsoleInputProvider      │
│  DesktopGameListener, DesktopInputProvider      │
│  (будущие: MobileUI, WebUI)                     │
├─────────────────────────────────────────────────┤
│  Seam Layer (Seam)                              │
│  GameListener interface ←── GameEvent sealed    │
│  InputProvider interface                        │
├─────────────────────────────────────────────────┤
│  Domain Layer (core model)                      │
│  Card, Player, HumanPlayer, Dealer,             │
│  Scoreboard, TurnOrder, Game, GameDriver        │
├─────────────────────────────────────────────────┤
│  Verification Layer                             │
│  GameSimulator (bulk invariant checks)          │
└─────────────────────────────────────────────────┘
```

### 3.2 Анализ зависимостей

**Что core НЕ зависит от (хорошо):**
- ❌ LibGDX, Android, JS — нет UI-зависимостей
- ❌ java.io, java.net — нет I/O в модели
- ❌ System.out — только в presentation layer
- ❌ Scanner, Console — только в ConsoleInputProvider

**Что core зависит от (нормально):**
- ✅ Lombok (compile-time) — @Getter, @Setter, @RequiredArgsConstructor
- ✅ Guava —声明了但未在 model 中使用 (только в build.gradle)
- ✅ java.util — стандартная библиотека

### 3.3 Нарушения разделения

#### Проблема 1: Player содержит GameListener (耦合)

```java
// Player.java:24
@Setter
protected GameListener listener = NopListener.INSTANCE;
```

**Проблема:** `Player` (доменная сущность) напрямую зависит от `GameListener` (seam-интерфейс). Каждый `makeMove()` вызывает `listener.onEvent(...)`. Это создаёт双向依赖 между domain и seam слоями.

**Влияние:** При добавлении нового UI (мобильный, веб) — каждый `Player` должен знать о `GameListener`. Это не致命но, но нарушает чистоту доменного слоя.

**Рекомендация:** Вынести генерацию событий из `Player` в `Game` (или `GameDriver`). `Player` должен только возвращать решение (какую карту играть), а `Game` — применять его и генерировать события. Это сделает `Player` чистой доменной сущностью без зависимостей от seam-слоя.

#### Проблема 2: Player содержит mutable hand (encapsulation)

```java
// Player.java:20
private final List<Card> hand = new ArrayList<>();
```

`getHand()` возвращает ссылку на внутренний список. Любой код может мутировать руку напрямую. `Dealer.deal()` делает `current.getHand().add(card)`, `Game.endRound()` делает `loser.getHand().clear()`.

**Рекомендация:** Для UI-портов нужно `Collections.unmodifiableList(hand)` для внешнего доступа + отдельный `protected` метод для внутренних мутаций. Или добавить `addCard()`/`removeCard()` методы в Player.

#### Проблема 3: Game — 417 строк,多重职责

`Game` содержит:
- Оркестрацию раундов (`setupRound`, `endRound`, `distributeObligatoryCards`)
- Вспомогательные методы (`countActiveGamers`, `countActiveGamersWithCards`, `mostCardsRounder`, `determineLoser`)
- Snapshot-методы (`snapshotScoreboard`, `snapshotHands`)
- Внутренний класс `GameDriver` (~80 строк)
- Game flow (`playGame`, `getWinner`, `getTrump`, `getPot`, `allCards`)

**Рекомендация:** Вынести `GameDriver` в отдельный класс. Snapshot-методы можно移到 `GameSnapshot` или оставить — они простые.

#### Проблема 4: HumanPlayer хардкодит SPADES

```java
// HumanPlayer.java:29
public HumanPlayer() {
    super(Card.Suit.SPADES);
}
```

По умолчанию человек всегда получает пики. Это работает, но при изменении правил (случайный выбор масти) сломается.

**Рекомендация:** Передавать trump через параметр (конструктор без default).

---

## 4. Связность (Coupling) — Аудит

### 4.1 Матрица зависимостей

| Класс | Зависит от | Тип связи |
|-------|-----------|-----------|
| `Card` | `Card.Suit`, `Card.Rank` | Вложенные enums (值型) |
| `Player` | `Card`, `GameEvent`, `GameListener` | ⚠️ Domain→Seam |
| `HumanPlayer` | `Player`, `InputProvider`, `GameEvent` | ⚠️ Domain→Seam |
| `Dealer` | `Card`, `TurnOrder`, `Player` | ✅ Domain→Domain |
| `Scoreboard` | `Card` | ✅ Domain→Domain |
| `TurnOrder` | `Player` | ✅ Domain→Domain |
| `Game` | `Player`, `Dealer`, `Scoreboard`, `TurnOrder`, `GameEvent`, `GameListener` | ⚠️ Domain→Seam |
| `GameDriver` | (inner of Game) | ✅ Same class |
| `GameSimulator` | `Game`, `Card`, `Player` | ✅ Domain→Domain |
| `GameEvent` | `Card`, `Player`, `Card.Suit` | ✅ Domain types only |
| `GameListener` | `GameEvent` | ✅ Seam only |
| `InputProvider` | `Card`, `Player` | ✅ Domain types only |

### 4.2 Оценка связности

| Критерий | Оценка | Комментарий |
|----------|--------|-------------|
| Domain→UI зависимости | ✅ Чисто | Нет java.awt, LibGDX, Android в core |
| Domain→Seam зависимости | ⚠️ Средняя | Player, HumanPlayer, Game зависят от GameListener/InputProvider |
| Seam→Domain зависимости | ✅ Нормально | GameEvent, InputProvider используют только domain-типы |
| Value objects | ✅ Хорошо | Card — record (immutable, auto equals/hashCode) |
| Mutable state | ⚠️ Проблема | hand мутабелен, pot мутабелен |

---

## 5. Готовность к мультиплатформенности

### 5.1 Что уже готово

| Фича | Статус | Комментарий |
|------|--------|-------------|
| GameEngine не зависит от UI | ✅ | Чистый Java, нет I/O |
| Sealed interface GameEvent | ✅ | 7 типов событий, pattern matching |
| GameListener seam | ✅ | Любой UI реализует onEvent() |
| InputProvider seam | ✅ | Любой UI реализует chooseLeadCard/chooseDefense |
| Stepwise GameDriver | ✅ | Пошаговый режим для UI (step/finishRound) |
| GameSnapshot | ✅ | Иммутабельный снимок для передачи в UI thread |
| i18n (Messages + CardLocalizer) | ✅ | EN/RU, 3 стиля рендеринга |
| Determinism (инжектируемый Random) | ✅ | Воспроизводимые игры для тестов |
| Test coverage (36 тестов + simulator) | ✅ | Invariant checks на 5000 seed'ов |

### 5.2 Что нужно доработать для UI-портов

| Проблема | Приоритет | Описание |
|----------|-----------|----------|
| Player содержит GameListener | 🔴 Высокий | Domain-сущность не должна знать о seam-слое |
| Player.hand мутабелен | 🔴 Высокий | UI не должен мутировать руку напрямую |
| HumanPlayer хардкодит SPADES | 🟡 Средний | Нужен выбор масти через параметр |
| GameSnapshot в desktop-libgdx | 🟡 Средний | GameState record должен быть в core |
| Player.getPot() мутабельная копия | 🟢 Низкий | `List.copyOf()` вместо `new ArrayList<>()` |
| GameDriver — inner class | 🟢 Низкий | Вынести в отдельный файл для чистоты |

---

## 6. Предложения по рефакторингу (приоритизированные)

### P0: Вынести генерацию событий из Player

**Цель:** Сделать `Player` чистой доменной сущностью без зависимостей от seam-слоя.

**Текущий код (Player.java):**
```java
protected void playLeadCard(List<Card> pot) {
    Card leadCard = chooseLeadCard();
    pot.add(leadCard);
    hand.remove(leadCard);
    listener.onEvent(new GameEvent.CardPlayed(this, leadCard)); // ← seam
}

protected void takePot(Card topCard, List<Card> pot) {
    int taken = pot.size();
    collectPot(pot);
    listener.onEvent(new GameEvent.PotTaken(this, topCard, taken)); // ← seam
}

protected void beatCard(Card topCard, Card beatCard, List<Card> pot) {
    pot.add(beatCard);
    hand.remove(beatCard);
    listener.onEvent(new GameEvent.CardBeaten(this, topCard, beatCard)); // ← seam
}
```

**Предложение:**
Player должен возвращать решение (какую карту играть), а Game/GameDriver — применять его и генерировать события.

```java
// Player.java — чистая доменная логика
public record MoveResult(Card card, MoveType type) {
    enum MoveType { LEAD, DEFEND, TAKE_POT }
}

public Optional<MoveResult> decideMove(List<Card> pot) {
    if (hand.isEmpty()) return Optional.empty();
    if (pot.isEmpty()) {
        Card lead = chooseLeadCard();
        return Optional.of(new MoveResult(lead, MoveResult.MoveType.LEAD));
    }
    Card topCard = pot.getLast();
    Optional<Card> defense = weakestDefense(topCard);
    if (defense.isEmpty()) {
        return Optional.of(new MoveResult(topCard, MoveResult.MoveType.TAKE_POT));
    }
    Card chosen = chooseDefenseCard(topCard, defense.get());
    return Optional.of(new MoveResult(chosen, MoveResult.MoveType.DEFEND));
}
```

```java
// GameDriver.java — применяет решение + генерирует события
public boolean step() {
    Optional<Player.MoveResult> result = current.decideMove(pot);
    if (result.isEmpty()) return false;
    applyMove(current, result.get());
    // ... advance turn, emit events
}

private void applyMove(Player player, Player.MoveResult move) {
    switch (move.type()) {
        case LEAD -> {
            player.getHand().remove(move.card());
            pot.add(move.card());
            listener.onEvent(new GameEvent.CardPlayed(player, move.card()));
        }
        case DEFEND -> {
            player.getHand().remove(move.card());
            pot.add(move.card());
            listener.onEvent(new GameEvent.CardBeaten(player, pot.get(pot.size()-2), move.card()));
        }
        case TAKE_POT -> {
            int taken = pot.size();
            player.getHand().addAll(pot);
            pot.clear();
            listener.onEvent(new GameEvent.PotTaken(player, move.card(), taken));
        }
    }
}
```

**Сложность:** Средняя (нужно реорганизовать GameDriver + Player)
**Выигрыш:** Player становится чистым domain-объектом, тестирование упрощается

---

### P1: Иммутабельный доступ к hand

**Цель:** Защитить руку игрока от внешней мутации.

```java
// Player.java
public List<Card> getUnmodifiableHand() {
    return Collections.unmodifiableList(hand);
}

// Внешний код использует getUnmodifiableHand()
// Внутренний код (Player, Game) использует getHand() или addCard()/removeCard()
```

**Сложность:** Низкая
**Выигрыш:** Защита от багов, чёткая семантика

---

### P2: GameState record в core

**Цель:** Иметь иммутабельный снимок состояния игры в ядре (для save/load/replay).

```java
// core/src/main/java/ru/anseranser/model/GameState.java (новый)
public record GameState(
    Map<Card.Suit, List<Card>> scoreboard,
    Map<Player, List<Card>> hands,
    Player dealer,
    List<Card> pot,
    int roundsPlayed
) {
    public static GameState capture(Game game) { ... }
}
```

Desktop-версия `GameSnapshot` расширяет его UI-specific данными (лог, etc.).

**Сложность:** Средняя
**Выигрыш:** Поддержка save/load, replay, сетевого мультиплеера

---

### P3: Убрать хардкод SPADES в HumanPlayer

**Цель:** HumanPlayer не должен знать о конкретной масти по умолчанию.

```java
// HumanPlayer.java
public HumanPlayer(Card.Suit trump) {
    super(trump);
}
// Удалить конструктор без параметров или сделать его deprecated
```

**Сложность:** Низкая
**Выигрыш:** Гибкость при изменении правил

---

### P4: Вынести GameDriver в отдельный класс

**Цель:** Уменьшить `Game.java` с 417 до ~340 строк, улучшить читаемость.

```java
// core/src/main/java/ru/anseranser/model/GameDriver.java (новый)
public class GameDriver {
    private final Game game;
    private final TurnOrder players;
    private final GameListener listener;
    // ... вся логика step/finishRound
}
```

**Сложность:** Средняя
**Выигрыш:** Чистота, читаемость, тестируемость

---

## 7. Итоговая оценка

| Критерий | Оценка (1-10) | Комментарий |
|----------|---------------|-------------|
| Чистота доменной модели | 7/10 | Card идеален, Player содержит seam-зависимость |
| Разделение слоёв | 8/10 | Хорошее разделение, есть нарушения в Player |
| Связность | 7/10 | Есть双向依赖 Player↔GameListener |
| Тестируемость | 9/10 | 36 тестов + simulator, deterministic |
| Готовность к UI-портам | 8/10 | GameDriver, GameEvent, InputProvider готовы |
| Иммутабельность | 6/10 | Card immutable, hand/pot мутабельны |
| i18n | 9/10 | Messages + CardLocalizer, 3 стиля |
| Документированность | 7/10 | Хорошие Javadoc, нет правил игры |

**Общая оценка: 7.5/10** — хорошо структурированный core, готовый к UI-портам с минимальными доработками.

---

## 8. Рекомендуемый порядок рефакторинга

| # | Задача | Приоритет | Сложность | Время |
|---|--------|-----------|-----------|-------|
| P0 | Вынести генерацию событий из Player | 🔴 | Средняя | 3-4 ч |
| P1 | Иммутабельный доступ к hand | 🔴 | Низкая | 1 ч |
| P2 | GameState record в core | 🟡 | Средняя | 2-3 ч |
| P3 | Убрать хардкод SPADES | 🟡 | Низкая | 0.5 ч |
| P4 | Вынести GameDriver | 🟢 | Средняя | 1-2 ч |
| | **Итого** | | | **7.5-10.5 ч** |

**Рекомендация:** Начать с P0 + P1 — максимальный эффект для готовности к UI-портам. P2-P4 опциональны и зависят от планов на мультиплеер/save-load.
