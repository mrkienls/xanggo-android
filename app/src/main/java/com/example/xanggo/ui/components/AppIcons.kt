package com.example.xanggo.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Biểu tượng Ghim Vị Trí / Trạm Xăng (Location Pin Vector)
 * Chuẩn Material Fintech với hình dáng giọt nước bo tròn và lỗ khuyết trung tâm.
 */
val LocationPinVector: ImageVector = ImageVector.Builder(
    name = "LocationPin",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.White)) {
        moveTo(12f, 2f)
        curveTo(8.13f, 2f, 5f, 5.13f, 5f, 9f)
        curveTo(5f, 14.25f, 12f, 22f, 12f, 22f)
        reflectiveCurveTo(19f, 14.25f, 19f, 9f)
        curveTo(19f, 5.13f, 15.87f, 2f, 12f, 2f)
        close()
        moveTo(12f, 11.5f)
        curveTo(10.62f, 11.5f, 9.5f, 10.38f, 9.5f, 9f)
        reflectiveCurveTo(10.62f, 6.5f, 12f, 6.5f)
        reflectiveCurveTo(14.5f, 7.62f, 14.5f, 9f)
        reflectiveCurveTo(13.38f, 11.5f, 12f, 11.5f)
        close()
    }
}.build()

/**
 * Biểu tượng Cột Bơm Xăng (Gas Station / Fuel Dispenser)
 */
val GasPumpVector: ImageVector = ImageVector.Builder(
    name = "GasPump",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.White)) {
        moveTo(19.77f, 7.23f)
        lineTo(16.06f, 3.5f)
        lineTo(15f, 4.56f)
        lineTo(17.11f, 6.67f)
        curveTo(16.17f, 7.03f, 15.5f, 7.93f, 15.5f, 9f)
        curveTo(15.5f, 10.38f, 16.62f, 11.5f, 18f, 11.5f)
        curveTo(18.36f, 11.5f, 18.69f, 11.42f, 19f, 11.29f)
        verticalLineTo(18.5f)
        curveTo(19f, 19.05f, 18.55f, 19.5f, 18f, 19.5f)
        reflectiveCurveTo(17f, 19.05f, 17f, 18.5f)
        verticalLineTo(14f)
        curveTo(17f, 12.9f, 16.1f, 12f, 15f, 12f)
        horizontalLineTo(14f)
        verticalLineTo(5f)
        curveTo(14f, 3.9f, 13.1f, 3f, 12f, 3f)
        horizontalLineTo(6f)
        curveTo(4.9f, 3f, 4f, 3.9f, 4f, 5f)
        verticalLineTo(21f)
        horizontalLineTo(14f)
        verticalLineTo(13.5f)
        horizontalLineTo(15.5f)
        verticalLineTo(18.5f)
        curveTo(15.5f, 19.88f, 16.62f, 21f, 18f, 21f)
        reflectiveCurveTo(20.5f, 19.88f, 20.5f, 18.5f)
        verticalLineTo(9f)
        curveTo(20.5f, 8.31f, 20.22f, 7.68f, 19.77f, 7.23f)
        close()
        moveTo(12f, 10f)
        horizontalLineTo(6f)
        verticalLineTo(5f)
        horizontalLineTo(12f)
        verticalLineTo(10f)
        close()
        moveTo(18f, 10f)
        curveTo(17.45f, 10f, 17f, 9.55f, 17f, 9f)
        reflectiveCurveTo(17.45f, 8f, 18f, 8f)
        reflectiveCurveTo(19f, 8.45f, 19f, 9f)
        reflectiveCurveTo(18.55f, 10f, 18f, 10f)
        close()
    }
}.build()

/**
 * Biểu tượng Quả Địa Cầu (Globe / World)
 */
val GlobeVector: ImageVector = ImageVector.Builder(
    name = "Globe",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.White)) {
        moveTo(12f, 2f)
        curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
        reflectiveCurveTo(6.48f, 22f, 12f, 22f)
        reflectiveCurveTo(22f, 17.52f, 22f, 12f)
        reflectiveCurveTo(17.52f, 2f, 12f, 2f)
        close()
        moveTo(11f, 19.93f)
        curveTo(7.05f, 19.44f, 4f, 16.08f, 4f, 12f)
        curveTo(4f, 11.38f, 4.08f, 10.79f, 4.21f, 10.21f)
        lineTo(9f, 15f)
        verticalLineTo(16f)
        curveTo(9f, 17.1f, 9.9f, 18f, 11f, 18f)
        verticalLineTo(19.93f)
        close()
        moveTo(17.9f, 17.39f)
        curveTo(17.64f, 16.58f, 16.9f, 16f, 16f, 16f)
        horizontalLineTo(15f)
        verticalLineTo(13f)
        curveTo(15f, 12.45f, 14.55f, 12f, 14f, 12f)
        horizontalLineTo(8f)
        verticalLineTo(10f)
        horizontalLineTo(10f)
        curveTo(10.55f, 10f, 11f, 9.55f, 11f, 9f)
        verticalLineTo(7f)
        horizontalLineTo(14f)
        curveTo(15.1f, 7f, 16f, 6.1f, 16f, 5f)
        verticalLineTo(4.59f)
        curveTo(18.93f, 6.13f, 20f, 8.95f, 20f, 12f)
        curveTo(20f, 14.08f, 19.2f, 15.97f, 17.9f, 17.39f)
        close()
    }
}.build()
