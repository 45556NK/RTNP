package com.rtnp.demo.combat

/**
 * 伤害类型。
 * 用于立场判断是否吸收某类伤害。
 */
enum class DamageType {
    /** 轨道炮子弹 */
    RAILGUN,
    /** 激光 */
    LASER,
    /** 脉冲 */
    PULSE,
    /** 爆炸伤害（导弹爆炸、压制爆炸、通用爆炸） */
    EXPLOSION,
    /** 通用环境伤害（无来源） */
    ENVIRONMENT,
}