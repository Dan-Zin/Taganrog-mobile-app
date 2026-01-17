package com.example.taganrog_map.ui

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import com.example.taganrog_map.R
import com.example.taganrog_map.data.Config
import com.example.taganrog_map.data.InitiativeCreateRequest
import com.example.taganrog_map.data.InitiativeRepository
import com.example.taganrog_map.data.Status
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import java.io.File

private val DEFAULT_CATEGORIES = listOf(
    "Дороги",
    "Освещение",
    "Экология",
    "Инфраструктура",
    "Дворы",
    "Прочее"
)

private const val TAG = "CreateInitiative"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateInitiativeScreen(
    onBackClick: () -> Unit,
    onCreated: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { InitiativeRepository() }
    val isLoading by repository.isLoading.collectAsState()
    val error by repository.error.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val createFailedMessage = stringResource(R.string.create_failed)
    val cameraDeniedMessage = stringResource(R.string.camera_permission_denied)

    var selectedLatLng by remember { mutableStateOf<LatLng?>(null) }
    var showForm by remember { mutableStateOf(false) }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(DEFAULT_CATEGORIES.first()) }
    var status by remember { mutableStateOf(Status.RED) }
    var attachments by remember { mutableStateOf<List<MediaAttachmentUi>>(emptyList()) }

    var showAiAssistant by remember { mutableStateOf(false) }
    var showDrawingStub by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var pendingPhoto by remember { mutableStateOf<MediaAttachmentUi?>(null) }
    var pendingVideo by remember { mutableStateOf<MediaAttachmentUi?>(null) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            scope.launch { snackbarHostState.showSnackbar(cameraDeniedMessage) }
        }
    }

    val takePhotoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            pendingPhoto?.let { attachment ->
                attachments = attachments + attachment
            }
        }
        pendingPhoto = null
    }

    val captureVideoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CaptureVideo()
    ) { success ->
        if (success) {
            pendingVideo?.let { attachment ->
                attachments = attachments + attachment
            }
        }
        pendingVideo = null
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            buildAttachmentFromUri(context, uri, null)?.let { attachment ->
                attachments = attachments + attachment
            }
        }
    }

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
                        attachments = attachments,
                        isLoading = isLoading,
                        onTitleChange = { title = it },
                        onDescriptionChange = { description = it },
                        onCategoryChange = { category = it },
                        onStatusChange = { status = it },
                        onTakePhoto = {
                            if (hasCameraPermission(context)) {
                                val (uri, name) = createTempCapture(context, "photo_", ".jpg")
                                pendingPhoto = MediaAttachmentUi(
                                    name = name,
                                    uri = uri,
                                    mimeType = "image/jpeg"
                                )
                                takePhotoLauncher.launch(uri)
                            } else {
                                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                            }
                        },
                        onCaptureVideo = {
                            if (hasCameraPermission(context)) {
                                val (uri, name) = createTempCapture(context, "video_", ".mp4")
                                pendingVideo = MediaAttachmentUi(
                                    name = name,
                                    uri = uri,
                                    mimeType = "video/mp4"
                                )
                                captureVideoLauncher.launch(uri)
                            } else {
                                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                            }
                        },
                        onPickFromGallery = { galleryLauncher.launch("*/*") },
                        onRemoveAttachment = { toRemove ->
                            attachments = attachments.filterNot { it == toRemove }
                        },
                        onDrawingClick = { showDrawingStub = true },
                        onAiAssistantClick = { showAiAssistant = true },
                        onCreateClick = {
                            val loc = selectedLatLng ?: return@CreateInitiativeForm
                            scope.launch {
                                val created = withContext(Dispatchers.IO) {
                                    val initiative = InitiativeCreateRequest(
                                        title = title.trim(),
                                        description = description.trim(),
                                        status = status.name,
                                        category = category,
                                        lat = loc.latitude,
                                        lon = loc.longitude,
                                        media = emptyList()
                                    )
                                    val (parts, tempFiles) = buildMultipartParts(context, attachments)
                    Log.d(TAG, "Create initiative: title=${initiative.title}, files=${parts.size}")
                                    try {
                                        repository.createInitiative(initiative, parts)
                                    } finally {
                                        tempFiles.forEach { it.delete() }
                                    }
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
    attachments: List<MediaAttachmentUi>,
    isLoading: Boolean,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onStatusChange: (Status) -> Unit,
    onTakePhoto: () -> Unit,
    onCaptureVideo: () -> Unit,
    onPickFromGallery: () -> Unit,
    onRemoveAttachment: (MediaAttachmentUi) -> Unit,
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
        MediaPickerSection(
            attachments = attachments,
            onTakePhoto = onTakePhoto,
            onCaptureVideo = onCaptureVideo,
            onPickFromGallery = onPickFromGallery,
            onRemoveAttachment = onRemoveAttachment
        )

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
private fun MediaPickerSection(
    attachments: List<MediaAttachmentUi>,
    onTakePhoto: () -> Unit,
    onCaptureVideo: () -> Unit,
    onPickFromGallery: () -> Unit,
    onRemoveAttachment: (MediaAttachmentUi) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (attachments.isEmpty()) {
            Text(
                text = stringResource(R.string.media_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                attachments.forEach { item ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = item.name, fontWeight = FontWeight.Medium)
                                Text(text = item.mimeType, style = MaterialTheme.typography.bodySmall)
                            }
                            TextButton(onClick = { onRemoveAttachment(item) }) {
                                Text(stringResource(R.string.remove_action))
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onTakePhoto, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.take_photo))
            }
            OutlinedButton(onClick = onCaptureVideo, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Movie, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.take_video))
            }
        }
        OutlinedButton(onClick = onPickFromGallery, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Image, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.pick_from_gallery))
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

private data class MediaAttachmentUi(
    val name: String,
    val uri: Uri,
    val mimeType: String
)

private fun hasCameraPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.CAMERA
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
}

private fun createTempCapture(context: Context, prefix: String, suffix: String): Pair<Uri, String> {
    val file = File.createTempFile(prefix, suffix, context.cacheDir)
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    return uri to file.name
}

private fun buildAttachmentFromUri(
    context: Context,
    uri: Uri,
    overrideMimeType: String?
): MediaAttachmentUi? {
    val resolver = context.contentResolver
    val mimeType = overrideMimeType ?: resolver.getType(uri) ?: "application/octet-stream"
    val name = resolveDisplayName(context, uri) ?: "media"
    Log.d(TAG, "Picked media: name=$name, mimeType=$mimeType, uri=$uri")
    return MediaAttachmentUi(name = name, uri = uri, mimeType = mimeType)
}

private fun resolveDisplayName(context: Context, uri: Uri): String? {
    if (uri.scheme != "content") {
        return uri.lastPathSegment
    }
    val resolver = context.contentResolver
    resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) {
                return cursor.getString(index)
            }
        }
    }
    return null
}

private fun buildMultipartParts(
    context: Context,
    attachments: List<MediaAttachmentUi>
): Pair<List<MultipartBody.Part>, List<File>> {
    val resolver = context.contentResolver
    val tempFiles = mutableListOf<File>()
    val parts = attachments.mapNotNull { attachment ->
        val mimeType = attachment.mimeType.ifBlank { resolver.getType(attachment.uri) }
            ?: "application/octet-stream"
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "bin"
        val tempFile = File.createTempFile("upload_", ".$extension", context.cacheDir)
        try {
            resolver.openInputStream(attachment.uri)?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            } ?: return@mapNotNull null
        } catch (_: Exception) {
            Log.e(TAG, "Failed to read uri=${attachment.uri}")
            tempFile.delete()
            return@mapNotNull null
        }
        tempFiles += tempFile
        Log.d(TAG, "Prepared file: name=${attachment.name}, size=${tempFile.length()} bytes")
        val requestBody = tempFile.asRequestBody(mimeType.toMediaTypeOrNull())
        MultipartBody.Part.createFormData("files[]", attachment.name, requestBody)
    }
    return parts to tempFiles
}
