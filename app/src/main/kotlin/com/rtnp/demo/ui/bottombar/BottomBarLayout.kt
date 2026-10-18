package com.rtnp.demo.ui.bottombar

import android.graphics.Rect
import android.graphics.RectF

/**
 * 底部栏布局结果（数据类，不可变）。
 * [buttonRects] 与 templates 索引一一对应。
 */
class BottomBarLayout(
    val buttonRects: List<RectF>,
    val barRect: Rect,
    val buttonRadius: Float,
    val buttonGap: Float
)