/*
 * Copyright (c) 2023 European Commission
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

package com.k689.identid.ui.component.wrap

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.password
import androidx.compose.ui.semantics.semantics
import com.k689.identid.ui.component.preview.PreviewTheme
import com.k689.identid.ui.component.preview.ThemeModePreviews
import com.k689.identid.ui.component.utils.EmptyTextToolbar
import com.k689.identid.ui.component.utils.HSpacer
import com.k689.identid.ui.component.utils.OneTimeLaunchedEffect
import com.k689.identid.ui.component.utils.SIZE_SMALL
import com.k689.identid.ui.component.utils.SPACING_SMALL
import com.k689.identid.util.ui.TestTag

internal fun sanitizePinInput(
    rawValue: String,
    length: Int,
): String = rawValue.filter(Char::isDigit).take(length)

internal fun pinSlotCharacter(
    pin: String,
    index: Int,
    visualTransformation: VisualTransformation,
): String {
    val digit = pin.getOrNull(index)?.toString().orEmpty()
    if (digit.isEmpty()) return ""

    return visualTransformation
        .filter(AnnotatedString(digit))
        .text
        .text
}

@Composable
fun WrapPinTextField(
    modifier: Modifier = Modifier.fillMaxWidth(),
    displayCode: String? = null,
    onPinUpdate: (code: String) -> Unit,
    length: Int,
    hasError: Boolean = false,
    errorMessage: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    pinWidth: Dp? = null,
    clearCode: Boolean = false,
    focusOnCreate: Boolean = false,
    shouldHideKeyboardOnCompletion: Boolean = false,
    enabled: Boolean = true,
) {
    val fieldsRange = 0 until length
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var pinCode by rememberSaveable(length) { mutableStateOf(displayCode?.let { sanitizePinInput(it, length) }.orEmpty()) }
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }

    LaunchedEffect(clearCode) {
        if (clearCode) {
            if (pinCode.isNotEmpty()) {
                pinCode = ""
                onPinUpdate.invoke("")
            }
            focusRequester.requestFocus()
        }
    }

    LaunchedEffect(displayCode, length) {
        val updatedDisplayCode = displayCode?.let { sanitizePinInput(it, length) }.orEmpty()
        if (updatedDisplayCode != pinCode) {
            pinCode = updatedDisplayCode
        }
    }

    CompositionLocalProvider(
        LocalTextToolbar provides EmptyTextToolbar,
        LocalTextSelectionColors provides
            TextSelectionColors(
                handleColor = Color.Transparent,
                backgroundColor = Color.Transparent,
            ),
    ) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DisableSelection {
                BasicTextField(
                    value = pinCode,
                    onValueChange = { newText ->
                        if (!enabled) return@BasicTextField

                        val sanitizedPin = sanitizePinInput(newText, length)
                        if (sanitizedPin == pinCode) return@BasicTextField

                        pinCode = sanitizedPin
                        onPinUpdate.invoke(sanitizedPin)

                        if (sanitizedPin.length == length && shouldHideKeyboardOnCompletion) {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        }
                    },
                    enabled = enabled,
                    singleLine = true,
                    textStyle =
                        LocalTextStyle.current.copy(
                            color = Color.Transparent,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        ),
                    cursorBrush = SolidColor(Color.Transparent),
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Done,
                        ),
                    keyboardActions =
                        KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                            },
                        ),
                    visualTransformation = visualTransformation,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onFocusChanged { isFocused = it.isFocused }
                            .semantics {
                                if (visualTransformation != VisualTransformation.None) {
                                    password()
                                }
                            },
                    decorationBox = { innerTextField ->
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        enabled = enabled,
                                        interactionSource = interactionSource,
                                        indication = null,
                                    ) {
                                        focusRequester.requestFocus()
                                        keyboardController?.show()
                                    },
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(
                                modifier = Modifier.wrapContentWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                for (currentTextField in fieldsRange) {
                                    val isActiveSlot =
                                        enabled &&
                                            (pinCode.length == currentTextField || (pinCode.length == length && currentTextField == fieldsRange.last))
                                    val borderColor =
                                        when {
                                            hasError -> MaterialTheme.colorScheme.error
                                            isFocused && isActiveSlot -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.outlineVariant
                                        }

                                    Box(
                                        modifier =
                                            Modifier
                                                .testTag(TestTag.pinTextField(currentTextField))
                                                .then(
                                                    pinWidth?.let { dp ->
                                                        Modifier
                                                            .width(dp)
                                                            .padding(vertical = SPACING_SMALL.dp)
                                                    } ?: Modifier
                                                        .weight(1f)
                                                        .wrapContentSize(),
                                                ).height(56.dp)
                                                .border(
                                                    width = 1.dp,
                                                    color = borderColor,
                                                    shape = RoundedCornerShape(SIZE_SMALL.dp),
                                                )
                                                .background(
                                                    color = MaterialTheme.colorScheme.surface,
                                                    shape = RoundedCornerShape(SIZE_SMALL.dp),
                                                ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = pinSlotCharacter(pinCode, currentTextField, visualTransformation),
                                            style =
                                                MaterialTheme.typography.titleLarge.copy(
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontWeight = FontWeight.Bold,
                                                ),
                                        )
                                    }

                                    if (currentTextField != fieldsRange.last) {
                                        HSpacer.Small()
                                    }
                                }
                            }

                            Box(
                                modifier =
                                    Modifier
                                        .matchParentSize()
                                        .padding(vertical = SPACING_SMALL.dp)
                                        .background(Color.Transparent),
                            ) {
                                Box(
                                    modifier =
                                        Modifier
                                            .size(1.dp)
                                            .align(Alignment.Center),
                                ) {
                                    innerTextField()
                                }
                            }
                        }
                    },
                )
            }
            errorMessage?.let {
                Text(
                    text = it,
                    style =
                        MaterialTheme.typography.headlineSmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.error,
                )
            }

            OneTimeLaunchedEffect {
                if (focusOnCreate) {
                    focusRequester.requestFocus()
                }
            }
        }
    }
}

/**
 * Preview composable of [WrapPinTextField].
 */
@ThemeModePreviews
@Composable
private fun PreviewWrapPinTextField() {
    PreviewTheme {
        WrapPinTextField(
            modifier = Modifier.wrapContentSize(),
            onPinUpdate = {},
            length = 4,
            visualTransformation = PasswordVisualTransformation(),
            pinWidth = 42.dp,
        )
    }
}
