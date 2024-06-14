package com.spark.app_main.model

import android.text.SpannableStringBuilder
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class MaterialDialogContent(
    @DrawableRes val iconRes: Int? = null,
    @StringRes val contentRes: Int? = null,
    val content: String? = null,
    val topTitle: String? = null,
    @StringRes val positiveButtonText: Int = 0,
    @StringRes val negativeButtonText: Int? = null,
    val bottomIconEnable: Boolean = false,
    val filterHighLightText: String? = null,
    val spannableContent: SpannableStringBuilder? = null,
    val answerContent: String? = null
)

data class MaterialDialogWithProgressContent(
    @DrawableRes val iconRes: Int,
    @StringRes val contentRes: Int,
    @StringRes val progressBarContent: Int,
    @StringRes val positiveButtonText: Int,
    @StringRes val negativeButtonText: Int,
    @StringRes val titleTextRes: Int? = null
)