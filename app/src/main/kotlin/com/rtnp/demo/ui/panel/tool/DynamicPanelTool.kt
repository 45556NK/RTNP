package com.rtnp.demo.ui.panel.tool

import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.ui.panel.data.DynamicProgressBarComponent
import com.rtnp.demo.ui.panel.data.PanelComponent

/**
 * 动态面板组件工具。
 * 外部通过此工具向单位的面板中添加 / 更新 / 移除动态进度条。
 *
 * 生命周期：持久化，调用者显式移除。
 */
object DynamicPanelTool {

    /**
     * 添加或更新一个动态进度条。
     * 如果同 slotKey 已存在，则更新其参数；否则新建并追加。
     *
     * @param host 宿主单位
     * @param placeholderId 目标占位符 ID
     * @param slotKey 唯一标识（同一单位内应唯一）
     * @param bgColor 背景色
     * @param progress 进度 0~1
     * @param fillColor 填充色
     * @param label 进度条上方文字
     * @param detailLayout 按住时弹出的详情栏内容
     */
    @JvmStatic
    fun addOrUpdate(
        host: PlacedObject,
        placeholderId: String,
        slotKey: String,
        bgColor: Int,
        progress: Float,
        fillColor: Int,
        label: String,
        detailLayout: List<PanelComponent> = emptyList()
    ): DynamicProgressBarComponent {
        val existing = host.dynamicPanelComponents.firstOrNull { it.slotKey == slotKey }
        if (existing != null) {
            existing.bgColor = bgColor
            existing.progress = progress
            existing.fillColor = fillColor
            existing.label = label
            existing.detailLayout = detailLayout
            return existing
        }
        val comp = DynamicProgressBarComponent(
            placeholderId = placeholderId,
            slotKey = slotKey,
            bgColor = bgColor,
            progress = progress,
            fillColor = fillColor,
            label = label,
            detailLayout = detailLayout
        )
        host.dynamicPanelComponents.add(comp)
        return comp
    }

    /**
     * 移除指定单位的某个动态组件。
     */
    @JvmStatic
    fun remove(host: PlacedObject, slotKey: String) {
        host.dynamicPanelComponents.removeAll { it.slotKey == slotKey }
    }

    /**
     * 移除指定单位的所有动态组件。
     * 通常在单位死亡时调用。
     */
    @JvmStatic
    fun clear(host: PlacedObject) {
        host.dynamicPanelComponents.clear()
    }

    /**
     * 取指定占位符下所有动态组件。
     * 返回顺序与插入顺序一致。
     */
    @JvmStatic
    fun getByPlaceholder(host: PlacedObject, placeholderId: String): List<DynamicProgressBarComponent> {
        return host.dynamicPanelComponents.filter { it.placeholderId == placeholderId }
    }
}