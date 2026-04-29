package com.k689.identid.interactor.dashboard

import com.k689.identid.model.storage.Bookmark
import com.k689.identid.model.storage.LoyaltyCard
import com.k689.identid.model.storage.loyaltyCardIdFromBookmarkId
import com.k689.identid.model.storage.loyaltyCardBookmarkId
import com.k689.identid.storage.dao.BookmarkDao
import com.k689.identid.storage.dao.LoyaltyCardDao
import com.k689.identid.util.business.convertMillisToDate
import kotlinx.coroutines.flow.Flow
import java.util.UUID

data class LoyaltyCardListItemUi(
    val id: String,
    val displayName: String,
    val barcodeValue: String,
    val barcodeFormat: String,
    val createdAtLabel: String,
)

data class LoyaltyCardDetailUi(
    val id: String,
    val displayName: String,
    val barcodeValue: String,
    val barcodeFormat: String,
    val createdAtLabel: String,
    val isBookmarked: Boolean,
)

interface LoyaltyCardsInteractor {
    fun observeLoyaltyCards(): Flow<List<LoyaltyCard>>

    suspend fun getAll(): List<LoyaltyCardListItemUi>

    suspend fun findExistingByBarcodeValue(barcodeValue: String): LoyaltyCardListItemUi?

    suspend fun getDetail(id: String): LoyaltyCardDetailUi?

    suspend fun save(displayName: String, barcodeValue: String, barcodeFormat: String): String

    suspend fun delete(id: String)

    suspend fun toggleBookmark(id: String): Boolean

    suspend fun getBookmarkedCards(): List<LoyaltyCard>
}

class LoyaltyCardsInteractorImpl(
    private val loyaltyCardDao: LoyaltyCardDao,
    private val bookmarkDao: BookmarkDao,
) : LoyaltyCardsInteractor {
    override fun observeLoyaltyCards(): Flow<List<LoyaltyCard>> = loyaltyCardDao.observeAll()

    override suspend fun getAll(): List<LoyaltyCardListItemUi> =
        loyaltyCardDao.retrieveAll().map { it.toListItemUi() }

    override suspend fun findExistingByBarcodeValue(barcodeValue: String): LoyaltyCardListItemUi? =
        loyaltyCardDao.retrieveByBarcodeValue(barcodeValue.trim())
            ?.toListItemUi()

    override suspend fun getDetail(id: String): LoyaltyCardDetailUi? {
        val card = loyaltyCardDao.retrieve(id) ?: return null
        return LoyaltyCardDetailUi(
            id = card.id,
            displayName = card.displayName,
            barcodeValue = card.barcodeValue,
            barcodeFormat = card.barcodeFormat,
            createdAtLabel = card.createdAt.toReadableDate(),
            isBookmarked = bookmarkDao.retrieve(loyaltyCardBookmarkId(card.id)) != null,
        )
    }

    override suspend fun save(
        displayName: String,
        barcodeValue: String,
        barcodeFormat: String,
    ): String {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        loyaltyCardDao.store(
            LoyaltyCard(
                id = id,
                displayName = displayName,
                barcodeValue = barcodeValue,
                barcodeFormat = barcodeFormat,
                createdAt = now,
                updatedAt = now,
            ),
        )
        return id
    }

    override suspend fun delete(id: String) {
        loyaltyCardDao.delete(id)
        bookmarkDao.delete(loyaltyCardBookmarkId(id))
    }

    override suspend fun toggleBookmark(id: String): Boolean {
        val bookmarkId = loyaltyCardBookmarkId(id)
        val existing = bookmarkDao.retrieve(bookmarkId)
        return if (existing == null) {
            bookmarkDao.store(Bookmark(bookmarkId))
            true
        } else {
            bookmarkDao.delete(bookmarkId)
            false
        }
    }

    override suspend fun getBookmarkedCards(): List<LoyaltyCard> {
        val bookmarkedIds =
            bookmarkDao.retrieveAll()
                .mapNotNull { bookmark -> loyaltyCardIdFromBookmarkId(bookmark.identifier) }
                .toSet()

        return loyaltyCardDao.retrieveAll().filter { it.id in bookmarkedIds }
    }
}

private fun LoyaltyCard.toListItemUi(): LoyaltyCardListItemUi =
    LoyaltyCardListItemUi(
        id = id,
        displayName = displayName,
        barcodeValue = barcodeValue,
        barcodeFormat = barcodeFormat,
        createdAtLabel = createdAt.toReadableDate(),
    )

private fun Long.toReadableDate(): String = convertMillisToDate(this)