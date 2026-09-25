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
import com.omnimail.app.model.EmailFolder
import com.omnimail.app.model.EmailMessage
import com.omnimail.app.ui.components.AccountBadge
import com.omnimail.app.ui.components.BulkActionBar

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UnifiedInboxScreen(
    emails: List<EmailMessage>,
    accounts: List<EmailAccount>,
    selectedAccountId: String?,
    selectedFolder: EmailFolder = EmailFolder.INBOX,
    onSelectAccount: (String?) -> Unit,
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

    val filteredEmails = remember(emails, selectedAccountId, activeQuickFilter, selectedFolder) {
        emails.filter { email ->
            val matchFolder = when (selectedFolder) {
                EmailFolder.STARRED -> email.isStarred
                else -> email.folder == selectedFolder
            }

            val matchAccount = if (selectedAccountId != null) {
                email.accountId == selectedAccountId
            } else true

            val matchFilter = when (activeQuickFilter) {
                "UNREAD" -> !email.isRead
                "ATTACHMENTS" -> email.hasAttachments
                "STARRED" -> email.isStarred
                else -> true
            }

            matchFolder && matchAccount && matchFilter
        }
    }

    val selectedAccount = remember(selectedAccountId, accounts) {
        accounts.find { it.id == selectedAccountId }
    }

    // If an individual Web Session account is selected, directly render the full-screen In-App Webmail
    if (selectedAccount != null && selectedAccount.authType == com.omnimail.app.model.AuthType.WEB_SESSION) {
        InAppWebmailView(
            account = selectedAccount,
            onBackToUnified = { onSelectAccount(null) },
            modifier = modifier
        )
        return
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

        // Account Switcher Bar (Filter: Semua Akun vs Akun Spesifik)
        if (accounts.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    val totalFolderEmails = emails.count { it.folder == selectedFolder }
                    FilterChip(
                        selected = selectedAccountId == null,
                        onClick = { onSelectAccount(null) },
                        label = {
                            Text(
                                "Semua Akun ($totalFolderEmails)",
                                fontWeight = if (selectedAccountId == null) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = if (selectedAccountId == null) {
                            { Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
                items(accounts, key = { it.id }) { acc ->
                    val accCount = emails.count { it.accountId == acc.id && it.folder == selectedFolder }
                    val isSelected = selectedAccountId == acc.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectAccount(if (isSelected) null else acc.id) },
                        label = {
                            Text(
                                "${acc.displayName.ifBlank { acc.email }} ($accCount)",
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(acc.colorHex))
                            )
                        }
                    )
                }
            }
        }

        // Quick Filter Chips Bar (Semua, Belum Dibaca, Lampiran, Berbintang)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                FilterChip(
                    selected = activeQuickFilter == "ALL",
                    onClick = { activeQuickFilter = "ALL" },
                    label = { Text("Filter: Semua (${filteredEmails.size})") }
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

        // Email List or Account Launcher
        if (filteredEmails.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
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
                            text = "Tambahkan akun email Anda untuk mulai membaca kotak masuk dan berkirim pesan.",
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
                    // Accounts exist, show account launcher cards
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Buka Kotak Masuk Akun Anda",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Pilih akun di bawah untuk membuka kotak masuk langsung dengan kata sandi asli:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        accounts.forEach { acc ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectAccount(acc.id) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(Color(acc.colorHex))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = acc.displayName.ifBlank { acc.email },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = acc.email,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (acc.authType == com.omnimail.app.model.AuthType.WEB_SESSION) {
                                            Text(
                                                text = "🟢 Terhubung dengan Sandi Asli (Webmail)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF2E7D32),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = { onSelectAccount(acc.id) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text("Buka Inbox", style = MaterialTheme.typography.labelMedium)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }

                        if (selectedFolder == EmailFolder.INBOX) {
                            Spacer(modifier = Modifier.height(6.dp))
                            TextButton(onClick = onManualSync) {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Coba Sinkronkan IMAP", style = MaterialTheme.typography.labelSmall)
                            }
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

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Menampilkan ${filteredEmails.size} email",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = onManualSync,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Sync,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sinkronkan Kotak Masuk")
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
