# Phase 0 Contract Freeze Agreement

Date: 2026-11-11
Status: Frozen and Agreed

This document records the frozen architecture, data models, table shapes, calculator signatures, navigation arguments, and ownership boundaries agreed upon before commencing implementation.

---

## 1. Five Table Shapes (Room Schema v1)

All identifiers and numeric values follow strict typing:
- Primary and Foreign Keys: `INTEGER` in SQLite -> `Long` in Kotlin.
- Distance and Mileage: `INTEGER` in SQLite -> `Long` in Kotlin (kilometers).
- Currency: `INTEGER` in SQLite -> `Long` in Kotlin (minor units, Philippine centavos: 100 centavos = ₱1.00). Floating-point currency is strictly forbidden.
- Fuel Liters: `REAL` in SQLite -> `Double` in Kotlin.
- Dates: `TEXT` in SQLite (ISO-8601: `YYYY-MM-DD` for service/expenses, `YYYY-MM-DDTHH:MM:SS` for fuel logs).

### Tables

1. `vehicle`
   - `id`: `INTEGER` PRIMARY KEY AUTOINCREMENT (`Long`)
   - `brand`: `TEXT` NOT NULL (`String`)
   - `model`: `TEXT` NOT NULL (`String`)
   - `year`: `INTEGER` NOT NULL (`Int`)
   - `plateNumber`: `TEXT` NULL (`String?`)
   - `currentMileageKm`: `INTEGER` NOT NULL (`Long`)
   - `photoPath`: `TEXT` NULL (`String?`)

2. `maintenance_rule`
   - `id`: `INTEGER` PRIMARY KEY AUTOINCREMENT (`Long`)
   - `vehicleId`: `INTEGER` NOT NULL (`Long`, FK -> `vehicle.id` ON DELETE CASCADE)
   - `name`: `TEXT` NOT NULL (`String`)
   - `intervalKm`: `INTEGER` NULL (`Long?`)
   - `intervalDays`: `INTEGER` NULL (`Long?`)
   - `baselineMileageKm`: `INTEGER` NULL (`Long?`)
   - `baselineDate`: `TEXT` NULL (`String?`)
   - `warningKm`: `INTEGER` NOT NULL (`Long`)
   - `warningDays`: `INTEGER` NOT NULL (`Long`)

3. `maintenance_log`
   - `id`: `INTEGER` PRIMARY KEY AUTOINCREMENT (`Long`)
   - `vehicleId`: `INTEGER` NOT NULL (`Long`, FK -> `vehicle.id` ON DELETE CASCADE)
   - `ruleId`: `INTEGER` NULL (`Long?`, FK -> `maintenance_rule.id` ON DELETE SET NULL)
   - `date`: `TEXT` NOT NULL (`String`)
   - `mileageKm`: `INTEGER` NOT NULL (`Long`)
   - `costMinor`: `INTEGER` NOT NULL (`Long`)
   - `description`: `TEXT` NOT NULL (`String`)

4. `fuel_log`
   - `id`: `INTEGER` PRIMARY KEY AUTOINCREMENT (`Long`)
   - `vehicleId`: `INTEGER` NOT NULL (`Long`, FK -> `vehicle.id` ON DELETE CASCADE)
   - `purchasedAt`: `TEXT` NOT NULL (`String`)
   - `mileageKm`: `INTEGER` NOT NULL (`Long`)
   - `liters`: `REAL` NOT NULL (`Double`)
   - `costMinor`: `INTEGER` NOT NULL (`Long`)
   - `tankState`: `TEXT` NOT NULL (`String`: `FULL`, `PARTIAL`, `GAP`)

5. `expense`
   - `id`: `INTEGER` PRIMARY KEY AUTOINCREMENT (`Long`)
   - `vehicleId`: `INTEGER` NOT NULL (`Long`, FK -> `vehicle.id` ON DELETE CASCADE)
   - `date`: `TEXT` NOT NULL (`String`)
   - `category`: `TEXT` NOT NULL (`String`)
   - `costMinor`: `INTEGER` NOT NULL (`Long`)
   - `description`: `TEXT` NOT NULL (`String`)

### Foreign Key & Indexing Rules
- Index all foreign key columns: `vehicleId`, `ruleId`.
- Index query paths:
  - `maintenance_log(vehicleId, ruleId, date)`
  - `fuel_log(vehicleId, purchasedAt)`
  - `expense(vehicleId, date)`
- Cascade rules:
  - Deleting a `vehicle` cascades to its rules, maintenance logs, fuel logs, and expenses atomically.
  - Deleting a `maintenance_rule` sets `ruleId` to `NULL` on linked `maintenance_log` records to preserve history.

---

## 2. Operation Outcomes

Common operation results are sealed in `com.corsa.app.model`:

```kotlin
sealed interface SaveOutcome {
    data class Success(val id: Long) : SaveOutcome
    data class ValidationErrors(val errors: Map<String, String>) : SaveOutcome
    data object MissingRecord : SaveOutcome
}

sealed interface DeleteOutcome {
    data object Deleted : DeleteOutcome
    data object Missing : DeleteOutcome
}
```

---

## 3. Pure Calculator Signatures

Calculators are pure, testable Kotlin objects with no Android or Room dependencies:

### Maintenance Status & Calculator
```kotlin
enum class MaintenanceStatus {
    NeedsBaseline,
    Scheduled,
    DueSoon,
    DueNow,
    Overdue
}

fun calculateMaintenance(
    ruleIntervalKm: Long?,
    ruleIntervalDays: Long?,
    ruleWarningKm: Long,
    ruleWarningDays: Long,
    anchorMileageKm: Long?,
    anchorDate: LocalDate?,
    currentMileageKm: Long,
    today: LocalDate
): MaintenanceStatus
```
- Exceeded enabled limit -> `Overdue`
- Exact hit on enabled limit -> `DueNow`
- Within configured warning distance or days -> `DueSoon`
- Outside warning range -> `Scheduled`
- Missing required anchor for an enabled limit -> `NeedsBaseline`

### Fuel Efficiency Calculator
```kotlin
enum class TankState { FULL, PARTIAL, GAP }

fun calculateFuel(logs: List<FuelRecordContract>): FuelSummary
```
- Segment measured strictly between consecutive `FULL` anchors.
- Intermediate `PARTIAL` fill liters are accumulated.
- Opening fill liters are excluded from consumption denominator.
- `GAP` resets continuity and invalidates the active segment.

---

## 4. Single-Source Spending (`UNION ALL`)

Fuel spending comes solely from `fuel_log`. Service spending comes solely from `maintenance_log`. Other expenses come from `expense`.

```sql
SELECT 'FUEL' AS sourceType, id AS sourceId, vehicleId, purchasedAt AS dateIso, 'Fuel' AS category, costMinor
FROM fuel_log
WHERE (:vehicleId IS NULL OR vehicleId = :vehicleId)
UNION ALL
SELECT 'MAINTENANCE' AS sourceType, id AS sourceId, vehicleId, date AS dateIso, 'Service' AS category, costMinor
FROM maintenance_log
WHERE (:vehicleId IS NULL OR vehicleId = :vehicleId)
UNION ALL
SELECT 'EXPENSE' AS sourceType, id AS sourceId, vehicleId, date AS dateIso, category, costMinor
FROM expense
WHERE (:vehicleId IS NULL OR vehicleId = :vehicleId)
ORDER BY dateIso DESC, sourceId DESC
```

Mapped to read model:
```kotlin
data class SpendingItem(
    val sourceType: SpendingSourceType,
    val sourceId: Long,
    val vehicleId: Long,
    val dateIso: String,
    val category: String,
    val costMinor: Long
)
```

---

## 5. Navigation Arguments Contract

All navigation carries only primitive IDs (`Long`). Fragments reload fresh immutable state from Room:
- `dashboardFragment` -> `vehicleListFragment` (no args)
- `vehicleListFragment` -> `vehicleDetailsFragment` (`vehicleId: Long`)
- `vehicleListFragment` -> `vehicleEditorFragment` (`vehicleId: Long = 0L`, 0 indicates Create)
- `vehicleDetailsFragment` -> `vehicleEditorFragment` (`vehicleId: Long`)
- `vehicleDetailsFragment` -> `maintenanceListFragment` (`vehicleId: Long`)
- `vehicleDetailsFragment` -> `fuelListFragment` (`vehicleId: Long`)
- `vehicleDetailsFragment` -> `expenseListFragment` (`vehicleId: Long`)
- `maintenanceListFragment` -> `serviceLogEditorFragment` (`vehicleId: Long`, `logId: Long = 0L`)
- `maintenanceListFragment` -> `maintenanceRuleEditorFragment` (`vehicleId: Long`, `ruleId: Long = 0L`)
- `fuelListFragment` -> `fuelEditorFragment` (`vehicleId: Long`, `fuelLogId: Long = 0L`)
- `expenseListFragment` -> `expenseEditorFragment` (`vehicleId: Long`, `expenseId: Long = 0L`)
- Bottom Navigation connects: Dashboard, My Cars (`vehicleListFragment`), Analytics.

---

## 6. Shared File Ownership and Team Boundary

- `CorsaDatabase`: Person A owns setup, type converters, and Room database class. Person B contributes `fuel_log` and `expense` entities/DAOs.
- `CorsaRepository`: Single concrete repository. Person A implements vehicle and maintenance methods; Person B implements fuel, expense, and spending query methods.
- `AppContainer`: Owned by Person B for manual dependency injection and repository provisioning.
- `nav_graph.xml`: Owned by Person B. Person A supplies fragment IDs, actions, and arguments for the Vehicle and Maintenance destinations.
- Database Versioning: Version 1 contains initial tables. No destructive migration is allowed once deployed; schema migrations must be written explicitly with tests.
