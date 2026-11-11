package com.corsa.app

import android.content.Context
import com.corsa.app.data.CorsaRepository
import com.corsa.app.data.local.CorsaDatabase

class AppContainer(context: Context) {
    private val database by lazy { CorsaDatabase.buildFileDatabase(context.applicationContext) }
    val repository by lazy { CorsaRepository(database) }
}
