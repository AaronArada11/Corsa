---
title: Corsa progress
tags:
  - projects/corsa
---

# CORSA progress

[[Projects/Corsa/Overview|Overview]] · [[Projects/Corsa/STATUS|Status]] · [[Projects/Corsa/decisions|Decisions]]

## 2026-11-09 Architecture notebook created

Date source is this work session. The owner supplied the project brief, confirmed that the proposal is already complete, requested app architecture, and selected cars first. The proposal's original completion date is unknown.

Created the four project notes using the vault's templates and linked the project from Home and Projects. The architecture covers one app module, MVVM responsibilities, five related Room tables, cost ownership, atomic writes, mileage validation, maintenance and fuel formulas, navigation, and a staged implementation backlog.

Compared a compact package-based app with a three-module domain/data/app structure. Selected the compact design and retained explicit operation outcomes and missing-record handling. The rationale is in [[Projects/Corsa/decisions|Decisions]].

No application repository was inspected, no Kotlin or XML application code was written, and no Android build, unit test, or device check was run. Architecture examples are expected behavior rather than test results.

Next step is to confirm the repository and approval state, then implement and verify car CRUD with Room before the dependent features.

## 2026-11-09 Visual direction added

The owner selected [Lay's car maintenance app concept](https://dribbble.com/shots/27346135-A-car-maintenance-app-concept) and requested the apple-design skill. Added the reference, proposed theme tokens, typography, Material component mapping, and compact/wider screen compositions to Overview. Reviewed Apple design references for accessibility, layout, typography, color, appearance, branding, and data entry, translating the principles to Android.

Retrieved the Dribbble page's description and published palette. The full design image could not be inspected reliably. Calculated contrast for proposed text and surface combinations and adjusted secondary text after the source gray measured 4.41:1 on graphite. Rendered screens, layouts, and accessibility behavior remain unverified because no Android app was run.

Final notebook verification passed. Checked exactly four required files, one document title per note, frontmatter, balanced code fences, reciprocal sibling links, 21 resolved wikilinks across the Corsa notes, and one Corsa entry in each index. Calculated all proposed text tokens against both surfaces per appearance. The minimum was 4.77:1 in dark mode and 6.40:1 in light mode. An initial broader index scan encountered Home's existing illustrative `[[Note name]]` link. The scoped Corsa checks passed without changing that unrelated example. Obsidian reading-view rendering was not checked.

## 2026-11-10 Spec-Driven Development plan and two-person work allocation

Inspected the Android app repository at `/Users/aaronarada/AndroidStudioProjects/Corsa`. Confirmed project baseline: Gradle application module targeting Android SDK 37 (minSdk 24) with Kotlin, AppCompat, Material, and ConstraintLayout dependencies. Room, Navigation Component, and Coroutines/Flow dependencies are not yet added.

Formulated a Spec-Driven Development (SDD) strategy dividing work equally across two developers:
- Defined the four-step lifecycle: Contract Freeze -> Test-First Authoring -> Minimal Implementation -> Verification.
- Structured equal parallel pillars: Person A (Vehicle & Maintenance domain, Room DB setup, Dashboard) and Person B (Fuel & Expense domain, Themes & Navigation shell, Analytics).
- Formulated an 8-point integration test matrix (IT-01 to IT-08) covering atomic mileage advancement, dual-limit maintenance states, cascade deletions, full-partial-full fuel segments, GAP resets, single-source spending queries, and accessibility/theme tokens.
- Documented decisions and updated project status in the Jarvis vault. Created the comprehensive SDD plan artifact for execution.
- Configured project currency as Philippine Peso (PHP, `₱`), stored as integer minor units (`costMinor: Long` centavos).
- Staged Room (`2.6.1`), Navigation Component (`2.8.5`), Lifecycle ViewModel (`2.8.7`), and Coroutines (`1.9.0`) in `gradle/libs.versions.toml` and `app/build.gradle.kts`. Configured JDK 25 path in `gradle.properties`.
- Authored detailed role profiles for Person A and Person B specifying file ownership, Git branching workflows, contract tests, and communication touchpoints.

## 2026-11-11 Vehicle profile CRUD planned

Read the Corsa architecture, status, decisions, and progress notes and inspected the current Android repository. Selected local vehicle profile CRUD as the first complete Person A feature. Created tasks/plan.md and tasks/todo.md with ten dependency-ordered tasks, acceptance criteria, focused checks, team ownership boundaries, and final device checks.

Confirmed that the repository is still the generated Hello World shell. Room, Navigation Component, Lifecycle, and coroutines dependencies are staged. Found that Room compiler setup still uses annotationProcessor, so the first task moves Kotlin Room processing to KSP. Current Android guidance for AGP 9 built-in Kotlin supports that setup without the Kotlin Android plugin.

Ran the existing local unit tests and debug assembly with isolated Gradle and Android caches. BUILD SUCCESSFUL completed in 36 seconds. The sandbox blocked the user-level Kotlin daemon directory, so Gradle used its in-process fallback. No feature code, Room schema, instrumentation test, or emulator workflow was implemented or verified.

## 2026-11-11 Phase 0 Contract Freeze and Domain Modeling completed

Completed the entry checkpoint for Phase 0 in `tasks/todo.md`:
- Codified and froze architecture contracts in `tasks/contract_freeze.md`, covering 5 Room tables, cascade and foreign key constraints, operation outcomes (`SaveOutcome`, `DeleteOutcome`), pure calculator contracts (`MaintenanceStatus`, `TankState`, `FuelSummary`), primitive-only navigation arguments, and team ownership boundaries (`CorsaDatabase`, `CorsaRepository`, `AppContainer`, `nav_graph.xml`).
- Created the project domain glossary at `GLOSSARY.md` establishing canonical definitions and avoided terms for Vehicle, Maintenance, Fuel & Efficiency, and Financials.
- Formulated and authored architectural decision records in `docs/adr/`:
  - `0001-single-source-spending.md`: Single-source spending via on-demand `UNION ALL` projection.
  - `0002-full-tank-efficiency-anchoring.md`: Full-to-full tank efficiency anchoring with explicit GAP resets.
  - `0003-integer-minor-currency-units.md`: Integer minor units (`costMinor: Long` in centavos) for financial storage and arithmetic.
  - `0004-deterministic-rule-anchoring.md`: Deterministic dual-threshold rule anchoring without driving-rate extrapolation.
- Implemented Kotlin contracts in `app/src/main/java/com/corsa/app/model/OperationOutcomes.kt`, `SpendingItem.kt`, and `app/src/main/java/com/corsa/app/calculation/MaintenanceStatus.kt`, `FuelModels.kt`.
- Authored and verified local unit tests in `app/src/test/java/com/corsa/app/model/OperationOutcomesTest.kt`. Ran `./gradlew testDebugUnitTest` — build and tests passed in 5 seconds.

## 2026-11-11 Vehicle profile CRUD completed

Audited the interrupted implementation before continuing it. The audit found that the branch built cleanly, but list retry terminated after the first storage error, several Android checks had only assembled the test APK, the file-reopen test covered one row, and the generated activity still had no usable navigation or vehicle workflow. Those completion claims were corrected before implementation continued.

Completed the Room-backed vehicle feature in small commits:
- strengthened live DAO and two-row file-reopen coverage;
- made list observation retry create a fresh subscription;
- added one application-owned database/repository and Navigation host;
- added accessible list, restorable add/edit form, observed details, and confirmed exact-ID deletion;
- added missing, validation, storage-failure, retry, duplicate-save, rotation/recreation, edit, cancel-delete, confirm-delete, and row-recycling checks;
- consolidated the executable plan/checklist under `tasks-personA/` and removed redundant repository Markdown.

Verification on the Android 17 `Small_Phone` emulator passed:
- `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:connectedDebugAndroidTest` completed successfully;
- focused DAO, adapter, list Fragment, editor Fragment, details Fragment, editor ViewModel, and details ViewModel commands resolved to real tests and passed;
- a file-backed database reopened two distinct complete vehicle rows;
- a vehicle created through the rendered form survived force-stop/relaunch and appeared as one accessible list action;
- create, view, edit, cancel deletion, and confirmed deletion ran through connected screen tests;
- light and dark appearances and 200% text scale were inspected; rows expanded vertically without clipping, fields remained scrollable, toolbar/Up labels were exposed, and actionable controls met the 48dp minimum.

The first visual inspection caught missing toolbar titles caused by using the wrong XML title namespace. The implementation was corrected to render titles and label Up navigation, then the full Gradle and emulator gate was rerun successfully.

## 2026-11-11 Partner dashboard design integrated

Pulled `origin/feature/dashboard-design` after the vehicle CRUD slice. The partner branch was a static Activity prototype: it replaced the working NavHost shell, omitted `CorsaApplication`, forced a dark empty-dashboard theme, and referenced missing Analytics/Settings menu destinations. Arena architecture comparison selected a functional integration: keep the Room/Navigation app shell, add a repository-backed `DashboardFragment` as start destination, expose only Dashboard and My Cars tabs, adopt partner palette/artwork through shared DayNight semantic tokens, and leave dead placeholder destinations out.

Work landed on `partner-dashboard-integration` then merged to `main`:
- merge commit preserved partner history while resolving Activity/layout/string conflicts into the functional shell;
- visual follow-up retinted vehicle list/editor/details and card geometry to the partner graphite/blue language;
- dashboard ViewModel/Fragment tests plus updated vehicle screen tests cover empty/garage/error states, tab navigation, and bottom-bar hide on the editor.

Verification on awake Android 17 emulator `Small_Phone_2`:
- `./gradlew testDebugUnitTest` passed;
- `./gradlew connectedDebugAndroidTest` passed 17/17;
- `./gradlew :app:assembleDebug` passed on `main` after merge;
- live UI dump showed empty garage copy, partner vehicle artwork, Add Car CTA, and labeled Dashboard/My Cars bottom navigation.

## 2026-11-11 Adopt partner dashboard design as the visual source of truth

Owner requested the partner design instead of the earlier adapted DayNight shell. Restored the partner empty-dashboard theme, logo/settings chrome, exact empty-state copy, blue Add vehicle CTA styling, Analytics tab, and floating nav treatment on top of the existing Room/Navigation host. Vehicle screens now share the same dark partner palette. Analytics and Settings are stub screens.

Re-verified on Android 17 `Small_Phone`: unit tests, assembleDebug, connectedDebugAndroidTest 17/17, and UI dump showing LOGO PLACEHOLDER, Settings, Your Garage Starts Here, partner subtitle, Add vehicle, and Dashboard/My Cars/Analytics tabs.

## 2026-11-12 Main branch merge reconciled

Reconciled the local project-knowledge commit with 27 commits from remote `main`, preserving both the repository knowledge base and the remote vehicle CRUD, dashboard, and vehicle-document history. Confirmed there are no unresolved index entries, conflict markers, or staged whitespace errors.

Lint exposed one integration issue: the dashboard theme declared `android:windowLightNavigationBar`, an API 27 attribute, in the base resources even though the app supports API 24. Moved the explicit setting behind a `values-v27` resource while retaining the shared dark dashboard theme for all supported versions.

Verified the resolved tree with `testDebugUnitTest`, `:app:assembleDebug`, and `:app:lintDebug`; Gradle completed successfully with 51 actionable tasks. Connected Android tests were not rerun during this merge reconciliation.
