package com.omnimail.app.ui.screens.accounts

import androidx.compose.animation.*
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.omnimail.app.model.*
import com.omnimail.app.ui.components.AccountBadge
import com.omnimail.app.ui.components.StatusIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountManagementScreen(
    accounts: List<EmailAccount>,
    groups: List<WorkspaceGroup>,
    onAddSingleAccount: (EmailAccount) -> Unit,
    onBulkImportAccounts: (List<EmailAccount>) -> Unit,
    onUpdateAccountProxy: (accountId: String, ProxyConfig?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showBulkImportDialog by remember { mutableStateOf(false) }
    var selectedGroupFilter by remember { mutableStateOf<String?>(null) }
    var selectedAccountForProxy by remember { mutableStateOf<EmailAccount?>(null) }

    val filteredAccounts = remember(accounts, selectedGroupFilter) {
        if (selectedGroupFilter == null) accounts
        else accounts.filter { it.workspaceGroupId == selectedGroupFilter }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Kelola Akun (${accounts.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Workspaces & Konfigurasi Proxy", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = { showBulkImportDialog = true }) {
                        Icon(Icons.Outlined.UploadFile, contentDescription = "Import CSV", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Tambah Akun", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Quick Action Banner
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Impor CSV Akun Massal", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Text("Tambah puluhan akun sekaligus via file spreadsheet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = { showBulkImportDialog = true },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Impor CSV")
                        }
                    }
                }
            }

            // Workspaces / Groups Overview
            item {
                Text(
                    text = "WORKSPACES / GRUP",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groups.forEach { group ->
                        val isSelected = selectedGroupFilter == group.id
                        val groupColor = Color(group.colorHex)

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) groupColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) groupColor else Color.Transparent
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedGroupFilter = if (isSelected) null else group.id
                                }
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(groupColor)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = group.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${group.accountIds.size} akun",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Accounts List Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DAFTAR AKUN AKTIF (${filteredAccounts.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    if (selectedGroupFilter != null) {
                        TextButton(onClick = { selectedGroupFilter = null }) {
                            Text("Reset Filter Grup", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Accounts rows
            items(filteredAccounts, key = { it.id }) { account ->
                AccountCardItem(
                    account = account,
                    onConfigureProxy = { selectedAccountForProxy = account }
                )
            }
        }
    }

    // Modal: Single Account Manual/OAuth Dialog (FR 1.1)
    if (showAddDialog) {
        AddSingleAccountDialog(
            groups = groups,
            onDismiss = { showAddDialog = false },
            onAddAccount = {
                onAddSingleAccount(it)
                showAddDialog = false
            }
        )
    }

    // Modal: CSV Bulk Import Dialog (FR 1.2)
    if (showBulkImportDialog) {
        BulkImportCsvDialog(
            groups = groups,
            onDismiss = { showBulkImportDialog = false },
            onImportComplete = { imported ->
                onBulkImportAccounts(imported)
                showBulkImportDialog = false
            }
        )
    }

    // Modal: Proxy Configuration Dialog (FR 1.4)
    selectedAccountForProxy?.let { account ->
        ProxyConfigDialog(
            account = account,
            onDismiss = { selectedAccountForProxy = null },
            onSaveProxy = { proxy ->
                onUpdateAccountProxy(account.id, proxy)
                selectedAccountForProxy = null
            }
        )
    }
}

@Composable
fun AccountCardItem(
    account: EmailAccount,
    onConfigureProxy: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(account.colorHex))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = account.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = account.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StatusIndicator(status = account.status)
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(8.dp))

            // Protocol, Push, & Proxy Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = { },
                        label = {
                            Text(
                                when (account.authType) {
                                    AuthType.OAUTH_GOOGLE -> "Google OAuth"
                                    AuthType.OAUTH_MICROSOFT -> "Microsoft OAuth"
                                    AuthType.IMAP_SMTP_MANUAL -> "IMAP/SMTP"
                                },
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )

                    if (account.isPushEnabled) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                "IMAP IDLE Push",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Proxy Setting Action
                TextButton(onClick = onConfigureProxy) {
                    Icon(
                        Icons.Outlined.VpnKey,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (account.proxyConfig?.enabled == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (account.proxyConfig?.enabled == true) "Proxy Aktif" else "Set Proxy",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (account.proxyConfig?.enabled == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun AddSingleAccountDialog(
    groups: List<WorkspaceGroup>,
    onDismiss: () -> Unit,
    onAddAccount: (EmailAccount) -> Unit
) {
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var selectedAuthType by remember { mutableStateOf(AuthType.IMAP_SMTP_MANUAL) }
    var selectedGroupId by remember { mutableStateOf(groups.firstOrNull()?.id ?: "") }
    var autoDetectedHost by remember { mutableStateOf("") }

    // Auto-detect server settings based on domain (FR 1.1)
    LaunchedEffect(emailInput) {
        val domain = emailInput.substringAfter("@", "")
        autoDetectedHost = when {
            domain.contains("gmail") -> "imap.gmail.com:993 (OAuth disarankan)"
            domain.contains("outlook") || domain.contains("hotmail") -> "outlook.office365.com:993"
            domain.contains("yahoo") -> "imap.mail.yahoo.com:993"
            domain.isNotBlank() -> "imap.$domain:993 (Otomatis)"
            else -> ""
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Tambah Akun Baru", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(14.dp))

                // OAuth Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            selectedAuthType = AuthType.OAUTH_GOOGLE
                            emailInput = "new.user@gmail.com"
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Google OAuth", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            selectedAuthType = AuthType.OAUTH_MICROSOFT
                            emailInput = "new.user@outlook.com"
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Microsoft", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text("Alamat Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (autoDetectedHost.isNotBlank()) {
                    Text(
                        text = "Server terdeteksi: $autoDetectedHost",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("Password / App Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Batal") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (emailInput.isNotBlank()) {
                                onAddAccount(
                                    EmailAccount(
                                        id = "acc_${System.currentTimeMillis()}",
                                        email = emailInput,
                                        displayName = emailInput.substringBefore("@"),
                                        authType = selectedAuthType,
                                        workspaceGroupId = selectedGroupId,
                                        colorHex = 0xFF3B82F6
                                    )
                                )
                            }
                        },
                        enabled = emailInput.isNotBlank()
                    ) {
                        Text("Simpan Akun")
                    }
                }
            }
        }
    }
}

@Composable
fun BulkImportCsvDialog(
    groups: List<WorkspaceGroup>,
    onDismiss: () -> Unit,
    onImportComplete: (List<EmailAccount>) -> Unit
) {
    var isImporting by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var statusText by remember { mutableStateOf("Siap memuat file CSV") }
    var failedAccounts by remember { mutableStateOf(listOf<String>()) }
    var importedCount by remember { mutableStateOf(0) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Bulk Account Import (CSV)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Format: email, password, imap_server, smtp_server",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Outlined.FilePresent,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("accounts_batch_50.csv (50 akun terdeteksi)", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Text("Ukuran: 4.2 KB", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (isImporting) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(statusText, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "${(progress * 100).toInt()}% selesai (${importedCount}/50 diverifikasi)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (failedAccounts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Akun Gagal Login (${failedAccounts.size}):",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                    failedAccounts.forEach { fail ->
                        Text("• $fail", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss, enabled = !isImporting) { Text("Tutup") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            isImporting = true
                            progress = 0.2f
                            statusText = "Memverifikasi koneksi IMAP..."
                            importedCount = 10

                            // Simulate batch progress
                            progress = 0.65f
                            statusText = "Menguji SSL/TLS Handshake..."
                            importedCount = 35

                            progress = 1.0f
                            statusText = "Verifikasi selesai!"
                            importedCount = 48
                            failedAccounts = listOf(
                                "user49@domain.com (Timeout)",
                                "user50@domain.com (Invalid Credentials)"
                            )
                            isImporting = false

                            // Generate batch accounts
                            val newAccounts = (1..5).map { i ->
                                EmailAccount(
                                    id = "bulk_acc_$i",
                                    email = "bulk.agent$i@campaign.io",
                                    displayName = "Campaign Agent $i",
                                    authType = AuthType.IMAP_SMTP_MANUAL,
                                    workspaceGroupId = groups.firstOrNull()?.id ?: "group_marketing",
                                    colorHex = 0xFFD84A1B
                                )
                            }
                            onImportComplete(newAccounts)
                        },
                        enabled = !isImporting
                    ) {
                        Text(if (isImporting) "Memproses..." else "Mulai Impor Massal")
                    }
                }
            }
        }
    }
}

@Composable
fun ProxyConfigDialog(
    account: EmailAccount,
    onDismiss: () -> Unit,
    onSaveProxy: (ProxyConfig?) -> Unit
) {
    var isProxyEnabled by remember { mutableStateOf(account.proxyConfig?.enabled ?: false) }
    var proxyType by remember { mutableStateOf(account.proxyConfig?.type ?: ProxyType.SOCKS5) }
    var host by remember { mutableStateOf(account.proxyConfig?.host ?: "") }
    var port by remember { mutableStateOf(account.proxyConfig?.port?.toString() ?: "1080") }
    var user by remember { mutableStateOf(account.proxyConfig?.username ?: "") }
    var pass by remember { mutableStateOf(account.proxyConfig?.password ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Konfigurasi Proxy", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Untuk akun: ${account.email}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Gunakan Proxy Khusus", fontWeight = FontWeight.Medium)
                    Switch(
                        checked = isProxyEnabled,
                        onCheckedChange = { isProxyEnabled = it }
                    )
                }

                if (isProxyEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(ProxyType.SOCKS5, ProxyType.HTTP, ProxyType.HTTPS).forEach { type ->
                            FilterChip(
                                selected = proxyType == type,
                                onClick = { proxyType = type },
                                label = { Text(type.name, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = host,
                            onValueChange = { host = it },
                            label = { Text("Host / IP") },
                            modifier = Modifier.weight(2f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = port,
                            onValueChange = { port = it },
                            label = { Text("Port") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = user,
                        onValueChange = { user = it },
                        label = { Text("Username (Opsional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Batal") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        val config = if (isProxyEnabled) {
                            ProxyConfig(
                                type = proxyType,
                                host = host,
                                port = port.toIntOrNull() ?: 1080,
                                username = user,
                                password = pass,
                                enabled = true
                            )
                        } else null
                        onSaveProxy(config)
                    }) {
                        Text("Simpan Proxy")
                    }
                }
            }
        }
    }
}
