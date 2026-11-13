---
title: Corsa decisions
tags:
  - projects/corsa
---

# CORSA decisions

## Preserve the empty dashboard while adding selected-vehicle content

Confirmed by the owner on 2026-11-12. Preserve the current empty dashboard and its dark theme/chrome exactly. Add a separate scrolling populated layout with a vehicle selector, neutral initial badge, existing private photo or unbranded silhouette, and real odometer. Use restrained opaque gradients and borders for the requested glass-like cards to keep contrast predictable without blur.

Vehicle identity and mileage come from the existing repository observation. Maintenance, fuel, and expense persistence are absent, so those sections explicitly report unavailable data, with disabled service/fuel actions. Document costs are not presented as complete ownership spending. Existing editor navigation remains unchanged; returning to Dashboard displays the observed saved vehicle.

Recorded 2026-11-09 from the owner's brief and clarifications. Historical decision dates are unknown unless stated below. Architecture choices are recommendations awaiting implementation, not claims about existing code.

[[Projects/Corsa/Overview|Overview]] · [[Projects/Corsa/STATUS|Status]] · [[Projects/Corsa/progress|Progress]]

## Cars first

Confirmed by the owner on 2026-11-09. The first version supports multiple cars. The original brief considered both cars and motorcycles. Motorcycle support is deferred, reducing the initial feature and test scope. That rationale is architecture analysis, not an owner-stated reason. Reconsider when the owner expands scope.

## Architecture documentation rather than another proposal

Confirmed by the owner on 2026-11-09. The proposal is already complete. These four notes define the app architecture and track future implementation. They do not establish that the instructor has approved development. No alternative deliverable is needed for this task.

## Keep the supplied technology direction

Kotlin, XML with Material components, Room over SQLite, and MVVM are documented in the owner's brief. Original choice dates and alternative evaluations are unknown. Local persistence is a course requirement. The proposed design adds a single Activity, Fragments, Flow, and manual dependency construction. Reconsider these additions if the existing app repository has established conventions.

## One app module with a repository and pure calculators

Recommended on 2026-11-09. Use one `:app` module with feature UI packages, a shared repository, Room storage, and pure maintenance and fuel calculators. ViewModels orchestrate screen state. Repository operations own validation and atomic persistence.

Two candidate structures were evaluated. The compact structure is the base because it hides transaction coordination without requiring use-case wrappers and three build modules. The layered alternative used separate app, domain, and data modules with repository ports. That alternative enforces imports more strongly but adds contracts and mapping for a small local app.

Keep the alternative's explicit save outcomes and missing-record behavior. Reject universal use-case wrappers for CRUD. Accept package-level discipline instead of compiler-enforced module boundaries. Reconsider when independently developed modules or another data implementation create a concrete need. No implementation deviations have occurred because no app code was changed.

An independent comparison also favored the compact structure and identified service ordering, incomplete baselines, and chronology validation as contracts to clarify. Those contracts are now explicit in Overview. The candidate drafts and comparison are design analysis from this session, not application evidence.

## Store each cost once

Recommended on 2026-11-09. Fuel and maintenance logs own their costs. Manual expenses cover other categories. A unified read query produces spending lists and totals.

The alternative was a universal expense ledger with linked detail records. It centralizes money but adds coordination to every source save and deletion. The chosen design avoids duplicated companion expense rows. Trade-off is a more involved read query. Reconsider if accounting requirements need transactions spanning multiple categories or currencies.

## Calculate maintenance from explicit anchors

Rule-based prediction rather than machine learning is documented in the brief. Recommended details are mileage and calendar intervals, the latest linked service or explicit baseline, and visible `NeedsBaseline`, `Scheduled`, `DueSoon`, `DueNow`, and `Overdue` states.

Either enabled deadline can trigger maintenance. Do not invent service history or use fixed universal intervals. Driving-rate calendar forecasts are deferred because sparse mileage records can produce misleading dates. Reconsider forecasts only with an agreed sampling and confidence rule. The formulas and boundary examples live in [[Projects/Corsa/Overview#Maintenance calculation contract|Overview]].

## Use full-tank fuel intervals

Recommended on 2026-11-09. Full-to-full intervals include partial fills and exclude the opening fill's liters. Explicit gaps break measurement continuity. Period efficiency uses total distance divided by total liters.

Dividing each distance increment by its individual purchase liters was considered and rejected because partial fills do not measure consumption for that increment. Trade-off is that efficiency remains unavailable until a valid interval closes. Reconsider only if another measurement method provides reliable consumption data.

## Deliver the core before optional features

The brief prioritizes vehicles, maintenance, fuel, expenses, dashboard/basic analytics, and rule-based predictions. Treat that as the proposed initial scope. Documents, depreciation, notifications, and advanced analytics remain deferred. Owner agreement on exact delivery milestones is still needed.

This keeps required CRUD and the differentiating feature within one demonstrable workflow. Reconsider optional scope after core persistence and calculation checks pass and available time is known. Cloud sync, accounts, and external vehicle integrations are not part of the supplied initial scope.

## Use the selected automotive visual reference

Confirmed by the owner on 2026-11-09. Use [Lay's car maintenance app concept](https://dribbble.com/shots/27346135-A-car-maintenance-app-concept) as the visual direction. The owner also explicitly requested the apple-design skill.

The recommended translation is a dark mechanical palette, strong mileage typography, and a prominent car image, with native Material controls. Apple design principles guide hierarchy, accessibility, and restrained branding. They do not change the app's Android platform or XML stack.

The light appearance, token values, component mapping, and compact/wider compositions in Overview are architecture recommendations. A static car image is preferred over a runtime 3D renderer. Keep real maintenance statuses rather than introducing an undefined health score. Reconsider the rendering approach only when a required interaction needs it.

## Adopt Spec-Driven Development (SDD) and two-person parallel workstreams

Confirmed by the owner on 2026-11-10. Adopt Spec-Driven Development (SDD) to structure implementation across a two-person team. No production code is written until data schemas, API contracts, mathematical specifications, and acceptance criteria are agreed upon and test cases are authored.

Work is partitioned into two equal pillars to minimize merge conflicts and eliminate blocking dependencies:
- **Person A (Vehicle & Maintenance Pillar)**: Owns Room schema setup, `vehicle`, `maintenance_rule`, and `maintenance_log` tables/DAOs, pure `MaintenanceCalculator` (with 100% unit tests), Vehicle & Maintenance CRUD ViewModels/Fragments, and the high-level Dashboard Fragment and alert coordinator.
- **Person B (Fuel, Expenses & Analytics Pillar)**: Owns Design system tokens (dark/light), Material themes, Navigation Component graph, `fuel_log` and `expense` tables/DAOs, pure `FuelCalculator` (with 100% unit tests), unified `UNION ALL` spending query, Fuel & Expense CRUD ViewModels/Fragments, and the Analytics Fragment.

Contracts for shared boundaries (`CorsaDatabase`, `CorsaRepository`, navigation args, and pure calculator signatures) are frozen in Phase 0 before feature implementation.

## Single currency: Philippine Peso (PHP, ₱)

Confirmed by the owner on 2026-11-10. Corsa uses Philippine Peso (PHP, `₱`) as its single application currency.

All financial amounts are stored in Room and calculated in memory as 64-bit integer minor currency units (`costMinor: Long`, centavos, where 100 centavos = ₱1.00). User decimal input (e.g. `150.50`) is parsed into integer minor units at the form boundary, and rendered with the `₱` symbol (e.g. `₱150.50`). Floating-point types are strictly forbidden for currency storage and summation to eliminate precision drift.


## Build vehicle profile CRUD first

Planned on 2026-11-11 as the first complete Person A feature. The slice covers local create, list, view, edit, and delete behavior for vehicle profiles. It establishes the vehicle identity and mileage data that maintenance needs.

The Room entity keeps the agreed nullable photoPath column, but photo picking and file handling remain deferred. Person B retains ownership of AppContainer and the navigation graph. Person A integrates vehicle destinations through the frozen shared contract instead of creating a second app shell.

The repository routes Room compilation through KSP. No repository interface, use-case layer, dependency injection framework, Safe Args, ViewBinding, or adapter library was added for this feature.
Vehicle data uses three shapes. VehicleDraft keeps raw form strings so the repository can report parsing and overflow errors by field. VehicleEntity keeps typed Room columns. Vehicle is the immutable typed read model exposed to ViewModels. This keeps parsing at one write boundary and prevents Room types from leaking into the UI.

Implemented on 2026-11-11. Because no competing Person B shell existed, the vehicle slice supplied the single application-owned `AppContainer` and Navigation host rather than leaving a non-runnable feature. Three destination-specific Fragments and ViewModels were selected over one conditional controller to keep lifecycle, restoration, and destructive-operation state local and testable. Navigation continues to pass only primitive vehicle IDs.

The repository-level Phase 0 glossary, contract-freeze file, and ADR copies were removed after unique navigation/ownership details were retained in `tasks-personA/plan.md`. This avoids a second decision authority; this vault remains authoritative for product intent and rationale.

## Integrate partner dashboard as a functional Room-aware destination

Decided on 2026-11-11 after inspecting `origin/feature/dashboard-design`. Preserve the partner design language and artwork, but do not replace the working application shell with the static Activity prototype.

Chosen shape:
- `DashboardFragment` + `DashboardViewModel` observe `CorsaRepository` and render empty, garage-count, loading, and retryable error states;
- bottom navigation has two real destinations only: Dashboard and My Cars;
- partner graphite/radial surfaces, blue actions, and large-text-safe vehicle rows apply through shared DayNight semantic colors rather than a forced dark empty-dashboard theme;
- Analytics/Settings controls and IDE-only partner files stay out until those features exist.

Rejected wholesale checkout of the partner branch because it disconnected Room-backed vehicle CRUD and referenced a missing navigation menu. A real merge commit keeps partner history visible while the functional shell remains the runtime authority.

## Prefer partner dashboard visuals over the adapted DayNight shell

Confirmed by the owner on 2026-11-11 after comparing the running app with the partner Android Studio design preview. Use the partner dark empty-dashboard look, chrome, copy, CTA styling, and three-tab bar as the visual source of truth.

Keep the Room-aware Fragment/Navigation implementation underneath. Analytics and Settings remain stub destinations until those features are built. Reconsider restoring a light appearance only if the owner asks for DayNight again.

## Simplify primary navigation to Dashboard and Settings

Decided by the owner on 2026-11-13. Remove the top-right Settings control and place Settings in the bottom navigation position previously occupied by Analytics. Remove My Cars from the bottom bar. Keep vehicle management reachable through a View My Cars action on the populated Dashboard, and retain the vehicle list and detail routes. Analytics stays a stub route for future work but is not a primary tab. This supersedes the earlier three-tab chrome decision without changing the partner palette or Room-backed shell.

## ## Include vehicle document management in active scope

Confirmed by the owner on 2026-11-11. Vehicle document management is added back to the active scope.

The feature lets users store registration, insurance, and service records locally to prepare for vehicle resale and eliminate manual paper tracking.

Add a `vehicle_document` table to Room. It links to `vehicle.id` with a cascading delete foreign key. Fields include `id`, `vehicleId`, `title`, `documentType`, `documentNumber`, `issueDate`, `expirationDate`, `costMinor`, `filePath`, `physicalLocation`, and `description`. Users can specify where physical copies are stored (e.g., glove compartment, home safe) and describe the document's purpose. Financial costs map to `costMinor` using 64-bit integer minor currency units. File attachments store app-private storage paths in `filePath`.

`CorsaRepository` exposes `saveDocument`, `deleteDocument`, and `observeVehicleDocuments(vehicleId)`. Document History renders as a tab inside Vehicle Details, displaying storage locations and document notes alongside digital previews. Expiration dates trigger warnings. Document costs feed into the unified spending query under manual expenses.

Room schema version bumps from 1 to 2. An explicit migration creates `vehicle_document` with indices without clearing existing data.

