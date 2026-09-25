package com.omnimail.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.omnimail.app.model.EmailFolder
import com.omnimail.app.model.WorkspaceGroup

@Composable
fun OmniDrawerContent(
    selectedFolder: EmailFolder,
    onSelectFolder: (EmailFolder) -> Unit,
    groups: List<WorkspaceGroup>,
    selectedGroupId: String?, // null = All Workspaces / Unified
    onSelectGroup: (String?) -> Unit,
    onNavigateAccounts: () -> Unit,
    onOpenBulkImport: () -> Unit,
    onNavigateSettings: () -> Unit,
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
                        text = "Mass Multi-Account Client",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Section: Workspaces / Groups
                item {
                    Text(
                        text = "WORKSPACES / GRUP AKUN",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                // All accounts item
                item {
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                Icons.Default.Layers,
                                contentDescription = null,
                                tint = if (selectedGroupId == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        label = {
                            Text(
                                "Semua Akun (Unified)",
                                fontWeight = if (selectedGroupId == null) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        selected = selectedGroupId == null,
                        onClick = { onSelectGroup(null) },
                        modifier = Modifier.padding(horizontal = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                items(groups) { group ->
                    val isSelected = selectedGroupId == group.id
                    val groupColor = Color(group.colorHex)

                    NavigationDrawerItem(
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(groupColor)
                            )
                        },
                        label = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = group.name,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = groupColor.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "${group.accountIds.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = groupColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        },
                        selected = isSelected,
                        onClick = { onSelectGroup(group.id) },
                        modifier = Modifier.padding(horizontal = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                item {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "FOLDERS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                // Folders list
                item {
                    FolderDrawerItem(
                        icon = Icons.Outlined.Inbox,
                        label = "Kotak Masuk (Inbox)",
                        unread = "28",
                        isSelected = selectedFolder == EmailFolder.INBOX,
                        onClick = { onSelectFolder(EmailFolder.INBOX) }
                    )
                }
                item {
                    FolderDrawerItem(
                        icon = Icons.Outlined.StarBorder,
                        label = "Berbintang (Starred)",
                        unread = null,
                        isSelected = selectedFolder == EmailFolder.STARRED,
                        onClick = { onSelectFolder(EmailFolder.STARRED) }
                    )
                }
                item {
                    FolderDrawerItem(
                        icon = Icons.Outlined.Send,
                        label = "Terkirim (Sent)",
                        unread = null,
                        isSelected = selectedFolder == EmailFolder.SENT,
                        onClick = { onSelectFolder(EmailFolder.SENT) }
                    )
                }
                item {
                    FolderDrawerItem(
                        icon = Icons.Outlined.Drafts,
                        label = "Draf (Drafts)",
                        unread = "3",
                        isSelected = selectedFolder == EmailFolder.DRAFTS,
                        onClick = { onSelectFolder(EmailFolder.DRAFTS) }
                    )
                }
                item {
                    FolderDrawerItem(
                        icon = Icons.Outlined.DeleteOutline,
                        label = "Sampah (Trash)",
                        unread = null,
                        isSelected = selectedFolder == EmailFolder.TRASH,
                        onClick = { onSelectFolder(EmailFolder.TRASH) }
                    )
                }
                item {
                    FolderDrawerItem(
                        icon = Icons.Outlined.ReportGmailerrorred,
                        label = "Spam",
                        unread = null,
                        isSelected = selectedFolder == EmailFolder.SPAM,
                        onClick = { onSelectFolder(EmailFolder.SPAM) }
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
                    onClick = onOpenBulkImport,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Outlined.UploadFile,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Import Akun Massal (CSV)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }

                TextButton(
                    onClick = onNavigateAccounts,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Outlined.ManageAccounts,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Kelola Akun & Proxy",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
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
