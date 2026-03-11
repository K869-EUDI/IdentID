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

package com.k689.identid.navigation.routes.transfer

import androidx.compose.runtime.remember
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.k689.identid.navigation.ModuleRoute
import com.k689.identid.navigation.TransferScreens
import com.k689.identid.ui.transfer.receive.ReceiveWalletDocumentListScreen
import com.k689.identid.ui.transfer.receive.ReceiveWalletNfcScreen
import com.k689.identid.ui.transfer.receive.ReceiveWalletQrScreen
import com.k689.identid.ui.transfer.receive.ReceiveWalletScreen
import com.k689.identid.ui.transfer.receive.ReceiveWalletViewModel
import com.k689.identid.ui.transfer.send.MoveWalletApprovalScreen
import com.k689.identid.ui.transfer.send.MoveWalletScreen
import org.koin.androidx.compose.koinViewModel

fun NavGraphBuilder.featureTransferGraph(navController: NavController) {
    navigation(
        startDestination = TransferScreens.MoveWallet.screenRoute,
        route = ModuleRoute.TransferModule.route,
    ) {
        // Move Wallet (sender - QR + NFC)
        composable(
            route = TransferScreens.MoveWallet.screenRoute,
        ) {
            MoveWalletScreen(
                navController,
                koinViewModel(),
            )
        }

        // Move Wallet Approval (sender - PIN + document list)
        composable(
            route = TransferScreens.MoveWalletApproval.screenRoute,
        ) {
            MoveWalletApprovalScreen(
                navController,
                koinViewModel(),
            )
        }

        // Receive Wallet (receiver - method chooser)
        composable(
            route = TransferScreens.ReceiveWallet.screenRoute,
        ) { entry ->
            val parentEntry = remember(entry) { navController.getBackStackEntry(ModuleRoute.TransferModule.route) }
            ReceiveWalletScreen(
                navController,
                koinViewModel<ReceiveWalletViewModel>(viewModelStoreOwner = parentEntry),
            )
        }

        // Receive Wallet NFC (receiver - NFC listener)
        composable(
            route = TransferScreens.ReceiveWalletNfc.screenRoute,
        ) { entry ->
            val parentEntry = remember(entry) { navController.getBackStackEntry(ModuleRoute.TransferModule.route) }
            ReceiveWalletNfcScreen(
                navController,
                koinViewModel<ReceiveWalletViewModel>(viewModelStoreOwner = parentEntry),
            )
        }

        // Receive Wallet QR (receiver - QR scanner)
        composable(
            route = TransferScreens.ReceiveWalletQr.screenRoute,
        ) { entry ->
            val parentEntry = remember(entry) { navController.getBackStackEntry(ModuleRoute.TransferModule.route) }
            ReceiveWalletQrScreen(
                navController,
                koinViewModel<ReceiveWalletViewModel>(viewModelStoreOwner = parentEntry),
            )
        }

        // Receive Wallet Document List (receiver - select & import)
        composable(
            route = TransferScreens.ReceiveWalletDocumentList.screenRoute,
        ) { entry ->
            val parentEntry = remember(entry) { navController.getBackStackEntry(ModuleRoute.TransferModule.route) }
            ReceiveWalletDocumentListScreen(
                navController,
                koinViewModel<ReceiveWalletViewModel>(viewModelStoreOwner = parentEntry),
            )
        }
    }
}
