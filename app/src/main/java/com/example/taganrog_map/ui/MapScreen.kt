package com.example.taganrog_map.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.taganrog_map.R
import com.example.taganrog_map.data.Config
import com.example.taganrog_map.data.InitiativeRepository
import kotlinx.coroutines.launch
import android.util.Log
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory.*
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonOptions
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

private const val TAG = "TaganrogMap"

private const val SOURCE_ID = "initiatives-source"

private const val LAYER_POINTS = "initiatives-layer"
private const val LAYER_CLUSTER_CIRCLE = "clusters-circle"
private const val LAYER_CLUSTER_COUNT = "clusters-count"

private const val ICON_RED = "pin-red"
private const val ICON_YELLOW = "pin-yellow"
private const val ICON_GREEN = "pin-green"

@Composable
fun MapScreen(
    refreshKey: Int = 0,
    onInitiativeClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    val repository = remember { InitiativeRepository() }
    val initiatives by repository.initiatives.collectAsState()
    var mapStyle by remember { mutableStateOf<Style?>(null) }

    LaunchedEffect(refreshKey) {
        repository.loadInitiatives()
    }
    
    // Обновляем карту при изменении инициатив
    LaunchedEffect(initiatives, mapStyle) {
        mapStyle?.let { style ->
            Log.d(TAG, "Updating map with ${initiatives.size} initiatives")
            val features: List<Feature> = initiatives.map { i ->
                Feature.fromGeometry(Point.fromLngLat(i.lon, i.lat)).apply {
                    addStringProperty("id", i.id)
                    addStringProperty("title", i.title)
                    addStringProperty("status", i.status.name)
                }
            }
            val fc = FeatureCollection.fromFeatures(features)
            val s = style.getSourceAs<GeoJsonSource>(SOURCE_ID)
            s?.setGeoJson(fc)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val mapView = MapView(ctx)

                mapView.getMapAsync { map ->
                    val styleUrl = "${Config.TILE_SERVER_URL}/styles/basic-preview/style.json"
                    map.setStyle(Style.Builder().fromUri(styleUrl)) { style ->
                        mapStyle = style
                    Log.d(TAG, "Map style ready, initiatives=${initiatives.size}")
                        // Центр Таганрога
                        map.cameraPosition = CameraPosition.Builder()
                            .target(LatLng(47.236, 38.897))
                            .zoom(13.0)
                            .build()

                        // Ограничения зума
                    map.setMinZoomPreference(12.0)
                        map.setMaxZoomPreference(19.0)

                        // Ограничение области (bounds Таганрога)
                        val bounds = LatLngBounds.Builder()
                            .include(LatLng(47.33, 38.99))  // северо-восток
                            .include(LatLng(47.17, 38.78))  // юго-запад
                            .build()
                        map.setLatLngBoundsForCameraTarget(bounds)

                        // Источник + слои (кластеры + одиночные точки)
                        addInitiativesWithClusters(style, context, initiatives)

                        // Клики
                        map.addOnMapClickListener { latLng ->
                            val screenPoint = map.projection.toScreenLocation(latLng)

                            // 1) Клик по кластеру => приблизить
                            val clusterHits = map.queryRenderedFeatures(screenPoint, LAYER_CLUSTER_CIRCLE)
                            if (clusterHits.isNotEmpty()) {
                                val f = clusterHits[0]
                                val geom = f.geometry() as? Point
                                if (geom != null) {
                                    val nextZoom = (map.cameraPosition.zoom + 2.0).coerceAtMost(19.0)
                                    map.animateCamera(
                                        CameraUpdateFactory.newLatLngZoom(
                                            LatLng(geom.latitude(), geom.longitude()),
                                            nextZoom
                                        ),
                                        350
                                    )
                                }
                                return@addOnMapClickListener true
                            }

                            // 2) Клик по одиночной точке => открыть детали
                            val pointHits = map.queryRenderedFeatures(screenPoint, LAYER_POINTS)
                            if (pointHits.isNotEmpty()) {
                                val f = pointHits[0]
                                val id = f.getStringProperty("id")
                                if (id != null) {
                                    onInitiativeClick(id)
                                }
                                true
                            } else {
                                false
                            }
                        }
                    }
                }

                mapView
            }
        )

        // Поисковая строка и кнопки
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Поисковая строка
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                singleLine = true
            )

            // Кнопки слоев и фильтров
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { /* TODO: показать слои */ },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.layers))
                }

                OutlinedButton(
                    onClick = { /* TODO: показать фильтры */ },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.filters))
                }
            }
        }
    }
}

private fun addInitiativesWithClusters(style: Style, context: Context, initiatives: List<com.example.taganrog_map.data.Initiative>) {
    // 1) Иконки пинов
    style.addImage(ICON_RED, drawableToBitmap(context, R.drawable.ic_pin_red))
    style.addImage(ICON_YELLOW, drawableToBitmap(context, R.drawable.ic_pin_yellow))
    style.addImage(ICON_GREEN, drawableToBitmap(context, R.drawable.ic_pin_green))

    // 2) GeoJSON features
    val features: List<Feature> = initiatives.map { i ->
        Feature.fromGeometry(Point.fromLngLat(i.lon, i.lat)).apply {
            addStringProperty("id", i.id)
            addStringProperty("title", i.title)
            addStringProperty("status", i.status.name) // RED/YELLOW/GREEN
        }
    }
    val fc = FeatureCollection.fromFeatures(features)

    // 3) Source без кластеризации, чтобы пины были видны на любом зуме
    val existing = style.getSource(SOURCE_ID)
    if (existing == null) {
        style.addSource(GeoJsonSource(SOURCE_ID, fc))
    } else {
        // если source уже есть — обновляем data
        val s = style.getSourceAs<GeoJsonSource>(SOURCE_ID)
        s?.setGeoJson(fc)
    }

    // 4) Кружок кластера (цвет = светофор по приоритету RED > YELLOW > GREEN)
    if (style.getLayer(LAYER_CLUSTER_CIRCLE) == null) {
        val circleLayer = CircleLayer(LAYER_CLUSTER_CIRCLE, SOURCE_ID).withProperties(circleColor("#4A90E2"),
            circleRadius(
                Expression.interpolate(
                    Expression.linear(),
                    Expression.get("point_count"),
                    Expression.stop(10, 18),
                    Expression.stop(50, 24),
                    Expression.stop(200, 30)
                )
            ),
            circleOpacity(0.9f),
            circleStrokeColor("#FFFFFF"),
            circleStrokeWidth(2f)
        )
        circleLayer.setFilter(Expression.has("point_count"))
        style.addLayer(circleLayer)
    }

    // 5) Число на кластере
    if (style.getLayer(LAYER_CLUSTER_COUNT) == null) {
        val countLayer = SymbolLayer(LAYER_CLUSTER_COUNT, SOURCE_ID).withProperties(
            textField(Expression.toString(Expression.get("point_count"))),
            textSize(12f),
            textColor("#111111"),
            textAllowOverlap(true),
            textIgnorePlacement(true)
        )
        countLayer.setFilter(Expression.has("point_count"))
        style.addLayer(countLayer)
    }

    // 6) Одиночные точки (не кластеры)
    if (style.getLayer(LAYER_POINTS) == null) {
        val pointsLayer = SymbolLayer(LAYER_POINTS, SOURCE_ID).withProperties(
            iconImage(
                Expression.match(
                    Expression.get("status"),
                    Expression.literal("RED"), Expression.literal(ICON_RED),
                    Expression.literal("YELLOW"), Expression.literal(ICON_YELLOW),
                    Expression.literal("GREEN"), Expression.literal(ICON_GREEN),
                    Expression.literal(ICON_RED)
                )
            ),
            iconAllowOverlap(true),
            iconIgnorePlacement(true),
            iconSize(1.1f)
        )
        pointsLayer.setFilter(Expression.not(Expression.has("point_count")))
        style.addLayer(pointsLayer)
    }
}

private fun drawableToBitmap(context: Context, resId: Int): Bitmap {
    val drawable = ContextCompat.getDrawable(context, resId)
        ?: error("Drawable not found: $resId")

    val width = drawable.intrinsicWidth.coerceAtLeast(48)
    val height = drawable.intrinsicHeight.coerceAtLeast(48)

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}
