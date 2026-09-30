# ТЗ: релизная сборка Android (`trumpcards`)

> Статус: **закрыто 30.09.2026.** Зафиксировано 27.09.2026 при прогоне полного build после
> Задания 2 (`trumpcards_review_and_refactor.md`). К игровой логике `core` отношения не имеет.
> Решение — см. §8: Lombok переведён в `compileOnly` в `:core` + точечный `-dontwarn`
> в `android/proguard-rules.pro`; `./gradlew build` (online) и `:android:assembleRelease`
> (online и offline) теперь зелёные.

---

## 1. Контекст

Модули: `core` (чистая Java 21), `console`, `desktop-libgdx`, `android` (Kotlin/Compose + `:core`).
Сборка: Gradle 9.6.1, AGP `8.9.0`, Kotlin `2.2.21`, `compileSdk/targetSdk = 36`, `minSdk = 26`.

Проверка после Задания 2:

```bash
# успешно — тесты всех модулей + debug-сборка
./gradlew :core:test :console:test :desktop-libgdx:test :android:testDebugUnitTest :android:assembleDebug
# BUILD SUCCESSFUL

# падает — полная сборка (release-вариант android)
./gradlew build
# BUILD FAILED
```

Debug-вариант (`assembleDebug`, `packageDebug`, `testDebugUnitTest`) собирается и проходит.
Падает именно **release-конвейер** Android (`minifyReleaseWithR8`, `l8DexDesugarLib*`).

---

## 2. Воспроизведение

### 2.1. Offline-режим

```bash
./gradlew build --offline
```

Первый падающий таск: `:android:l8DexDesugarLibDebug`.

```
Execution failed for task ':android:l8DexDesugarLibDebug'.
> Failed to calculate the value of task ... property 'libConfiguration'.
  > Could not resolve all files for configuration ':android:coreLibraryDesugaring'.
    > Could not download desugar_jdk_libs-2.1.4.jar (com.android.tools:desugar_jdk_libs:2.1.4):
        No cached version available for offline mode
    > Could not download desugar_jdk_libs_configuration-2.1.4.jar:
        No cached version available for offline mode
```

**Наблюдение:** Android-модуль непроверяем в offline-режиме, пока `desugar_jdk_libs:2.1.4`
не попадёт в кэш. Для CI это значит: либо прогрев кэша, либо запрет `--offline` для android-тасков.
(После §8 зависимость скачана в кэш, и `--offline` работает.)

### 2.2. Online-режим

```bash
./gradlew build
```

Падающий таск: `:android:minifyReleaseWithR8` (перед этим `:android:packageDebug` — успешно).

```
Execution failed for task ':android:minifyReleaseWithR8'.
> A failure occurred while executing ... R8Task$R8Runnable
  > Compilation failed to complete

Missing class lombok.javac.handlers.HandleUtilityClass
   (referenced from: java.util.List kotlinx.coroutines.internal.FastServiceLoader.load(java.lang.Class, java.lang.ClassLoader))
Missing class lombok.javac.handlers.HandleVal
   (referenced from: kotlinx.coroutines.internal.FastServiceLoader.load(...))
Missing class lombok.javac.handlers.HandleValue
Missing class lombok.javac.handlers.HandleWith
Missing class lombok.javac.handlers.HandleWithBy
Missing class lombok.javac.handlers.singulars.JavacGuavaMapSingularizer
Missing class lombok.javac.handlers.singulars.JavacGuavaSetListSingularizer
Missing class lombok.javac.handlers.singulars.JavacGuavaTableSingularizer
Missing class lombok.javac.handlers.singulars.JavacJavaUtilListSingularizer
Missing class lombok.javac.handlers.singulars.JavacJavaUtilMapSingularizer
Missing class lombok.javac.handlers.singulars.JavacJavaUtilSetSingularizer
```

При повторном прогоне с исключением R8-таска:

```bash
./gradlew :android:assembleRelease -x :android:minifyReleaseWithR8
# падает на :android:l8DexDesugarLibRelease
> A failure occurred while executing ... L8DexWorkAction
  > Compilation failed to complete
Error: No source specified
```

---

## 3. Причины (подтверждено — см. §8)

1. **Lombok и R8 full mode.** `core` объявляет `implementation(libs.lombok)` и использует
   compile-time аннотации (`@Getter`, `@Setter`, `@RequiredArgsConstructor`). R8 в full mode
   трактует «missing class» как ошибку сборки; сообщения о `lombok.javac.handlers.*` приходят
   из анализа `kotlinx.coroutines.internal.FastServiceLoader` (ServiceLoader-путь).
   Направления фикса:
   - перевести Lombok в `compileOnly` в `core` (`compileOnly(libs.lombok)` +
     `annotationProcessor(libs.lombok)`), чтобы он не попадал на runtime classpath;
   - либо добавить `-dontwarn lombok.**` / `-keep` в `android/proguard-rules.pro`;
   - проверить, что R8 не анализирует compile-time-only классы через ServiceLoader.
2. **`l8DexDesugarLibRelease` «No source specified».** Похоже на некорректную конфигурацию
   `coreLibraryDesugaring` для release-варианта (десугар применяется, но вход пуст).
   Направления: проверить, что `coreLibraryDesugaring(libs.desugar.jdk.libs)` корректно
   настроен, что десугаринг действительно нужен при `minSdk 26`, и что `record`/`java.time`
   не требуют библиотеки десугара (при `minSdk 26` многие API уже нативные).
3. **Отсутствие кэша `desugar_jdk_libs`.** Чисто среда/CI; не код.

---

## 4. Что НЕ делать

- **Не менять игровую логику `core`** ради этого ТЗ.
- Не откатывать интерфейс `CardDealer`, `ScriptedCardDealer` и доработки по
  `trumpcards_review_and_refactor.md`.
- Не «глушить» ошибку глобальным отключением `minifyEnabled` в release: цель — релизный
  `.aab`, соответствующий `android_task.md` (R8 обязателен).

---

## 5. Задачи

- [x] Воспроизвести оба режима на чистом кэше; зафиксировать минимальный набор шагов.
- [x] Устранить ошибку `:android:minifyReleaseWithR8` (Lombok/R8), минимально и локально
      (в `core` и/или `android/proguard-rules.pro`).
- [x] Устранить `:android:l8DexDesugarLibRelease` (`No source specified`) — оказалось
      следствием исключения R8-таска, отдельного фикса не требует (см. §8).
- [x] Обеспечить успешный `./gradlew :android:assembleRelease` и `:android:bundleRelease`.
- [x] Обеспечить возможность offline-сборки android (прогрев кэша `desugar_jdk_libs`
      или документированное требование сети для android-тасков).
- [x] Не сломать debug-сборку и тесты (`:android:testDebugUnitTest`, `assembleDebug`).

---

## 6. Критерии приёмки

- [x] `./gradlew build` (online) — **BUILD SUCCESSFUL** (117 задач).
- [x] `./gradlew :android:assembleRelease` — **BUILD SUCCESSFUL**,
      `android/build/outputs/apk/release/android-release-unsigned.apk` на месте.
- [x] `./gradlew :android:bundleRelease` — **BUILD SUCCESSFUL**,
      `android/build/outputs/bundle/release/android-release.aab` на месте.
- [x] `./gradlew :android:testDebugUnitTest :android:assembleDebug` — зелёные (как сейчас).
- [x] `:core:test` — зелёные (86 тестов), поведение домена не изменено.
- [x] Изменения ограничены `core/build.gradle.kts` и `android/proguard-rules.pro`,
      без правок игровых правил.
- [x] В отчёте явно указаны: причина, минимальный фикс, оставшиеся ограничения — см. §8.

---

## 7. Замечания для агента

- Сначала собери диагностику: `./gradlew :android:minifyReleaseWithR8 --stacktrace` и
  `./gradlew :android:dependencies --configuration coreLibraryDesugaring`.
- Проверь, попадает ли Lombok в runtime classpath (`:core:dependencies`).
- R8 в full mode не «прощает» missing classes; предпочитай точечный `-dontwarn lombok.**`
  и `compileOnly` для Lombok, а не отключение minify.
- После фикса прогони `trumpcards_review_and_refactor.md` регрессию: `:core:test`.

---

## 8. Решение (30.09.2026)

### Корневая причина

`core/build.gradle.kts` объявлял Lombok как `implementation`, поэтому jar Lombok (включая
`lombok.javac.handlers.*`) попадал в **runtime** classpath `:core` и транзитивно в release
classpath `:android`. R8 в full mode анализирует и runtime-классы, и ServiceLoader-пути,
поэтому «missing class» стали ошибкой сборки.

### Фикс (минимальный, локальный)

1. `core/build.gradle.kts`: `implementation(libs.lombok)` → **`compileOnly(libs.lombok)`**
   (`annotationProcessor` оставлен). Lombok — чисто compile-time процессор, ему незачем
   быть в runtime. После этого из ошибок R8 исчезли все `lombok.javac.handlers.*`.
2. Остался один reference: `lombok.Generated` — аннотация, которую Lombok вешает на
   сгенерированные методы (`Card.Rank.getValue()` и др.) и которая сохраняется в байткоде.
   Сам класс аннотации не поставляется, поэтому в `android/proguard-rules.pro` добавлен
   точечный **`-dontwarn lombok.Generated`** (ровно то правило, что AGP сам предложил в
   `android/build/outputs/mapping/release/missing_rules.txt`).

### Почему `l8DexDesugarLibRelease` «No source specified»

Это **не отдельный баг**: при искусственном прогоне с `-x :android:minifyReleaseWithR8`
L8 получает пустой вход (его источник — выход минификации). В обычном прогоне
`minifyReleaseWithR8` падал раньше, а L8 после его успеха отрабатывает нормально. Отдельного
фикса не потребовалось.

### Проверка

```bash
./gradlew build                          # BUILD SUCCESSFUL (117 задач)
./gradlew :android:assembleRelease       # BUILD SUCCESSFUL
./gradlew :android:bundleRelease         # BUILD SUCCESSFUL
./gradlew :android:assembleRelease --offline   # BUILD SUCCESSFUL (кэш прогрет)
./gradlew :core:test                     # 86 тестов, 0 failures
```

Артефакты: `android/build/outputs/apk/release/android-release-unsigned.apk`,
`android/build/outputs/bundle/release/android-release.aab`.

### Оставшиеся ограничения

- **Первый прогон android-сборки требует сети** (скачать `desugar_jdk_libs:2.1.4` в кэш);
  после прогрева кэша `--offline` работает, включая `assembleRelease`. Для чистого CI —
  либо прогрев кэша, либо разрешить сеть для android-тасков.
- R8 печатает безобидное предупреждение `An error occurred when parsing kotlin metadata`
  (R8 из AGP 8.9.0 старее Kotlin 2.2.21). Это warning, не ошибка; можно убрать обновлением
  AGP/R8, когда появится совместимая версия.
- Release-артефакт без подписи (`-unsigned.apk` / неподписанный `.aab`) — нужен keystore
  для публикации; это отдельная задача, не связанная с данным ТЗ.
