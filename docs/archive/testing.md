# Тестирование `core`

Практическое руководство: как устроены тесты домена, как добавить новый
golden-сценарий, какие инварианты обязаны оставаться зелёными в любом PR, и как
трактовать «капнутые» партии.

Запуск всего домена:

```bash
./gradlew :core:test
```

Один класс:

```bash
./gradlew :core:test --tests 'ru.anseranser.model.determinism.GoldenScenarioTest'
```

---

## 1. Слои тестов

| Слой | Пакет / классы | Назначение |
|---|---|---|
| Unit | `ru.anseranser.model.*` (`DealerTest`, `ScoreboardTest`, `PlayerMoveTest`, `CardLocalizerTest`, `MessagesTest`) | Изолированные классы домена; `PlayerMoveTest` пинит единый `Player.nextActionKind` (C1). |
| Driver | `GameDriverTest`, `GameDriverAsyncTest`, `GameSerializationTest`, `GameStateTest`, `HumanPlayerStage1Test` | Пошаговый движок, async-ввод, save/restore, snapshots. |
| Эквивалентность путей | `determinism.SyncAsyncEquivalenceTest` | Sync (блокирующий) и async (`resume`) пути дают одинаковый поток игровых событий (C1/I9). |
| Golden / сценарии | `ru.anseranser.model.determinism.GoldenScenarioTest` | Конкретные правила на жёстко заданных раскладках. |
| Воспроизводимость | `determinism.ReplayDeterminismTest`, `determinism.RoundLevelSimulatorTest` | Один seed → один и тот же поток событий/путь партии. |
| Инвариант табло | `ru.anseranser.model.ScoreboardLadderTest` | Карта, уходящая на табло, всегда равна `Scoreboard.nextRequiredRank` (правило `rules.md` §6 / находка C3) — 300 сидов × обе колоды. |
| Симулятор | `GameSimulator`, `GameSimulatorTest`, `determinism.RoundLevelSimulatorTest` | Массовые seed'ы, инварианты карт, round-level replay. |

Ключевые инструменты детерминизма (в `determinism`):

- `ScriptedCardDealer` — раскладка `Map<Card.Suit, List<Card>>` без RNG, с билдером.
- `FixedRng` — предопределённый `Rng` (например, `0` → первый дилер = место 0).
- `LeadCardStrategy` — задаёт, какой картой ходит конкретный игрок.
- `EventLog` — канонизирует поток событий для сравнения (сортировка по мастям).

---

## 2. Как добавить новый golden-сценарий

1. **Сформулируй проверяемое правило** по `rules.md` (например: «игрок без
   защиты забирает банк»). Один сценарий — одно правило.

2. **Задай раскладку через `ScriptedCardDealer.builder(...)`.** Указывай только
   те карты, что важны для правила, остальное дораздаётся детерминированно:

   ```java
   Map<Card.Suit, List<Card>> layout = ScriptedCardDealer.builder(DeckSize.THIRTY_SIX)
           .give(Card.Suit.SPADES, new Card(Card.Suit.CLUBS, Card.Rank.EIGHT))
           .give(Card.Suit.CLUBS, new Card(Card.Suit.CLUBS, Card.Rank.SEVEN))
           .fillOrder(Card.Suit.SPADES, Card.Suit.DIAMONDS, Card.Suit.HEARTS) // CLUBS не получает заливку
           .build();
   ```

   Билдер сам валидирует: дубликаты, карты табло, ранги чужой колоды, суммарный
   размер (`deckSize − 4`).

3. **Убери оставшуюся случайность:** `game.setCardDealer(new ScriptedCardDealer(...))`
   и `game.setRng(new FixedRng(0))` (детерминированный первый дилер).

4. **Прокрути нужную часть игры** через `Game.GameDriver` (или `shuffleAndDeal()` +
   `distributeObligatoryCards()` для сцен без ходов) и собери события в список.

5. **Зафиксируй ожидаемый результат** по правилу, а не по факту: проигравший,
   вытолкнутая карта и её ранг, флаг выбывания, кто забрал банк и его размер.

6. Прогони `./gradlew :core:test` и убедись, что сценарий зелёный и быстрый
   (без `Thread.sleep`, без сети/ФС).

Шаблон нового сценария — см. `GoldenScenarioTest` (5 готовых примеров:
обязательная передача, взятие банка, отбился-и-ведёт-сам, выталкивание SEVEN,
выбывание по ACE + замороженный победитель полной скриптованной партии).

---

## 3. Обязательные инварианты (не ломать в PR)

1. **Сохранение карт.** В любой момент: сумма карт в руках + банке + табло + пул
   дилера = размер колоды; все карты уникальны (`GameSimulatorTest`,
   `GameSerializationTest`).
2. **Единственный победитель.** В конце партии ровно один `gamer`
   (`GameSimulatorTest`, `GameDriverAsyncTest`).
3. **Детерминизм по seed.** Один и тот же `SplitMix64(seed)` дважды даёт один и
   тот же поток событий (`ReplayDeterminismTest`) и один и тот же путь по раундам
   (`RoundLevelSimulatorTest`).
4. **Детерминизм по раскладке.** `ScriptedCardDealer` не зависит от RNG
   (`ScriptedCardDealerTest`), golden-сценарии воспроизводимы.
5. **Терминация.** Любая партия завершается, даже патологическая (кап
   `MAX_ROUND_MOVES`).
6. **Правила `rules.md`.** Обязательная передача, бой, взятие банка, выталкивание
   следующего козыря, выбывание по ACE — покрыты golden-сценариями.
7. **Непрерывность табло.** На табло всегда лежит непрерывная лестница `base..top`, и
   выталкиваемая карта — именно `Scoreboard.nextRequiredRank(loser.getTrump())`
   (`ScoreboardLadderTest`; §6 `rules.md`, находка C3).
8. **UI-контракты.** `GameState`/snapshot'ы остаются неизменяемыми
   (`GameStateTest`); `Dealer.deck()`/`Player.getHand()` — read-only.

**Любой PR, меняющий игровую логику, обязан оставить все пункты зелёными. Если
поведение меняется намеренно — сначала согласовать и обновить golden-ожидания явно.**

---

## 4. Трактовка «капнутых» (`cappedRounds`) партий

- `MAX_ROUND_MOVES = 10_000` — защитный предел ходов **одного раунда**. Нормальный
  раунд завершается за десятки ходов; кап срабатывает на патологических раздачах
  (например, двое передают друг другу козыри без возможности завершить раунд).
- При срабатывании капа проигравшим становится игрок с наибольшим числом карт
  (`RoundResolver.mostCards(players)`), и партия продолжается.
- `GameSimulator.Result.cappedRounds()` сообщает, сколько раундов игры упёрлись в кап;
  `GameSimulator.countCapped(results)` — сколько **игр** из набора капнули.
- `GameSimulator.RoundSnapshot.moves()` — число ходов конкретного раунда; для
  капнутого раунда `>= 10_000`.

**Как относиться к непустому `cappedRounds`:**

- Это **честный сигнал**, а не падение теста. Зелёный прогон с ненулевым числом
  капнутых игр допустим и ожидаем: движок обязан завершаться.
- Патологические игры редки: из 50 000 seed'ов капнули 4 (12129, 24847, 30649, 49128).
- Капнутая партия всё равно обязана быть **воспроизводимой** (см.
  `ReplayDeterminismTest.pathologicalCappedSeed_isReproducible_andActuallyCapped`).
- Если капнутых игр становится аномально много — это сигнал о регрессии правил/AI,
  стоит разобраться, а не молча принимать.

---

## 5. Быстрые правила гигиены тестов

- Никаких `Thread.sleep`, таймаутов, сети, файлов — тесты должны быть пригодны для CI.
- Используй `ScriptedCardDealer` + `FixedRng` вместо перебора seed'ов там, где нужен
  конкретный сценарий.
- Для сравнения потоков событий используй `EventLog` (канонизация), а не
  `Map.toString()` — порядок enum-ключей в `HashMap` не стабилен между JVM.
- Держи golden-ожидания привязанными к `rules.md`, а не к текущему выводу движка.
