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

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DrawerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.k689.identid.ui.component.AppIcons
import com.k689.identid.ui.component.utils.SIZE_LARGE
import com.k689.identid.ui.component.utils.SPACING_EXTRA_LARGE
import com.k689.identid.ui.component.utils.SPACING_LARGE
import com.k689.identid.ui.component.wrap.WrapImage

@Composable
fun HomeDrawer(
    drawerState: DrawerState,
    menuItems: List<DrawerMenuItem>,
    onMenuItemClick: (DrawerMenuItem) -> Unit,
    content: @Composable () -> Unit,
) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(280.dp).fillMaxHeight(),
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                windowInsets = WindowInsets(0, 0, 0, 0),
            ) {
                Spacer(Modifier.statusBarsPadding())
                Spacer(Modifier.height(SIZE_LARGE.dp))
                WrapImage(
                    iconData = AppIcons.CardLogo,
                    modifier =
                        Modifier
                            .padding(horizontal = 26.dp, vertical = SIZE_LARGE.dp)
                            .height(56.dp),
                    contentScale = ContentScale.FillHeight,
                )

                val menuGroups = remember { menuItems.groupBy { it.groupName } }

                menuGroups.forEach { (group, items) ->
                    Text(
                        text = stringResource(group.titleRes),
                        style =
                            MaterialTheme.typography.labelLarge.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                            ),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    items.forEach { item ->
                        NavigationDrawerItem(
                            label = {
                                Text(
                                    text = stringResource(id = item.titleRes),
                                    style =
                                        MaterialTheme.typography.labelLarge.copy(
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 20.sp,
                                        ),
                                )
                            },
                            selected = false,
                            onClick = { onMenuItemClick(item) },
                            modifier = Modifier.padding(vertical = 3.dp, horizontal = 0.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "Made with the sweat and tears of unpaid labour <3",
                    style =
                        MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Light,
                            fontSize = 13.sp,
                        ),
                    modifier = Modifier.padding(start = SPACING_LARGE.dp, end = SPACING_LARGE.dp, bottom = SPACING_EXTRA_LARGE.dp),
                )
            }
        },
        content = content,
    )
}
