package com.rtnp.demo.ui.bottombar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.view.MotionEvent
import com.rtnp.demo.core.GameConstants
import com.rtnp.demo.ui.bottombar.tools.BottomBarAnimTool
import com.rtnp.demo.ui.bottombar.tools.BottomBarDrawTool
import com.rtnp.demo.ui.bottombar.tools.BottomBarIconTool
import com.rtnp.demo.ui.bottombar.tools.BottomBarItemPanelProxy
import com.rtnp.demo.ui.bottombar.tools.*

/**
 * 新底部栏系统（V2）。
 */
class BottomBarV2(private val context: Context) {

    private val templates = mutableListOf<BottomBarButtonTemplate>()
    private val activeStates = mutableMapOf<String, Boolean>()
    private var layout: BottomBarLayout? = null

    private var screenWidth = 0
    private var screenHeight = 0

    private var pressedId: String? = null

    init {
        // 清空静态单例的状态残留（防止新旧 GameView 切换时状态泄漏）
        BottomBarAnimTool.clear()
        BottomBarItemPanelProxy.hide()
        BottomBarUnitInfoPanelProxy.hide()

        BottomBarItemPanelProxy.init(context)
        BottomBarUnitInfoPanelProxy.init(context)
        BottomBarTemplateRegistration.registerAll(this)
    }

    internal fun registerTemplate(template: BottomBarButtonTemplate): Boolean {
        if (templates.any { it.id == template.id }) return false
        templates.add(template)
        BottomBarIconTool.register(template.id, template.iconPath, context)
        activeStates[template.id] = false
        return true
    }

    // ==================== 激活态 ====================

    fun setButtonActive(id: String, active: Boolean) {
        if (!activeStates.containsKey(id)) return
        if (activeStates[id] == active) return
        activeStates[id] = active
        BottomBarAnimTool.setActive(id, active)
        updateDimStates()
    }

    fun isButtonActive(id: String): Boolean = activeStates[id] ?: false

    private fun updateDimStates() {
        val anyActive = activeStates.values.any { it }
        for (t in templates) {
            BottomBarAnimTool.setDimmed(t.id, anyActive && !(activeStates[t.id] ?: false))
        }
    }

    // ==================== 布局 ====================

    fun setScreenSize(w: Int, h: Int) {
        screenWidth = w
        screenHeight = h
        BottomBarItemPanelProxy.setScreenSize(w, h)
        BottomBarUnitInfoPanelProxy.setScreenSize(w, h)   // ★ 新增
    }

    fun getRect(): Rect = layout?.barRect ?: Rect()

    fun contains(x: Float, y: Float): Boolean {
        if (BottomBarItemPanelProxy.contains(x, y)) return true
        val r = layout?.barRect ?: return false
        return !r.isEmpty && x >= r.left && x <= r.right && y >= r.top && y <= r.bottom
    }

    private fun computeLayout(): BottomBarLayout? {
        if (screenWidth <= 0 || screenHeight <= 0) return null

        // 决策顶排抬升目标
        var liftTarget = 0f
        // 按下中的按钮
        if (pressedId != null) {
            val idx = templates.indexOfFirst { it.id == pressedId }
            if (idx >= 0 && BottomBarLayoutTool.getRow(idx) == 0) {
                liftTarget = maxOf(liftTarget, BottomBarLayoutTool.UPPER_LIFT_BOTTOM_ACTIVE)
            }
        }
        // 已激活的按钮
        for (i in templates.indices) {
            if (activeStates[templates[i].id] == true) {
                val lift = if (BottomBarLayoutTool.getRow(i) == 0)
                    BottomBarLayoutTool.UPPER_LIFT_BOTTOM_ACTIVE
                else BottomBarLayoutTool.UPPER_LIFT_TOP_ACTIVE
                liftTarget = maxOf(liftTarget, lift)
            }
        }
        BottomBarAnimTool.setUpperRowLiftTarget(liftTarget)

        val scales = FloatArray(templates.size) { BottomBarAnimTool.getScale(templates[it].id) }
        return BottomBarLayoutTool.compute(
            screenWidth, screenHeight, templates, scales,
            BottomBarAnimTool.getUpperRowLiftProgress()
        )
    }

    // ==================== 触摸 ====================

    fun onTouchEvent(event: MotionEvent): Boolean {
        if (BottomBarItemPanelProxy.isShowing()) {
            if (BottomBarItemPanelProxy.onTouchEvent(event)) return true
        }

        val l = computeLayout() ?: return false
        layout = l

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val idx = findIndexAt(l, event.x, event.y)
                if (idx >= 0) {
                    pressedId = templates[idx].id
                    BottomBarAnimTool.onPress(pressedId!!)
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> return pressedId != null
            MotionEvent.ACTION_UP -> {
                val pid = pressedId ?: return false
                pressedId = null
                BottomBarAnimTool.onRelease(pid)

                val l2 = computeLayout() ?: return false
                layout = l2
                val hit = findIndexAt(l2, event.x, event.y)
                val origIdx = templates.indexOfFirst { it.id == pid }
                if (hit >= 0 && hit == origIdx) {
                    handleClick(hit)
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                pressedId?.let { BottomBarAnimTool.onCancel(it) }
                pressedId = null
                return true
            }
        }
        return false
    }

    private fun handleClick(idx: Int) {
        if (idx < 0 || idx >= templates.size) return
        val t = templates[idx]

        // 一次性动作：只调 onClick，不进入激活态
        if (!t.isToggle) {
            t.onClick()
            return
        }

        val wasActive = activeStates[t.id] ?: false
        if (wasActive) {
            // 取消激活：只切状态 + onDeactivate，不调 onClick
            setButtonActive(t.id, false)
            t.onDeactivate()
        } else {
            // 激活：互斥取消其他 → setActive → onClick
            val mutex = t.mutexGroup
            if (mutex != null) {
                for (other in templates) {
                    if (other.id != t.id && other.mutexGroup == mutex && activeStates[other.id] == true) {
                        setButtonActive(other.id, false)
                        other.onDeactivate()
                    }
                }
            }
            setButtonActive(t.id, true)
            t.onClick()
        }
    }

    private fun findIndexAt(l: BottomBarLayout, x: Float, y: Float): Int {
        for (i in l.buttonRects.indices) {
            if (l.buttonRects[i].contains(x, y)) return i
        }
        return -1
    }

    // ==================== 绘制 ====================

    fun draw(canvas: Canvas) {
        BottomBarAnimTool.update()

        val l = computeLayout()
        layout = l

        if (l != null) {
            val labelTextSize = GameConstants.getSmallFontSize(screenHeight)

            for (i in templates.indices) {
                val t = templates[i]
                val rect = l.buttonRects[i]

                BottomBarDrawTool.drawButton(
                    canvas = canvas,
                    rect = rect,
                    icon = BottomBarIconTool.get(t.id),
                    alpha = BottomBarAnimTool.getAlpha(t.id),
                    active = activeStates[t.id] ?: false,
                    displayName = t.displayName,
                    labelTextSize = labelTextSize,
                    labelAlpha = BottomBarAnimTool.getLabelAlpha(t.id),
                    labelOffsetProgress = BottomBarAnimTool.getLabelOffsetProgress(t.id)
                )
            }
        }

        BottomBarItemPanelProxy.draw(canvas)
        BottomBarUnitInfoPanelProxy.draw(canvas)
    }
}