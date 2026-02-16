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

package com.k689.identid.interactor.dashboard

import com.k689.identid.extension.business.safeAsync
import com.k689.identid.provider.UuidProvider
import com.k689.identid.util.business.FULL_DATETIME_PATTERN
import com.k689.identid.util.business.formatLocalDateTime
import com.k689.identid.extension.common.toExpandableListItems
import com.k689.identid.util.common.createKeyValue
import com.k689.identid.controller.core.WalletCoreDocumentsController
import com.k689.identid.extension.core.getLocalizedDocumentName
import com.k689.identid.extension.core.sortRecursivelyBy
import com.k689.identid.model.core.ClaimDomain
import com.k689.identid.model.core.ClaimPathDomain.Companion.toClaimPathDomain
import com.k689.identid.model.core.TransactionLogDataDomain
import com.k689.identid.model.core.TransactionLogDataDomain.Companion.getTransactionTypeLabel
import com.k689.identid.ui.dashboard.transactions.detail.model.TransactionDetailsCardUi
import com.k689.identid.ui.dashboard.transactions.detail.model.TransactionDetailsDataSharedHolderUi
import com.k689.identid.ui.dashboard.transactions.detail.model.TransactionDetailsUi
import com.k689.identid.ui.dashboard.transactions.model.TransactionStatusUi
import com.k689.identid.ui.dashboard.transactions.model.TransactionStatusUi.Companion.toUiText
import com.k689.identid.ui.dashboard.transactions.model.toTransactionStatusUi
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import eu.europa.ec.eudi.wallet.transactionLogging.TransactionLog
import eu.europa.ec.eudi.wallet.transactionLogging.presentation.PresentedDocument
import com.k689.identid.R
import com.k689.identid.provider.resources.ResourceProvider
import com.k689.identid.ui.component.AppIcons
import com.k689.identid.ui.component.ListItemDataUi
import com.k689.identid.ui.component.ListItemMainContentDataUi
import com.k689.identid.ui.component.ListItemTrailingContentDataUi
import com.k689.identid.ui.component.wrap.ExpandableListItemUi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import java.util.Locale

sealed class TransactionDetailsInteractorPartialState {
    data class Success(
        val transactionDetailsUi: TransactionDetailsUi,
    ) : TransactionDetailsInteractorPartialState()

    data class Failure(val error: String) : TransactionDetailsInteractorPartialState()
}

sealed class TransactionDetailsInteractorRequestDataDeletionPartialState {
    data object Success : TransactionDetailsInteractorRequestDataDeletionPartialState()
    data class Failure(
        val errorMessage: String
    ) : TransactionDetailsInteractorRequestDataDeletionPartialState()
}

sealed class TransactionDetailsInteractorReportSuspiciousTransactionPartialState {
    data object Success : TransactionDetailsInteractorReportSuspiciousTransactionPartialState()
    data class Failure(
        val errorMessage: String
    ) : TransactionDetailsInteractorReportSuspiciousTransactionPartialState()
}

interface TransactionDetailsInteractor {
    fun getTransactionDetails(
        transactionId: String
    ): Flow<TransactionDetailsInteractorPartialState>

    fun requestDataDeletion(transactionId: String): Flow<TransactionDetailsInteractorRequestDataDeletionPartialState>
    fun reportSuspiciousTransaction(transactionId: String): Flow<TransactionDetailsInteractorReportSuspiciousTransactionPartialState>
}

class TransactionDetailsInteractorImpl(
    private val walletCoreDocumentsController: WalletCoreDocumentsController,
    private val resourceProvider: ResourceProvider,
    private val uuidProvider: UuidProvider,
) : TransactionDetailsInteractor {

    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override fun getTransactionDetails(transactionId: String): Flow<TransactionDetailsInteractorPartialState> =
        flow {
            walletCoreDocumentsController.getTransactionLog(id = transactionId)
                ?.let { transaction ->

                    val userLocale = resourceProvider.getLocale()

                    val transactionUiStatus = transaction.status.toTransactionStatusUi()
                    val transactionUiDate = transaction.creationLocalDateTime.formatLocalDateTime(
                        pattern = FULL_DATETIME_PATTERN
                    )

                    val relyingPartyData: TransactionLog.RelyingParty?
                    val dataShared: List<ExpandableListItemUi.NestedListItem>?

                    when (transaction) {
                        is TransactionLogDataDomain.IssuanceLog -> {
                            //TODO change this once Core supports more transaction types
                            relyingPartyData = null
                            dataShared = null
                        }

                        is TransactionLogDataDomain.PresentationLog -> {
                            relyingPartyData = transaction.relyingParty

                            dataShared = transaction.documents.toGroupedNestedClaims(
                                documentSupportingText = resourceProvider.getString(R.string.transaction_details_collapsed_supporting_text),
                                itemIdentifierPrefix = resourceProvider.getString(R.string.transaction_details_data_shared_prefix_id),
                                userLocale = userLocale,
                                resourceProvider = resourceProvider,
                                uuidProvider = uuidProvider,
                            )
                        }

                        is TransactionLogDataDomain.SigningLog -> {
                            //TODO change this once Core supports more transaction types
                            relyingPartyData = null
                            dataShared = null
                        }
                    }

                    val transactionDetailsUi = TransactionDetailsUi(
                        transactionId = transactionId,
                        transactionDetailsCardUi = TransactionDetailsCardUi(
                            transactionTypeLabel = transaction.getTransactionTypeLabel(
                                resourceProvider
                            ),
                            transactionStatusLabel = transactionUiStatus.toUiText(resourceProvider),
                            transactionIsCompleted = when (transactionUiStatus) {
                                TransactionStatusUi.Completed -> true
                                TransactionStatusUi.Failed -> false
                            },
                            transactionDate = transactionUiDate,
                            relyingPartyName = relyingPartyData?.name,
                            relyingPartyIsVerified = relyingPartyData?.isVerified
                        ),
                        transactionDetailsDataShared = TransactionDetailsDataSharedHolderUi(
                            dataSharedItems = dataShared ?: emptyList()
                        ),
                        transactionDetailsDataSigned = null //TODO change this once Core adds support for it
                    )

                    emit(
                        TransactionDetailsInteractorPartialState.Success(
                            transactionDetailsUi = transactionDetailsUi
                        )
                    )
                } ?: emit(
                TransactionDetailsInteractorPartialState.Failure(
                    error = genericErrorMsg
                )
            )
        }.safeAsync {
            TransactionDetailsInteractorPartialState.Failure(
                error = it.localizedMessage ?: genericErrorMsg
            )
        }

    override fun requestDataDeletion(transactionId: String): Flow<TransactionDetailsInteractorRequestDataDeletionPartialState> {
        return flowOf(
            TransactionDetailsInteractorRequestDataDeletionPartialState.Success
        )
    }

    override fun reportSuspiciousTransaction(transactionId: String): Flow<TransactionDetailsInteractorReportSuspiciousTransactionPartialState> {
        return flowOf(
            TransactionDetailsInteractorReportSuspiciousTransactionPartialState.Success
        )
    }

    private fun List<PresentedDocument>.toGroupedNestedClaims(
        documentSupportingText: String,
        itemIdentifierPrefix: String,
        userLocale: Locale,
        resourceProvider: ResourceProvider,
        uuidProvider: UuidProvider
    ): List<ExpandableListItemUi.NestedListItem> {
        return this.mapIndexed { index, presentedDocument ->
            val domainClaims: MutableList<ClaimDomain> = mutableListOf()

            presentedDocument.claims.forEach { presentedClaim ->
                val elementIdentifier = when (presentedDocument.format) {
                    is MsoMdocFormat -> presentedClaim.path.last()
                    is SdJwtVcFormat -> presentedClaim.path.joinToString(".")
                }

                val itemPath = when (presentedDocument.format) {
                    is MsoMdocFormat -> listOf(elementIdentifier)
                    is SdJwtVcFormat -> presentedClaim.path
                }.toClaimPathDomain()

                createKeyValue(
                    item = presentedClaim.value!!,
                    groupKey = elementIdentifier,
                    disclosurePath = itemPath,
                    resourceProvider = resourceProvider,
                    claimMetaData = presentedClaim.metadata,
                    allItems = domainClaims,
                    uuidProvider = uuidProvider
                )
            }

            val uniqueId = itemIdentifierPrefix + index

            ExpandableListItemUi.NestedListItem(
                header = ListItemDataUi(
                    itemId = uniqueId,
                    mainContentData = ListItemMainContentDataUi.Text(
                        text = presentedDocument.metadata.getLocalizedDocumentName(
                            userLocale = userLocale,
                            fallback = presentedDocument.metadata?.display?.firstOrNull()?.name
                                ?: when (val format = presentedDocument.format) {
                                    is MsoMdocFormat -> format.docType
                                    is SdJwtVcFormat -> format.vct
                                }
                        )
                    ),
                    supportingText = documentSupportingText,
                    trailingContentData = ListItemTrailingContentDataUi.Icon(
                        iconData = AppIcons.KeyboardArrowDown
                    )
                ),
                nestedItems = domainClaims
                    .sortRecursivelyBy {
                        it.displayTitle.lowercase()
                    }.map { domainClaim ->
                        domainClaim.toExpandableListItems(docId = uniqueId)
                    },
                isExpanded = false
            )
        }
    }
}