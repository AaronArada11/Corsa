# Single Source Spending via Unified Query

We store transaction costs exclusively within their native domain tables (`fuel_log`, `maintenance_log`, and `expense`) and project a unified spending view through an on-demand `UNION ALL` query, rather than synchronizing a central expense ledger.

A synchronized ledger duplicates financial records and invites consistency bugs whenever fuel or service entries are updated or deleted; the unified query guarantees a single source of truth and eliminates duplicate financial bookkeeping.
