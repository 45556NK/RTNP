package com.rtnp.demo.factory.templates

import android.graphics.Color
import android.graphics.RectF
import com.rtnp.demo.TranslationTable
import com.rtnp.demo.core.InventoryItem
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.core.TemplateRegistry
import com.rtnp.demo.factory.FactorySlotData
import com.rtnp.demo.factory.FactoryTemplate
import com.rtnp.demo.factory.FactoryTools
import com.rtnp.demo.logger.Logger
import com.rtnp.demo.logic.DockingSystem
import com.rtnp.demo.strategy.CooldownMode
import com.rtnp.demo.ui.InfoBoxTool
import com.rtnp.demo.ui.WarningMessage
import com.rtnp.demo.ui.hexselector.HexSelectorTool

/**
 * 船坞
 * 消耗一定的资源来生产舰船
 *
 * 生产队列（"梯队"）为模板级数据，存储在 FactorySlotData 的 "queue" 参数中。
 * 序列化格式："单位名:数量;单位名:数量"（如 "测试单位:3;测试工程:1"）
 */
class ShipyardFactoryTemplate : FactoryTemplate() {

    override val id = "shipyard"
    override val displayName = "船坞"
    override val description = "消耗一定的资源来生产舰船"

    override val cooldown = 0f
    override val cooldownMode = CooldownMode.MANUAL

    override val defaultParams: Map<String, Any> = mapOf(
        "productionRate" to 100f,
        "queue" to "",
        "_progress" to 0f,
        "_consumedResource" to false,
        "_warnedFull" to false
    )

    // ==================== 队列序列化 ====================

    private fun parseQueue(s: String): List<Pair<String, Int>> {
        if (s.isEmpty()) return emptyList()
        return s.split(";").mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size != 2) return@mapNotNull null
            val name = parts[0]
            val count = parts[1].toIntOrNull() ?: return@mapNotNull null
            if (name.isEmpty() || count <= 0) null else Pair(name, count)
        }
    }

    private fun serializeQueue(queue: List<Pair<String, Int>>): String {
        return queue.filter { it.second > 0 }.joinToString(";") { "${it.first}:${it.second}" }
    }

    // ==================== 每帧更新 ====================

    override fun onUpdate(
        host: PlacedObject,
        slotIndex: Int,
        deltaTime: Float,
        tools: FactoryTools,
        slotData: FactorySlotData
    ) {
        val productionRate = slotData.getFloat("productionRate") ?: 100f
        val queue = parseQueue(slotData.getString("queue") ?: "")

        if (queue.isEmpty()) {
            tools.clearButtonState(host, slotIndex)
            slotData.put("_progress", 0f)
            slotData.put("_consumedResource", false)
            return
        }

        val (targetName, _) = queue.first()
        val targetTemplate = TemplateRegistry.templates.firstOrNull { it.name == targetName }
        if (targetTemplate == null) {
            Logger.e("Shipyard", "找不到模板: $targetName")
            return
        }

        val buildAmount = targetTemplate.buildAmount.takeIf { it > 0f } ?: 1000f

        var progress = slotData.getFloat("_progress") ?: 0f
        var consumedResource = slotData.getBoolean("_consumedResource") ?: false
        var warnedFull = slotData.getBoolean("_warnedFull") ?: false

        val totalSlots = DockingSystem.getSlotCount(host)
        val occupiedSlots = host.dockedList.size + host.dockingList.size
        val hasFreeSlot = totalSlots > 0 && occupiedSlots < totalSlots

        // 停泊位满：暂停并警告
        if (!hasFreeSlot) {
            if (!warnedFull) {
                WarningMessage.show(TranslationTable.get("shipyard_warn_dock_full"))
                warnedFull = true
                slotData.put("_warnedFull", true)
            }
            return
        } else {
            if (warnedFull) {
                warnedFull = false
                slotData.put("_warnedFull", false)
            }
        }

        // 扣资源（仅第一次）
        if (!consumedResource) {
            val costItem = targetTemplate.costItem
            val costAmount = targetTemplate.costAmount
            if (!costItem.isNullOrEmpty() && costAmount > 0) {
                val have = host.collectedItems?.get(costItem) ?: 0
                if (have < costAmount) {
                    return
                }
                host.collectedItems?.put(costItem, have - costAmount)
            }
            consumedResource = true
            slotData.put("_consumedResource", true)
        }

        // 推进生产
        progress += deltaTime * productionRate
        if (progress >= buildAmount) {
            progress = 0f
            consumedResource = false
            slotData.put("_progress", 0f)
            slotData.put("_consumedResource", false)

            // 从队列移除 1 个
            val newQueue = queue.toMutableList()
            val first = newQueue[0]
            if (first.second <= 1) {
                newQueue.removeAt(0)
            } else {
                newQueue[0] = Pair(first.first, first.second - 1)
            }
            slotData.put("queue", serializeQueue(newQueue))

            // ★ 通知选择器刷新信息框（局部更新文本）
            HexSelectorTool.refreshInfoBox()

            val newUnit = tools.spawnUnit(targetName, host.worldX, host.worldY)
            if (newUnit != null) {
                DockingSystem.requestDock(newUnit, host)
            }

            tools.clearButtonState(host, slotIndex)
        } else {
            slotData.put("_progress", progress)
            val progressRatio = (progress / buildAmount).coerceIn(0f, 1f)
            tools.setProgressBar(host, slotIndex, progressRatio, Color.argb(200, 100, 200, 255))
        }
    }

    // ==================== 点击工厂按钮 ====================

    override fun onClicked(
        host: PlacedObject,
        slotIndex: Int,
        tools: FactoryTools,
        slotData: FactorySlotData
    ) {
        // 只列出普通单位：过滤敌对 + 不可操控
        val allTemplates = TemplateRegistry.templates.filter {
            it.type == "unit" &&
            it.faction != 1 &&
            it.playerControllable
        }

        val hexBgColor = Color.argb(38, 200, 200, 200)

        val hexItems = mutableListOf<HexSelectorTool.HexItem>()
        hexItems.add(
            HexSelectorTool.HexItem(
                backgroundColor = hexBgColor,
                infoText = "空",
                hexText = "空",
                iconPath = null
            )
        )

        for (t in allTemplates) {
            val hasTexture = t.useTexture && !t.texturePath.isNullOrEmpty()
            if (hasTexture) {
                hexItems.add(
                    HexSelectorTool.HexItem(
                        backgroundColor = hexBgColor,
                        infoText = t.name,
                        hexText = "",
                        iconPath = t.texturePath
                    )
                )
            } else {
                hexItems.add(
                    HexSelectorTool.HexItem(
                        backgroundColor = hexBgColor,
                        infoText = t.name,
                        hexText = t.name,
                        iconPath = null
                    )
                )
            }
        }

        val screenW = tools.getScreenWidth()
        val screenH = tools.getScreenHeight()

        val provider = object : HexSelectorTool.InfoBoxProvider {
            override fun buildInfoBox(infoBoxId: Int, selectedIndex: Int, infoBoxRect: RectF) {
                buildShipyardInfoBox(infoBoxId, infoBoxRect, host, slotData)
            }

            override fun updateInfoBox(infoBoxId: Int, selectedIndex: Int) {
                // 只刷新文本，不重建按钮
                refreshShipyardInfoText(infoBoxId, host, slotData)
            }

            override fun onButtonClick(infoBoxId: Int, buttonIndex: Int, selectedIndex: Int) {
                when (buttonIndex) {
                    0 -> {
                        handleMinus(selectedIndex, host, slotData, allTemplates)
                        refreshShipyardInfoText(infoBoxId, host, slotData)
                    }
                    1 -> {
                        handlePlus(selectedIndex, host, slotData, allTemplates)
                        refreshShipyardInfoText(infoBoxId, host, slotData)
                    }
                    2 -> {
                        HexSelectorTool.cancelSelection()
                        HexSelectorTool.hide(true)
                    }
                }
            }
        }

        HexSelectorTool.show(
            startX = screenW / 2f,
            startY = screenH / 2f,
            hexItems = hexItems,
            listener = object : HexSelectorTool.SelectorListener {
                override fun onConfirmed(index: Int) {}
                override fun onCancelled() {}
            },
            infoBoxProvider = provider
        )
    }

    // ==================== 信息框构建 ====================

    /** 构建完整信息框（文本 + 3 个按钮），仅在首次选中时调用 */
    private fun buildShipyardInfoBox(
        infoBoxId: Int,
        infoBoxRect: RectF,
        host: PlacedObject,
        slotData: FactorySlotData
    ) {
        // 文本
        refreshShipyardInfoText(infoBoxId, host, slotData)

        // 按钮
        val W = infoBoxRect.width()
        val H = infoBoxRect.height()
        val btnSize = H / 4f
        val gap = H / 20f
        val btnY = H - btnSize - gap

        val minusX = W / 2f - btnSize - gap / 2f
        val plusX = W / 2f + gap / 2f
        val backX = W - btnSize - gap

        // 减少（浅红）
        InfoBoxTool.addButton(
            taskId = infoBoxId,
            x = minusX,
            y = btnY,
            size = btnSize,
            bgColor = Color.rgb(255, 182, 193),
            label = TranslationTable.get("shipyard_btn_minus"),
            texturePath = "images/ui/panel_minus_btn.png",
            iconScale = 0.8f
        )

        // 增加（浅蓝）
        InfoBoxTool.addButton(
            taskId = infoBoxId,
            x = plusX,
            y = btnY,
            size = btnSize,
            bgColor = Color.rgb(173, 216, 230),
            label = TranslationTable.get("shipyard_btn_plus"),
            texturePath = "images/ui/panel_plus_btn.png",
            iconScale = 0.8f
        )

        // 返回（浅灰，右下角）
        InfoBoxTool.addButton(
            taskId = infoBoxId,
            x = backX,
            y = btnY,
            size = btnSize,
            bgColor = Color.rgb(200, 200, 200),
            label = TranslationTable.get("shipyard_btn_back"),
            texturePath = "images/ui/panel_back_btn.png",
            iconScale = 0.8f
        )
    }

    /** 只刷新文本，不重建按钮 */
    private fun refreshShipyardInfoText(
        infoBoxId: Int,
        host: PlacedObject,
        slotData: FactorySlotData
    ) {
        val queue = parseQueue(slotData.getString("queue") ?: "")
        val totalCount = queue.sumOf { it.second }

        // 按资源类型累积消耗
        val resourceMap = mutableMapOf<String, Int>()
        for ((name, count) in queue) {
            val t = TemplateRegistry.templates.firstOrNull { it.name == name } ?: continue
            val costItem = t.costItem
            val costAmount = t.costAmount
            if (!costItem.isNullOrEmpty() && costAmount > 0) {
                resourceMap[costItem] = (resourceMap[costItem] ?: 0) + count * costAmount
            }
        }

        val quantityLabel = TranslationTable.get("shipyard_quantity")
        val resourceLabel = TranslationTable.get("shipyard_resource")

        val line1 = "$quantityLabel: $totalCount"

        val line2: String
        var hasInsufficient = false
        if (resourceMap.isEmpty()) {
            line2 = "$resourceLabel: -"
        } else {
            val sb = StringBuilder("$resourceLabel: ")
            var first = true
            for ((item, needed) in resourceMap) {
                if (!first) sb.append("; ")
                first = false
                val have = host.collectedItems?.get(item) ?: 0
                sb.append("$item: $needed/$have")
                if (have < needed) hasInsufficient = true
            }
            line2 = sb.toString()
        }

        val textColor = if (hasInsufficient) Color.RED else Color.WHITE
        InfoBoxTool.setText(infoBoxId, "$line1\n$line2", textColor)
    }

    // ==================== 加减逻辑 ====================

    private fun handleMinus(
        selectedIndex: Int,
        host: PlacedObject,
        slotData: FactorySlotData,
        allTemplates: List<InventoryItem>
    ) {
        if (selectedIndex <= 0) return
        val selectedTemplate = allTemplates.getOrNull(selectedIndex - 1) ?: return

        val queue = parseQueue(slotData.getString("queue") ?: "").toMutableList()
        val idx = queue.indexOfFirst { it.first == selectedTemplate.name }
        if (idx < 0) return

        val current = queue[idx].second
        if (current <= 1) {
            queue.removeAt(idx)
        } else {
            queue[idx] = Pair(queue[idx].first, current - 1)
        }
        slotData.put("queue", serializeQueue(queue))
    }

    private fun handlePlus(
        selectedIndex: Int,
        host: PlacedObject,
        slotData: FactorySlotData,
        allTemplates: List<InventoryItem>
    ) {
        if (selectedIndex <= 0) return
        val selectedTemplate = allTemplates.getOrNull(selectedIndex - 1) ?: return

        val costItem = selectedTemplate.costItem
        val costAmount = selectedTemplate.costAmount

        // 统计队列中该资源已规划的消耗
        val queue = parseQueue(slotData.getString("queue") ?: "").toMutableList()
        var existingCost = 0
        for ((name, count) in queue) {
            val t = TemplateRegistry.templates.firstOrNull { it.name == name } ?: continue
            if (t.costItem == costItem) {
                existingCost += count * t.costAmount
            }
        }

        // 检查资源是否足够
        if (!costItem.isNullOrEmpty() && costAmount > 0) {
            val have = host.collectedItems?.get(costItem) ?: 0
            if (existingCost + costAmount > have) {
                WarningMessage.show(TranslationTable.get("shipyard_warn_no_resource"))
                return
            }
        }

        // 加入队列
        val idx = queue.indexOfFirst { it.first == selectedTemplate.name }
        if (idx >= 0) {
            queue[idx] = Pair(queue[idx].first, queue[idx].second + 1)
        } else {
            queue.add(Pair(selectedTemplate.name, 1))
        }
        slotData.put("queue", serializeQueue(queue))
    }
}