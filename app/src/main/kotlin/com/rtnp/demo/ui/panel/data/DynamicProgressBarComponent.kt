package com.rtnp.demo.ui.panel.data

/**
 * 动态进度条组件。
 * 由外部（如立场模板）在运行时添加到面板中。
 *
 * - 通过 placeholderId 声明插入到哪个占位符之后
 * - slotKey 是唯一标识，用于去重/更新/移除
 * - 按住时在面板左侧显示 detailLayout 详情栏
 */
class DynamicProgressBarComponent(
    val placeholderId: String,
    val slotKey: String,
    var bgColor: Int,
    var progress: Float,
    var fillColor: Int,
    var label: String,
    var detailLayout: List<PanelComponent> = emptyList()
) : PanelComponent() {
    /** 是否被按住（用于显示详情栏） */
    var isPressed: Boolean = false
}