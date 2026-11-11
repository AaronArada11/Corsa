# Consecutive Full-Tank Anchoring with Explicit Gaps

We calculate fuel efficiency strictly across intervals bounded by consecutive `FULL` tank states, accumulating intervening `PARTIAL` fills, excluding the opening fill volume from the consumption divisor, and providing an explicit `GAP` tank state to reset measurement continuity.

Directly calculating kilometers-per-liter on individual partial fills yields inaccurate consumption figures because tank fullness is unknown; anchoring by full tanks guarantees exact physical fuel consumption while `GAP` allows recording expenditures without poisoning efficiency metrics.
