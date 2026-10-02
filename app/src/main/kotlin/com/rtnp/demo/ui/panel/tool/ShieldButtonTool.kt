package com.rtnp.demo.ui.panel.tool

import android.content.Context
import android.graphics.Color
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.image.ImageManager
import com.rtnp.demo.shield.ShieldManager
import com.rtnp.demo.ui.panel.data.InteractiveButtonComponent
import com.rtnp.demo.TranslationTable
import com.rtnp.demo.ui.WarningMessage

object ShieldButtonTool {

    private const val BORDER_IMAGE = "images/strategy_border.svg"

    /** 浅蓝底色 */
    private const val SHIELD_BG_COLOR = 0x40ADD8E6  // argb(64, 173, 216, 230)

    /**
     * 创建立场按钮。
     * @param slotIndex 槽位索引
     * @param vw 屏幕宽度
     * @param context 上下文
     * @param unit 宿主单位
     */
    @JvmStatic
    fun create(slotIndex: Int, vw: Int, context: Context, unit: PlacedObject): InteractiveButtonComponent {
        val size = InteractiveButtonTool.calculateSize(vw)
        val borderBitmap = ImageManager.getInstance(context).getBitmap(BORDER_IMAGE)

        val config = unit.shieldSlotConfigs.getOrNull(slotIndex)
        val shieldId = config?.shieldId ?: ""
        val template = if (shieldId.isNotEmpty()) ShieldManager.getTemplate(shieldId) else null

        val iconBitmap = template?.iconPath?.let {
            ImageManager.getInstance(context).getBitmap(it)
        }

        val label = when {
            iconBitmap != null -> ""
            template != null -> template.displayName
            else -> "空立场"
        }

        val animLabel = template?.displayName ?: ""

        val btn = InteractiveButtonComponent(
            label = label,
            width = size,
            height = size,
            backgroundColor = SHIELD_BG_COLOR,
            borderBitmap = borderBitmap,
            iconBitmap = iconBitmap,
            animLabel = animLabel,
            onClick = {
                if (shieldId.isEmpty()) return@InteractiveButtonComponent
                val tmpl = ShieldManager.getTemplate(shieldId) ?: return@InteractiveButtonComponent
                // 只有需要激活的模板才能响应点击
                if (!tmpl.requiresActivation) return@InteractiveButtonComponent
                val data = config?.params ?: return@InteractiveButtonComponent
                // 已激活，忽略
                if (data.isActive) return@InteractiveButtonComponent
                // 冷却中，弹出警告
                if (data.cooldownRemaining > 0f) {
                    WarningMessage.show(TranslationTable.get("shield_cooling"))
                    return@InteractiveButtonComponent
                }
                ShieldManager.activateShield(unit, slotIndex)
            },
            onPressed = { i -> ButtonAnimTool.defaultOnPress(i) },
            onReleased = { i -> ButtonAnimTool.defaultOnRelease(i) },
            onCancelled = { i -> ButtonAnimTool.defaultOnCancel(i) }
        )
        btn.customData = ShieldButtonData(slotIndex)
        return btn
    }

    /**
     * 每帧同步按钮状态（冷却进度条）。
     */
    @JvmStatic
    fun updateShieldButtonData(buttons: List<InteractiveButtonComponent>, unit: PlacedObject) {
        for (btn in buttons) {
            val data = btn.customData as? ShieldButtonData ?: continue
            val config = unit.shieldSlotConfigs.getOrNull(data.slotIndex)
            if (config == null || config.shieldId.isEmpty()) {
                btn.cooldownProgress = 0f
                btn.energyProgress = 0f
                continue
            }
            val slotData = config.params
            // 冷却中：显示冷却进度条（矩形填充）
            if (slotData.cooldownRemaining > 0f && slotData.cooldownDuration > 0f) {
                val progress = (slotData.cooldownRemaining / slotData.cooldownDuration).coerceIn(0f, 1f)
                btn.cooldownProgress = progress
                btn.cooldownColor = if (slotData.cooldownBarColor != 0) slotData.cooldownBarColor
                                    else Color.argb(89, 255, 130, 130)
                btn.cooldownDirection = CooldownRenderTool.Direction.BOTTOM_UP
                btn.energyProgress = 0f
            }
            // 激活中：显示激活剩余时间进度条（环形）
            else if (slotData.isActive && slotData.activationDuration > 0f && slotData.remainingTime > 0f) {
                val progress = (slotData.remainingTime / slotData.activationDuration).coerceIn(0f, 1f)
                btn.energyProgress = progress
                btn.energyColor = if (slotData.activationBarColor != 0) slotData.activationBarColor
                                  else Color.argb(200, 100, 200, 255)
                btn.energyDirection = EnergyRingRenderTool.Direction.CLOCKWISE
                btn.cooldownProgress = 0f
            } else {
                btn.cooldownProgress = 0f
                btn.energyProgress = 0f
            }
        }
    }

    /**
     * 对外方法：为指定槽位设置冷却。
     * 若调用者不想通过 ShieldTools.cooldown，可直接调用此方法。
     */
    @JvmStatic
    @JvmOverloads
    fun setCooldownForSlot(unit: PlacedObject, slotIndex: Int, duration: Float, color: Int = 0) {
        val config = unit.shieldSlotConfigs.getOrNull(slotIndex) ?: return
        val data = config.params
        data.cooldownDuration = duration
        data.cooldownRemaining = duration
        if (color != 0) data.cooldownBarColor = color
    }
}

data class ShieldButtonData(val slotIndex: Int)