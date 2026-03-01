package com.yuhai94.awcli

import android.app.Application
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

class AwCliApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Security.removeProvider("BC")
        Security.addProvider(BouncyCastleProvider())
        val absolutePath = filesDir.absolutePath
        System.setProperty("user.home", absolutePath)
        AppContainer.initialize(this)
    }
}
