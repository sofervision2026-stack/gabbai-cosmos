package com.example

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.example.ui.components.AmountCurrencyField
import com.example.ui.components.AppDropdown
import com.example.ui.components.MemberField
import com.example.ui.components.SuggestField
import com.example.ui.screens.AddTransactionDialog
import com.example.ui.theme.GabbaiCosmosTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class FieldsIsolationTest {
    @get:Rule val rule = createComposeRule()

    private fun show(content: @Composable () -> Unit) {
        rule.setContent { GabbaiCosmosTheme { Column { content() } } }
        rule.waitForIdle()
    }

    @Test fun dropdown() = show { AppDropdown("a", 1, listOf(1, 2), { "$it" }, {}) }
    @Test fun suggest() = show { SuggestField("a", "", {}, listOf("x")) }
    @Test fun amount() = show { AmountCurrencyField("a", "", {}, "GEL", {}) }
    @Test fun member() = show { MemberField("a", "", {}, listOf("x"), {}) }
    @Test fun dialogOverAnimatedBackground() {
        rule.mainClock.autoAdvance = false
        rule.setContent { GabbaiCosmosTheme { com.example.ui.components.CosmicAnimatedBackground { AddTransactionDialog(listOf("x"), {}, {}, { _, _, _, _, _, _, _, _, _ -> }) } } }
        rule.mainClock.advanceTimeBy(500)
        rule.onNodeWithTag("btn_save_tx").assertExists()
    }
    @Test fun dialog() = show { AddTransactionDialog(listOf("x"), {}, {}, { _, _, _, _, _, _, _, _, _ -> }) }
}
