package com.rtnp.demo.ui.panel.tool

import android.content.Context
import android.graphics.Color
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.factory.FactoryManager
import com.rtnp.demo.image.ImageManager
import com.rtnp.demo.ui.panel.data.InteractiveButtonComponent

object FactoryButtonTool {

    private const val BORDER_IMAGE = "images/strategy_border.svg"

    /** 工厂槽底色：浅黄色 */
    private const val FACTORY_BG_COLOR = 0x40FFFFAA  // argb(64, 255, 255, 170)

    private val buttonStates = mutableMapOf<Long, MutableMap<Int, BarState>>()

    enum class BarType { COOLDOWN, PROGRESS }

    data class BarState(
        val type: BarType,
        val progress: Float,
        val color: Int,
        val cooldownDirection: CooldownRenderTool.Direction = CooldownRenderTool.Direction.BOTTOM_UP,
        val progressDirection: EnergyRingRenderTool.Direction = EnergyRingRenderTool.Direction.CLOCKWISE
    )

    /**
     * 工厂模板主动设置进度条/冷却条。
     * 同一槽位只能存在一种（后设置覆盖前者）。
     */
    @JvmStatic
    fun setBar(
        unit: PlacedObject,
        slotIndex: Int,
        type: BarType,
        progress: Float,
        color: Int,
        cooldownDirection: CooldownRenderTool.Direction = CooldownRenderTool.Direction.BOTTOM_UP,
        progressDirection: EnergyRingRenderTool.Direction = EnergyRingRenderTool.Direction.CLOCKWISE
    ) {
        val map = buttonStates.getOrPut(unit.uniqueId) { mutableMapOf() }
        map[slotIndex] = BarState(type, progress.coerceIn(0f, 1f), color, cooldownDirection, progressDirection)
    }

    /** 清空指定槽位的进度显示 */
    @JvmStatic
    fun clear(unit: PlacedObject, slotIndex: Int) {
        buttonStates[unit.uniqueId]?.remove(slotIndex)
    }

    // ==================== 按钮创建 ====================

    @JvmStatic
    fun create(slotIndex: Int, vw: Int, context: Context, unit: PlacedObject): InteractiveButtonComponent {
        val size = InteractiveButtonTool.calculateSize(vw)
        val borderBitmap = ImageManager.getInstance(context).getBitmap(BORDER_IMAGE)

        val config = unit.factorySlotConfigs.getOrNull(slotIndex)
        val factoryId = config?.factoryId ?: ""
        val template = if (factoryId.isNotEmpty()) FactoryManager.getTemplate(factoryId) else null

        val iconBitmap = template?.iconPath?.let {
            ImageManager.getInstance(context).getBitmap(it)
        }

        val label = when {
            iconBitmap != null -> ""
            template != null -> template.displayName
            else -> "空工厂"
        }

        val animLabel = template?.displayName ?: ""

        val btn = InteractiveButtonComponent(
            label = label,
            width = size,
            height = size,
            backgroundColor = FACTORY_BG_COLOR,
            borderBitmap = borderBitmap,
            iconBitmap = iconBitmap,
            animLabel = animLabel,
            onClick = {
                if (factoryId.isEmpty()) return@InteractiveButtonComponent
                FactoryManager.onClickFactory(unit, slotIndex)
            },
            onPressed = { i -> ButtonAnimTool.defaultOnPress(i) },
            onReleased = { i -> ButtonAnimTool.defaultOnRelease(i) },
            onCancelled = { i -> ButtonAnimTool.defaultOnCancel(i) }
        )
        btn.customData = FactoryButtonData(slotIndex)
        return btn
    }

    // ==================== 每帧同步 ====================

    @JvmStatic
    fun updateFactoryButtonData(buttons: List<InteractiveButtonComponent>, unit: PlacedObject) {
        val stateMap = buttonStates[unit.uniqueId]
        for (btn in buttons) {
            val data = btn.customData as? FactoryButtonData ?: continue
            val state = stateMap?.get(data.slotIndex)
            if (state == null) {
                btn.cooldownProgress = 0f
                btn.energyProgress = 0f
                continue
            }
            when (state.type) {
                BarType.COOLDOWN -> {
                    btn.cooldownProgress = state.progress
                    btn.cooldownColor = state.color
                    btn.cooldownDirection = state.cooldownDirection
                    btn.energyProgress = 0f
                }
                BarType.PROGRESS -> {
                    btn.energyProgress = state.progress
                    btn.energyColor = state.color
                    btn.energyDirection = state.progressDirection
                    btn.cooldownProgress = 0f
                }
            }
        }
    }
}

data class FactoryButtonData(val slotIndex: Int)