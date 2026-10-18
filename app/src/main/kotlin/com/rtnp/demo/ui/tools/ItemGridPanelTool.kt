package com.rtnp.demo.ui.tools

import android.graphics.Canvas
import android.graphics.RectF
import android.view.MotionEvent
import com.rtnp.demo.core.InventoryItem
import com.rtnp.demo.image.ImageManager
import com.rtnp.demo.ui.tools.ItemGridPanelAnimTool
import com.rtnp.demo.ui.tools.ItemGridPanelDrawTool
import com.rtnp.demo.ui.tools.ItemGridPanelLayoutTool

/**
 * 通用物品展示面板（主工具，object 单例，有状态）。
 */
object ItemGridPanelTool {

    /** 面板用途模式（供触摸层决策） */
    enum class PanelMode { ITEM, BUILD }

    private const val TAP_MOVE_THRESHOLD_RATIO = 1f / 100f

    private var isVisible = false
    private var items: List<InventoryItem> = emptyList()
    private var multiSelect = false
    private var mode: PanelMode = PanelMode.ITEM
    private var onSelectionChanged: ((InventoryItem?) -> Unit)? = null

    private val selectedItems = mutableListOf<InventoryItem>()

    private var screenWidth = 0
    private var screenHeight = 0

    private var scrollY = 0f
    private var maxScroll = 0f

    private var touchDownX = 0f
    private var touchDownY = 0f
    private var touchDownScroll = 0f
    private var isDragging = false
    private var dragging = false

    private val panelRect = RectF()
    private var context: android.content.Context? = null

    // ==================== 系统 ====================

    fun init(ctx: android.content.Context) {
        context = ctx.applicationContext
    }

    fun setScreenSize(w: Int, h: Int) {
        screenWidth = w
        screenHeight = h
        refreshLayout()
    }

    private fun refreshLayout() {
        if (screenWidth <= 0 || screenHeight <= 0) return
        val r = ItemGridPanelLayoutTool.computePanelRect(screenWidth, screenHeight)
        panelRect.set(r)
        maxScroll = ItemGridPanelLayoutTool.getMaxScroll(panelRect, items.size)
        scrollY = scrollY.coerceIn(0f, maxScroll)
    }

    // ==================== 显示 / 隐藏 ====================

    fun show(
        items: List<InventoryItem>,
        multiSelect: Boolean,
        mode: PanelMode = PanelMode.ITEM,
        onSelectionChanged: (InventoryItem?) -> Unit
    ) {
        if (items.isEmpty()) { hide(); return }
        this.items = items
        this.multiSelect = multiSelect
        this.mode = mode
        this.onSelectionChanged = onSelectionChanged
        this.selectedItems.clear()
        this.scrollY = 0f
        isVisible = true

        ItemGridPanelAnimTool.clear()
        refreshLayout()
    }

    fun hide() {
        isVisible = false
        items = emptyList()
        selectedItems.clear()
        onSelectionChanged = null
        scrollY = 0f
        maxScroll = 0f
        ItemGridPanelAnimTool.clear()
    }

    fun isShowing(): Boolean = isVisible

    fun getSelectedItems(): List<InventoryItem> = selectedItems.toList()

    fun getMode(): PanelMode = mode

    fun getPanelRect(): RectF = panelRect

    // ==================== 触摸 ====================

    fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isVisible) return false
        val x = event.x
        val y = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (!panelRect.contains(x, y)) return false
                touchDownX = x
                touchDownY = y
                touchDownScroll = scrollY
                isDragging = false
                dragging = true
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!dragging) return false
                val dx = x - touchDownX
                val dy = y - touchDownY
                if (!isDragging) {
                    val threshold = screenWidth * TAP_MOVE_THRESHOLD_RATIO
                    if (Math.hypot(dx.toDouble(), dy.toDouble()) > threshold) isDragging = true
                }
                if (isDragging) scrollY = (touchDownScroll - dy).coerceIn(0f, maxScroll)
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (!dragging) return false
                dragging = false
                if (!isDragging && panelRect.contains(x, y)) handleTap(x, y)
                isDragging = false
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                dragging = false
                isDragging = false
                return true
            }
        }
        return false
    }

    private fun handleTap(x: Float, y: Float) {
        val idx = ItemGridPanelLayoutTool.hitTestSlot(panelRect, items.size, scrollY, x, y)
        if (idx < 0) return
        val item = items[idx]
        val alreadySelected = selectedItems.contains(item)

        if (alreadySelected) {
            selectedItems.remove(item)
            ItemGridPanelAnimTool.setTarget(item.name, 0f)
            onSelectionChanged?.invoke(null)
        } else {
            if (!multiSelect) {
                for (old in selectedItems) ItemGridPanelAnimTool.setTarget(old.name, 0f)
                selectedItems.clear()
            }
            selectedItems.add(item)
            ItemGridPanelAnimTool.setTarget(item.name, 1f)
            onSelectionChanged?.invoke(item)
        }
    }

    // ==================== 绘制 ====================

    fun draw(canvas: Canvas) {
        if (!isVisible) return
        if (panelRect.isEmpty) return

        ItemGridPanelAnimTool.update()

        ItemGridPanelDrawTool.drawPanelBackground(canvas, panelRect)

        canvas.save()
        ItemGridPanelDrawTool.clipToPanel(canvas, panelRect)

        for (i in items.indices) {
            val slotRect = ItemGridPanelLayoutTool.getSlotRect(panelRect, items.size, i, scrollY)
            if (slotRect.bottom < panelRect.top || slotRect.top > panelRect.bottom) continue

            val item = items[i]
            val selected = selectedItems.contains(item)
            val icon = loadIcon(item)
            val contentScale = ItemGridPanelAnimTool.getScale(item.name)

            ItemGridPanelDrawTool.drawSlot(
                canvas = canvas,
                slotRect = slotRect,
                item = item,
                selected = selected,
                contentScale = contentScale,
                icon = icon
            )
        }
        canvas.restore()
    }

    private fun loadIcon(item: InventoryItem): android.graphics.Bitmap? {
        if (!item.useTexture || item.texturePath.isNullOrEmpty()) return null
        val ctx = context ?: return null
        return try {
            ImageManager.getInstance(ctx).getBitmap(item.texturePath!!)
        } catch (_: Exception) { null }
    }
}