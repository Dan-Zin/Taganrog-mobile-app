package com.example.taganrog_map

import android.app.Application
import org.maplibre.android.MapLibre
import org.maplibre.android.WellKnownTileServer

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        // API key не нужен, но параметр обязателен для SDK.
        // Можно передать пустую строку.
        MapLibre.getInstance(this, "", WellKnownTileServer.MapLibre)
    }
}
