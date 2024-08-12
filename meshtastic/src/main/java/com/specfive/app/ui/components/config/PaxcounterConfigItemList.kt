package com.specfive.app.ui.components.config

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
import com.specfive.app.ModuleConfigProtos
import com.specfive.app.copy
import com.specfive.app.ui.components.EditTextPreference
import com.specfive.app.ui.components.PreferenceCategory
import com.specfive.app.ui.components.PreferenceFooter
import com.specfive.app.ui.components.SwitchPreference

@Composable
fun PaxcounterConfigItemList(
    paxcounterConfig: ModuleConfigProtos.ModuleConfig.PaxcounterConfig,
    enabled: Boolean,
    onSaveClicked: (ModuleConfigProtos.ModuleConfig.PaxcounterConfig) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var paxcounterInput by remember(paxcounterConfig) { mutableStateOf(paxcounterConfig) }

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item { PreferenceCategory(text = "Paxcounter Config") }

        item {
            SwitchPreference(title = "Paxcounter enabled",
                checked = paxcounterInput.enabled,
                enabled = enabled,
                onCheckedChange = {
                    paxcounterInput = paxcounterInput.copy { this.enabled = it }
                })
        }
        item { Divider() }

        item {
            EditTextPreference(title = "Update interval (seconds)",
                value = paxcounterInput.paxcounterUpdateInterval,
                enabled = enabled,
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                onValueChanged = {
                    paxcounterInput = paxcounterInput.copy { paxcounterUpdateInterval = it }
                })
        }

        item {
            PreferenceFooter(
                enabled = paxcounterInput != paxcounterConfig,
                onCancelClicked = {
                    focusManager.clearFocus()
                    paxcounterInput = paxcounterConfig
                },
                onSaveClicked = {
                    focusManager.clearFocus()
                    onSaveClicked(paxcounterInput)
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PaxcounterConfigPreview() {
    PaxcounterConfigItemList(
        paxcounterConfig = ModuleConfigProtos.ModuleConfig.PaxcounterConfig.getDefaultInstance(),
        enabled = true,
        onSaveClicked = { },
    )
}
