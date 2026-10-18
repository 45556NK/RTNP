package com.rtnp.demo.ui.bottombar

import android.graphics.Bitmap

/**
 * 底部栏按钮配置（数据类，不可变更 id / iconPath / displayName）。
 */
class BottomBarButton(
    val id: String,
    val iconPath: String,
    val displayName: String,
    var onClick: (() -> Unit)? = null,
    var icon: Bitmap? = null,
    var isActive: Boolean = false
)