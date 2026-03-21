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

package com.k689.identid.ui.dashboard.authenticate

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.k689.identid.R
import com.k689.identid.ui.component.AppIcons
import com.k689.identid.ui.component.ListItemDataUi
import com.k689.identid.ui.component.ListItemLeadingContentDataUi
import com.k689.identid.ui.component.ListItemMainContentDataUi
import com.k689.identid.ui.component.ListItemTrailingContentDataUi
import com.k689.identid.ui.component.content.ContentScreen
import com.k689.identid.ui.component.content.ContentTitle
import com.k689.identid.ui.component.content.ScreenNavigateAction
import com.k689.identid.ui.component.preview.PreviewTheme
import com.k689.identid.ui.component.preview.ThemeModePreviews
import com.k689.identid.ui.component.utils.SPACING_LARGE
import com.k689.identid.ui.component.utils.SPACING_MEDIUM
import com.k689.identid.ui.component.utils.VSpacer
import com.k689.identid.ui.component.wrap.WrapListItem
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

@Composable
internal fun AuthenticateScreen(
    navController: NavController,
    viewModel: AuthenticateViewModel,
) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()

    ContentScreen(
        isLoading = state.isLoading,
        navigatableAction = ScreenNavigateAction.CANCELABLE,
        onBack = { navController.popBackStack() },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            ContentTitle(
                title = stringResource(R.string.home_screen_authenticate),
                subtitle = stringResource(R.string.home_screen_authenticate_description),
            )

            VSpacer.Medium()

            WrapListItem(
                modifier = Modifier.fillMaxWidth(),
                item =
                    ListItemDataUi(
                        itemId = "in_person",
                        leadingContentData = ListItemLeadingContentDataUi.Icon(iconData = AppIcons.PresentDocumentInPerson),
                        mainContentData =
                            ListItemMainContentDataUi.Text(
                                text = stringResource(R.string.home_screen_authenticate_option_in_person),
                            ),
                        trailingContentData = ListItemTrailingContentDataUi.Icon(iconData = AppIcons.KeyboardArrowRight),
                    ),
                onItemClick = { viewModel.setEvent(Event.OnInPersonClick) },
                mainContentVerticalPadding = SPACING_LARGE.dp,
                mainContentTextStyle = MaterialTheme.typography.titleMedium,
            )

            VSpacer.Small()

            WrapListItem(
                modifier = Modifier.fillMaxWidth(),
                item =
                    ListItemDataUi(
                        itemId = "online",
                        leadingContentData = ListItemLeadingContentDataUi.Icon(iconData = AppIcons.PresentDocumentOnline),
                        mainContentData =
                            ListItemMainContentDataUi.Text(
                                text = stringResource(R.string.home_screen_add_document_option_online),
                            ),
                        trailingContentData = ListItemTrailingContentDataUi.Icon(iconData = AppIcons.KeyboardArrowRight),
                    ),
                onItemClick = { viewModel.setEvent(Event.OnOnlineClick) },
                mainContentVerticalPadding = SPACING_LARGE.dp,
                mainContentTextStyle = MaterialTheme.typography.titleMedium,
            )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.onEach { effect ->
            when (effect) {
                is Effect.Navigation.SwitchScreen -> {
                    navController.navigate(effect.screenRoute)
                }
            }
        }.collect()
    }
}

@ThemeModePreviews
@Composable
private fun AuthenticateScreenPreview() {
    PreviewTheme {
        ContentScreen(
            isLoading = false,
            navigatableAction = ScreenNavigateAction.CANCELABLE,
            onBack = {},
        ) { paddingValues ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
            ) {
                ContentTitle(
                    title = "Authenticate",
                    subtitle = "Authenticate by sharing your data in person or online.",
                )

                VSpacer.Medium()

                WrapListItem(
                    modifier = Modifier.fillMaxWidth(),
                    item =
                        ListItemDataUi(
                            itemId = "in_person",
                            leadingContentData = ListItemLeadingContentDataUi.Icon(iconData = AppIcons.PresentDocumentInPerson),
                            mainContentData =
                                ListItemMainContentDataUi.Text(
                                    text = "In person",
                                ),
                            trailingContentData = ListItemTrailingContentDataUi.Icon(iconData = AppIcons.KeyboardArrowRight),
                        ),
                    onItemClick = {},
                    mainContentVerticalPadding = SPACING_LARGE.dp,
                    mainContentTextStyle = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}
