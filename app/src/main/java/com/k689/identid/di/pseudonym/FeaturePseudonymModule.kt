package com.k689.identid.di.pseudonym

import com.k689.identid.controller.crypto.PseudonymKeyManager
import com.k689.identid.controller.pseudonym.PseudonymRepository
import com.k689.identid.controller.pseudonym.PseudonymTransactionLogger
import com.k689.identid.interactor.pseudonym.PseudonymInteractor
import com.k689.identid.interactor.pseudonym.PseudonymInteractorImpl
import com.k689.identid.storage.dao.PseudonymDao
import com.k689.identid.storage.dao.PseudonymTransactionLogDao
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@ComponentScan("com.k689.identid.ui.pseudonym", "com.k689.identid.di.pseudonym")
class FeaturePseudonymModule

@Single
fun providePseudonymKeyManager(): PseudonymKeyManager = PseudonymKeyManager()

@Single
fun providePseudonymRepository(
    pseudonymDao: PseudonymDao,
    keyManager: PseudonymKeyManager,
): PseudonymRepository = PseudonymRepository(pseudonymDao, keyManager)

@Single
fun providePseudonymTransactionLogger(
    dao: PseudonymTransactionLogDao,
): PseudonymTransactionLogger = PseudonymTransactionLogger(dao)

@Factory
fun providePseudonymInteractor(
    pseudonymRepository: PseudonymRepository,
    transactionLogger: PseudonymTransactionLogger,
): PseudonymInteractor = PseudonymInteractorImpl(pseudonymRepository, transactionLogger)
