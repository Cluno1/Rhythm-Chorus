package io.github.cluno1.sonorus.shared.presentation.components.icons

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RhythmIconIntrinsicMeasurementTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun dropdownMenuCanMeasureDefaultSizedMaterialSymbol() {
        composeRule.setContent {
            MaterialTheme {
                Box {
                    DropdownMenu(
                        expanded = true,
                        onDismissRequest = {},
                    ) {
                        DropdownMenuItem(
                            text = { Text("English") },
                            leadingIcon = { Text("en") },
                            trailingIcon = {
                                Icon(
                                    imageVector = MaterialSymbolIcon("check"),
                                    contentDescription = null,
                                )
                            },
                            onClick = {},
                        )
                    }
                }
            }
        }

        composeRule.waitForIdle()
    }
}
