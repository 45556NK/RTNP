package com.rtnp.demo.shield

import com.rtnp.demo.shield.templates.AntiExplosionShieldTemplate
import com.rtnp.demo.shield.templates.BasicShieldTemplate

object ShieldTemplateRegistration {

    fun registerShieldTemplates() {
        ShieldManager.register(BasicShieldTemplate())
        ShieldManager.register(AntiExplosionShieldTemplate())
    }
}