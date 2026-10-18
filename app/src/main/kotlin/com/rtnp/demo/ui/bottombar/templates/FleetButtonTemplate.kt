package com.rtnp.demo.ui.bottombar.templates

import com.rtnp.demo.ui.WarningMessage
import com.rtnp.demo.ui.bottombar.BottomBarButtonTemplate

class FleetButtonTemplate : BottomBarButtonTemplate() {

    override val id = "fleet"
    override val displayName = "舰队"
    override val iconPath = "images/fleet_btn.png"
    override val isToggle: Boolean = false  // 临时占位（未来会改成切换型）

    override fun onClick() {
        WarningMessage.show("新的舰队列表还在制作中...")
    }
}