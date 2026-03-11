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

package com.k689.identid.service

import android.app.Activity
import android.content.ComponentName
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation

object NfcServiceModeController {
    private fun setPreferredService(
        activity: Activity,
        serviceClass: Class<*>,
    ) {
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return
        val cardEmulation = CardEmulation.getInstance(adapter)
        val component = ComponentName(activity, serviceClass)
        cardEmulation.setPreferredService(activity, component)
    }

    private fun unsetPreferredService(activity: Activity) {
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return
        val cardEmulation = CardEmulation.getInstance(adapter)
        cardEmulation.unsetPreferredService(activity)
    }

    fun activateTransferMode(activity: Activity) {
        setPreferredService(activity, TransferNfcEngagementService::class.java)
    }

    fun activateCredentialSharingMode(activity: Activity) {
        unsetPreferredService(activity)
        setPreferredService(activity, NfcEngagementService::class.java)
    }
}
