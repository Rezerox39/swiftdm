package com.abhi.armz.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Radii used across ARMz. Cards 12-14dp, sheets 20dp+, pills fully round. */
object SwiftShape {
    val card = RoundedCornerShape(12.dp)
    val cardLarge = RoundedCornerShape(14.dp)
    val sheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val iconTile = RoundedCornerShape(12.dp)
    val segment = RoundedCornerShape(9.dp)
    val segmentThumb = RoundedCornerShape(7.dp)
    val capsule = RoundedCornerShape(percent = 50)
    val button = RoundedCornerShape(percent = 50)
    val hairline = 0.5.dp
}

val CapsuleShape = RoundedCornerShape(percent = 50)

val SwiftShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(22.dp),
)
