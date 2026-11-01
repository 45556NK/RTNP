package com.rtnp.demo.strategy

import com.rtnp.demo.strategy.templates.AntimatterBombStrategy
import com.rtnp.demo.strategy.templates.AreaTeleportStrategy
import com.rtnp.demo.strategy.templates.BeaconLauncherStrategy
import com.rtnp.demo.strategy.templates.BlockBombStrategy
import com.rtnp.demo.strategy.templates.ClusterBombStrategy
import com.rtnp.demo.strategy.templates.DeployTeleportAnchorStrategy
import com.rtnp.demo.strategy.templates.FocusFireStrategy
import com.rtnp.demo.strategy.templates.RegenerationEngineeringStrategy
import com.rtnp.demo.strategy.templates.RepairBeamStrategy
import com.rtnp.demo.strategy.templates.*

/**
 * 策略模板注册表（显式注册）
 *
 * 所有策略模板都在这里手动实例化并注册。
 * 不再使用 DexFile 扫描。
 */
object StrategyTemplateRegistration {

    /**
     * 注册所有策略模板
     * 该方法应在 StrategyManager.init() 中调用。
     */
    fun registerStrategyTemplates() {
        // 反物质炸弹
        StrategyManager.register(AntimatterBombStrategy())
        // 区域传送
        StrategyManager.register(AreaTeleportStrategy())
        // 灯塔发射器
        StrategyManager.register(BeaconLauncherStrategy())
        // 区块炸弹
        StrategyManager.register(BlockBombStrategy())
        // 集群炸弹
        StrategyManager.register(ClusterBombStrategy())
        // 部署传送锚点
        StrategyManager.register(DeployTeleportAnchorStrategy())
        // 脉冲塔
        StrategyManager.register(FocusFireStrategy())
        // 再生工程
        StrategyManager.register(RegenerationEngineeringStrategy())
        // 热重组
        StrategyManager.register(RepairBeamStrategy())
        // 尘埃组装无人机
        StrategyManager.register(SpawnRepairShipStrategy())
        // 空间跨越
        StrategyManager.register(SpaceLeapStrategy())
    }
}