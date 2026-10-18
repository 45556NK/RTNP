package com.rtnp.demo.ui.bottombar.tools

import android.content.Context
import android.graphics.Bitmap
import com.rtnp.demo.image.ImageManager

/**
 * 底部栏贴图注册表（工具，静态）。
 *
 * 职责：
 *  - 按 id 缓存贴图 Bitmap
 *  - 供系统绘制时查询
 *
 * 系统不知道路径，路径只在注册时由模板提供。
 */
object BottomBarIconTool {

    private val bitmapCache = mutableMapOf<String, Bitmap?>()
    private val pathCache = mutableMapOf<String, String>()

    /** 注册一个贴图（加载失败时缓存 null，避免重复尝试） */
    @JvmStatic
    fun register(id: String, iconPath: String, context: Context) {
        pathCache[id] = iconPath
        bitmapCache[id] = try {
            ImageManager.getInstance(context).getBitmap(iconPath)
        } catch (_: Exception) {
            null
        }
    }

    /** 查询贴图 */
    @JvmStatic
    fun get(id: String): Bitmap? = bitmapCache[id]

    /** 注销 */
    @JvmStatic
    fun unregister(id: String) {
        bitmapCache.remove(id)
        pathCache.remove(id)
    }

    /** 清空 */
    @JvmStatic
    fun clear() {
        bitmapCache.clear()
        pathCache.clear()
    }
}