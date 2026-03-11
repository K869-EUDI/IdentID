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

import android.nfc.cardemulation.HostApduService
import android.os.Bundle

class TransferNfcEngagementService : HostApduService() {
    companion object {
        private const val SW_OK = "9000"
        private const val SW_FAIL = "6F00"
        private const val INS_GET_DATA = "CA"
        private const val AID = "F0010203040506"
        private const val SELECT_APDU_HEADER = "00A40400"
    }

    override fun processCommandApdu(
        commandApdu: ByteArray?,
        extras: Bundle?,
    ): ByteArray {
        if (commandApdu == null) {
            return hexToBytes(SW_FAIL)
        }

        val apduHex = bytesToHex(commandApdu)

        if (isSelectAidApdu(apduHex)) {
            return hexToBytes(SW_OK)
        }

        if (isGetDataApdu(apduHex)) {
            val payload = TransferNfcPayloadStore.getPayload().toByteArray(Charsets.UTF_8)
            return payload + hexToBytes(SW_OK)
        }

        return hexToBytes(SW_FAIL)
    }

    override fun onDeactivated(reason: Int) {}

    private fun isSelectAidApdu(apduHex: String): Boolean {
        val aidLengthHex = String.format("%02X", AID.length / 2)
        val selectCommand = SELECT_APDU_HEADER + aidLengthHex + AID
        return apduHex.startsWith(selectCommand)
    }

    private fun isGetDataApdu(apduHex: String): Boolean {
        // 00 CA 00 00
        return apduHex.length >= 8 && apduHex.substring(0, 4) == "00$INS_GET_DATA"
    }

    private fun bytesToHex(bytes: ByteArray): String = bytes.joinToString(separator = "") { "%02X".format(it) }

    private fun hexToBytes(hex: String): ByteArray {
        val out = ByteArray(hex.length / 2)
        var i = 0
        while (i < hex.length) {
            out[i / 2] = ((hex[i].digitToInt(16) shl 4) + hex[i + 1].digitToInt(16)).toByte()
            i += 2
        }
        return out
    }
}
