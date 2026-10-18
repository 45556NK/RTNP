package com.rtnp.demo.ui.bottombar

/**
 * 底部栏模板与外部（GameView）的桥接回调。
 * 只放最基本的"外部动作请求"，不放业务数据。
 */
object BottomBarGlobal {
    /** 暂停按钮被点击时，外部应触发暂停 */
    var onPauseRequested: (() -> Unit)? = null
}