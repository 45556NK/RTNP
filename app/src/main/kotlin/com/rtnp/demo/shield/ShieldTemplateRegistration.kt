package com.rtnp.demo.shield

import com.rtnp.demo.shield.templates.AntiExplosionShieldTemplate
import com.rtnp.demo.shield.templates.*

object ShieldTemplateRegistration {

    fun registerShieldTemplates() {
        ShieldManager.register(BasicShieldTemplate())
        ShieldManager.register(AntiExplosionShieldTemplate())
        ShieldManager.register(AreaShieldTemplate())
    }
}