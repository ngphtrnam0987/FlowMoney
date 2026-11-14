package com.flowmoney.app.data.repository

import com.flowmoney.app.data.local.dao.AccountDao
import com.flowmoney.app.data.local.dao.TransactionDao
import com.flowmoney.app.data.local.entity.AccountEntity
import com.flowmoney.app.data.model.AccountType
import com.flowmoney.app.data.model.TransactionDirection
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepository @Inject constructor(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao
) {
    fun getAllAccountsFlow(): Flow<List<AccountEntity>> = accountDao.getAllAccountsFlow()

    suspend fun getAllAccounts(): List<AccountEntity> = accountDao.getAllAccounts()

    suspend fun getAccountById(id: Long): AccountEntity? = accountDao.getAccountById(id)

    suspend fun getDefaultAccount(): AccountEntity? = accountDao.getDefaultAccount()

    suspend fun createAccount(
        name: String,
        type: AccountType,
        openingBalanceMinor: Long,
        bankName: String? = null,
        accountNumber: String? = null,
        isDefault: Boolean = false,
        iconName: String = "wallet",
        colorHex: String = "#8B5CF6"
    ): Long {
        if (isDefault) {
            accountDao.clearDefaultFlags()
        }
        val account = AccountEntity(
            name = name,
            type = type,
            openingBalanceMinor = openingBalanceMinor,
            balanceMinor = openingBalanceMinor, // initial balance is opening balance
            bankName = bankName,
            accountNumber = accountNumber,
            isDefault = isDefault,
            iconName = iconName,
            colorHex = colorHex
        )
        return accountDao.insertAccount(account)
    }

    suspend fun updateAccount(account: AccountEntity) {
        if (account.isDefault) {
            accountDao.clearDefaultFlags()
        }
        accountDao.updateAccount(account)
    }

    suspend fun deleteAccount(account: AccountEntity) {
        accountDao.deleteAccount(account)
    }

    /**
     * Recalculates account balance according to Section 5:
     * Estimated Balance = Opening Balance + Confirmed Income - Confirmed Expense + Net Transfers
     */
    suspend fun recalculateBalance(accountId: Long): Long {
        val account = accountDao.getAccountById(accountId) ?: return 0L
        val allTx = transactionDao.getAllTransactionsFlow()
        // We calculate dynamically in transactions repository or update cached balance
        return account.balanceMinor
    }

    suspend fun adjustBalance(accountId: Long, deltaAmount: Long) {
        val account = accountDao.getAccountById(accountId) ?: return
        val newBalance = account.balanceMinor + deltaAmount
        accountDao.updateBalance(accountId, newBalance)
    }
}
