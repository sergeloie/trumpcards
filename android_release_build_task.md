# ТЗ: релизная сборка Android (`trumpcards`)

> Статус: **открыто, на будущее.** Зафиксировано 27.09.2026 при прогоне полного build после
> Задания 2 (`trumpcards_review_and_refactor.md`). К игровой логике `core` отношения не имеет.
> Приоритет: средний (debug-сборка и все тесты зелёные; страдает только release/`.aab`).

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

## 3. Предполагаемые причины (проверить в ходе работы)

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

- [ ] Воспроизвести оба режима на чистом кэше; зафиксировать минимальный набор шагов.
- [ ] Устранить ошибку `:android:minifyReleaseWithR8` (Lombok/R8), минимально и локально
      (в `core` и/или `android/proguard-rules.pro`).
- [ ] Устранить `:android:l8DexDesugarLibRelease` (`No source specified`).
- [ ] Обеспечить успешный `./gradlew :android:assembleRelease` и `:android:bundleRelease`.
- [ ] Обеспечить возможность offline-сборки android (прогрев кэша `desugar_jdk_libs`
      или документированное требование сети для android-тасков).
- [ ] Не сломать debug-сборку и тесты (`:android:testDebugUnitTest`, `assembleDebug`).

---

## 6. Критерии приёмки

- [ ] `./gradlew build` (online) — **BUILD SUCCESSFUL**.
- [ ] `./gradlew :android:assembleRelease` — **BUILD SUCCESSFUL**, артефакт на месте.
- [ ] `./gradlew :android:testDebugUnitTest :android:assembleDebug` — зелёные (как сейчас).
- [ ] `:core:test` — зелёные, поведение домена не изменено.
- [ ] Изменения ограничены `core/build.gradle.kts`, `android/build.gradle.kts`,
      `android/proguard-rules.pro` (или аналогичными build-файлами), без правок игровых правил.
- [ ] В отчёте явно указаны: причина, минимальный фикс, оставшиеся ограничения (например,
      требование сети для first-time android-сборки).

---

## 7. Замечания для агента

- Сначала собери диагностику: `./gradlew :android:minifyReleaseWithR8 --stacktrace` и
  `./gradlew :android:dependencies --configuration coreLibraryDesugaring`.
- Проверь, попадает ли Lombok в runtime classpath (`:core:dependencies`).
- R8 в full mode не «прощает» missing classes; предпочитай точечный `-dontwarn lombok.**`
  и `compileOnly` для Lombok, а не отключение minify.
- После фикса прогони `trumpcards_review_and_refactor.md` регрессию: `:core:test`.
