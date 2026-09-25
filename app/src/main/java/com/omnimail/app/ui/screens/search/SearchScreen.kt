package com.omnimail.app.ui.screens.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnimail.app.model.EmailMessage
import com.omnimail.app.ui.screens.inbox.EmailRowItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchScreen(
    allEmails: List<EmailMessage>,
    onEmailClick: (EmailMessage) -> Unit,
    onToggleStar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterHasAttachment by remember { mutableStateOf(false) }
    var filterUnreadOnly by remember { mutableStateOf(false) }
    var isServerSearchActive by remember { mutableStateOf(false) }

    val recentSearches = remember {
        mutableStateListOf<String>()
    }

    val searchResults = remember(searchQuery, filterHasAttachment, filterUnreadOnly, allEmails) {
        if (searchQuery.isBlank()) emptyList()
        else {
            allEmails.filter { email ->
                val matchesQuery = email.subject.contains(searchQuery, ignoreCase = true) ||
                        email.senderName.contains(searchQuery, ignoreCase = true) ||
                        email.senderEmail.contains(searchQuery, ignoreCase = true) ||
                        email.snippet.contains(searchQuery, ignoreCase = true) ||
                        email.accountEmail.contains(searchQuery, ignoreCase = true)

                val matchesAttachment = !filterHasAttachment || email.hasAttachments
                val matchesUnread = !filterUnreadOnly || !email.isRead

                matchesQuery && matchesAttachment && matchesUnread
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search TextField
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Cari di seluruh akun (pengirim, subjek, kata kunci)...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary)
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Search Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = filterHasAttachment,
                    onClick = { filterHasAttachment = !filterHasAttachment },
                    label = { Text("Ada Lampiran") },
                    leadingIcon = {
                        Icon(Icons.Outlined.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )
            }
            item {
                FilterChip(
                    selected = filterUnreadOnly,
                    onClick = { filterUnreadOnly = !filterUnreadOnly },
                    label = { Text("Hanya Belum Dibaca") },
                    leadingIcon = {
                        Icon(Icons.Outlined.MarkEmailUnread, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (searchQuery.isBlank()) {
            // Recent Searches Section
            Text(
                text = "PENCARIAN TERAKHIR",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 6.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(recentSearches) { term ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { searchQuery = term }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = term,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { recentSearches.remove(term) },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Hapus", modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        } else {
            // Search Results Count & Server Search Trigger
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ditemukan ${searchResults.size} hasil di lokal",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = { isServerSearchActive = !isServerSearchActive }) {
                    Icon(Icons.Outlined.Cloud, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isServerSearchActive) "Pencarian Server Aktif" else "Cari ke Server (IMAP)")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (searchResults.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tidak ada email yang cocok dengan \"$searchQuery\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 60.dp)
                ) {
                    items(searchResults, key = { it.id }) { email ->
                        EmailRowItem(
                            email = email,
                            isSelected = false,
                            isSelectionMode = false,
                            onClick = { onEmailClick(email) },
                            onLongClick = { },
                            onToggleStar = { onToggleStar(email.id) }
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }
    }
}
