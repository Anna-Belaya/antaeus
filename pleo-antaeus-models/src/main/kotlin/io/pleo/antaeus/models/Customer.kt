package io.pleo.antaeus.models

import java.math.BigDecimal

data class Customer(
    val id: Int,
    val currency: Currency,
    val balance: BigDecimal
)
