package com.omnimail.app.ui.screens.inbox

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnimail.app.model.EmailAccount
import com.omnimail.app.model.EmailMessage
import com.omnimail.app.model.WorkspaceGroup
import com.omnimail.app.ui.components.AccountBadge
import com.omnimail.app.ui.components.BulkActionBar

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UnifiedInboxScreen(
    emails: List<EmailMessage>,
    accounts: List<EmailAccount>,
    groups: List<WorkspaceGroup>,
    selectedGroupId: String?,
    onSelectGroup: (String?) -> Unit,
    onEmailClick: (EmailMessage) -> Unit,
    onToggleStar: (String) -> Unit,
    onDeleteEmail: (String) -> Unit,
    onMarkAsRead: (List<String>) -> Unit,
    onDeleteBatch: (List<String>) -> Unit,
    onArchiveBatch: (List<String>) -> Unit,
    onNavigateToAccounts: () -> Unit = {},
    onManualSync: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedEmailIds by remember { mutableStateOf(setOf<String>()) }
    val isSelectionMode = selectedEmailIds.isNotEmpty()

    var activeQuickFilter by remember { mutableStateOf("ALL") } // ALL, UNREAD, ATTACHMENTS, STARRED

    val filteredEmails = remember(emails, selectedGroupId, activeQuickFilter) {
        emails.filter { email ->
            val matchGroup = if (selectedGroupId != null) {
                val group = groups.find { it.id == selectedGroupId }
                group?.accountIds?.contains(email.accountId) == true
            } else true

            val matchFilter = when (activeQuickFilter) {
                "UNREAD" -> !email.isRead
                "ATTACHMENTS" -> email.hasAttachments
                "STARRED" -> email.isStarred
                else -> true
            }

            matchGroup && matchFilter
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Bulk Action Header if selection mode is active
        AnimatedVisibility(
            visible = isSelectionMode,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            BulkActionBar(
                selectedCount = selectedEmailIds.size,
                onClearSelection = { selectedEmailIds = emptySet() },
                onMarkAsRead = {
                    onMarkAsRead(selectedEmailIds.toList())
                    selectedEmailIds = emptySet()
                },
                onDelete = {
                    onDeleteBatch(selectedEmailIds.toList())
                    selectedEmailIds = emptySet()
                },
                onArchive = {
                    onArchiveBatch(selectedEmailIds.toList())
                    selectedEmailIds = emptySet()
                }
            )
        }

        // Quick Filter Chips Bar
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                FilterChip(
                    selected = activeQuickFilter == "ALL",
                    onClick = { activeQuickFilter = "ALL" },
                    label = { Text("Semua (${filteredEmails.size})") },
                    leadingIcon = if (activeQuickFilter == "ALL") {
                        { Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }
            item {
                FilterChip(
                    selected = activeQuickFilter == "UNREAD",
                    onClick = { activeQuickFilter = "UNREAD" },
                    label = { Text("Belum Dibaca") },
                    leadingIcon = {
                        Icon(Icons.Outlined.MarkEmailUnread, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )
            }
            item {
                FilterChip(
                    selected = activeQuickFilter == "ATTACHMENTS",
                    onClick = { activeQuickFilter = "ATTACHMENTS" },
                    label = { Text("Ada Lampiran") },
                    leadingIcon = {
                        Icon(Icons.Outlined.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )
            }
            item {
                FilterChip(
                    selected = activeQuickFilter == "STARRED",
                    onClick = { activeQuickFilter = "STARRED" },
                    label = { Text("Berbintang") },
                    leadingIcon = {
                        Icon(Icons.Outlined.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )
            }
        }

        // Email List
        if (filteredEmails.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                if (accounts.isEmpty()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Outlined.MarkEmailRead,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Text(
                            text = "Belum Ada Akun Terhubung",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tambahkan akun email Anda (Gmail dengan Sandi Aplikasi, Outlook, Yahoo, atau IMAP kustom) untuk mulai mengelola kotak masuk.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = onNavigateToAccounts,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tambah Akun Email")
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Outlined.MailOutline,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "Kotak Masuk Masih Kosong",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Tarik email terbaru dari server IMAP akun yang terhubung.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = onManualSync,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sinkronkan Sekarang")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(filteredEmails, key = { it.id }) { email ->
                    val isSelected = selectedEmailIds.contains(email.id)

                    EmailRowItem(
                        email = email,
                        isSelected = isSelected,
                        isSelectionMode = isSelectionMode,
                        onClick = {
                            if (isSelectionMode) {
                                selectedEmailIds = if (isSelected) {
                                    selectedEmailIds - email.id
                                } else {
                                    selectedEmailIds + email.id
                                }
                            } else {
                                onEmailClick(email)
                            }
                        },
                        onLongClick = {
                            selectedEmailIds = if (isSelected) {
                                selectedEmailIds - email.id
                            } else {
                                selectedEmailIds + email.id
                            }
                        },
                        onToggleStar = { onToggleStar(email.id) }
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        thickness = 0.5.dp
                    )
                }

                // Lazy Loading indicator at the bottom (FR 2.4 Offline Caching & Lazy Loading)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Menampilkan 50 email terakhir dari cache lokal",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = { /* Trigger fetch older emails from IMAP */ },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                Icons.Outlined.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Muat Email Lebih Lama dari Server")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EmailRowItem(
    email: EmailMessage,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleStar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        !email.isRead -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        else -> MaterialTheme.colorScheme.surface
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Selection Checkbox or Sender Avatar
        if (isSelectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() },
                modifier = Modifier.padding(end = 8.dp)
            )
        } else {
            // Sender Avatar Initial
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(email.accountColorHex).copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = email.senderName.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(email.accountColorHex)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
        }

        // Email Content Body
        Column(modifier = Modifier.weight(1f)) {
            // Header Row: Sender Name + Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = email.senderName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (!email.isRead) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = email.formattedTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (!email.isRead) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (!email.isRead) FontWeight.Bold else FontWeight.Normal
                )
            }

            // Subject Line
            Text(
                text = email.subject,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (!email.isRead) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Snippet Preview
            Text(
                text = email.snippet,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Footer Row: Account Badge + Icons (Attachments, Star)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Colored Account Tag
                AccountBadge(
                    accountEmail = email.accountEmail,
                    colorHex = email.accountColorHex
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (email.hasAttachments) {
                        Icon(
                            Icons.Outlined.AttachFile,
                            contentDescription = "Has attachments",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggleStar,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (email.isStarred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Star",
                            tint = if (email.isStarred) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
