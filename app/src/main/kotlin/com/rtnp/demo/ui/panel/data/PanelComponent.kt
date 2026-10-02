package com.rtnp.demo.ui.panel.data

import android.graphics.Bitmap

sealed class PanelComponent {

    var computedHeight: Float = 0f
    var layoutY: Float = 0f

    data class Text(
        val content: String,
        val textSize: Float = 28f
    ) : PanelComponent()

    data class Image(
        val bitmap: Bitmap?,
        val width: Float,
        val height: Float
    ) : PanelComponent()

    data class Button(
        val label: String,
        val isActive: Boolean = false,
        val activeColor: Int = android.graphics.Color.YELLOW,
        val subText: String? = null,
        val cooldownProgress: Float? = null
    ) : PanelComponent()

    data class Spacer(val height: Float = 10f) : PanelComponent()

    /**
     * 占位符：不占高度、不渲染，仅标记动态组件的插入点。
     * 每个模板中占位符 id 必须唯一。
     */
    data class Placeholder(val id: String) : PanelComponent()
}