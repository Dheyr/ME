package com.omnimail.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.omnimail.app.model.WorkspaceGroup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OmniTopAppBar(
    title: String,
    subtitle: String? = null,
    isSyncing: Boolean = false,
    onOpenDrawer: () -> Unit,
    onOpenSearch: () -> Unit,
    onManualSync: () -> Unit,
    currentFilterName: String = "Semua Akun (Unified)",
    currentFilterColor: Long = 0xFF6366F1,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onFilterClick() }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(currentFilterColor))
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = "Pilih Filter",
                        modifier = Modifier.size(18.dp)
                    )
                }
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onOpenDrawer) {
                Icon(Icons.Default.Menu, contentDescription = "Menu")
            }
        },
        actions = {
            if (isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(20.dp)
                        .padding(end = 4.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                IconButton(onClick = onManualSync) {
                    Icon(
                        Icons.Outlined.Sync,
                        contentDescription = "Sync Now",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onOpenSearch) {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    )
}

enum class NavigationTab {
    INBOX,
    SEARCH,
    ACCOUNTS,
    SETTINGS
}

@Composable
fun OmniBottomBar(
    currentTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    totalUnread: Int = 0,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        NavigationBarItem(
            selected = currentTab == NavigationTab.INBOX,
            onClick = { onTabSelected(NavigationTab.INBOX) },
            icon = {
                BadgedBox(badge = {
                    if (totalUnread > 0) {
                        Badge {
                            Text("$totalUnread")
                        }
                    }
                }) {
                    Icon(
                        if (currentTab == NavigationTab.INBOX) Icons.Filled.Inbox else Icons.Outlined.Inbox,
                        contentDescription = "Kotak Masuk"
                    )
                }
            },
            label = { Text("Inbox") }
        )

        NavigationBarItem(
            selected = currentTab == NavigationTab.SEARCH,
            onClick = { onTabSelected(NavigationTab.SEARCH) },
            icon = {
                Icon(
                    if (currentTab == NavigationTab.SEARCH) Icons.Filled.Search else Icons.Outlined.Search,
                    contentDescription = "Pencarian"
                )
            },
            label = { Text("Cari") }
        )

        NavigationBarItem(
            selected = currentTab == NavigationTab.ACCOUNTS,
            onClick = { onTabSelected(NavigationTab.ACCOUNTS) },
            icon = {
                Icon(
                    if (currentTab == NavigationTab.ACCOUNTS) Icons.Filled.ManageAccounts else Icons.Outlined.ManageAccounts,
                    contentDescription = "Akun"
                )
            },
            label = { Text("Akun") }
        )

        NavigationBarItem(
            selected = currentTab == NavigationTab.SETTINGS,
            onClick = { onTabSelected(NavigationTab.SETTINGS) },
            icon = {
                Icon(
                    if (currentTab == NavigationTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                    contentDescription = "Pengaturan"
                )
            },
            label = { Text("Setting") }
        )
    }
}
