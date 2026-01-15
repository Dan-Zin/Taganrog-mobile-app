package com.example.taganrog_map.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.taganrog_map.R
import com.example.taganrog_map.data.InitiativeRepository
import com.example.taganrog_map.data.Status
import kotlinx.coroutines.launch
import androidx.compose.runtime.*

@Composable
fun FeedScreen(
    onInitiativeClick: (String) -> Unit = {}
) {
    val repository = remember { InitiativeRepository() }
    val initiatives by repository.initiatives.collectAsState()
    val isLoading by repository.isLoading.collectAsState()
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        scope.launch {
            repository.loadInitiatives()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Заголовок
        Text(
            text = stringResource(R.string.feed_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        // Индикатор загрузки
        if (isLoading && initiatives.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // Список инициатив
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(initiatives) { initiative ->
                    InitiativeCard(
                        initiative = initiative,
                        onClick = { onInitiativeClick(initiative.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun InitiativeCard(
    initiative: com.example.taganrog_map.data.Initiative,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Заголовок и статус
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = initiative.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                StatusChip(status = initiative.status)
            }

            // Категория
            if (initiative.category.isNotEmpty()) {
                Text(
                    text = initiative.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Описание
            if (initiative.description.isNotEmpty()) {
                Text(
                    text = initiative.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Адрес
            if (initiative.address.isNotEmpty()) {
                Text(
                    text = initiative.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Дата создания
            if (initiative.createdAt.isNotEmpty()) {
                Text(
                    text = "Создано: ${initiative.createdAt}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun StatusChip(status: Status) {
    val (text, color) = when (status) {
        Status.RED -> Pair(R.string.status_red, colorResource(R.color.status_red))
        Status.YELLOW -> Pair(R.string.status_yellow, colorResource(R.color.status_yellow))
        Status.GREEN -> Pair(R.string.status_green, colorResource(R.color.status_green))
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color
    ) {
        Text(
            text = stringResource(text),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
