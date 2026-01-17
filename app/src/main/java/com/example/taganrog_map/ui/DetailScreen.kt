package com.example.taganrog_map.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.taganrog_map.R
import com.example.taganrog_map.data.Config
import com.example.taganrog_map.data.InitiativeRepository
import com.example.taganrog_map.data.MediaItem
import com.example.taganrog_map.data.Status
import android.content.Intent
import android.net.Uri
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    initiativeId: String,
    onBackClick: () -> Unit
) {
    val repository = remember { InitiativeRepository() }
    var initiative by remember { mutableStateOf<com.example.taganrog_map.data.Initiative?>(null) }
    val isLoading by repository.isLoading.collectAsState()
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(initiativeId) {
        scope.launch {
            initiative = repository.getInitiative(initiativeId)
        }
    }

    val currentInitiative = initiative
    if (currentInitiative == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(R.string.initiative_not_found))
        }
        return
    }

    var selectedTab by remember { mutableStateOf(0) }
    val processInfo = remember {
        ProcessInfo(
            responsibleOrg = "ГорСвет",
            responsibleDepartment = "Эксплуатационный участок №3",
            responsibleName = "Иванов И.И.",
            responsibleRole = "Инженер",
            responsibleEmail = "ivanov@gorsvet.ru",
            responsiblePhone = "+7 (863) 000-00-00",
            budgetAllocated = "350 000 ₽",
            budgetSpent = "342 000 ₽",
            budgetSource = "Региональный",
            startDate = "2026-01-22",
            endDate = "2026-01-25",
            progressPercent = 100,
            documents = listOf(
                DocumentInfo("Акт выполненных работ.pdf", "ACT · 2.1 MB"),
                DocumentInfo("Фотоотчет \"ПОСЛЕ\".jpg", "PHOTO · 5.6 MB")
            ),
            comments = listOf(
                CommentInfo("Администрация", "Работы выполнены, лампы заменены.", true)
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
    ) {
        HeroHeader(onBackClick = onBackClick, media = currentInitiative.media)

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-24).dp),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                HeaderBlock(
                    title = currentInitiative.title,
                    address = currentInitiative.address.ifBlank { stringResource(R.string.address_unknown) },
                    likes = 89
                )

                StatusTimeline(status = currentInitiative.status)

                StatusBadge(status = currentInitiative.status)

                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(stringResource(R.string.details_tab)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(stringResource(R.string.history_tab)) }
                    )
                }

                if (selectedTab == 0) {
                    DetailsTab(
                        status = currentInitiative.status,
                        description = currentInitiative.description,
                        authorName = currentInitiative.author.name,
                        createdAt = currentInitiative.createdAt,
                        media = currentInitiative.media,
                        processInfo = processInfo
                    )
                } else {
                    HistoryTab()
                }
            }
        }
    }
}

@Composable
private fun HeroHeader(
    onBackClick: () -> Unit,
    media: List<MediaItem>
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(Color(0xFFE0E0E0))
    ) {
        if (media.isNotEmpty()) {
            MediaCarousel(media)
        }
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .padding(16.dp)
                .size(40.dp)
                .background(Color.White, CircleShape)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
        }
    }
}

@Composable
private fun MediaCarousel(media: List<MediaItem>) {
    val context = LocalContext.current
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        userScrollEnabled = media.size > 1
    ) {
        items(media) { item ->
            val url = resolveMediaUrl(item.url)
            val isVideo = item.mediaType.startsWith("video/")
            Box(
                modifier = Modifier
                    .width(screenWidth)
                    .fillMaxHeight()
                    .clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    }
            ) {
                if (isVideo) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1F1F1F)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                } else {
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderBlock(title: String, address: String, likes: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.ThumbUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(text = likes.toString(), fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = address, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatusTimeline(status: Status) {
    val steps = listOf(
        stringResource(R.string.stage_review),
        stringResource(R.string.stage_approval),
        stringResource(R.string.stage_execution),
        stringResource(R.string.stage_done)
    )
    val activeIndex = when (status) {
        Status.RED -> 0
        Status.YELLOW -> 2
        Status.GREEN -> 3
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, label ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            color = if (index <= activeIndex) MaterialTheme.colorScheme.primary else Color(0xFFBDBDBD),
                            shape = CircleShape
                        )
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (index <= activeIndex) MaterialTheme.colorScheme.primary else Color(0xFF8D8D8D)
                )
            }
            if (index != steps.lastIndex) {
                Spacer(Modifier.weight(1f))
                Divider(
                    modifier = Modifier
                        .height(1.dp)
                        .weight(2f),
                    color = if (index < activeIndex) MaterialTheme.colorScheme.primary else Color(0xFFBDBDBD)
                )
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatusBadge(status: Status) {
    val (text, color) = when (status) {
        Status.RED -> Pair(stringResource(R.string.status_label_red), Color(0xFFFFCDD2))
        Status.YELLOW -> Pair(stringResource(R.string.status_label_yellow), Color(0xFFFFF9C4))
        Status.GREEN -> Pair(stringResource(R.string.status_label_green), Color(0xFFC8E6C9))
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DetailsTab(
    status: Status,
    description: String,
    authorName: String,
    createdAt: String,
    media: List<MediaItem>,
    processInfo: ProcessInfo
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        SectionTitle(stringResource(R.string.section_description))
        Text(text = description.ifBlank { stringResource(R.string.description_placeholder) })

        MediaSection(media)

        if (status == Status.RED) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = stringResource(R.string.process_waiting),
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            ResponsibleCard(processInfo)
            TimelineCard(processInfo)
            FinanceCard(processInfo)
            DocumentsSection(processInfo.documents)
        }

        AuthorSection(authorName, createdAt)
        CommentsSection(processInfo.comments)
    }
}

@Composable
private fun HistoryTab() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.history_placeholder),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun ResponsibleCard(info: ProcessInfo) {
    SectionTitle(stringResource(R.string.section_responsible))
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                    Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, contentDescription = null)
                    }
                }
                Column {
                    Text(info.responsibleOrg, fontWeight = FontWeight.SemiBold)
                    Text(info.responsibleDepartment, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            InfoRow(label = stringResource(R.string.responsible_name), value = info.responsibleName)
            InfoRow(label = stringResource(R.string.responsible_role), value = info.responsibleRole)
            InfoRow(label = stringResource(R.string.responsible_email), value = info.responsibleEmail)
            InfoRow(label = stringResource(R.string.responsible_phone), value = info.responsiblePhone)
        }
    }
}

@Composable
private fun TimelineCard(info: ProcessInfo) {
    SectionTitle(stringResource(R.string.section_timeline))
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(stringResource(R.string.timeline_start), style = MaterialTheme.typography.bodySmall)
                    Text(info.startDate, fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.timeline_end), style = MaterialTheme.typography.bodySmall)
                    Text(info.endDate, fontWeight = FontWeight.SemiBold)
                }
            }
            LinearProgressIndicator(
                progress = info.progressPercent / 100f,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = stringResource(R.string.timeline_progress, info.progressPercent),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun FinanceCard(info: ProcessInfo) {
    SectionTitle(stringResource(R.string.section_finance))
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(stringResource(R.string.finance_budget), style = MaterialTheme.typography.bodySmall)
                    Text(info.budgetAllocated, fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.finance_spent), style = MaterialTheme.typography.bodySmall)
                    Text(info.budgetSpent, fontWeight = FontWeight.SemiBold)
                }
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = stringResource(R.string.finance_source, info.budgetSource),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun DocumentsSection(documents: List<DocumentInfo>) {
    SectionTitle(stringResource(R.string.section_documents))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        documents.forEach { doc ->
            Card(shape = RoundedCornerShape(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.Description, contentDescription = null)
                        Column {
                            Text(doc.name, fontWeight = FontWeight.SemiBold)
                            Text(doc.meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Icon(Icons.Default.Download, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun MediaSection(media: List<MediaItem>) {
    val context = LocalContext.current
    SectionTitle(stringResource(R.string.media_section))

    if (media.isEmpty()) {
        Text(
            text = stringResource(R.string.media_empty),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        media.forEach { item ->
            val url = resolveMediaUrl(item.url)
            val name = url.substringAfterLast("/")
            val isVideo = item.mediaType.startsWith("video/")

            Card(shape = RoundedCornerShape(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isVideo) Icons.Default.Movie else Icons.Default.Image,
                            contentDescription = null
                        )
                        Column {
                            Text(name, fontWeight = FontWeight.SemiBold)
                            Text(
                                item.mediaType,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    TextButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        }
                    ) {
                        Text(stringResource(R.string.open_action))
                    }
                }
            }
        }
    }
}

private fun resolveMediaUrl(url: String): String {
    if (url.startsWith("http://") || url.startsWith("https://")) {
        return url
    }
    val base = Config.API_BASE_URL
        .removeSuffix("api/")
        .removeSuffix("api")
        .trimEnd('/')
    return base + url
}

@Composable
private fun AuthorSection(authorName: String, createdAt: String) {
    SectionTitle(stringResource(R.string.section_author))
    Card(shape = RoundedCornerShape(16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, contentDescription = null)
                }
            }
            Column {
                Text(authorName, fontWeight = FontWeight.SemiBold)
                Text(createdAt.ifBlank { stringResource(R.string.date_placeholder) }, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun CommentsSection(comments: List<CommentInfo>) {
    SectionTitle(stringResource(R.string.section_comments))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        comments.forEach { comment ->
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)) {
                            Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                                Text(comment.author.take(1))
                            }
                        }
                        Text(comment.author, fontWeight = FontWeight.SemiBold)
                        if (comment.official) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = stringResource(R.string.comment_official),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    Text(comment.text)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.comment_placeholder)) },
                enabled = false
            )
            IconButton(onClick = {}) {
                Icon(Icons.Default.Send, contentDescription = null)
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

private data class ProcessInfo(
    val responsibleOrg: String,
    val responsibleDepartment: String,
    val responsibleName: String,
    val responsibleRole: String,
    val responsibleEmail: String,
    val responsiblePhone: String,
    val budgetAllocated: String,
    val budgetSpent: String,
    val budgetSource: String,
    val startDate: String,
    val endDate: String,
    val progressPercent: Int,
    val documents: List<DocumentInfo>,
    val comments: List<CommentInfo>
)

private data class DocumentInfo(
    val name: String,
    val meta: String
)

private data class CommentInfo(
    val author: String,
    val text: String,
    val official: Boolean
)
