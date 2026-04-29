/*
 * Copyright (c) 2026 European Commission
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

package com.k689.identid.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.k689.identid.R
import com.k689.identid.app.MainActivity
import com.k689.identid.controller.core.WalletCoreDocumentsController
import com.k689.identid.provider.resources.ResourceProvider
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Instant

class ExpiryNotificationWorkManager(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams),
    KoinComponent {
    private val walletCoreDocumentsController: WalletCoreDocumentsController by inject()
    private val resourceProvider: ResourceProvider by inject()

    companion object {
        const val EXPIRY_WORK_NAME = "expiryWorker"
        const val CHANNEL_ID = "document_expiry_channel"
        const val NOTIFICATION_ID = 1001
    }

    override suspend fun doWork(): Result {
        try {
            val documents = walletCoreDocumentsController.getAllDocuments()
            val now = Instant.now()

            var hasExpiringDocuments = false

            for (document in documents) {
                when {
                    document is IssuedDocument -> {
                        val expiryDate = document.getValidUntil().getOrNull()
                        if (expiryDate != null) {
                            val secondsUntilExpiry = expiryDate.epochSecond - now.epochSecond
                            if (secondsUntilExpiry in 0..(3 * 24 * 3600)) {
                                hasExpiringDocuments = true
                                break
                            }
                        }
                    }
                }
            }

            if (hasExpiringDocuments) {
                sendNotification(
                    R.string.expired_document_notification,
                    R.string.expired_document_notification_description,
                )
            }
            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }

    private fun sendNotification(
        @StringRes title: Int,
        @StringRes content: Int,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(applicationContext, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notificationManager = NotificationManagerCompat.from(applicationContext)

        val channel =
            NotificationChannel(
                CHANNEL_ID,
                "Document Expiry",
                NotificationManager.IMPORTANCE_DEFAULT,
            )
        notificationManager.createNotificationChannel(channel)

        val intent =
            Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

        val pendingIntent: PendingIntent =
            PendingIntent.getActivity(
                applicationContext,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE,
            )

        val builder =
            NotificationCompat
                .Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(resourceProvider.getString(title))
                .setContentText(resourceProvider.getString(content))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }
}
