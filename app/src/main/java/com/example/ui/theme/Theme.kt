package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.ui.settings.AppSettings
import com.example.ui.settings.LightingMode
import com.example.ui.settings.LocalAppSettings

private val NightScheme = darkColorScheme(primary=NightCyan,onPrimary=NightDeepSpace,secondary=NightGold,onSecondary=NightDeepSpace,tertiary=NightPurple,background=NightDeepSpace,onBackground=NightTextPrimary,surface=NightDarkSurface,onSurface=NightTextPrimary,surfaceVariant=NightSurfaceCard,onSurfaceVariant=NightTextSecondary,error=NightRose,onError=Color.White)
private val DayScheme = lightColorScheme(primary=DayForest,onPrimary=DayOnAccent,secondary=DayGold,onSecondary=DayOnAccent,tertiary=DayWine,background=DaySky,onBackground=DayTextPrimary,surface=DaySurface,onSurface=DayTextPrimary,surfaceVariant=DayCard,onSurfaceVariant=DayTextSecondary,error=DayWine,onError=DayOnAccent,outline=DayBorder)

data class GabbaiPalette(val background:Color,val glass:Color,val surface:Color,val border:Color,val text:Color,val secondaryText:Color,val mutedText:Color,val isNight:Boolean)
val LocalGabbaiPalette = staticCompositionLocalOf { GabbaiPalette(NightDeepSpace,NightSurfaceGlass,NightDarkSurface,NightBorderGlow,NightTextPrimary,NightTextSecondary,NightTextMuted,true) }

@Composable
fun GabbaiCosmosTheme(settings: AppSettings = AppSettings(), content: @Composable () -> Unit) {
    val night = settings.lighting == LightingMode.NIGHT
    val palette = if(night) GabbaiPalette(NightDeepSpace,NightSurfaceGlass,NightDarkSurface,NightBorderGlow,NightTextPrimary,NightTextSecondary,NightTextMuted,true) else GabbaiPalette(DaySky,DayGlass,DaySurface,DayBorder,DayTextPrimary,DayTextSecondary,DayTextMuted,false)
    // Very large system font scales break Georgian/Russian layouts; keep text large but bounded.
    val density = androidx.compose.ui.platform.LocalDensity.current
    val safeDensity = androidx.compose.ui.unit.Density(density.density, density.fontScale.coerceIn(0.85f, 1.2f))
    // Keep the global text/currency state in sync with the saved settings.
    androidx.compose.runtime.SideEffect {
        com.example.ui.settings.I18n.lang = settings.language
        com.example.ui.settings.I18n.defaultCurrency = settings.currency
    }
    val direction = if (settings.language == com.example.ui.settings.AppLanguage.HE) androidx.compose.ui.unit.LayoutDirection.Rtl else androidx.compose.ui.unit.LayoutDirection.Ltr
    CompositionLocalProvider(LocalAppSettings provides settings, LocalGabbaiPalette provides palette, androidx.compose.ui.platform.LocalDensity provides safeDensity, androidx.compose.ui.platform.LocalLayoutDirection provides direction) {
        MaterialTheme(colorScheme = if(night) NightScheme else DayScheme, typography = Typography, content = content)
    }
}
