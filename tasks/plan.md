# Implementation plan: Vehicle profile CRUD

## Overview

Build the first complete Person A feature. A user can create, list, view, edit, and delete local vehicle profiles. Room persists the profiles across app restarts. This feature establishes the vehicle identity and mileage data that maintenance will use later.

The feature includes brand, model, model year, an optional plate number, and current mileage. The Room entity keeps the agreed nullable `photoPath` column, but this feature has no photo picker or file handling. Maintenance, fuel, expenses, dashboard content, analytics, and settings remain outside this plan.

## Evidence and current state

- `Overview.md` and `decisions.md` assign vehicle persistence, vehicle CRUD, and Room setup to Person A.
- The repository contains the generated `MainActivity` and Hello World layout. It has no database, repository, fragments, ViewModels, or navigation graph.
- Room, Navigation Component, Lifecycle, and coroutines dependencies are already staged. This conflicts with an older line in `STATUS.md` that says they are absent. The repository is the source of truth for the current implementation.
- `GRADLE_USER_HOME=/tmp/corsa-gradle ANDROID_USER_HOME=/tmp/corsa-android ./gradlew --no-daemon --no-watch-fs --no-configuration-cache testDebugUnitTest assembleDebug` passed on 2026-11-11. Gradle fell back to in-process Kotlin compilation because the sandbox blocked the user-level Kotlin daemon directory.
- `app/build.gradle.kts` routes the Room compiler through `annotationProcessor`. The app uses Kotlin, so the feature must move the compiler to KSP before it adds Room types. Android's current AGP 9 guidance requires KSP 2.3.6 or newer and does not require the `org.jetbrains.kotlin.android` plugin.

## Architecture decisions

- Use one `:app` module, one concrete `CorsaRepository`, Room DAOs, Flow observations, Fragments, XML layouts, and ViewModels.
- Use the existing dependency catalog. Add only the KSP plugin that Kotlin Room code generation needs. Do not add a dependency injection framework, repository interface, use-case layer, Safe Args, ViewBinding, or adapter library.
- Use three vehicle shapes. `VehicleDraft` keeps raw form strings, `VehicleEntity` keeps Room types, and `Vehicle` is the immutable typed read model exposed by the repository. This keeps parsing and validation at one write boundary without exposing Room types to the UI.
- Store `id`, `currentMileageKm`, and future money values as `Long`. Store `year` as `Int`. Trim brand and model. Convert a blank plate number to `null`.
- Accept model years from 1886 through the next calendar year. Pass the current year into validation so tests do not depend on the wall clock.
- Keep `photoPath` nullable in the schema to match the frozen vehicle table. Do not expose photo behavior in this feature.
- Use explicit insert and update DAO methods. Do not use `REPLACE`. Editing a missing ID returns a missing-record outcome instead of inserting a new row.
- Person B keeps ownership of `AppContainer`, `activity_main.xml`, and `nav_graph.xml`. Before UI integration, freeze the repository provider and the vehicle destination IDs with Person B. Person A must not create a competing app shell.
- Pass only vehicle IDs between destinations. Each destination reloads the current row from Room.

## Dependency graph

```text
Phase 0 shared contract freeze
        |
KSP and schema export
        |
Vehicle entity, DAO, and database
        |
Repository validation and outcomes
        |
        +-------------------+
        |                   |
Vehicle list state     Vehicle editor state
        |                   |
Vehicle list UI        Add and edit UI
        |                   |
        +---------+---------+
                  |
        Details and delete flow
                  |
      End-to-end persistence check

Person B app shell and navigation contract
                  |
          All vehicle UI tasks
```

## Task list

### Entry checkpoint: Phase 0 contract freeze

- [x] The team agrees on the five table shapes, shared outcomes, calculator signatures, navigation arguments, and database versioning rules recorded in the Corsa notes.
- [x] This feature begins only after that shared agreement. Maintenance, fuel, and expense implementation remain outside this plan.

### Phase 1: Persistence foundation

- [ ] Task 1: Configure Kotlin Room processing.
- [ ] Task 2: Persist and observe vehicle rows.
- [ ] Task 3: Validate vehicle writes through the repository.

### Checkpoint: Persistence foundation

- [ ] Room code generation succeeds and exports schema version 1.
- [ ] Focused DAO and repository tests pass on an emulator or device.
- [ ] The debug app and local unit tests build without warnings caused by the feature configuration.

### Phase 2: Vehicle screens

- [ ] Task 4: Expose vehicle list screen state.
- [ ] Task 5: Render vehicle list rows.
- [ ] Task 6: Integrate the vehicle list with the app shell.
- [ ] Task 7: Expose add and edit screen state.
- [ ] Task 8: Render the add and edit form.
- [ ] Task 9: Expose vehicle details and deletion state.
- [ ] Task 10: Render vehicle details and confirmed deletion.

### Checkpoint: Complete vehicle CRUD

- [ ] A user can create, list, view, edit, and delete two distinct vehicle profiles.
- [ ] Saved profiles survive app restart and an edit never changes another profile.
- [ ] Invalid input stays in the form with field errors and causes no database write.
- [ ] Rotation preserves the editor draft. A missing vehicle shows an explanation and a route back.
- [ ] The complete Gradle, emulator, accessibility, light-theme, and dark-theme checks pass.

## Acceptance boundary

The feature is complete when the repository tests and rendered workflow prove these behaviors:

- Create two vehicles and read both back after reopening a file-backed Room database.
- Edit one vehicle without changing its ID or the other vehicle.
- Reject blank brand, blank model, an out-of-range year, negative mileage, and numeric overflow without a write.
- Normalize a blank plate number to `null`.
- Keep entered values after validation or storage failure and prevent duplicate save submissions.
- Confirm deletion. Cancellation keeps the vehicle, deletion removes only the selected vehicle, and a repeated delete reports that the row is already missing.
- Render empty, content, loading, field-error, storage-error, and missing-record states.
- Preserve the draft through rotation and process restoration.

## Risks and mitigations

| Risk | Impact | Mitigation |
| --- | --- | --- |
| KSP is not configured for AGP 9 built-in Kotlin | High | Configure KSP first, replace `annotationProcessor` with `ksp`, and compile a real Room entity before UI work. |
| Person A and Person B edit the app shell or navigation graph at the same time | High | Freeze destination IDs and the repository provider first. Let Person B own the shared files and integrate Person A's destinations once. |
| A generic upsert recreates a deleted vehicle during edit | High | Use separate insert and update DAO methods. Treat an update count of zero as a missing vehicle. |
| A later child table makes `REPLACE` cascade-delete history | High | Do not use `REPLACE` now or later. |
| Model-year validation changes as time passes | Medium | Pass the current year into validation and test the exact lower and upper boundaries. |
| Vehicle mileage becomes lower than retained history after maintenance and fuel arrive | Medium | Allow correction in this feature because no history exists. Add the recorded cross-history constraint when those tables land. |
| A photo column is mistaken for photo feature scope | Low | Keep only the nullable schema field. Do not add file permissions, pickers, image loading, or cleanup code. |

## Verification commands

Run focused tests while each task is in progress. Run the full set at the final checkpoint.

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
./gradlew :app:installDebug
```

The connected test and install commands require a running emulator or an attached device. The final manual check must create two vehicles, restart the app, edit one vehicle, cancel one deletion, confirm one deletion, and inspect the result in both system themes.

## Open assumptions

- Person B supplies the agreed `AppContainer` and Navigation Component shell before Task 6 integrates the list screen.
- Model year `1886` is the lower bound. The next calendar year is the upper bound.
- Database version 1 contains the agreed vehicle table, including nullable `photoPath`. Later tables use explicit migrations if version 1 has already shipped.

## Deferred work

- Vehicle photos.
- Maintenance rules, maintenance logs, and maintenance calculations.
- Dashboard alerts and summaries.
- Fuel, expenses, analytics, settings, background reminders, cloud sync, and motorcycle support.

