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

package com.k689.identid.controller.storage

import com.k689.identid.config.BiometricUiConfig.Parser.MAX_INCORRECT_ATTEMPTS
import com.k689.identid.config.StorageConfig

interface PinStorageController {
    fun retrievePin(): String

    fun setPin(pin: String)

    fun validatePin(pin: String): Boolean

    fun retrieveIncorrectPinEntryTime(): Long

    fun retrieveIncorrectAttempts(): Int

    fun canValidatePin(): Boolean

    fun isPinValid(pin: String): Boolean
}

class PinStorageControllerImpl(
    private val storageConfig: StorageConfig,
) : PinStorageController {
    override fun retrievePin(): String = storageConfig.pinStorageProvider.retrievePin()

    override fun setPin(pin: String) {
        storageConfig.pinStorageProvider.setPin(pin)
    }

    override fun canValidatePin(): Boolean = storageConfig.pinStorageProvider.getIncorrectPinAttempts() < MAX_INCORRECT_ATTEMPTS

    override fun validatePin(pin: String): Boolean {
        if (!canValidatePin()) {
            return false
        }
        val isValid = storageConfig.pinStorageProvider.isPinValid(pin)
        if (!isValid) {
            storageConfig.pinStorageProvider.setIncorrectPinAttempts()
        }
        return isValid
    }

    override fun isPinValid(pin: String): Boolean = storageConfig.pinStorageProvider.isPinValid(pin)

    override fun retrieveIncorrectPinEntryTime(): Long = storageConfig.pinStorageProvider.lastIncorrectPinEntryTime()

    override fun retrieveIncorrectAttempts(): Int = storageConfig.pinStorageProvider.getIncorrectPinAttempts()
}
