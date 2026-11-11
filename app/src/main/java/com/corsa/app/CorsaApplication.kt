package com.corsa.app

import android.app.Application

class CorsaApplication : Application() {
    val container by lazy { AppContainer(this) }
}
