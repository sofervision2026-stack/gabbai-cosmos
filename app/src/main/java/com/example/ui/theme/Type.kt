package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

private val GabbaiFont = FontFamily(Font(R.font.noto_sans_georgian))
val Typography = Typography(
    displaySmall=TextStyle(fontFamily=GabbaiFont,fontWeight=FontWeight.Bold,fontSize=30.sp,lineHeight=38.sp,letterSpacing=0.sp),
    headlineMedium=TextStyle(fontFamily=GabbaiFont,fontWeight=FontWeight.Bold,fontSize=24.sp,lineHeight=32.sp,letterSpacing=0.sp),
    titleLarge=TextStyle(fontFamily=GabbaiFont,fontWeight=FontWeight.Bold,fontSize=21.sp,lineHeight=28.sp,letterSpacing=0.sp),
    titleMedium=TextStyle(fontFamily=GabbaiFont,fontWeight=FontWeight.SemiBold,fontSize=17.sp,lineHeight=24.sp,letterSpacing=0.sp),
    titleSmall=TextStyle(fontFamily=GabbaiFont,fontWeight=FontWeight.SemiBold,fontSize=14.sp,lineHeight=21.sp,letterSpacing=0.sp),
    bodyLarge=TextStyle(fontFamily=GabbaiFont,fontWeight=FontWeight.Normal,fontSize=16.sp,lineHeight=24.sp,letterSpacing=0.sp),
    bodyMedium=TextStyle(fontFamily=GabbaiFont,fontWeight=FontWeight.Normal,fontSize=14.sp,lineHeight=21.sp,letterSpacing=0.sp),
    bodySmall=TextStyle(fontFamily=GabbaiFont,fontWeight=FontWeight.Normal,fontSize=12.sp,lineHeight=18.sp,letterSpacing=0.sp),
    labelLarge=TextStyle(fontFamily=GabbaiFont,fontWeight=FontWeight.SemiBold,fontSize=14.sp,lineHeight=20.sp,letterSpacing=0.sp),
    labelMedium=TextStyle(fontFamily=GabbaiFont,fontWeight=FontWeight.Medium,fontSize=12.sp,lineHeight=17.sp,letterSpacing=0.sp),
    labelSmall=TextStyle(fontFamily=GabbaiFont,fontWeight=FontWeight.Medium,fontSize=10.sp,lineHeight=15.sp,letterSpacing=0.sp)
)
