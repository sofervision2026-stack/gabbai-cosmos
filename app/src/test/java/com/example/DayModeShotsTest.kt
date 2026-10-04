package com.example

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.ui.viewmodel.GabbaiScreen
import com.example.ui.viewmodel.GabbaiViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Day lighting in each language: screens must render (screenshots are written for visual review). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class DayModeShotsTest {
    @get:Rule val rule = createEmptyComposeRule()

    @Suppress("UNCHECKED_CAST")
    private fun shot(name: String, dialog: Boolean = false) {
        rule.mainClock.advanceTimeBy(800)
        val cls = Class.forName("android.view.WindowManagerGlobal")
        val inst = cls.getMethod("getInstance").invoke(null)
        val views = (cls.getDeclaredField("mViews").apply { isAccessible = true }.get(inst) as List<android.view.View>).toList()
        val v = if (dialog) (views.drop(1).lastOrNull { it.width > 1 } ?: views.last()) else views.first()
        val dir = File(System.getProperty("shots.dir") ?: "build/shots").apply { mkdirs() }
        val bmp = Bitmap.createBitmap(v.width.coerceAtLeast(1), v.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        rule.runOnUiThread { v.draw(android.graphics.Canvas(bmp)) }
        File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun windowCount(): String {
        val cls = Class.forName("android.view.WindowManagerGlobal")
        val inst = cls.getMethod("getInstance").invoke(null)
        val views = (cls.getDeclaredField("mViews").apply { isAccessible = true }.get(inst) as List<android.view.View>)
        return views.joinToString { "${it.javaClass.simpleName}:${it.width}x${it.height}" }
    }

    private fun run(lang: String) {
        ApplicationProvider.getApplicationContext<Context>().getSharedPreferences("gabbai_settings", Context.MODE_PRIVATE)
            .edit().putString("language", lang).putString("lighting", "DAY").commit()
        val sc = ActivityScenario.launch(MainActivity::class.java)
        rule.mainClock.autoAdvance = false
        shot("day_${lang}_1_home")
        for ((i, s) in listOf(GabbaiScreen.FINANCES, GabbaiScreen.ALIYOT, GabbaiScreen.MEMBERS).withIndex()) {
            sc.onActivity { ViewModelProvider(it)[GabbaiViewModel::class.java].setScreen(s) }
            shot("day_${lang}_${i + 2}_${s.name.lowercase()}")
        }
        sc.onActivity { ViewModelProvider(it)[GabbaiViewModel::class.java].setScreen(GabbaiScreen.FINANCES) }
        rule.mainClock.advanceTimeBy(500)
        rule.onNodeWithTag("fab_add_transaction").performClick()
        rule.mainClock.advanceTimeBy(1500)
        org.robolectric.shadows.ShadowLooper.idleMainLooper(); rule.mainClock.advanceTimeBy(500)
        println("VIEWS " + windowCount())
        shot("day_${lang}_5_finance_form", dialog = true)
        sc.close()
    }

    @Test fun georgian() = run("KA")
    @Test fun hebrew() = run("HE")
    @Test fun russian() = run("RU")
}
