package com.omnimail.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.omnimail.app.data.SampleData
import com.omnimail.app.model.*
import com.omnimail.app.ui.components.*
import com.omnimail.app.ui.screens.accounts.AccountManagementScreen
import com.omnimail.app.ui.screens.accounts.BulkImportCsvDialog
import com.omnimail.app.ui.screens.compose.ComposeEmailScreen
import com.omnimail.app.ui.screens.detail.EmailDetailScreen
import com.omnimail.app.ui.screens.inbox.UnifiedInboxScreen
import com.omnimail.app.ui.screens.search.GlobalSearchScreen
import com.omnimail.app.ui.screens.settings.SettingsScreen
import com.omnimail.app.ui.theme.OmniMailTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OmniMailApp()
        }
    }
}

@Composable
fun OmniMailApp() {
    val systemDark = isSystemInDarkTheme()
    var isDarkTheme by remember { mutableStateOf(systemDark) }

    OmniMailTheme(darkTheme = isDarkTheme) {
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val coroutineScope = rememberCoroutineScope()

        // Master State
        var accounts by remember { mutableStateOf(SampleData.sampleAccounts) }
        var groups by remember { mutableStateOf(SampleData.sampleGroups) }
        var emails by remember { mutableStateOf(SampleData.sampleEmails) }
        var settings by remember { mutableStateOf(SyncSettings()) }

        // Navigation State
        var currentTab by remember { mutableStateOf(NavigationTab.INBOX) }
        var selectedFolder by remember { mutableStateOf(EmailFolder.INBOX) }
        var selectedGroupId by remember { mutableStateOf<String?>(null) } // null = Unified all

        // Active screens & modals
        var activeEmailDetail by remember { mutableStateOf<EmailMessage?>(null) }
        var isComposingEmail by remember { mutableStateOf(false) }
        var showDrawerBulkImport by remember { mutableStateOf(false) }
        var showTopFilterDialog by remember { mutableStateOf(false) }
        var isManualSyncing by remember { mutableStateOf(false) }

        // Total Unread Count
        val totalUnread = emails.count { !it.isRead }

        // Current Filter Header Title & Color
        val (headerTitle, headerColor) = remember(selectedGroupId, groups) {
            if (selectedGroupId == null) {
                "Semua Akun (Unified)" to 0xFF6366F1
            } else {
                val group = groups.find { it.id == selectedGroupId }
                (group?.name ?: "Grup") to (group?.colorHex ?: 0xFF6366F1)
            }
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                OmniDrawerContent(
                    selectedFolder = selectedFolder,
                    onSelectFolder = { folder ->
                        selectedFolder = folder
                        currentTab = NavigationTab.INBOX
                        activeEmailDetail = null
                        coroutineScope.launch { drawerState.close() }
                    },
                    groups = groups,
                    selectedGroupId = selectedGroupId,
                    onSelectGroup = { groupId ->
                        selectedGroupId = groupId
                        currentTab = NavigationTab.INBOX
                        activeEmailDetail = null
                        coroutineScope.launch { drawerState.close() }
                    },
                    onNavigateAccounts = {
                        currentTab = NavigationTab.ACCOUNTS
                        activeEmailDetail = null
                        coroutineScope.launch { drawerState.close() }
                    },
                    onOpenBulkImport = {
                        showDrawerBulkImport = true
                        coroutineScope.launch { drawerState.close() }
                    },
                    onNavigateSettings = {
                        currentTab = NavigationTab.SETTINGS
                        activeEmailDetail = null
                        coroutineScope.launch { drawerState.close() }
                    }
                )
            }
        ) {
            Scaffold(
                topBar = {
                    if (activeEmailDetail == null && !isComposingEmail && currentTab == NavigationTab.INBOX) {
                        OmniTopAppBar(
                            title = headerTitle,
                            subtitle = "${accounts.size} akun terhubung",
                            isSyncing = isManualSyncing,
                            currentFilterName = headerTitle,
                            currentFilterColor = headerColor,
                            onOpenDrawer = {
                                coroutineScope.launch { drawerState.open() }
                            },
                            onOpenSearch = { currentTab = NavigationTab.SEARCH },
                            onManualSync = {
                                coroutineScope.launch {
                                    isManualSyncing = true
                                    delay(1500)
                                    isManualSyncing = false
                                }
                            },
                            onFilterClick = { showTopFilterDialog = true }
                        )
                    }
                },
                bottomBar = {
                    if (activeEmailDetail == null && !isComposingEmail) {
                        OmniBottomBar(
                            currentTab = currentTab,
                            onTabSelected = { tab ->
                                currentTab = tab
                                activeEmailDetail = null
                            },
                            totalUnread = totalUnread
                        )
                    }
                },
                floatingActionButton = {
                    if (activeEmailDetail == null && !isComposingEmail && currentTab == NavigationTab.INBOX) {
                        ExtendedFloatingActionButton(
                            onClick = { isComposingEmail = true },
                            icon = { Icon(Icons.Default.Edit, contentDescription = "Tulis Email") },
                            text = { Text("Tulis Email") },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White,
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when {
                        // Compose Screen Overlay
                        isComposingEmail -> {
                            ComposeEmailScreen(
                                accounts = accounts,
                                onDiscard = { isComposingEmail = false },
                                onSend = { senderId, to, subject, body, attachments ->
                                    val senderAcc = accounts.find { it.id == senderId } ?: accounts.first()
                                    val newSentEmail = EmailMessage(
                                        id = "msg_${System.currentTimeMillis()}",
                                        accountId = senderAcc.id,
                                        accountEmail = senderAcc.email,
                                        accountColorHex = senderAcc.colorHex,
                                        senderName = senderAcc.displayName,
                                        senderEmail = senderAcc.email,
                                        recipients = listOf(to),
                                        subject = subject,
                                        snippet = body.take(80),
                                        bodyText = body,
                                        timestamp = System.currentTimeMillis(),
                                        formattedTime = "Baru saja",
                                        isRead = true,
                                        hasAttachments = attachments.isNotEmpty(),
                                        attachments = attachments,
                                        folder = EmailFolder.SENT
                                    )
                                    emails = listOf(newSentEmail) + emails
                                    isComposingEmail = false
                                }
                            )
                        }

                        // Detail Screen Overlay
                        activeEmailDetail != null -> {
                            EmailDetailScreen(
                                email = activeEmailDetail!!,
                                onBack = { activeEmailDetail = null },
                                onReply = { email ->
                                    isComposingEmail = true
                                },
                                onDelete = { id ->
                                    emails = emails.filter { it.id != id }
                                    activeEmailDetail = null
                                },
                                onToggleStar = { id ->
                                    emails = emails.map {
                                        if (it.id == id) it.copy(isStarred = !it.isStarred) else it
                                    }
                                    activeEmailDetail = activeEmailDetail?.copy(isStarred = !(activeEmailDetail?.isStarred ?: false))
                                }
                            )
                        }

                        // Main Navigation Tabs
                        else -> {
                            when (currentTab) {
                                NavigationTab.INBOX -> {
                                    UnifiedInboxScreen(
                                        emails = emails,
                                        accounts = accounts,
                                        groups = groups,
                                        selectedGroupId = selectedGroupId,
                                        onSelectGroup = { selectedGroupId = it },
                                        onEmailClick = { email ->
                                            emails = emails.map {
                                                if (it.id == email.id) it.copy(isRead = true) else it
                                            }
                                            activeEmailDetail = email.copy(isRead = true)
                                        },
                                        onToggleStar = { id ->
                                            emails = emails.map {
                                                if (it.id == id) it.copy(isStarred = !it.isStarred) else it
                                            }
                                        },
                                        onDeleteEmail = { id ->
                                            emails = emails.filter { it.id != id }
                                        },
                                        onMarkAsRead = { ids ->
                                            emails = emails.map {
                                                if (ids.contains(it.id)) it.copy(isRead = true) else it
                                            }
                                        },
                                        onDeleteBatch = { ids ->
                                            emails = emails.filter { !ids.contains(it.id) }
                                        },
                                        onArchiveBatch = { ids ->
                                            emails = emails.filter { !ids.contains(it.id) }
                                        }
                                    )
                                }
                                NavigationTab.SEARCH -> {
                                    GlobalSearchScreen(
                                        allEmails = emails,
                                        onEmailClick = { email ->
                                            emails = emails.map {
                                                if (it.id == email.id) it.copy(isRead = true) else it
                                            }
                                            activeEmailDetail = email.copy(isRead = true)
                                        },
                                        onToggleStar = { id ->
                                            emails = emails.map {
                                                if (it.id == id) it.copy(isStarred = !it.isStarred) else it
                                            }
                                        }
                                    )
                                }
                                NavigationTab.ACCOUNTS -> {
                                    AccountManagementScreen(
                                        accounts = accounts,
                                        groups = groups,
                                        onAddSingleAccount = { newAcc ->
                                            accounts = accounts + newAcc
                                        },
                                        onBulkImportAccounts = { importedList ->
                                            accounts = accounts + importedList
                                        },
                                        onUpdateAccountProxy = { accId, proxy ->
                                            accounts = accounts.map {
                                                if (it.id == accId) it.copy(proxyConfig = proxy) else it
                                            }
                                        }
                                    )
                                }
                                NavigationTab.SETTINGS -> {
                                    SettingsScreen(
                                        currentSettings = settings,
                                        isDarkTheme = isDarkTheme,
                                        onToggleDarkTheme = { isDarkTheme = it },
                                        onUpdateSettings = { settings = it }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick Top Filter Modal Dialog
        if (showTopFilterDialog) {
            Dialog(onDismissRequest = { showTopFilterDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Tampilkan Kotak Masuk", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (selectedGroupId == null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedGroupId = null
                                            showTopFilterDialog = false
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Layers, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Semua Akun (Unified)", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            items(groups) { grp ->
                                val isCur = selectedGroupId == grp.id
                                val grpColor = Color(grp.colorHex)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isCur) grpColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedGroupId = grp.id
                                            showTopFilterDialog = false
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(grpColor)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(grp.name, modifier = Modifier.weight(1f))
                                        Text("${grp.accountIds.size} akun", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showTopFilterDialog = false }) { Text("Tutup") }
                        }
                    }
                }
            }
        }

        // Drawer shortcut for Bulk Import
        if (showDrawerBulkImport) {
            BulkImportCsvDialog(
                groups = groups,
                onDismiss = { showDrawerBulkImport = false },
                onImportComplete = { imported ->
                    accounts = accounts + imported
                    showDrawerBulkImport = false
                }
            )
        }
    }
}
