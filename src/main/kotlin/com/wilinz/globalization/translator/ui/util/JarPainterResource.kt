package com.wilinz.globalization.translator.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.loadSvgPainter
import androidx.compose.ui.unit.Density

@Composable
fun javaPainterResource(resourcePath: String): Painter {
    return remember(resourcePath) {
        val resourceStream = object {}.javaClass.classLoader.getResourceAsStream(resourcePath)
            ?: throw IllegalArgumentException("Resource $resourcePath not found")

        loadSvgPainter(resourceStream, Density(1f))
    }
}