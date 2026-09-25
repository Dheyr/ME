package com.omnimail.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnimail.app.model.EmailAccount
import com.omnimail.app.model.EmailFolder
import com.omnimail.app.model.EmailMessage

@Composable
fun OmniDrawerContent(
    selectedFolder: EmailFolder,
    onSelectFolder: (EmailFolder) -> Unit,
    accounts: List<EmailAccount>,
    selectedAccountId: String?, // null = Semua Akun (Unified)
    onSelectAccount: (String?) -> Unit,
    onNavigateAccounts: () -> Unit,
    onNavigateSettings: () -> Unit,
    emails: List<EmailMessage> = emptyList(),
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(
        modifier = modifier.width(320.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // App Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.AllInbox,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "OmniMail",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Multi-Account Email Client",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Section: Folders
                item {
                    Text(
                        text = "KOTAK SURAT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                val inboxUnread = emails.count { it.folder == EmailFolder.INBOX && !it.isRead }
                val draftsCount = emails.count { it.folder == EmailFolder.DRAFTS }
                val starredCount = emails.count { it.isStarred }
                val sentCount = emails.count { it.folder == EmailFolder.SENT }
                val trashCount = emails.count { it.folder == EmailFolder.TRASH }
                val spamCount = emails.count { it.folder == EmailFolder.SPAM }

                item {
                    FolderDrawerItem(
                        icon = Icons.Outlined.Inbox,
                        label = "Kotak Masuk (Inbox)",
                        unread = if (inboxUnread > 0) "$inboxUnread" else null,
                        isSelected = selectedFolder == EmailFolder.INBOX,
                        onClick = { onSelectFolder(EmailFolder.INBOX) }
                    )
                }
                item {
                    FolderDrawerItem(
                        icon = Icons.Outlined.StarBorder,
                        label = "Berbintang (Starred)",
                        unread = if (starredCount > 0) "$starredCount" else null,
                        isSelected = selectedFolder == EmailFolder.STARRED,
                        onClick = { onSelectFolder(EmailFolder.STARRED) }
                    )
                }
                item {
                    FolderDrawerItem(
                        icon = Icons.Outlined.Send,
                        label = "Terkirim (Sent)",
                        unread = if (sentCount > 0) "$sentCount" else null,
                        isSelected = selectedFolder == EmailFolder.SENT,
                        onClick = { onSelectFolder(EmailFolder.SENT) }
                    )
                }
                item {
                    FolderDrawerItem(
                        icon = Icons.Outlined.Drafts,
                        label = "Draf (Drafts)",
                        unread = if (draftsCount > 0) "$draftsCount" else null,
                        isSelected = selectedFolder == EmailFolder.DRAFTS,
                        onClick = { onSelectFolder(EmailFolder.DRAFTS) }
                    )
                }
                item {
                    FolderDrawerItem(
                        icon = Icons.Outlined.DeleteOutline,
                        label = "Sampah (Trash)",
                        unread = if (trashCount > 0) "$trashCount" else null,
                        isSelected = selectedFolder == EmailFolder.TRASH,
                        onClick = { onSelectFolder(EmailFolder.TRASH) }
                    )
                }
                item {
                    FolderDrawerItem(
                        icon = Icons.Outlined.ReportGmailerrorred,
                        label = "Spam",
                        unread = if (spamCount > 0) "$spamCount" else null,
                        isSelected = selectedFolder == EmailFolder.SPAM,
                        onClick = { onSelectFolder(EmailFolder.SPAM) }
                    )
                }

                // Section: Akun Email
                item {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "AKUN EMAIL (${accounts.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                // Unified / Semua Akun Item
                item {
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                Icons.Default.Layers,
                                contentDescription = null,
                                tint = if (selectedAccountId == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        label = {
                            Text(
                                "Semua Akun (Unified)",
                                fontWeight = if (selectedAccountId == null) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        selected = selectedAccountId == null,
                        onClick = { onSelectAccount(null) },
                        modifier = Modifier.padding(horizontal = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                items(accounts, key = { it.id }) { acc ->
                    val isSelected = selectedAccountId == acc.id
                    val accColor = Color(acc.colorHex)
                    val accUnread = emails.count { it.accountId == acc.id && !it.isRead }

                    NavigationDrawerItem(
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(accColor)
                            )
                        },
                        label = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = acc.displayName.ifBlank { acc.email },
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f, fill = false),
                                    maxLines = 1
                                )
                                if (accUnread > 0) {
                                    Surface(
                                        shape = CircleShape,
                                        color = accColor.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "$accUnread",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = accColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        },
                        selected = isSelected,
                        onClick = { onSelectAccount(acc.id) },
                        modifier = Modifier.padding(horizontal = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Bottom Actions in Drawer
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                TextButton(
                    onClick = onNavigateAccounts,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Outlined.ManageAccounts,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Kelola Akun & Server",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Medium
                    )
                }

                TextButton(
                    onClick = onNavigateSettings,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Outlined.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Pengaturan Aplikasi",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun FolderDrawerItem(
    icon: ImageVector,
    label: String,
    unread: String?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        icon = {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        label = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                if (unread != null) {
                    Text(
                        text = unread,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        selected = isSelected,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 4.dp),
        shape = RoundedCornerShape(12.dp)
    )
}
