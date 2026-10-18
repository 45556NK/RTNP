package com.rtnp.demo.ui.bottombar

abstract class BottomBarButtonTemplate {

    abstract val id: String
    abstract val displayName: String
    abstract val iconPath: String

    /** 是否切换型按钮。true = 有激活态；false = 一次性动作，不进入激活态 */
    open val isToggle: Boolean = true

    /** 互斥组名，同组内只有一个能激活。null = 不互斥 */
    open val mutexGroup: String? = null

    /** 从"非激活"→"激活"时调用（仅 isToggle=true 的按钮） */
    open fun onClick() {}

    /** 从"激活"→"非激活"时调用（仅 isToggle=true 的按钮） */
    open fun onDeactivate() {}
}