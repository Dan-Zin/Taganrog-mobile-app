package com.example.taganrog_map.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.taganrog_map.R
import com.example.taganrog_map.data.Config
import com.example.taganrog_map.data.InitiativeCreateRequest
import com.example.taganrog_map.data.InitiativeRepository
import com.example.taganrog_map.data.Status
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

private val DEFAULT_CATEGORIES = listOf(
    "Дороги",
    "Освещение",
    "Экология",
    "Инфраструктура",
    "Дворы",
    "Прочее"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateInitiativeScreen(
    onBackClick: () -> Unit,
    onCreated: () -> Unit
) {
    val repository = remember { InitiativeRepository() }
    val isLoading by repository.isLoading.collectAsState()
    val error by repository.error.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val createFailedMessage = stringResource(R.string.create_failed)

    var selectedLatLng by remember { mutableStateOf<LatLng?>(null) }
    var showForm by remember { mutableStateOf(false) }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(DEFAULT_CATEGORIES.first()) }
    var status by remember { mutableStateOf(Status.RED) }

    var showAiAssistant by remember { mutableStateOf(false) }
    var showDrawingStub by remember { mutableStateOf(false) }
    var showMediaStub by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(error) {
        if (!error.isNullOrBlank()) {
            snackbarHostState.showSnackbar(error ?: "")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.create_initiative)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            InitiativeLocationPicker(
                modifier = Modifier.fillMaxSize(),
                onLocationSelected = { selectedLatLng = it }
            )

            LocationOverlay(
                selectedLatLng = selectedLatLng,
                onConfirm = { showForm = true },
                onClear = { selectedLatLng = null }
            )

            if (showForm) {
                ModalBottomSheet(
                    onDismissRequest = { showForm = false },
                    sheetState = sheetState
                ) {
                    CreateInitiativeForm(
                        title = title,
                        description = description,
                        category = category,
                        status = status,
                        categories = DEFAULT_CATEGORIES,
                        location = selectedLatLng,
                        isLoading = isLoading,
                        onTitleChange = { title = it },
                        onDescriptionChange = { description = it },
                        onCategoryChange = { category = it },
                        onStatusChange = { status = it },
                        onMediaClick = { showMediaStub = true },
                        onDrawingClick = { showDrawingStub = true },
                        onAiAssistantClick = { showAiAssistant = true },
                        onCreateClick = {
                            val loc = selectedLatLng ?: return@CreateInitiativeForm
                            scope.launch {
                                val created = withContext(Dispatchers.IO) {
                                    repository.createInitiative(
                                        InitiativeCreateRequest(
                                            title = title.trim(),
                                            description = description.trim(),
                                            status = status.name,
                                            category = category,
                                            lat = loc.latitude,
                                            lon = loc.longitude
                                        )
                                    )
                                }
                                if (created != null) {
                                    onCreated()
                                } else {
                                    snackbarHostState.showSnackbar(createFailedMessage)
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    if (showAiAssistant) {
        AiAssistantDialog(
            onDismiss = { showAiAssistant = false },
            onApply = { suggestionTitle, suggestionDescription ->
                title = if (title.isBlank()) suggestionTitle else title
                if (description.isBlank()) description = suggestionDescription
                showAiAssistant = false
            }
        )
    }

    if (showDrawingStub) {
        PlaceholderDialog(
            title = stringResource(R.string.drawing_stub_title),
            message = stringResource(R.string.drawing_stub_message),
            onDismiss = { showDrawingStub = false }
        )
    }

    if (showMediaStub) {
        PlaceholderDialog(
            title = stringResource(R.string.media_stub_title),
            message = stringResource(R.string.media_stub_message),
            onDismiss = { showMediaStub = false }
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun InitiativeLocationPicker(
    modifier: Modifier = Modifier,
    onLocationSelected: (LatLng) -> Unit
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val mapView = MapView(ctx)
            mapView.getMapAsync { map ->
                val styleUrl = "${Config.TILE_SERVER_URL}/styles/basic-preview/style.json"
                map.setStyle(Style.Builder().fromUri(styleUrl)) { _ ->
                    map.cameraPosition = CameraPosition.Builder()
                        .target(LatLng(47.236, 38.897))
                        .zoom(13.0)
                        .build()

                    map.setMinZoomPreference(11.0)
                    map.setMaxZoomPreference(19.0)

                    val bounds = LatLngBounds.Builder()
                        .include(LatLng(47.33, 38.99))
                        .include(LatLng(47.17, 38.78))
                        .build()
                    map.setLatLngBoundsForCameraTarget(bounds)

                    map.addOnMapClickListener { latLng ->
                        onLocationSelected(latLng)
                        true
                    }
                }
            }
            mapView
        }
    )
}

@Composable
private fun LocationOverlay(
    selectedLatLng: LatLng?,
    onConfirm: () -> Unit,
    onClear: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .background(Color.White.copy(alpha = 0.9f), shape = MaterialTheme.shapes.medium)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (selectedLatLng == null) {
                    stringResource(R.string.tap_to_select_location)
                } else {
                    stringResource(R.string.location_selected)
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .background(Color.White.copy(alpha = 0.95f), shape = MaterialTheme.shapes.medium)
                .padding(16.dp)
        ) {
            if (selectedLatLng != null) {
                Text(
                    text = "Lat: %.5f, Lon: %.5f".format(
                        selectedLatLng.latitude,
                        selectedLatLng.longitude
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onClear,
                    enabled = selectedLatLng != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.change_location))
                }
                Button(
                    onClick = onConfirm,
                    enabled = selectedLatLng != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.confirm_location))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateInitiativeForm(
    title: String,
    description: String,
    category: String,
    status: Status,
    categories: List<String>,
    location: LatLng?,
    isLoading: Boolean,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onStatusChange: (Status) -> Unit,
    onMediaClick: () -> Unit,
    onDrawingClick: () -> Unit,
    onAiAssistantClick: () -> Unit,
    onCreateClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    var categoryExpanded by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.create_initiative),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        if (location != null) {
            Text(
                text = "Lat: %.5f, Lon: %.5f".format(location.latitude, location.longitude),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text(stringResource(R.string.initiative_title)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        ExposedDropdownMenuBox(
            expanded = categoryExpanded,
            onExpandedChange = { categoryExpanded = !categoryExpanded }
        ) {
            OutlinedTextField(
                value = category,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.initiative_category_label)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = categoryExpanded,
                onDismissRequest = { categoryExpanded = false }
            ) {
                categories.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item) },
                        onClick = {
                            onCategoryChange(item)
                            categoryExpanded = false
                        }
                    )
                }
            }
        }

        ExposedDropdownMenuBox(
            expanded = statusExpanded,
            onExpandedChange = { statusExpanded = !statusExpanded }
        ) {
            OutlinedTextField(
                value = status.name,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.status_label)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = statusExpanded,
                onDismissRequest = { statusExpanded = false }
            ) {
                Status.values().forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item.name) },
                        onClick = {
                            onStatusChange(item)
                            statusExpanded = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text(stringResource(R.string.initiative_description)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        SectionHeader(title = stringResource(R.string.media_section))
        MediaStubRow(onMediaClick = onMediaClick)

        SectionHeader(title = stringResource(R.string.innovations_section))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onDrawingClick, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Brush, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.drawing_stub_title))
            }
            OutlinedButton(onClick = onAiAssistantClick, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Psychology, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.ai_assistant))
            }
        }

        Button(
            onClick = onCreateClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = title.isNotBlank() && !isLoading
        ) {
            Text(stringResource(R.string.create_action))
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun MediaStubRow(onMediaClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.media_empty),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onMediaClick, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.take_photo))
            }
            OutlinedButton(onClick = onMediaClick, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Movie, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.take_video))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onMediaClick, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Image, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.pick_from_gallery))
            }
            OutlinedButton(onClick = onMediaClick, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Mic, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.voice_stub))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun AiAssistantDialog(
    onDismiss: () -> Unit,
    onApply: (String, String) -> Unit
) {
    var prompt by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf(emptyList<String>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    suggestions = listOf(
                        "Яма на дороге возле дома №15",
                        "Отсутствует освещение на улице",
                        "Нужна уборка мусора во дворе"
                    )
                }
            ) {
                Text(stringResource(R.string.generate_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        title = { Text(stringResource(R.string.ai_assistant)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text(stringResource(R.string.ai_prompt_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )
                if (suggestions.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.ai_suggestions_title),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        suggestions.forEach { suggestion ->
                            OutlinedButton(
                                onClick = { onApply(suggestion, suggestion) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(suggestion)
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun PlaceholderDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        },
        title = { Text(title) },
        text = { Text(message) }
    )
}
