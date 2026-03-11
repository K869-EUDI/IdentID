/*
 * Copyright (c) 2025 European Commission
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European
 * Commission - subsequent versions of the EUPL (the "Licence"); You may not use this work
 * except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific language
 * governing permissions and limitations under the Licence.
 */

package com.k689.identid.di.transfer

import com.k689.identid.controller.core.WalletCoreDocumentsController
import com.k689.identid.controller.storage.PinStorageController
import com.k689.identid.controller.transfer.NearbyTransferManager
import com.k689.identid.controller.transfer.NearbyTransferManagerImpl
import com.k689.identid.controller.transfer.TransferSessionManager
import com.k689.identid.controller.transfer.TransferSessionManagerImpl
import com.k689.identid.controller.transfer.WalletTransferController
import com.k689.identid.controller.transfer.WalletTransferControllerImpl
import com.k689.identid.interactor.transfer.MoveWalletInteractor
import com.k689.identid.interactor.transfer.MoveWalletInteractorImpl
import com.k689.identid.interactor.transfer.ReceiveWalletInteractor
import com.k689.identid.interactor.transfer.ReceiveWalletInteractorImpl
import com.k689.identid.provider.resources.ResourceProvider
import com.k689.identid.storage.dao.BookmarkDao
import com.k689.identid.storage.dao.RevokedDocumentDao
import com.k689.identid.storage.dao.TransactionLogDao
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@ComponentScan("com.k689.identid.ui.transfer", "com.k689.identid.di.transfer")
class FeatureTransferModule

@Single
fun provideNearbyTransferManager(): NearbyTransferManager = NearbyTransferManagerImpl()

@Single
fun provideTransferSessionManager(): TransferSessionManager = TransferSessionManagerImpl()

@Single
fun provideWalletTransferController(
    walletCoreDocumentsController: WalletCoreDocumentsController,
    bookmarkDao: BookmarkDao,
    transactionLogDao: TransactionLogDao,
    revokedDocumentDao: RevokedDocumentDao,
): WalletTransferController =
    WalletTransferControllerImpl(
        walletCoreDocumentsController,
        bookmarkDao,
        transactionLogDao,
        revokedDocumentDao,
    )

@Single
fun provideMoveWalletInteractor(
    resourceProvider: ResourceProvider,
    walletTransferController: WalletTransferController,
    nearbyTransferManager: NearbyTransferManager,
    transferSessionManager: TransferSessionManager,
): MoveWalletInteractor =
    MoveWalletInteractorImpl(
        resourceProvider,
        walletTransferController,
        nearbyTransferManager,
        transferSessionManager,
    )

@Factory
fun provideReceiveWalletInteractor(
    resourceProvider: ResourceProvider,
    walletCoreDocumentsController: WalletCoreDocumentsController,
    walletTransferController: WalletTransferController,
    nearbyTransferManager: NearbyTransferManager,
    transferSessionManager: TransferSessionManager,
    bookmarkDao: BookmarkDao,
    transactionLogDao: TransactionLogDao,
    revokedDocumentDao: RevokedDocumentDao,
): ReceiveWalletInteractor =
    ReceiveWalletInteractorImpl(
        resourceProvider,
        walletCoreDocumentsController,
        walletTransferController,
        nearbyTransferManager,
        transferSessionManager,
        bookmarkDao,
        transactionLogDao,
        revokedDocumentDao,
    )
