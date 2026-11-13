---
title: Corsa status
tags:
  - projects/corsa
---

# CORSA status

Snapshot 2026-11-13. Vehicle profile CRUD, vehicle document management, and a Room-aware partner dashboard shell are implemented. The current working tree passes local unit tests, debug assembly, and Android lint. The Android app uses Kotlin, minSdk 24, and targetSdk 37.

[[Projects/Corsa/Overview|Overview]] · [[Projects/Corsa/progress|Progress]] · [[Projects/Corsa/decisions|Decisions]]

## Completed work and current state

- On 2026-11-13, the bottom bar contains Dashboard and Settings. The Dashboard toolbar Settings button, My Cars tab, and Analytics tab were removed. A populated Dashboard offers View My Cars; vehicle editor and details return to Dashboard when the list is absent from the back stack. Settings and Analytics remain stub destinations in the navigation graph. Three focused Android 17 navigation tests passed, as did unit tests, debug assembly, and lint. The full connected suite compiled but could not start because the emulator reported an unknown API level.
- On 2026-11-12, added the populated Dashboard: observed vehicle selection, neutral brand initial, optional plate, prominent odometer, bounded private-photo loading with silhouette fallback, and separate gradient cards. The original empty-state XML subtree, shared theme, toolbar, bottom navigation, and editor navigation are unchanged.
- Maintenance schedules/urgency, monthly PHP spending, and fuel efficiency explicitly show unavailable or insufficient-data copy. Service/fuel actions are disabled and labeled unavailable; Add vehicle and the selector's Add another vehicle use the existing editor. No new persistence tables or invented totals/statuses were added.

- The owner supplied the product brief, course requirements, and proposed stack.
- Cars first is confirmed.
- The four project notes define the architecture, five-table model, package ownership, navigation, calculation contracts, and implementation sequence.
- The owner selected Lay's Dribbble reference and requested apple-design guidance. Overview records the visual direction, theme tokens, component mapping, and responsive compositions.
- Confirmed Android application repository at `/Users/aaronarada/AndroidStudioProjects/Corsa`.
- Defined Spec-Driven Development (SDD) methodology and equal two-person task allocation: Person A (Vehicle & Maintenance pillar, DB setup, Dashboard) and Person B (Fuel, Expenses, Design Tokens/NavGraph, Analytics).
- On 2026-11-11, completed the vehicle profile CRUD plan and executable checklist under `tasks-personA/`. The app now has a single application-owned Room database/repository, Navigation host, vehicle list, add/edit form, details, retryable storage states, and confirmed targeted deletion.
- Room schema version 1 persists vehicle rows with nullable plate and photo path fields. Repository validation covers required text, model-year bounds, mileage parsing/overflow, normalization, missing edits, and idempotent missing deletes.
- Vehicle drafts survive saved-state recreation, repeated saves are guarded, navigation passes only primitive IDs, and observed list/details screens update from Room.
- Phase 0 duplication was reduced: unique navigation and ownership contracts remain in `tasks-personA/plan.md`; the duplicate repository glossary, contract-freeze file, and four ADR copies were removed because the vault remains the product/decision authority.
- Partner `origin/feature/dashboard-design` visual language remains the app look: forced dark empty-dashboard theme, partner copy and blue CTA, while Room-backed vehicle CRUD remains on the Navigation host. Its original three-tab chrome has been superseded by the two-tab bar above.
- Analytics and Settings are stub destinations so the partner chrome works without inventing those features yet.
- Verification passed on Android 17 emulators: unit tests, debug assembly, connected Android tests 17/17, and live UI dump confirming logo, Settings, empty-garage copy, Add vehicle CTA, and all three tabs.
- Vehicle document management is present in Room schema version 2 with an explicit migration, repository operations, private attachment handling, an editor, and a history tab in Vehicle Details.
- On 2026-11-12, reconciled the local project-knowledge commit with the 27 remote `main` commits. The merged tree keeps the API-27 light-navigation-bar setting in a version-qualified resource so minSdk 24 remains supported; unit tests, debug assembly, and lint all pass.

## Highest-priority next action

Begin the maintenance slice on top of the verified vehicle identity and mileage foundation: define the version 2 migration and maintenance entity/DAO tests before adding maintenance screens.

## Blockers and unknowns

Instructor approval status and specific delivery deadline date remain unrecorded. Work allocation, repository location, and single currency (PHP / centavos) are resolved.

## Proposed implementation sequence

| Unit | Implementation scope | Completion evidence |
| --- | --- | --- |
| 1 | Project setup, Room schema, car list/details/editor, optional car photo | Create, view, update, and delete a car. Reopen the database and verify persistence and isolation between cars. |
| 2 | Maintenance logs, rule CRUD, baselines, pure status calculator | Validate missing baseline, exact deadline, overdue state, either-limit behavior, and recalculation after history edits or deletion. |
| 3 | Fuel CRUD, full/partial/gap states, efficiency calculator | Verify full-partial-full calculations, missing anchors, gaps, historical insertion, zero distance, and weighted period totals. |
| 4 | Manual expenses and unified spending query | Show each source once. Verify edit/delete totals, category totals, and monthly/yearly date boundaries. |
| 5 | Dashboard, car filters, basic charts, settings | Complete the main workflow with empty states, field errors, and readable chart summaries. |
| 6 | Integration checks and course evidence | Record test environment, expected results, actual results, defects, and retest outcomes. |

The order is an architecture recommendation. Team assignments and a delivery schedule are not agreed here.

## Checks required before calling the core complete

- Unit-test maintenance statuses at, before, and after each enabled limit, including rules with no history.
- Unit-test fuel intervals and aggregate efficiency. Verify that excluded efficiency records still contribute to fuel spending.
- Test Room CRUD for all five tables, foreign keys, cross-car rule rejection, transactional rollback, and database reopen.
- Verify confirmed car deletion removes only its related records. Verify rule deletion retains service history.
- Verify historical mileage validation and that deleting a log does not lower known current mileage.
- Drive a car/service/fuel/expense workflow on an emulator or device, restart the app, and verify saved records and totals.
- Check form draft preservation, storage-error handling, navigation to deleted records, accessible labels, and missing photos.
- Check rendered light/dark screens, text contrast, 48dp touch targets, large font scales, TalkBack order, and keyboard avoidance against the documented design direction.
- Test explicit migrations whenever a released database schema changes.

Vehicle-only checks in this list have been executed, and the partner dashboard chrome is verified for empty garage, garage count, retryable load failure, Settings/Analytics stubs, and three-tab navigation. Maintenance, fuel, expense, real analytics, and cross-table checks remain future work.

## Items needing owner review

Inspect the repository at `/Users/aaronarada/AndroidStudioProjects/Corsa` and confirm its current code state, approval status, team and timeline, minimum Android version, and single currency. Choose service warning defaults and a chart library before implementing those screens. Exact service intervals remain car-specific configuration.

Optional documents, depreciation, background reminders, advanced analytics, and motorcycle support stay outside the initial implementation plan until scope changes.

## Snapshot evidence

The owner's brief and 2026-11-09 clarifications establish the product direction, completed proposal, and cars-first choice. The architecture in [[Projects/Corsa/Overview|Overview]] and [[Projects/Corsa/decisions|Decisions]] was created in this session. Vault file and link checks are recorded in [[Projects/Corsa/progress|Progress]].
