package io.pleo.antaeus.core.exceptions

class SideUpdateException(invoiceId: Int) :
    Exception("Invoice with invoiceId = '$invoiceId' has been already paid")
