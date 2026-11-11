package com.corsa.app.calculation

/**
 * Status of a maintenance rule based on current vehicle mileage and date.
 */
enum class MaintenanceStatus {
    /**
     * Rule lacks necessary historical or baseline anchors to calculate a deadline.
     */
    NeedsBaseline,

    /**
     * Rule is configured and active, but deadlines are beyond the warning thresholds.
     */
    Scheduled,

    /**
     * Vehicle mileage or date is within the configured warning threshold of the deadline.
     */
    DueSoon,

    /**
     * Vehicle mileage or date has reached the deadline exactly.
     */
    DueNow,

    /**
     * Vehicle mileage or date has exceeded the deadline.
     */
    Overdue
}
