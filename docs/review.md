# Код-ревью проекта `trumpcards`

Документ подготовлен в рамках **Задания 1** ТЗ `trumpcards_review_and_refactor.md`.

Объём: весь репозиторий; модуль `core` разобран на полную глубину (реализация, а не только
сигнатуры), UI-порты (`console`, `desktop-libgdx`, `android`) проверены по одному критерию —
не дублируют ли они игровую логику, которая обязана жить только в `core`.
`rules.md` использован как source of truth по правилам; расхождения зафиксированы как находки,
а не исправлены «по ходу».

Базовая линия на момент ревью: `./gradlew :core:test` — зелёный (45 c).

---

## 1. Карта архитектуры

### 1.1. Модули и зависимости

| Модуль | Тип | Зависимости | Назначение |
|---|---|---|---|
| `core` | `java-library` | `gson`, `lombok` | Доменный движок: правила, состояние, события, i18n, абстракция ввода. Чистая Java 21. |
| `console` | `application` | `:core` | Тонкий composition root: `System.in/out` + запуск. |
| `desktop-libgdx` | `application` | `:core`, libGDX | Рендер и mouse-ввод, движок на отдельном потоке. |
| `android` | android app | `:core`, Compose/Room/Koin | Compose-UI + async-режим `GameDriver`. |

Пакеты `core`:

- `ru.anseranser.model` — домен: `Game`, `Game.GameDriver`, `Player`, `Dealer`, `TurnOrder`,
  `Scoreboard`, `Card`, `DeckSize`, `DecisionStrategy`, `AiDecisionStrategy`, `Rng`, `SplitMix64`,
  `GameState`, `SavedGame`, `GameSimulator`.
- `ru.anseranser.event` — контракт вывода: `GameEvent` (sealed), `GameListener`, `NopListener`,
  `ConsoleGameListener`.
- `ru.anseranser.input` — абстракция ввода: `InputProvider`, `HumanDecisionStrategy`,
  `ConsoleInputProvider`, `NoopInputProvider`.
- `ru.anseranser.i18n` — локализация: `Messages`, `CardLocalizer`.

### 1.2. Кто на кого ссылается

- `core` **не ссылается** ни на один платформенный модуль, ни на libGDX/Android/Compose. Единственные
  внешние зависимости — Gson (только `SavedGame`) и Lombok (только генерация).
  Домен чистый.
- `console` зависит только от `core` и использует исключительно публичные seams:
  `Game(List<Player>, DeckSize)`, `DecisionStrategy` (`HumanDecisionStrategy`/`AiDecisionStrategy`),
  `ConsoleGameListener`, `ConsoleInputProvider`.
- `desktop-libgdx` зависит от `core` и читает его через `GameDriver`, `GameState` (через
  `GameSnapshot`), `GameEvent`, `DecisionStrategy`. Прямого доступа к внутренним коллекциям нет.
- `android` зависит от `core`; читает `GameDriver`, `GameState`, `GameEvent`. Собственный пакет
  `trumpcards.domain` (`GameState`, `GameReducer`, `GameIntent`, `GameEffect`) — это **UI-состояние
  экрана (MVI), а не игровые правила**; он не дублирует `core` и не содержит правил боя/раздачи/табло.

### 1.3. Где домен протекает наружу

Проверенные точки:

- **Не протекает:** игровые правила реализованы только в `core`. Ни `console`, ни
  `desktop-libgdx`, ни `android` не определяют, что бьёт кого, кто забирает банк, как идёт
  обязательная передача или выбывание. Все три порта — рендер + ввод + запуск.
- **Контр-пример, который проверялся:** `android/.../domain/GameState.kt` и `GameReducer.kt`
  выглядят как «параллельный домен», но `gameReducer` оперирует только фазами экрана
  (`MENU/PLAYING/GAME_OVER`), а `domain.GameState` — это immutable UI-модель, наполняемая из
  `core.GameState` в `GameEngineBridge.captureState`. Правил там нет.
- **Мелкое протекание (Желательно):** `desktop-libgdx/GameSnapshot.opponents()` и `humanHand()`
  знают, что человек — это `Card.Suit.SPADES` (хардкод), тогда как `core` сознательно не
  фиксирует «кто человек» (R6, composition-root concern). Аналогично `GameScreen` печатает
  `Card.Suit.SPADES.name()` как «Your trump». Это не игровое правило, но доменный факт
  (человеческий слот) протёк в UI как константа.
- **Инкапсуляция домена:** `core` отдаёт наружу `GameState`-snapshot (R2/R5), `getPot()` возвращает
  копию, `Player.getHand()` — unmodifiable view, `Scoreboard.snapshot()` — копии. Единственное
  исключение — `Dealer.deck()` (см. находку **C4**).

### 1.4. Контракт потокобезопасности (R7)

`Game` задокументирован как не потокобезопасный: мутировать только с «engine»-потока; снапшоты
снимать на engine-потоке и передавать в UI.

- `desktop-libgdx`: движок на отдельном потоке (`DesktopLauncher.gameThread`); `DesktopGameListener`
  снимает `GameSnapshot` на engine-потоке и публикует в render-поток через `Gdx.app.postRunnable`.
  **Контракт не нарушен.**
- `android`: `GameEngineBridge` держит собственный `game-loop`, `captureState` вызывается на нём,
  а в Compose состояние уходит через `StateFlow`. **Контракт не нарушен.**
- Мелкое замечание: `DesktopInputProvider.validChoices` пишется с engine-потока и читается с
  UI-потока; поле `volatile`, значение — неизменяемый `List.copyOf`, поэтому гонки данных нет.

**Вывод по UI-портам:** дублирования игровой логики не найдено; нарушений R7 не найдено.
Найденные мелочи (хардкод SPADES в desktop) — косметические.

---

## 2. Реестр находок

Легенда серьёзности: **Критично** / **Важно** / **Желательно**.

### C1 — Правило «что делать сейчас» продублировано: `GameDriver.humanTurn()` vs `Player.makeMove()`

- **Где:** `core/model/Game.java` → `Game.GameDriver.humanTurn()` (async-путь) против
  `core/model/Player.java` → `makeMove()`.
- **Суть:** обычный путь `GameDriver.step()` вызывает `current.makeMove(pot)`, внутри которого
  единообразно через `DecisionStrategy` выбирается действие (банк пуст → веду / есть чем отбиться →
  отбиваю / нечем → забираю банк). Но при `asyncInput == true` `step()` уходит в `humanTurn()`,
  который **заново** реализует то же ветвление, напрямую вызывая
  `current.playLeadCard(...)`, `current.takePot(...)`, `current.beatCard(...)` и не трогая
  `DecisionStrategy`.
- **Категория:** дублирование логики, скрытая связность, риск для корректности правил.
- **Серьёзность:** **Критично.**
- **Оценка:** M.
- **Статус:** ✅ исправлено — правило вынесено в `Player.nextActionKind(pot, freshLead)`
  (`LEAD`/`DEFEND`/`TAKE_POT`) и применено к `Player.makeMove()` и к `GameDriver.humanTurn()`.
  Драйвер больше не решает, какое действие легально: он только превращает решение в паузу
  (`awaitingHuman`) или в вызов `applyLead/applyDefense/applyTakePot`. Эквивалентность путей
  закреплена тестом `determinism/SyncAsyncEquivalenceTest`.

### C2 — «Мёртвый» раскомментированный блок внутри незакрытого javadoc в `Game.java`

- **Где:** `core/model/Game.java`, сразу после `setupRound()` (~строки 217–231).
- **Суть:** открывается `/**`, затем без сигнатуры метода идёт тело `for (Player p : players) {
  if (p.isGamer() && !p.getHand().isEmpty()) return p; } return null;`, и всё это закрывается
  `*/` в javadoc следующего метода `mostCardsRounder()`. Компилируется (потому что фактически это
  один комментарий), но выглядит как повреждённый исходник и провоцирует ошибки при следующей
  правке. Это, по-видимому, остаток третьей копии `determineLoser()`/`mostCardsRounder()`.
- **Категория:** нечитаемость, мёртвый код, риск при рефакторинге.
- **Серьёзность:** **Критично** (для дальнейших правок), хотя на поведение не влияет.
- **Оценка:** S.
- **Статус:** ✅ исправлено — блок удалён.

### C3 — `endRound()` выбирает карту на табло не по `nextRequiredRank`, а по минимуму в банке+руке

- **Где:** `core/model/Game.java` → `endRound(Player loser)`.
- **Суть:** по `rules.md` §6 и `docs/game_state_machine.yaml` (`ROUND_END`) карта, уходящая на табло
  проигравшего, — это «следующая по старшинству для его табло», т.е. результат
  `Scoreboard.nextRequiredRank(trump)`. Код же берёт **минимальный по достоинству козырь** из
  `pile` (рука проигравшего + банк). Обычно эти величины совпадают (все младшие козыри уже на
  табло), но это опирается на неявный инвариант, нигде не задокументированный и не проверяемый
  напрямую; при 52-карточной колоде или изменённом наборе рангов соответствие не гарантировано.
- **Категория:** риск для корректности правил, скрытая связность, неявное допущение.
- **Серьёзность:** **Критично** (потенциальное расхождение с `rules.md` — требует отдельного
  согласования, **молча не исправляется**, см. §4).
- **Оценка:** M.

### C4 — `Dealer.deck()` отдаёт «живую» мутируемую коллекцию (нарушение R5)

- **Где:** `core/model/Dealer.java` → `deck()` (`return deck;`).
- **Суть:** рядом в проекте заявлен принцип неизменяемых snapshot'ов (`Player.getHand()`,
  `Scoreboard.snapshot()`, `GameState`). `Dealer.deck()` — единственное исключение: внешний код
  может и читать, и мутировать внутреннюю колоду (`Game.endRound()` действительно делает
  `dealer.deck().addAll(pile)`, `Game.restore()` — `clear()/addAll()`).
- **Категория:** утечка инкапсуляции.
- **Серьёзность:** **Критично** (для Задания 2 интерфейс раздачи обязан это закрыть).
- **Оценка:** S.

### I1 — Четыре почти идентичных прохода по `players` + пятая мёртвая копия

- **Где:** `core/model/Game.java`: `countActiveGamers`, `countActiveGamersWithCards`,
  `mostCardsRounder`, `determineLoser` (+ мёртвая копия из C2).
- **Суть:** `mostCardsRounder` и `determineLoser` отличаются только критерием отбора; счётчики —
  тоже обходы того же множества. Унифицируются одним параметризованным обходом `TurnOrder`.
- **Категория:** дублирование логики.
- **Серьёзность:** **Важно.**
- **Оценка:** S.
- **Статус:** ✅ исправлено — все четыре запроса выражены через один приватный обход
  `Game.playersWhere(Predicate)` + производный `activeGamersWithCards()`; поведение сохранено
  (тай-брейк `mostCardsRounder` по-прежнему берёт первого по кругу, см. `rules.md` §8).

### I2 — Дублирование `playerByTrump`

- **Где:** `Game.GameDriver.playerByTrump(String)` (метод экземпляра) и `Game.playerByTrump(Game, String)`
  (статический).
- **Суть:** идентичная логика в двух местах.
- **Категория:** дублирование, скрытая связность.
- **Серьёзность:** **Важно.**
- **Оценка:** S.
- **Статус:** ✅ исправлено — удалён метод экземпляра `GameDriver.playerByTrump(String)`;
  `GameDriver.restore` использует единственный статический `Game.playerByTrump(Game, String)`.

### I3 — Сериализация размазана по трём местам

- **Где:** `GameDriver.save()`, `Game.restore(...)`, ручная пересборка `Player`/`Scoreboard`/`pot`/
  `deck`/`rng`/счётчиков.
- **Суть:** один компонент (`SavedGameMapper`) был бы единой точкой «состояние ⇄ портируемая форма».
- **Категория:** связность/читаемость.
- **Серьёзность:** **Важно.**
- **Оценка:** M.
- **Статус:** ✅ исправлено — добавлен `core/model/SavedGameMapper`: версия формата, раскладка
  полей дефолт `DeckSize`, пересборка `Player`/`Scoreboard`/`pot`/`deck`/`rng`/счётчиков и
  resolution места по масти живут только там. `GameDriver.save()` и `Game.restore(...)` — тонкие
  делегаты. Flow-состояние драйвера передаётся через его же
  `toDriverState()`/`applyDriverState`, так что приватные поля остаются приватными.

### I4 — `Game` — god-class (739 строк), `GameDriver` — non-static inner class

- **Где:** `core/model/Game.java`.
- **Суть:** `Game` совмещает оркестрацию раунда, snapshot'ы событий, сериализацию и пошаговый
  движок (вложенный класс). `GameDriver` напрямую читает приватные поля `Game.this`
  (`pot`, `dealer`, `scoreboard`, `rng`, `players`, `dealerSeat`, `roundsPlayed`, `cappedRounds`),
  что создаёт неявную сильную связность без явного контракта.
- **Категория:** связность, нечитаемость.
- **Серьёзность:** **Важно.**
- **Оценка:** L.
- **Статус:** частично — I4a ✅ (`finishRound()` разложен на `resolveLoser()`/`advanceDealer()`/`endGame()`,
  порядок вызовов не менялся); остаются I4b (`RoundResolver`, опционально) и I4c
  (связность `GameDriver`). План — `docs/refactoring-plan.md`.

### I5 — `ownerBySuit`/`snapshotHands` на `java.util.HashMap` (аудит недетерминизма)

- **Где:** `Game.distributeObligatoryCards()` (`ownerBySuit`, строки ~143), `Game.snapshotHands()` (~360).
- **Суть:** порядок обхода `HashMap` не детерминирован. Проверено: в `distributeObligatoryCards`
  внешний цикл идёт по `players` (`TurnOrder` = список, стабилен), а `ownerBySuit` используется
  только через `get(suit)`; порядок `HashMap` на результат не влияет. `snapshotHands` кладёт данные
  в map, которая идёт только в событие для рендера. **Вердикт: детерминизм не нарушен**, но
  `EnumMap` был бы однозначнее и дешевле. Финальное подтверждение — в `docs/determinism-audit.md`
  (Задание 3).
- **Категория:** аудит недетерминизма / читаемость.
- **Серьёзность:** **Важно.**
- **Оценка:** S.
- **Статус:** ✅ исправлено — `ownerBySuit` и `snapshotHands` переведены на `EnumMap`
  (`new EnumMap<>(Card.Suit.class)`), поведение не изменилось.

### I6 — Неиспользуемая зависимость `guava` в `core`

- **Где:** `core/build.gradle.kts` → `implementation(libs.guava)`.
- **Суть:** во всём `core` нет ни одного `import com.google.common.*`.
- **Категория:** чистота зависимостей.
- **Серьёзность:** **Важно** (раздувает classpath, скрывает реальные зависимости).
- **Оценка:** S.
- **Статус:** ✅ исправлено — `implementation(libs.guava)` и обе записи `guava` из
  `gradle/libs.versions.toml` удалены.

### I7 — `Game.allCards()` не включает банк

- **Где:** `core/model/Game.java` → `allCards()`.
- **Суть:** метод обещает «все карты в игре» (руки + табло + колода дилера), но пропускает `pot`.
  Сейчас вызывается в конце партии, когда банк пуст, поэтому тесты зелёные; но семантика вводит в
  заблуждение (`GameSerializationTest` вынужден вручную складывать `allCards().size() + getPot().size()`).
- **Категория:** неоднозначность/инкапсуляция.
- **Серьёзность:** **Важно.**
- **Оценка:** S.
- **Статус:** ✅ исправлено — `allCards()` теперь включает `pot`; `GameSerializationTest` больше не
  добавляет банк вручную, и его проверка сохранения в середине розыгрыша (непустой банк) стала
  прямым тестом на это.

### I8 — Пробелы в тестовом покрытии

- **Где:** `core/src/test`.
- **Суть:** детерминизм проверяется только по сиду ГПСЧ и только по итоговому победителю; нет
  возможности задать раскладку напрямую; нет golden-сценариев на конкретные правила; нет
  воспроизводимости по раундам/событиям; `GameSimulator` проверяет только финальные инварианты.
  `humanTurn()` (async) тестируется smoke-тестом на завершаемость, но не на совпадение с
  `makeMove()`-путём по конкретным раскладкам.
- **Категория:** пробелы в покрытии.
- **Серьёзность:** **Важно** (это предмет Заданий 2–3).
- **Оценка:** L.

### I9 — Sync- и async-пути хода расходятся по поведению

- **Где:** `GameDriver.step()` (sync человек через `Player.makeMove`) vs `humanTurn()` (async).
- **Суть:** desktop использует sync-путь (блокирующий `DesktopInputProvider`), android — async.
  Два разных кодовых пути для одного правила → расхождение поведения не покрыто тестами.
  Следствие находки C1.
- **Категория:** дублирование/скрытая связность.
- **Серьёзность:** **Важно.**
- **Оценка:** (входит в C1).
- **Статус:** ✅ исправлено вместе с C1; пути сведены к `Player.nextActionKind`, а их
  эквивалентность по событиям проверяется `determinism/SyncAsyncEquivalenceTest`.

### D1 — Битые javadoc-ссылки `{@link #playGame(Random)}`

- **Где:** `Game.playGame(Rng)` и javadoc `Game.GameDriver` — ссылаются на несуществующий
  `playGame(Random)`.
- **Категория:** документация.
- **Серьёзность:** **Желательно.**
- **Оценка:** S.
- **Статус:** ✅ исправлено — ссылка `{@link #playGame(Random)}` в javadoc `GameDriver`
  заменена на `{@link #playGame(Rng)}`; в `GameDriverTest` упоминание `playGame(Random)`
  тоже приведено к `playGame(Rng)`.

### D2 — Хардкод человеческого слота `SPADES` в desktop

- **Где:** `desktop-libgdx/GameScreen.java`, `GameSnapshot.java`.
- **Суть:** человек определяется как `Card.Suit.SPADES`, что дублирует composition-root выбор и
  противоречит решению R6 («кто человек — решает composition root»).
- **Категория:** протекание доменного факта в UI.
- **Серьёзность:** **Желательно.**
- **Оценка:** S.

### D3 — Порядок потребления RNG как часть контракта детерминизма не задокументирован

- **Где:** `Game.chooseFirstDealer()` (`rng.nextInt`), `Game.shuffleAndDeal`, `Dealer.shuffle`.
- **Суть:** итог зависит от **порядка** вызовов RNG (выбор дилера → шаффл). Любая вставка лишнего
  `nextInt` изменит все игры по сиду. Полезно зафиксировать в `docs/determinism-audit.md`.
- **Категория:** документация/скрытая связность.
- **Серьёзность:** **Желательно.**
- **Оценка:** S.
- **Статус:** ✅ исправлено — порядок потребления RNG зафиксирован в
  `docs/determinism-audit.md` §3 (один `nextInt` на выбор дилера, затем `size-1` вызовов
  в `Dealer.shuffle`; `deal` RNG не использует).

### D4 — `SplitMix64()` по умолчанию сеется `System.nanoTime()`

- **Где:** `core/model/SplitMix64.java` → `SEED_COUNTER = new AtomicLong(System.nanoTime())`.
- **Суть:** единственный источник платформенной недетерминированности в `core`. Это ожидаемое
  поведение для неинициализированной игры, но должно быть явно зафиксировано в аудите; тесты
  обязаны инжектировать `Rng`/раскладку.
- **Категория:** аудит недетерминизма.
- **Серьёзность:** **Желательно.**
- **Оценка:** S.
- **Статус:** ✅ исправлено — зафиксировано в `docs/determinism-audit.md` §3.1
  (дефолтное зерно из `System.nanoTime()` + контракт: детерминизм только при явной инъекции
  `Rng`; в тестах — `FixedRng`/`ScriptedCardDealer`).

---

## 3. Является ли `DecisionStrategy` единственной точкой выбора хода?

**Короткий ответ: нет.**

- **Sync-путь (asyncInput = false):** да. `GameDriver.step()` → `Player.makeMove(pot)` →
  `strategy.chooseLeadCard(...)` / `strategy.chooseDefenseCard(...)`. Здесь `DecisionStrategy` —
  действительно единственная точка принятия решения, единая и для человека, и для бота.
- **Async-путь (asyncInput = true):** нет. `GameDriver.humanTurn()` **не вызывает**
  `Player.makeMove()` и, соответственно, `DecisionStrategy`. Он самостоятельно решает, какое
  действие сейчас легально (`pot.isEmpty()` → LEAD; иначе есть ли `validDefenses` → DEFEND;
  иначе автозабор банка), и напрямую вызывает `playLeadCard/beatCard/takePot`.

То есть `GameDriver`/`Game` **ветвятся по `asyncInput`** и в async-ветке параллельно реализуют
часть игровой логики отдельно от `Player.makeMove()`. `isHuman()` используется для эмита
`AwaitingHumanInput` и для выбора ветки, но не для выбора самого действия. Это ровно тот риск,
о котором предупреждает ТЗ: правка правила в одном месте не отразится в другом.

**Целевое состояние (Задание 4) — ✅ достигнуто:** `Player.makeMove()` разложен на
`nextActionKind(pot, freshLead)` → `LEAD/DEFEND/TAKE_POT` и `applyLead/applyDefense/applyTakePot`,
и оба пути (`makeMove()` и `GameDriver.humanTurn()`) пользуются ими. `GameDriver` теперь отвечает
только за паузу/возобновление и подстановку карты из `resume(Card)`, но не решает, какое действие
легально — это проверяется в одном месте в `Player`.

---

## 4. Расхождения с `rules.md` (не исправляются молча)

| # | Правило (`rules.md`) | Код | Статус |
|---|---|---|---|
| 1 | §6: на табло проигравшего кладётся карта «следующая по старшинству для его табло» | `endRound()` берёт минимальный козырь из банка+руки (C3) | Совпадает при текущих инвариантах, но зависит от неявного допущения. **Требует отдельного согласования**, если решим менять. |
| 2 | §8: при лимите 10000 ходов проигрывает игрок с наибольшим числом карт; при равенстве — случайно | `mostCardsRounder()` берёт максимум, **при равенстве первый по кругу**, без случайного выбора | Мягкое расхождение: тай-брейк детерминированный, а не случайный. Зафиксировано как находка, поведение не менялось. |
| 3 | §2.3: обмен без цепочки, тузы не передаются, передаются все совпадения | `distributeObligatoryCards()` — соответствует | Рахождений нет. |
| 4 | §5: раунд кончается, когда остаётся один игрок с картами | `advanceAfterTurn` + `finishRound` — соответствует | Расхождений нет. |

---

## 5. Приоритизированный бэклог (основа Задания 4)

### Критично

| # | Находка | Действие | Оценка |
|---|---|---|---|
| 1 | **C1/I9** — дублирование правила хода в `makeMove()` vs `humanTurn()` | ✅ унифицировано через `nextActionKind()` + `apply*()`; `GameDriver` — только пауза/resume | M |
| 2 | **C2** — мёртвый расскомментированный блок в `Game.java` | ✅ удалён | S |
| 3 | **C3** — `endRound()` vs `nextRequiredRank` | Задокументировать/согласовать; при желании заменить на `nextRequiredRank` под защитой golden-тестов | M |
| 4 | **C4** — `Dealer.deck()` живой список | ✅ закрыто интерфейсом раздачи (Задание 2) | S |

### Важно

| # | Находка | Действие | Оценка |
|---|---|---|---|
| 5 | **I1** — 4 похожих обхода игроков | ✅ сведены к одному параметризованному обходу (`Game.playersWhere`) | S |
| 6 | **I2** — двойной `playerByTrump` | ✅ оставлен один статический метод | S |
| 7 | **I3** — сериализация в 3 местах | ✅ сведена в `SavedGameMapper` | M |
| 8 | **I4** — god-class `Game` / inner `GameDriver` | I4a ✅ (`finishRound()` разложен); остаются `RoundResolver` (I4b) и связность `GameDriver` (I4c) | L |
| 9 | **I5** — `HashMap` в `distributeObligatoryCards`/`snapshotHands` | ✅ `EnumMap` | S |
| 10 | **I6** — неиспользуемая `guava` | ✅ удалена из `core` и version catalog | S |
| 11 | **I7** — `allCards()` без банка | ✅ `pot` включён в `allCards()` | S |
| 12 | **I8** — пробелы в покрытии | Задания 2–3: `ScriptedCardDealer`, golden-тесты, round-level симулятор | L |
| 13 | **UI** — sync/async пути не покрыты совместно | ✅ `determinism/SyncAsyncEquivalenceTest` (сравнение потоков событий) | M |

### Желательно

| # | Находка | Действие | Оценка |
|---|---|---|---|
| 14 | **D1** — битые javadoc-ссылки | ✅ приведены к `playGame(Rng)` | S |
| 15 | **D2** — хардкод `SPADES` в desktop | Вынести определение human-слота в composition root | S |
| 16 | **D3** — порядок RNG не задокументирован | ✅ зафиксирован в `docs/determinism-audit.md` §3 | S |
| 17 | **D4** — `SplitMix64()` на `nanoTime` | ✅ зафиксирован в аудите §3.1 (единственный платформенный источник) | S |

---

## 6. Что подтверждено как «уже хорошо» (не откатывать)

- `Rng`/`SplitMix64` — сериализуемое состояние, инжектируется; нет `java.util.Random`.
- `DecisionStrategy` + `AiDecisionStrategy`/`HumanDecisionStrategy` — верная абстракция, домен не
  знает про ввод (R6).
- `GameDriver` — единая пошаговая машина для `playGame()` и UI; sync/async в одном типе.
- R5/R2 snapshots: `Player.getHand()`, `Scoreboard.snapshot()`, `Game.getPot()`, `GameState`.
- R7-контракт потокобезопасности задокументирован и не нарушен в UI-портах.
- `TurnOrder` — детерминированный обход вместо прежнего `ThreadLocalRandom`-anchor.
- `DeckSize` — единый источник рангов/базового ранга для 36 и 52 карт.
- `docs/game_state_machine.yaml` как формальная модель — соответствует коду, кроме тай-брейка §8.

---

## 7. Заключение

Игровая логика изолирована в `core`; UI-порты правил не дублируют; контракт R7 не нарушен.
Главные риски — **дублирование правила хода (C1/I9)** и **повреждённый участок `Game.java` (C2)**,
которые делают текущий код хрупким для следующих правок; плюс **неявное допущение в `endRound()` (C3)**
и **живая колода `Dealer.deck()` (C4)**. Бэклог из §5 лёг в основу `docs/refactoring-plan.md`
(Задание 4); из него уже закрыты C1/C2/C4/I1/I2/I3/I6/I7/I8/I9/D1/D3/D4, а оставшийся объём
(I4, I5, D2) и вынесенные за рамки C3 и `rules.md` §8 разобраны в плане.
