package com.example

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.ViewModelProvider
import com.example.ui.viewmodel.GabbaiScreen
import com.example.ui.viewmodel.GabbaiViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Opens real dropdowns in the running app: tapping them must open the searchable picker, not crash. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class DropdownTapTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun shot(name: String, dialog: Boolean = false) {
        val dir = File(System.getProperty("shots.dir") ?: "build/shots").apply { mkdirs() }
        rule.mainClock.advanceTimeBy(800)
        val views = windowViews()
        val v = if (dialog) views.last() else views.first()
        val bmp = Bitmap.createBitmap(v.width.coerceAtLeast(1), v.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        rule.runOnUiThread { v.draw(android.graphics.Canvas(bmp)) }
        File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun windowViews(): List<android.view.View> {
        val cls = Class.forName("android.view.WindowManagerGlobal")
        val inst = cls.getMethod("getInstance").invoke(null)
        val f = cls.getDeclaredField("mViews").apply { isAccessible = true }
        return (f.get(inst) as List<android.view.View>).toList()
    }

    private fun open(screen: GabbaiScreen) {
        rule.mainClock.autoAdvance = false
        rule.runOnUiThread { ViewModelProvider(rule.activity)[GabbaiViewModel::class.java].setScreen(screen) }
        rule.mainClock.advanceTimeBy(800)
    }

    private fun tapDropdown(label: String) {
        val l = com.example.ui.settings.L(label)
        rule.onAllNodes(hasScrollToNodeAction()).let { lists ->
            for (i in 0 until lists.fetchSemanticsNodes().size) {
                runCatching { lists[i].performScrollToNode(hasContentDescription(l)) }
            }
        }
        rule.mainClock.advanceTimeBy(500)
        rule.onAllNodesWithContentDescription(l).onFirst().performClick()
        rule.mainClock.advanceTimeBy(800)
    }

    @Test fun financeFormDropdownsOpenWithoutCrash() {
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(800)
        shot("1_dashboard")
        open(GabbaiScreen.FINANCES)
        shot("2_finances")
        rule.onNodeWithTag("fab_add_transaction").performClick()
        rule.mainClock.advanceTimeBy(800)
        tapDropdown("გადახდის მეთოდი")
        rule.onNodeWithTag("picker_dialog").assertExists()
        rule.onNodeWithTag("picker_search").performTextInput("a")
        rule.mainClock.advanceTimeBy(500)
        rule.onNodeWithTag("picker_dialog").assertExists()
        shot("3_payment_picker", dialog = true)
    }

    @Test fun financeFilterCategoryOpens() {
        open(GabbaiScreen.FINANCES)
        tapDropdown("კატეგორია")
        rule.onNodeWithTag("picker_dialog").assertExists()
        shot("4_category_picker", dialog = true)
    }

    @Test fun financeFormLooksRight() {
        open(GabbaiScreen.FINANCES)
        rule.onNodeWithTag("fab_add_transaction").performClick()
        rule.mainClock.advanceTimeBy(800)
        shot("3b_finance_form", dialog = true)
    }

    @Test fun aliyotScreenRenders() {
        open(GabbaiScreen.ALIYOT)
        shot("5_aliyot")
        open(GabbaiScreen.SETTINGS)
        shot("6_settings")
    }
}
