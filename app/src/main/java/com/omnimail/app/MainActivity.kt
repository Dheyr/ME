package com.omnimail.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.omnimail.app.data.OmniStorage
import com.omnimail.app.model.*
import com.omnimail.app.service.EmailService
import com.omnimail.app.ui.components.*
import com.omnimail.app.ui.screens.accounts.AccountManagementScreen
import com.omnimail.app.ui.screens.accounts.BulkImportCsvDialog
import com.omnimail.app.ui.screens.compose.ComposeEmailScreen
import com.omnimail.app.ui.screens.detail.EmailDetailScreen
import com.omnimail.app.ui.screens.inbox.UnifiedInboxScreen
import com.omnimail.app.ui.screens.search.GlobalSearchScreen
import com.omnimail.app.ui.screens.settings.SettingsScreen
import com.omnimail.app.ui.theme.OmniMailTheme
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
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    var isDarkTheme by remember { mutableStateOf(systemDark) }

    OmniMailTheme(darkTheme = isDarkTheme) {
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val coroutineScope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }

        // Persistent Master State (Clean by default - zero dummy data)
        var accounts by remember { mutableStateOf(OmniStorage.loadAccounts(context)) }
        var groups by remember { mutableStateOf(OmniStorage.loadGroups(context)) }
        var emails by remember { mutableStateOf(OmniStorage.loadEmails(context)) }
        var settings by remember { mutableStateOf(OmniStorage.loadSettings(context)) }

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

        // Function: Real background sync for all accounts via IMAP
        val syncAllAccounts: () -> Unit = {
            if (accounts.isEmpty()) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Belum ada akun yang terhubung. Tambahkan akun terlebih dahulu.")
                }
            } else {
                coroutineScope.launch {
                    isManualSyncing = true
                    var totalNew = 0
                    val allFetched = mutableListOf<EmailMessage>()
                    val updatedAccounts = accounts.toMutableList()

                    for ((idx, acc) in accounts.withIndex()) {
                        val res = EmailService.fetchInboxEmails(acc, limit = 25)
                        if (res.isSuccess) {
                            val fetched = res.getOrDefault(emptyList())
                            allFetched.addAll(fetched)
                            val unread = fetched.count { !it.isRead }
                            updatedAccounts[idx] = acc.copy(unreadCount = unread, status = AccountStatus.ONLINE)
                        } else {
                            updatedAccounts[idx] = acc.copy(status = AccountStatus.ERROR)
                        }
                    }

                    if (allFetched.isNotEmpty()) {
                        val existingIds = emails.map { it.id }.toSet()
                        val newlyAdded = allFetched.filter { it.id !in existingIds }
                        totalNew = newlyAdded.size
                        val merged = (newlyAdded + emails).sortedByDescending { it.timestamp }
                        emails = merged
                        OmniStorage.saveEmails(context, merged)
                    }
                    accounts = updatedAccounts
                    OmniStorage.saveAccounts(context, updatedAccounts)

                    isManualSyncing = false
                    snackbarHostState.showSnackbar(
                        if (totalNew > 0) "$totalNew email baru berhasil ditarik!" else "Semua kotak masuk sudah diperbarui."
                    )
                }
            }
        }

        // Auto-sync on startup if accounts are present
        LaunchedEffect(Unit) {
            if (accounts.isNotEmpty()) {
                syncAllAccounts()
            }
        }

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
                snackbarHost = { SnackbarHost(snackbarHostState) },
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
                            onManualSync = { syncAllAccounts() },
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
                            onClick = {
                                if (accounts.isEmpty()) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Tambahkan akun terlebih dahulu untuk mengirim email.")
                                    }
                                } else {
                                    isComposingEmail = true
                                }
                            },
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
                                    val senderAcc = accounts.find { it.id == senderId } ?: accounts.firstOrNull()
                                    if (senderAcc != null) {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Sedang mengirim email via SMTP...")
                                            val res = EmailService.sendEmail(
                                                fromAccount = senderAcc,
                                                recipients = listOf(to),
                                                subject = subject,
                                                body = body
                                            )
                                            if (res.isSuccess) {
                                                val newSentEmail = EmailMessage(
                                                    id = "sent_${System.currentTimeMillis()}",
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
                                                OmniStorage.saveEmails(context, emails)
                                                isComposingEmail = false
                                                snackbarHostState.showSnackbar("Email berhasil dikirim ke $to!")
                                            } else {
                                                val err = res.exceptionOrNull()?.message ?: "Gagal mengirim email"
                                                snackbarHostState.showSnackbar("Gagal kirim: $err")
                                            }
                                        }
                                    }
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
                                    OmniStorage.saveEmails(context, emails)
                                    activeEmailDetail = null
                                },
                                onToggleStar = { id ->
                                    emails = emails.map {
                                        if (it.id == id) it.copy(isStarred = !it.isStarred) else it
                                    }
                                    OmniStorage.saveEmails(context, emails)
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
                                            OmniStorage.saveEmails(context, emails)
                                            activeEmailDetail = email.copy(isRead = true)
                                        },
                                        onToggleStar = { id ->
                                            emails = emails.map {
                                                if (it.id == id) it.copy(isStarred = !it.isStarred) else it
                                            }
                                            OmniStorage.saveEmails(context, emails)
                                        },
                                        onDeleteEmail = { id ->
                                            emails = emails.filter { it.id != id }
                                            OmniStorage.saveEmails(context, emails)
                                        },
                                        onMarkAsRead = { ids ->
                                            emails = emails.map {
                                                if (ids.contains(it.id)) it.copy(isRead = true) else it
                                            }
                                            OmniStorage.saveEmails(context, emails)
                                        },
                                        onDeleteBatch = { ids ->
                                            emails = emails.filter { !ids.contains(it.id) }
                                            OmniStorage.saveEmails(context, emails)
                                        },
                                        onArchiveBatch = { ids ->
                                            emails = emails.filter { !ids.contains(it.id) }
                                            OmniStorage.saveEmails(context, emails)
                                        },
                                        onNavigateToAccounts = { currentTab = NavigationTab.ACCOUNTS },
                                        onManualSync = { syncAllAccounts() }
                                    )
                                }
                                NavigationTab.SEARCH -> {
                                    GlobalSearchScreen(
                                        allEmails = emails,
                                        onEmailClick = { email ->
                                            emails = emails.map {
                                                if (it.id == email.id) it.copy(isRead = true) else it
                                            }
                                            OmniStorage.saveEmails(context, emails)
                                            activeEmailDetail = email.copy(isRead = true)
                                        },
                                        onToggleStar = { id ->
                                            emails = emails.map {
                                                if (it.id == id) it.copy(isStarred = !it.isStarred) else it
                                            }
                                            OmniStorage.saveEmails(context, emails)
                                        }
                                    )
                                }
                                NavigationTab.ACCOUNTS -> {
                                    AccountManagementScreen(
                                        accounts = accounts,
                                        groups = groups,
                                        onAddSingleAccount = { newAcc ->
                                            val updated = accounts + newAcc
                                            accounts = updated
                                            OmniStorage.saveAccounts(context, updated)

                                            groups = groups.map { grp ->
                                                if (grp.id == newAcc.workspaceGroupId) grp.copy(accountIds = grp.accountIds + newAcc.id) else grp
                                            }
                                            OmniStorage.saveGroups(context, groups)

                                            // Automatically trigger sync for newly added account
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Akun ${newAcc.email} ditambahkan. Menarik email...")
                                                val res = EmailService.fetchInboxEmails(newAcc, limit = 25)
                                                if (res.isSuccess) {
                                                    val fetched = res.getOrDefault(emptyList())
                                                    val merged = (fetched + emails).distinctBy { it.id }.sortedByDescending { it.timestamp }
                                                    emails = merged
                                                    OmniStorage.saveEmails(context, merged)
                                                    val refreshedAccounts = accounts.map {
                                                        if (it.id == newAcc.id) it.copy(unreadCount = fetched.count { m -> !m.isRead }) else it
                                                    }
                                                    accounts = refreshedAccounts
                                                    OmniStorage.saveAccounts(context, refreshedAccounts)
                                                    snackbarHostState.showSnackbar("${fetched.size} email berhasil ditarik dari ${newAcc.email}!")
                                                }
                                            }
                                        },
                                        onBulkImportAccounts = { importedList ->
                                            val updated = accounts + importedList
                                            accounts = updated
                                            OmniStorage.saveAccounts(context, updated)
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("${importedList.size} akun berhasil diimpor! Menarik email...")
                                                syncAllAccounts()
                                            }
                                        },
                                        onUpdateAccountProxy = { accId, proxy ->
                                            val updated = accounts.map {
                                                if (it.id == accId) it.copy(proxyConfig = proxy) else it
                                            }
                                            accounts = updated
                                            OmniStorage.saveAccounts(context, updated)
                                        },
                                        onDeleteAccount = { accId ->
                                            val deleted = accounts.find { it.id == accId }
                                            val updated = accounts.filter { it.id != accId }
                                            accounts = updated
                                            emails = emails.filter { it.accountId != accId }
                                            groups = groups.map { it.copy(accountIds = it.accountIds.filter { id -> id != accId }) }
                                            OmniStorage.saveAccounts(context, updated)
                                            OmniStorage.saveEmails(context, emails)
                                            OmniStorage.saveGroups(context, groups)
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Akun ${deleted?.email ?: ""} telah dihapus.")
                                            }
                                        }
                                    )
                                }
                                NavigationTab.SETTINGS -> {
                                    SettingsScreen(
                                        currentSettings = settings,
                                        isDarkTheme = isDarkTheme,
                                        onToggleDarkTheme = { isDarkTheme = it },
                                        onUpdateSettings = {
                                            settings = it
                                            OmniStorage.saveSettings(context, it)
                                        }
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
                    val updated = accounts + imported
                    accounts = updated
                    OmniStorage.saveAccounts(context, updated)
                    showDrawerBulkImport = false
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("${imported.size} akun berhasil diimpor! Menarik email...")
                        syncAllAccounts()
                    }
                }
            )
        }
    }
}
