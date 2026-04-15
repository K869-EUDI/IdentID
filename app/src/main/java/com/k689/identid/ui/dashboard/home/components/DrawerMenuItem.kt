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

package com.k689.identid.ui.dashboard.home.components

import androidx.annotation.StringRes
import com.k689.identid.R
import com.k689.identid.navigation.DashboardScreens
import com.k689.identid.navigation.TransferScreens

enum class DrawerMenuItem(
    @param:StringRes val titleRes: Int,
    val route: String? = null,
    val groupName: DrawerMenuType,
) {
    Pseudonyms(titleRes = R.string.pseudonym_list_title, route = DashboardScreens.PseudonymList.screenRoute, groupName = DrawerMenuType.OPERATION),
    Transactions(titleRes = R.string.transactions_screen_title, route = DashboardScreens.Transactions.screenRoute, groupName = DrawerMenuType.OPERATION),

    MoveWallet(titleRes = R.string.transfer_move_wallet_title, route = TransferScreens.MoveWallet.screenRoute, groupName = DrawerMenuType.WALLET),

    ReceiveWallet(titleRes = R.string.transfer_receive_title, route = TransferScreens.ReceiveWallet.screenRoute, groupName = DrawerMenuType.WALLET),
    ChangePin(titleRes = R.string.dashboard_side_menu_option_change_pin, groupName = DrawerMenuType.SETTINGS),
    Preferences(titleRes = R.string.preferences_screen_title, route = DashboardScreens.Preferences.screenRoute, groupName = DrawerMenuType.SETTINGS),
    ;

    enum class DrawerMenuType(
        @param:StringRes val titleRes: Int,
    ) {
        OPERATION(titleRes = R.string.dashboard_side_menu_group_operations),
        WALLET(titleRes = R.string.dashboard_side_menu_group_wallet),
        SETTINGS(titleRes = R.string.dashboard_side_menu_group_settings),
    }

    companion object {
        val all: List<DrawerMenuItem> = entries.toList()
    }
}
