package org.sjbtimdan.linden.model

data class Account(
    val id: Long,
    val name: String,
    val currency: Currency,
    val initialBalance: Long = 0,
    /** When true the account is kept in history but hidden from the ledger, pickers and filters. */
    val hidden: Boolean = false,
)
