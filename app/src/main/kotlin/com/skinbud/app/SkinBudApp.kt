package com.skinbud.app

import android.app.Application
import com.skinbud.app.data.GlowRepository
import com.skinbud.app.data.ProductRepository
import com.skinbud.app.data.RoutineRepository
import com.skinbud.app.reminders.ReminderScheduler

class SkinBudApp : Application() {

    lateinit var glowRepository: GlowRepository
        private set

    lateinit var routineRepository: RoutineRepository
        private set

    lateinit var productRepository: ProductRepository
        private set

    lateinit var cartRepository: com.skinbud.app.data.CartRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        glowRepository = GlowRepository(this)
        routineRepository = RoutineRepository(this)
        productRepository = ProductRepository(this)
        cartRepository = com.skinbud.app.data.CartRepository(this)

        // Initialize reminder notifications
        ReminderScheduler.setupNotificationChannel(this)
    }

    companion object {
        lateinit var instance: SkinBudApp
            private set
    }
}
