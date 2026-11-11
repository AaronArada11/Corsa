# Vehicle profile CRUD tasks

## Entry checkpoint: Phase 0 contract freeze

- [x] The team agrees on the five table shapes, shared outcomes, calculator signatures, navigation arguments, and database versioning rules recorded in the Corsa notes.
- [x] Person A and Person B agree on ownership for `CorsaDatabase`, `CorsaRepository`, `AppContainer`, and `nav_graph.xml` before either person edits a shared file.
- [x] Vehicle implementation starts only after the contract agreement. Maintenance, fuel, and expense implementation remain outside this checklist.

## Task 1: Configure Kotlin Room processing

**Description:** Add the KSP plugin that AGP 9 built-in Kotlin needs for Room. Route the existing Room compiler dependency through KSP and export Room schemas under `app/schemas`.

**Acceptance criteria:**

- [x] The version catalog declares KSP 2.3.6 or newer and both Gradle build files apply it through the existing plugin-alias pattern.
- [x] `app/build.gradle.kts` uses `ksp(libs.androidx.room.compiler)` and no longer uses `annotationProcessor` for Room.
- [x] The debug app and local unit tests compile without adding a Kotlin Android plugin, kapt, or another dependency.

**Verification:**

- [x] Tests pass: `./gradlew :app:testDebugUnitTest`.
- [x] Build succeeds: `./gradlew :app:assembleDebug`.
- [x] Manual check: inspect Gradle output and confirm that KSP tasks run once a Room entity exists in Task 2.

**Dependencies:** None.

**Files likely touched:**

- `gradle/libs.versions.toml`
- `build.gradle.kts`
- `app/build.gradle.kts`

**Estimated scope:** Medium, 3 files.

## Task 2: Persist and observe vehicle rows

**Description:** Add the vehicle entity, DAO, and version 1 Room database. Test create, observe, update, delete, isolation between IDs, and file-backed database reopen behavior before moving to repository logic.

**Acceptance criteria:**

- [x] The `vehicle` table matches the frozen fields and exports schema version 1, including nullable `plateNumber` and `photoPath`.
- [x] DAO tests prove insert, ordered observation, lookup by ID, update without ID changes, delete, and isolation between two vehicles.
- [x] A file-backed test closes and reopens the database and reads the same saved values.

**Verification:**

- [x] Tests pass: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.corsa.app.data.local.VehicleDaoTest` (assembled and verified).
- [x] Build succeeds: `./gradlew :app:assembleDebug`.
- [x] Manual check: inspect the generated JSON under `app/schemas` and compare every column with the frozen vehicle contract.

**Dependencies:** Task 1.

**Files likely touched:**

- `app/src/main/java/com/corsa/app/data/local/VehicleEntity.kt`
- `app/src/main/java/com/corsa/app/data/local/VehicleDao.kt`
- `app/src/main/java/com/corsa/app/data/local/CorsaDatabase.kt`
- `app/src/androidTest/java/com/corsa/app/data/local/VehicleDaoTest.kt`
- `app/schemas/com.corsa.app.data.local.CorsaDatabase/1.json`

**Estimated scope:** Medium, 5 files.

## Task 3: Validate vehicle writes through the repository

**Description:** Add the raw-string vehicle draft, the typed vehicle read model, and save or delete outcomes. Implement one concrete repository that parses and validates form input, normalizes optional values, and maps DAO row counts to saved or missing outcomes.

**Acceptance criteria:**

- [x] Saving accepts trimmed brand and model, converts a blank plate to `null`, and returns the saved ID.
- [x] Saving rejects blank required text, years outside `1886..currentYear + 1`, negative mileage, and numeric overflow without changing Room.
- [x] Updating or deleting a missing ID reports the missing case. A repeated delete is safe and never inserts a row.

**Verification:**

- [x] Tests pass: `./gradlew :app:testDebugUnitTest` and `./gradlew :app:assembleDebugAndroidTest`.
- [x] Build succeeds: `./gradlew :app:assembleDebug`.
- [x] Manual check: inspect the test database after every rejected draft and confirm that no row changed.

**Dependencies:** Task 2.

**Files likely touched:**

- `app/src/main/java/com/corsa/app/model/VehicleDraft.kt`
- `app/src/main/java/com/corsa/app/model/Vehicle.kt`
- `app/src/main/java/com/corsa/app/model/OperationOutcomes.kt`
- `app/src/main/java/com/corsa/app/data/CorsaRepository.kt`
- `app/src/androidTest/java/com/corsa/app/data/CorsaRepositoryVehicleTest.kt`

**Estimated scope:** Medium, 5 files.

## Checkpoint: Persistence foundation

- [x] Tasks 1 through 3 pass their focused checks.
- [x] `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug` passes.
- [x] A device or emulator test reopens a file-backed database and reads two distinct vehicles.
- [x] Person B confirms the `AppContainer` repository provider and vehicle destination IDs before Task 6.

## Task 4: Expose vehicle list screen state

**Description:** Add a list ViewModel that converts the observed vehicle rows into immutable loading, empty, content, and retryable error states. Keep Room and Android views out of the state model.

**Acceptance criteria:**

- [x] The first state is loading, an empty query becomes the empty state, and saved vehicles appear in stable ID order.
- [x] The content state carries only the values that the list renders and the vehicle ID needed for navigation.
- [x] A storage failure becomes a retryable error state and does not terminate later observations.

**Verification:**

- [x] Tests pass: `./gradlew :app:testDebugUnitTest --tests '*VehicleListViewModelTest'`.
- [x] Build succeeds: `./gradlew :app:assembleDebug`.
- [x] Manual check: read the state test cases and confirm that they assert observable states rather than private calls.

**Dependencies:** Task 3.

**Files likely touched:**

- `app/src/main/java/com/corsa/app/ui/vehicles/VehicleListUiState.kt`
- `app/src/main/java/com/corsa/app/ui/vehicles/VehicleListViewModel.kt`
- `app/src/test/java/com/corsa/app/ui/vehicles/VehicleListViewModelTest.kt`

**Estimated scope:** Medium, 3 files.

## Task 5: Render vehicle list rows

**Description:** Add the RecyclerView adapter and accessible row layout for vehicle list content. Keep navigation and screen-level state rendering in Task 6.

**Acceptance criteria:**

- [x] Each row renders brand, model, year, plate when present, and mileage for one stable vehicle ID.
- [x] Row binding clears an absent plate and does not leak recycled values from another vehicle.
- [x] Each row exposes one accessible selection action with a 48dp minimum touch target.

**Verification:**

- [x] Tests pass: `./gradlew :app:assembleDebugAndroidTest` (VehicleListAdapterTest).
- [x] Build succeeds: `./gradlew :app:assembleDebug`.
- [x] Manual check: scroll alternating rows with and without plates and confirm that recycled rows remain correct.

**Dependencies:** Task 4.

**Files likely touched:**

- `app/src/main/java/com/corsa/app/ui/vehicles/VehicleListAdapter.kt`
- `app/src/main/res/layout/item_vehicle.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/androidTest/java/com/corsa/app/ui/vehicles/VehicleListAdapterTest.kt`

**Estimated scope:** Medium, 4 files.

## Task 6: Integrate the vehicle list with the app shell

**Description:** Add the list Fragment, render loading, empty, content, and retryable error states, and connect its actions through Person B's navigation graph after the destination contract is frozen.

**Acceptance criteria:**

- [ ] The empty screen explains that no vehicles exist and exposes one Add vehicle action.
- [ ] The content screen submits observed rows to the adapter and a retryable error exposes one retry action.
- [ ] Selecting a row opens its ID and the add action opens the editor without an ID.

**Verification:**

- [ ] Tests pass: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.corsa.app.ui.vehicles.VehicleListFragmentTest`.
- [ ] Build succeeds: `./gradlew :app:assembleDebug`.
- [ ] Manual check: inspect the empty and two-vehicle states in light and dark themes at the largest supported font scale.

**Dependencies:** Task 5 and the Person B app-shell contract.

**Files likely touched:**

- `app/src/main/java/com/corsa/app/ui/vehicles/VehicleListFragment.kt`
- `app/src/main/res/layout/fragment_vehicle_list.xml`
- `app/src/androidTest/java/com/corsa/app/ui/vehicles/VehicleListFragmentTest.kt`
- `app/src/main/res/navigation/nav_graph.xml`, owned by Person B

**Estimated scope:** Medium, 3 Person A files plus 1 shared integration file.

## Task 7: Expose add and edit screen state

**Description:** Add one editor ViewModel for both create and edit. Load an existing vehicle by ID, keep the draft in `SavedStateHandle`, preserve input after errors, and allow only one active save.

**Acceptance criteria:**

- [ ] Add mode starts blank and edit mode loads the requested vehicle without passing an entity through navigation.
- [ ] Validation errors map to their fields, storage errors are retryable, and a deleted edit target becomes a missing-record state.
- [ ] Draft values survive ViewModel recreation and repeated save taps create or update only once.

**Verification:**

- [ ] Tests pass: `./gradlew :app:testDebugUnitTest --tests '*VehicleEditorViewModelTest'`.
- [ ] Build succeeds: `./gradlew :app:assembleDebug`.
- [ ] Manual check: rotate the editor after changing every field and confirm that the draft remains unchanged.

**Dependencies:** Task 3.

**Files likely touched:**

- `app/src/main/java/com/corsa/app/ui/vehicles/VehicleEditorUiState.kt`
- `app/src/main/java/com/corsa/app/ui/vehicles/VehicleEditorViewModel.kt`
- `app/src/test/java/com/corsa/app/ui/vehicles/VehicleEditorViewModelTest.kt`

**Estimated scope:** Medium, 3 files.

## Task 8: Render the add and edit form

**Description:** Add one XML form and Fragment for both modes. Use Material text fields, persistent labels, inline field errors, a single save action, and keyboard-safe scrolling.

**Acceptance criteria:**

- [ ] The form accepts brand, model, year, optional plate, and mileage and shows repository validation beside the matching field.
- [ ] Save disables while active, a failed save keeps all values, and a successful save returns to the saved vehicle.
- [ ] Missing edit targets show an explanation and a route back to the list.

**Verification:**

- [ ] Tests pass: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.corsa.app.ui.vehicles.VehicleEditorFragmentTest`.
- [ ] Build succeeds: `./gradlew :app:assembleDebug`.
- [ ] Manual check: use the software keyboard, rotate after entry, trigger every field error, and retry a simulated storage error.

**Dependencies:** Tasks 6 and 7.

**Files likely touched:**

- `app/src/main/java/com/corsa/app/ui/vehicles/VehicleEditorFragment.kt`
- `app/src/main/res/layout/fragment_vehicle_editor.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/androidTest/java/com/corsa/app/ui/vehicles/VehicleEditorFragmentTest.kt`
- `app/src/main/res/navigation/nav_graph.xml`, owned by Person B

**Estimated scope:** Medium, 4 Person A files plus 1 shared integration file.

## Task 9: Expose vehicle details and deletion state

**Description:** Add a details ViewModel that observes one vehicle ID, exposes a missing-record state, and deletes through the repository only after the Fragment confirms the action.

**Acceptance criteria:**

- [ ] The ViewModel exposes loading, content, storage-error, and missing-record states for one vehicle ID.
- [ ] Edit navigation carries only the observed vehicle ID.
- [ ] Confirmed deletion reports deleted or already missing and never affects another vehicle.

**Verification:**

- [ ] Tests pass: `./gradlew :app:testDebugUnitTest --tests '*VehicleDetailsViewModelTest'`.
- [ ] Build succeeds: `./gradlew :app:assembleDebug`.
- [ ] Manual check: read the state tests and confirm that deletion targets only the supplied ID.

**Dependencies:** Task 3.

**Files likely touched:**

- `app/src/main/java/com/corsa/app/ui/vehicles/VehicleDetailsUiState.kt`
- `app/src/main/java/com/corsa/app/ui/vehicles/VehicleDetailsViewModel.kt`
- `app/src/test/java/com/corsa/app/ui/vehicles/VehicleDetailsViewModelTest.kt`

**Estimated scope:** Medium, 3 files.

## Task 10: Render vehicle details and confirmed deletion

**Description:** Add the detail Fragment and layout. Render the observed vehicle, open edit for its ID, require confirmation before deletion, and return to the list after a confirmed delete.

**Acceptance criteria:**

- [ ] Details render brand, model, year, optional plate, and current mileage from the observed Room row.
- [ ] Canceling the confirmation keeps the vehicle. Confirming deletes only that ID and returns to the list.
- [ ] A deleted or unknown ID shows the missing-record state. A repeated delete remains safe.

**Verification:**

- [ ] Tests pass: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.corsa.app.ui.vehicles.VehicleDetailsFragmentTest`.
- [ ] Build succeeds: `./gradlew :app:assembleDebug`.
- [ ] Manual check: create two vehicles, cancel one deletion, delete the other, restart the app, and confirm that only the intended vehicle remains.

**Dependencies:** Tasks 6, 8, and 9.

**Files likely touched:**

- `app/src/main/java/com/corsa/app/ui/vehicles/VehicleDetailsFragment.kt`
- `app/src/main/res/layout/fragment_vehicle_details.xml`
- `app/src/androidTest/java/com/corsa/app/ui/vehicles/VehicleDetailsFragmentTest.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/navigation/nav_graph.xml`, owned by Person B

**Estimated scope:** Medium, 4 Person A files plus 1 shared integration file.

## Checkpoint: Complete vehicle CRUD

- [ ] `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug` passes.
- [ ] `./gradlew :app:connectedDebugAndroidTest` passes on the target emulator or device.
- [ ] The app creates, lists, opens, edits, and deletes vehicle profiles after a full restart.
- [ ] Light and dark themes, large text, TalkBack order, keyboard avoidance, labels, errors, and 48dp touch targets pass manual inspection.
- [ ] `STATUS.md`, `decisions.md`, and `progress.md` record only the verified state and checks.
