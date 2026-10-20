package pl.milosz.warrantycontrol.model

data class Device(
    val id: Int,
    val name: String,
    val store: String,
    val purchaseDate: String,
    val warrantyMonths: Int
) {
    fun warrantyEndDate(): java.time.LocalDate {
        return java.time.LocalDate.parse(purchaseDate)
            .plusMonths(warrantyMonths.toLong())
    }

    fun isWarrantyActive(): Boolean {
        return !warrantyEndDate().isBefore(java.time.LocalDate.now())
    }

    fun daysUntilWarrantyEnds(): Long {
        return java.time.temporal.ChronoUnit.DAYS.between(
            java.time.LocalDate.now(),
            warrantyEndDate()
        )
    }
}
