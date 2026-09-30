# План рефакторинга `core` и UI-портов

Документ подготовлен в рамках **Задания 4** ТЗ `trumpcards_review_and_refactor.md`.
Основа — бэклог из `docs/review.md` (§5), аудит `docs/determinism-audit.md` и гайд
`docs/testing.md`.

К этому моменту из бэклога **уже закрыты** C1, C2, C4, I1, I2, I3, I6, I7, I8, I9, D1,
D3, D4 (см. статусы «✅ исправлено» в `docs/review.md`). Здесь описан **оставшийся**
объём: **I4, I5, D2**, плюс явно вынесенные за рамки C3 и `rules.md` §8.

---

## 0. Принципы выполнения

1. **Один шаг = один маленький коммит**, сохраняющий поведение.
2. После каждого коммита **`./gradlew :core:test` зелёный** (сейчас 85 тестов).
   Golden/replay/round-level тесты (`ru.anseranser.model.determinism.*`) — страховочная
   сетка: любое непреднамеренное изменение правил падает именно там.
3. **Правила игры не меняются.** Расхождения с `rules.md` (C3, §8) — только по
   отдельному согласованию, не «по ходу».
4. **Публичное поведение сохраняется** (`playGame`, `GameDriver`, `save`/`restore`,
   поток событий) для всех UI-портов, кроме явно согласованного breaking change.
5. UI-модули после затронувших их шагов проверяются так:
   `./gradlew :console:compileJava :desktop-libgdx:compileJava --offline`
   (и `:desktop-libgdx:test`, `:android:assembleDebug` при правках android).

---

## 1. Что уже сделано (контекст)

| Находка | Что сделано |
|---|---|
| **I1** (4 обхода игроков) | сведены в один общий обход (теперь `RoundResolver.where` + семантические запросы) |
| **C1/I9** (дубль правила хода) | `Player.nextActionKind` + `applyLead/applyDefense/applyTakePot`; `GameDriver` — только пауза/resume |
| **I2** (`playerByTrump`) | оставлен один статический метод |
| **I3** (сериализация) | вынесена в `SavedGameMapper` |
| **I7** (`allCards` без банка) | банк включён |
| **I6** (guava) | зависимость удалена |
| **C2/C4/D1/D3/D4** | мёртвый блок, read-only `deck()`, javadoc, RNG-порядок/зерно |

Поэтому в I4 часть работы (дублирование обходов и `playerByTrump`, дублирование
правила хода) **уже выполнена** — оставшийся объём I4 меньше, чем в первичной оценке.

---

## 2. Остаток бэклога

| # | Находка | Требование ТЗ (Задание 4) | Оценка | Риск |
|---|---|---|---|---|
| I5 | `HashMap` в `distributeObligatoryCards`/`snapshotHands` | — | S | низкий — ✅ выполнено |
| I4a | `finishRound()` смешивает 4 смысла | п.4 | S | низкий — ✅ выполнено |
| I4b | Обходы игроков в отдельном `RoundResolver` | п.1 | S | низкий — ✅ выполнено |
| I4c | `GameDriver` читает приватные поля `Game.this` | п.1 (п.3) | M | средний |
| D2 | Хардкод human-слота `SPADES` в UI | п.5 | M | средний |
| C3 | `endRound()` vs `nextRequiredRank` | — | — | **вне рамок** (решение заказчика) |
| §8 | случайный тай-брейк при лимите ходов | — | — | **вне рамок** (решение заказчика) |

---

## Шаг 1 — I5: `EnumMap` вместо `HashMap` (S, риск низкий) — ✅ выполнено

**Где:** `Game.distributeObligatoryCards()` (`ownerBySuit`), `Game.snapshotHands()`.

**Было:** `new java.util.HashMap<>()` — порядок обхода не определён.
**Стало:** `new java.util.EnumMap<>(Card.Suit.class)` — порядок обхода = порядок
объявления мастей, дешевле и однозначнее.

**Почему безопасно:** аудит §2 уже доказал, что результат **не** зависит от порядка
`HashMap` (внешний цикл — по `TurnOrder`, доступ к `ownerBySuit` только `get(suit)`).
Это улучшение читаемости, а не фикс поведения. `snapshotHands()` дополнительно
используется только в событии `RoundStarted` для рендера.

**Проверка:** golden-тест `obligatoryExchange_movesEverySeventhToItsSuitOwner` +
`GameTest.distributeObligatoryCards_*` + `ReplayDeterminismTest` (поток событий
не меняется: `EventLog` сортирует карты по `Card.Suit`).

**Коммит:** `use EnumMap for suit-keyed game maps`.

---

## Шаг 2 — I4a: разложить `finishRound()` на именованные шаги (S, риск низкий) — ✅ выполнено

**Где:** `Game.GameDriver.finishRound()`.

**Было:** один метод делает всё сразу — выбирает проигравшего (обычный/капнутый
раунд), выталкивает карту на табло, выбывает игрока, передаёт ход дилеру и решает,
начинать новый раунд или завершать партию.

**Стало:** последовательность маленьких методов с одним смыслом:

```java
public void finishRound() {
    if (phase != Phase.ROUND_ENDED) return;
    Player loser = resolveLoser();        // capped → mostCardsRounder(), иначе determineLoser()
    boolean eliminated = endRound(loser);
    if (eliminated) loser.setGamer(false);
    dealerSeat = advanceDealer(loser);    // loser, либо nextDealer(loser) при выбывании
    if (countActiveGamers() > 1) beginRound(); else endGame();
}
```

**Почему безопасно:** чистое извлечение методов без изменения порядка вызовов и
ветвлений; `cappedRounds++` остаётся внутри `resolveLoser()`.

**Проверка:** `GameDriverTest` (фазовый поток), `GoldenScenarioTest`
(`fullScriptedGame_...`, `roundEnd_pushesNextTrump...`), `ReplayDeterminismTest`.

**Коммит:** `split finishRound into named steps`.

**Факт:** `finishRound()` теперь делегирует в `resolveLoser()` (внутри — `cappedRounds++`),
`advanceDealer(loser)` и `endGame()`; `endRound(loser)` уже существовал. Порядок
вызовов и ветвлений не менялся — `:core:test` зелёный (85 тестов).

---

## Шаг 3 — I4b: `RoundResolver` (S, риск низкий, **опционально**) — ✅ выполнено

**Где:** обходы игроков в `Game` (`playersWhere`, `activeGamersWithCards`,
`countActiveGamers`, `countActiveGamersWithCards`, `mostCardsRounder`,
`determineLoser`).

**Контекст:** Задание 4 п.1 просит вынести эти четыре обхода в `RoundResolver`.
После I1 дублирование уже устранено (`playersWhere`), поэтому отдельный класс даёт
теперь только более явную границу ответственности:

```java
final class RoundResolver {                       // package-private
    static List<Player> activeWithCards(TurnOrder players) { ... }
    static int countActive(TurnOrder players) { ... }
    static Player mostCards(TurnOrder players) { ... }
    static Player firstWithCards(TurnOrder players) { ... }
}
```

**Решение:** делать **только если** после шага 2 `Game` всё ещё воспринимается как
god-class; приоритет низкий, можно отложить. Если делаем — `Game` делегирует в
`RoundResolver`, никаких изменений поведения.

**Проверка:** весь `:core:test` (в т.ч. `GameTest`, golden).

**Коммит (если делаем):** `extract round player queries into RoundResolver`.

**Факт:** сделан (выбран статический вариант без состояния). Новый пакетно-приватный
`RoundResolver` содержит единственный общий обход `where(TurnOrder, Predicate)` и
семантические запросы `activeWithCards`, `countActive`, `countActiveWithCards`,
`mostCards`, `firstWithCards`. `Game` больше не держит ни одного из них — вызовы в
`advanceAfterTurn()`, `finishRound()`, `resolveLoser()` идут прямо в `RoundResolver`.
Порядок обхода и тай-брейк по месту сохранены.

---

## Шаг 4 — I4c: ограничить связность `GameDriver` (M, риск средний)

**Где:** `Game.GameDriver` напрямую читает/пишет приватные поля `Game.this`
(`pot`, `dealer`, `scoreboard`, `rng`, `players`, `dealerSeat`, `roundsPlayed`,
`cappedRounds`).

**Варианты (по возрастанию объёма):**

- **A. Точечные пакетные методы доступа (рекомендуется).** Заменить прямые обращения
  на явные package-private методы `Game` (часть уже появилась в «serialization seam»
  из I3): `players()`, `pot()`/`setPot()`, `dealer()`, `scoreboard()`, `rng()`,
  `dealerSeat()`/`setDealerSeat()`, счётчики. Это документирует и **явно ограничивает**
  связность, не меняя тип и публичный API.
- **B. Вынести `GameDriver` в top-level класс.** Даёт настоящий «god-class fix», но
  **ломает публичный тип** `Game.GameDriver`, который используют `console`,
  `desktop-libgdx`, `android` и тесты. Это **согласованный breaking change**: требует
  правок во всех портах и в `GameSerializationTest`/`GameDriver*Test`.

**Рекомендация:** выполнить **A**. Вариант B — только по явному согласованию, отдельным
шагом, с обновлением всех портов.

**Почему риск средний:** правки «механические», но затрагивают горячий путь движка;
golden/replay-тесты обязаны остаться зелёными.

**Проверка:** весь `:core:test`; дополнительно
`:console:compileJava :desktop-libgdx:compileJava` (для B — ещё `:android`).

**Коммит:** `route GameDriver state access through Game accessors` (A).

---

## Шаг 5 — D2: human-слот из composition root, а не константа в UI (M, риск средний)

**Где (хардкод `Card.Suit.SPADES` как «человек»):**

| Модуль | Файл | Что зашито |
|---|---|---|
| core (composition root) | `console/ConsoleLauncher.java` | `suit == SPADES → HumanDecisionStrategy` |
| desktop | `DesktopLauncher.java` | `suit == SPADES → HumanDecisionStrategy` |
| desktop | `GameSnapshot.java` | `opponents()`/`humanHand()` знают, что человек — SPADES |
| desktop | `GameScreen.java` | `"Your trump: " + Card.Suit.SPADES.name()` |
| android | `GameEngineBridge.kt` | `suit == SPADES → HumanDecisionStrategy` |
| android | `presentation/screens/gameover/GameOverScreen.kt` | «You WIN!» по `SPADES` |

**Было:** решение «кто человек» (R6 — composition root concern) продублировано
константой `SPADES` в UI-слое, где оно не должно приниматься.
**Стало:** composition root определяет человеческий слот **один раз** и **передаёт**
его вниз:

```java
// DesktopLauncher (composition root): единственное место выбора
Card.Suit humanSeat = Card.Suit.SPADES;         // дефолт сохраняем
... new Game(playerList(humanSeat, input), deckSize);
GameSnapshot.capture(game, humanSeat, log);      // прокидываем в view
```

`GameSnapshot.capture(Game, Card.Suit humanSeat, List<String> log)` сохраняет слот в
записи; `opponents()`/`humanHand()` и `GameScreen` используют `snap.humanSeat()`
вместо литерала. Аналогично на android: слот хранится в `GameEngineBridge` и пробрасывается
в экран/`GameOverScreen`.

**Почему это не игровая логика:** выбор «кто управляется человеком» остаётся в
composition root (R6 не нарушается), меняется только способ доставки этого факта в
рендер; правила боя/раздачи/табло не затрагиваются.

**Проверка:** `:desktop-libgdx:test`, `:desktop-libgdx:compileJava`,
`:console:compileJava`; для android — `:android:assembleDebug` + ручной smoke.
Дополнительно: grep не находит `SPADES` как «человека» в рендер-слое (остаётся только
в composition root).

**Коммит(ы):** `pass the human seat from the composition root` (по одному на модуль:
desktop, android — console тривиален).

---

## Вне рамок этого плана (требуют решения заказчика)

Эти пункты **сознательно не меняются** и остаются задокументированными, а не
«исправленными молча» (см. `docs/review.md` §4 и `docs/determinism-audit.md` §4.3):

- **C3** — `endRound()` кладёт на табло минимальный козырь из банка+руки, а `rules.md`
  §6 формулирует «следующая по старшинству карта для табло» (`Scoreboard.nextRequiredRank`).
  При текущих инвариантах величины совпадают, но совпадение неявное. Решение: либо
  заменить на `nextRequiredRank` под защитой golden-тестов, либо зафиксировать
  инвариант — **по согласованию**.
- **`rules.md` §8** — при лимите 10000 ходов проигрывает игрок с наибольшим числом карт,
  при равенстве — **случайно**; код выбирает детерминированно (первый по кругу).
  Расхождение в тай-брейке; предлагается либо обновить `rules.md`, либо задокументировать
  детерминированный тай-брейк как уточнение.

Пока решение не принято, оба пункта не трогаем: golden/replay-тесты закрепляют текущее
поведение как эталон.

---

## Порядок коммитов (предлагаемый)

| # | Коммит | Шаг | Оценка |
|---|---|---|---|
| 1 | `use EnumMap for suit-keyed game maps` | I5 | S |
| 2 | `split finishRound into named steps` | I4a | S | ✅ |
| 3 | `route GameDriver state access through Game accessors` | I4c-A | M |
| 4 | `pass the human seat from the composition root` | D2 | M |
| 5 | `extract round player queries into RoundResolver` | I4b | S | ✅ |

Каждый коммит — самостоятельный, поведение-сохраняющий, с зелёным `:core:test`.

---

## Definition of done

- [ ] Каждый шаг — отдельный коммит; `:core:test` зелёный после каждого.
- [ ] Golden/replay/round-level тесты не менялись (или менялись только как явное
      подтверждение согласованного изменения поведения — но не в этих шагах).
- [ ] Публичный API `Game`/`GameDriver`/`SavedGame` не изменился (кроме варианта I4c-B,
      который отдельно согласуется).
- [ ] UI-порты компилируются; хардкод human-слота убран из рендер-слоя (D2).
- [ ] Расхождения с `rules.md` (C3, §8) по-прежнему задокументированы отдельно и не
      «исправлены» молча.
