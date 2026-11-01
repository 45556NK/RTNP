package com.rtnp.demo.strategy.tools

import android.graphics.Color
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.ui.panel.data.PanelComponent
import com.rtnp.demo.ui.panel.tool.TemplateProgressBarTool

/**
 * 策略进度条工具（供右侧栏显示策略进度）。
 *
 * 数据会写入 host.dynamicPanelComponents，slotKey = "strategy_$strategyId"
 * 目前右侧栏模板尚未加 Placeholder("strategy_slot")，因此暂不渲染；
 * 后续在 NormalUnitTemplate / BaseUnitTemplate / NonControllableUnitTemplate
 * 里加一行 components.add(PanelComponent.Placeholder("strategy_slot")) 后即刻生效。
 *
 * 使用方式：StrategyTools.progressBar.xxx(...)
 */
object StrategyProgressBarTool {

    /** 右侧栏中策略进度条的占位符 ID */
    const val STRATEGY_PLACEHOLDER_ID = "strategy_slot"

    /** 默认背景色（半透明白） */
    private const val DEFAULT_BG_COLOR = 0x64C8C8C8

    /**
     * 语义版：显示进度条（带详情栏）。
     *
     * @param host 宿主单位
     * @param strategyId 策略 ID（模板里的 `override val id`）
     * @param displayName 详情栏第一行显示的标题（一般为策略的 displayName）
     * @param current 当前值（如已充能量）
     * @param max 最大值（如充能总量），<=0 时视为 0 进度
     * @param color 填充色
     * @param extraDescription 详情栏额外描述（可选）
     */
    @JvmStatic
    @JvmOverloads
    fun setProgressBar(
        host: PlacedObject,
        strategyId: String,
        displayName: String,
        current: Float,
        max: Float,
        color: Int,
        extraDescription: String? = null
    ) {
        val ratio = if (max > 0f) (current / max).coerceIn(0f, 1f) else 0f
        val percent = (ratio * 100).toInt()
        val label = "$displayName: $percent%"

        val detailLayout = buildDetailLayout(
            displayName = displayName,
            current = current,
            max = max,
            extraDescription = extraDescription
        )

        setProgressBarRaw(
            host = host,
            strategyId = strategyId,
            progress = ratio,
            color = color,
            label = label,
            detailLayout = detailLayout
        )
    }

    /**
     * 底层版：显示进度条（调用者自行提供 label 和详情栏）。
     */
    @JvmStatic
    @JvmOverloads
    fun setProgressBarRaw(
        host: PlacedObject,
        strategyId: String,
        progress: Float,
        color: Int,
        label: String = "",
        detailLayout: List<PanelComponent> = emptyList()
    ) {
        val slotKey = "strategy_$strategyId"
        TemplateProgressBarTool.show(
            host = host,
            placeholderId = STRATEGY_PLACEHOLDER_ID,
            slotKey = slotKey,
            bgColor = DEFAULT_BG_COLOR,
            progress = progress.coerceIn(0f, 1f),
            fillColor = color,
            label = label,
            detailLayout = detailLayout
        )
    }

    /**
     * 移除进度条。
     */
    @JvmStatic
    fun clearButtonState(host: PlacedObject, strategyId: String) {
        val slotKey = "strategy_$strategyId"
        TemplateProgressBarTool.hide(host, slotKey)
    }

    /**
     * 构造详情栏布局（语义版默认实现）。
     */
    @JvmStatic
    @JvmOverloads
    fun buildDetailLayout(
        displayName: String,
        current: Float,
        max: Float,
        extraDescription: String? = null
    ): List<PanelComponent> {
        val list = mutableListOf<PanelComponent>()
        list.add(PanelComponent.Text(content = displayName, textSize = 32f))
        list.add(PanelComponent.Spacer(10f))
        if (max > 0f) {
            list.add(PanelComponent.Text(
                content = "${current.toInt()}/${max.toInt()}",
                textSize = 28f
            ))
        } else {
            list.add(PanelComponent.Text(
                content = current.toInt().toString(),
                textSize = 28f
            ))
        }
        if (!extraDescription.isNullOrEmpty()) {
            list.add(PanelComponent.Text(content = extraDescription, textSize = 24f))
        }
        return list
    }
}