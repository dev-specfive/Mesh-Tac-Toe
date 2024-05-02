package com.spark.app.ui.components.config

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import com.spark.app.ModuleConfigProtos
import com.spark.app.copy
import com.spark.app.ui.components.EditTextPreference
import com.spark.app.ui.components.PreferenceCategory
import com.spark.app.ui.components.PreferenceFooter
import com.spark.app.ui.components.SwitchPreference

@Composable
fun AmbientLightingConfigItemList(
    ambientLightingConfig: ModuleConfigProtos.ModuleConfig.AmbientLightingConfig,
    enabled: Boolean,
    onSaveClicked: (ModuleConfigProtos.ModuleConfig.AmbientLightingConfig) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var ambientLightingInput by remember(ambientLightingConfig) {
        mutableStateOf(ambientLightingConfig)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item { PreferenceCategory(text = "Ambient Lighting Config") }

        item {
            SwitchPreference(title = "LED state",
                checked = ambientLightingInput.ledState,
                enabled = enabled,
                onCheckedChange = {
                    ambientLightingInput = ambientLightingInput.copy { ledState = it }
                })
        }
        item { Divider() }

        item {
            EditTextPreference(title = "Current",
                value = ambientLightingInput.current,
                enabled = enabled,
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                onValueChanged = {
                    ambientLightingInput = ambientLightingInput.copy { current = it }
                })
        }

        item {
            EditTextPreference(title = "Red",
                value = ambientLightingInput.red,
                enabled = enabled,
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                onValueChanged = { ambientLightingInput = ambientLightingInput.copy { red = it } })
        }

        item {
            EditTextPreference(title = "Green",
                value = ambientLightingInput.green,
                enabled = enabled,
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                onValueChanged = { ambientLightingInput = ambientLightingInput.copy { green = it } })
        }

        item {
            EditTextPreference(title = "Blue",
                value = ambientLightingInput.blue,
                enabled = enabled,
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                onValueChanged = { ambientLightingInput = ambientLightingInput.copy { blue = it } })
        }

        item {
            PreferenceFooter(
                enabled = ambientLightingInput != ambientLightingConfig,
                onCancelClicked = {
                    focusManager.clearFocus()
                    ambientLightingInput = ambientLightingConfig
                },
                onSaveClicked = {
                    focusManager.clearFocus()
                    onSaveClicked(ambientLightingInput)
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AmbientLightingConfigPreview() {
    AmbientLightingConfigItemList(
        ambientLightingConfig = ModuleConfigProtos.ModuleConfig.AmbientLightingConfig.getDefaultInstance(),
        enabled = true,
        onSaveClicked = { },
    )
}
