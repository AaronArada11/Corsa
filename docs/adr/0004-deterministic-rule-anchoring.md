# Deterministic Rule Anchoring Without Driving-Rate Forecasts

Next maintenance deadlines are calculated solely from the latest completed maintenance log (or explicit rule baseline) using deterministic distance and calendar thresholds, deliberately rejecting daily driving-rate extrapolation or machine-learning date forecasts.

Sparse or intermittent odometer observations produce volatile daily distance averages that output erratic, misleading calendar estimates for vehicle owners; deterministic dual-anchor evaluation provides clear, explainable statuses (`NeedsBaseline`, `Scheduled`, `DueSoon`, `DueNow`, `Overdue`) that recompute reliably on app resume and odometer updates.
