package ru.anseranser.trumpcards

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import ru.anseranser.trumpcards.di.appModule

class TrumpCardsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@TrumpCardsApp)
            modules(appModule)
        }
    }
}
