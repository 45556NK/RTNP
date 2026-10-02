package com.rtnp.demo.core

import com.rtnp.demo.data.units.B1CarrierRocketTemplate
import com.rtnp.demo.data.units.ClusterRocketTemplate
import com.rtnp.demo.data.units.DriftingBeaconTemplate
import com.rtnp.demo.data.units.PulseTowerTemplate
import com.rtnp.demo.data.units.RepairShipTemplate
import com.rtnp.demo.data.units.TeleportAnchorTemplate
import com.rtnp.demo.data.units.TestTextureTemplate   // ★ 添加

object UnitTemplateRegistration {
    fun registerNormalUnitTemplates(registry: TemplateRegistry) {
        registry.addTemplate(B1CarrierRocketTemplate().provide())
        registry.addTemplate(ClusterRocketTemplate().provide())
        registry.addTemplate(DriftingBeaconTemplate().provide())
        registry.addTemplate(PulseTowerTemplate().provide())
        registry.addTemplate(RepairShipTemplate().provide())
        registry.addTemplate(TeleportAnchorTemplate().provide())
        registry.addTemplate(TestTextureTemplate().provide())   // ★ 添加
    }
}