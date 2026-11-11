# Integer Minor Currency Units for Financial Records

All financial amounts in Room tables, domain models, and aggregations are represented as 64-bit signed integers in minor currency units (`costMinor: Long`, centavos, where 100 centavos = ₱1.00 PHP).

Floating-point representations (`Float` or `Double`) accumulate precision and rounding errors across summations; storing money as integer minor units eliminates numerical drift while converting to decimal only at user input and display boundaries.
