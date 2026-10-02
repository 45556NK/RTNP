package com.rtnp.demo.ui.panel.tool

import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.ui.panel.data.PanelComponent

/**
 * 给外部（如立场模板）使用的封装工具。
 * 只传基本参数即可在单位面板中显示动态进度条。
 */
object TemplateProgressBarTool {

    /**
     * 显示或更新动态进度条。
     */
    @JvmStatic
    fun show(
        host: PlacedObject,
        placeholderId: String,
        slotKey: String,
        bgColor: Int,
        progress: Float,
        fillColor: Int,
        label: String,
        detailLayout: List<PanelComponent> = emptyList()
    ) {
        DynamicPanelTool.addOrUpdate(
            host, placeholderId, slotKey,
            bgColor, progress, fillColor, label, detailLayout
        )
    }

    /**
     * 移除指定进度条。
     */
    @JvmStatic
    fun hide(host: PlacedObject, slotKey: String) {
        DynamicPanelTool.remove(host, slotKey)
    }

    /**
     * 移除全部动态进度条。
     */
    @JvmStatic
    fun clear(host: PlacedObject) {
        DynamicPanelTool.clear(host)
    }
}