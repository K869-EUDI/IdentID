package com.k689.identid.theme

import androidx.annotation.StringRes
import com.k689.identid.R
import com.k689.identid.provider.resources.ResourceProvider

enum class AppTheme(
    @param:StringRes val labelRes: Int,
) {
    SYSTEM(labelRes = R.string.preferences_theme_option_system_default),
    LIGHT(labelRes = R.string.preferences_theme_option_light),
    DARK(labelRes = R.string.preferences_theme_option_dark),
    ;

    companion object {
        fun AppTheme.toUiText(resourceProvider: ResourceProvider): String = resourceProvider.getString(labelRes)
    }
}
