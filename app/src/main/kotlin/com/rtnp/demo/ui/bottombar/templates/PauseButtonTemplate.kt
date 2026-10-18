package com.rtnp.demo.ui.bottombar.templates

import com.rtnp.demo.ui.bottombar.BottomBarButtonTemplate
import com.rtnp.demo.ui.bottombar.BottomBarGlobal

class PauseButtonTemplate : BottomBarButtonTemplate() {

    override val id = "pause"
    override val displayName = "暂停"
    override val iconPath = "images/pause_btn.png"
    override val isToggle: Boolean = false  // 一次性动作，不进入激活态

    override fun onClick() {
        BottomBarGlobal.onPauseRequested?.invoke()
    }
}