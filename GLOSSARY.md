# Corsa Domain Glossary

Corsa is a local-first Android vehicle ownership application for tracking vehicle records, rule-based maintenance schedules, fuel efficiency, and total ownership costs.

## Vehicle

**Vehicle**:
A motorized passenger automobile tracked in the application.
_Avoid_: Car, Automobile, Profile, Asset

**Odometer**:
The cumulative distance travelled by a vehicle expressed in whole kilometers.
_Avoid_: Mileage counter, Distance reading, Odo

**Plate Number**:
The alphanumeric registration identifier assigned to a vehicle by a licensing authority.
_Avoid_: License plate, Conduction sticker, Tag, Rego

## Maintenance

**Maintenance Rule**:
A recurring service policy configured for a vehicle by distance, elapsed time, or both.
_Avoid_: Service schedule, Interval rule, Reminder rule, Inspection plan

**Maintenance Log**:
A historical record of a completed service, inspection, or repair performed on a vehicle.
_Avoid_: Service entry, Repair ticket, Service record, Work order

**Baseline**:
An explicit starting distance and calendar date used to calculate deadlines for a maintenance rule before any service is recorded.
_Avoid_: Initial anchor, Starting point, Default service

**Anchor**:
The reference point—either the latest linked maintenance log or an explicit baseline—from which the next deadline is calculated.
_Avoid_: Reference mark, Origin, Pivot

**Warning Threshold**:
The lead distance in kilometers or lead time in days before a deadline at which an upcoming service enters warning status.
_Avoid_: Alert window, Lead time, Notice period

**Maintenance Status**:
The lifecycle state of a maintenance rule: `NeedsBaseline`, `Scheduled`, `DueSoon`, `DueNow`, or `Overdue`.
_Avoid_: Vehicle health score, Urgency index, Service priority

## Fuel & Efficiency

**Fuel Log**:
A record of a fuel purchase capturing odometer reading, volume in liters, total cost, and tank state.
_Avoid_: Gas receipt, Fill-up entry, Refuel record

**Tank State**:
The fill condition of the fuel tank at purchase: `FULL` (filled to brim), `PARTIAL` (incomplete fill), or `GAP` (unrecorded history).
_Avoid_: Fuel level, Fill type, Tank condition

**Efficiency Segment**:
The distance travelled between two consecutive `FULL` tank states, divided by the fuel consumed across that interval.
_Avoid_: Consumption cycle, Mileage interval, Fuel economy stretch

**GAP**:
A declared disruption or unrecorded period in fuel history that resets efficiency calculation without discarding cost history.
_Avoid_: Missing log, Void interval, Reset marker

## Financials & Spending

**Expense**:
A monetary cost of vehicle ownership that is neither a fuel purchase nor a maintenance service (such as insurance, registration, tolls, parking, or accessories).
_Avoid_: Misc cost, Ledger item, Out-of-pocket, General fee

**Spending Item**:
A single line item in the unified view of all vehicle costs across fuel purchases, maintenance logs, and expenses.
_Avoid_: Transaction, Ledger entry, Expense report

**Minor Currency Unit**:
Financial amounts represented as 64-bit integers in Philippine centavos (where 100 centavos equals ₱1.00).
_Avoid_: Decimal currency, Float balance, Dollar cents
