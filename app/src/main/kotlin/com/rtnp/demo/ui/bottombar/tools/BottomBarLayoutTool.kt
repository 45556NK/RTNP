package com.rtnp.demo.ui.bottombar.tools

import android.graphics.Rect
import android.graphics.RectF
import com.rtnp.demo.ui.bottombar.BottomBarButtonTemplate
import com.rtnp.demo.ui.bottombar.BottomBarLayout

/**
 * 底部栏布局算法（工具，纯静态）。
 *
 * 输入：
 *  - 每个按钮的当前 scale（1.0 / 0.9 / 1.5）
 *  - 顶排抬升进度（0 ~ 0.8，表示抬升 baseD 的倍数）
 */
object BottomBarLayoutTool {

    const val DIAMETER_RATIO = 1f / 10f
    const val GAP_RATIO = 1f / 5f
    const val UPPER_LIFT_BOTTOM_ACTIVE = 0.8f
    const val UPPER_LIFT_TOP_ACTIVE = 0.5f

    @JvmStatic
    fun compute(
        screenWidth: Int,
        screenHeight: Int,
        templates: List<BottomBarButtonTemplate>,
        scales: FloatArray,
        upperLiftProgress: Float
    ): BottomBarLayout {
        val baseD = screenWidth * DIAMETER_RATIO
        val baseR = baseD / 2f
        val gap = baseD * GAP_RATIO
        val margin = gap

        val upperLift = upperLiftProgress * baseD

        val bottomIdx = mutableListOf<Int>()
        val topIdx = mutableListOf<Int>()
        for (i in templates.indices) {
            if (getRow(i) == 0) bottomIdx.add(i) else topIdx.add(i)
        }

        val rects = arrayOfNulls<RectF>(templates.size)

        // 底排：从右到左
        var cursorRight = screenWidth - margin
        for (idx in bottomIdx) {
            val s = scales[idx]
            val r = baseR * s
            val cx = cursorRight - r
            val cy = screenHeight - margin - r
            rects[idx] = RectF(cx - r, cy - r, cx + r, cy + r)
            cursorRight = cx - r - gap
        }

        // 顶排：基础底部 = 底排（未激活）顶部 - gap
        val topBaseBottom = screenHeight - margin - 2f * baseR - gap
        cursorRight = screenWidth - margin
        for (idx in topIdx) {
            val s = scales[idx]
            val r = baseR * s
            val cx = cursorRight - r
            val cy = topBaseBottom - upperLift - r
            rects[idx] = RectF(cx - r, cy - r, cx + r, cy + r)
            cursorRight = cx - r - gap
        }

        val orderedRects = List(templates.size) { idx -> rects[idx]!! }

        val barRect = Rect()
        if (orderedRects.isNotEmpty()) {
            var minX = Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var maxX = -Float.MAX_VALUE
            var maxY = -Float.MAX_VALUE
            for (r in orderedRects) {
                if (r.left < minX) minX = r.left
                if (r.top < minY) minY = r.top
                if (r.right > maxX) maxX = r.right
                if (r.bottom > maxY) maxY = r.bottom
            }
            barRect.set(minX.toInt(), minY.toInt(), maxX.toInt(), maxY.toInt())
        }

        return BottomBarLayout(orderedRects, barRect, baseR, gap)
    }

    @JvmStatic
    fun getRow(index: Int): Int {
        if (index <= 0) return 0
        return if (index % 2 == 1) 0 else 1
    }
}