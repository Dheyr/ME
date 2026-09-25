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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.omnimail.app.model.*
import com.omnimail.app.service.EmailService
import com.omnimail.app.ui.components.StatusIndicator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountManagementScreen(
    accounts: List<EmailAccount>,
    groups: List<WorkspaceGroup>,
    onAddSingleAccount: (EmailAccount) -> Unit,
    onBulkImportAccounts: (List<EmailAccount>) -> Unit,
    onUpdateAccountProxy: (accountId: String, ProxyConfig?) -> Unit,
    onDeleteAccount: (accountId: String) -> Unit = {},
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
                            Text("Tambah puluhan akun sekaligus via teks/file spreadsheet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

            // Workspace Groups Filter Chips
            item {
                Text(
                    text = "Grup Workspace",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedGroupFilter == null,
                        onClick = { selectedGroupFilter = null },
                        label = { Text("Semua (${accounts.size})") }
                    )
                    groups.forEach { group ->
                        val groupAccounts = accounts.count { it.workspaceGroupId == group.id }
                        val isSelected = selectedGroupFilter == group.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedGroupFilter = if (isSelected) null else group.id },
                            label = { Text("${group.name} ($groupAccounts)") },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(group.colorHex))
                                )
                            }
                        )
                    }
                }
            }

            // Account List Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Daftar Akun (${filteredAccounts.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tambah Akun", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            if (filteredAccounts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Outlined.Email, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                            Text("Belum Ada Akun", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "Tambahkan akun email Anda (Gmail, Outlook, Yahoo, atau IMAP lainnya) untuk mulai menggunakan aplikasi.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(onClick = { showAddDialog = true }, shape = RoundedCornerShape(8.dp)) {
                                Text("Tambah Akun Pertama")
                            }
                        }
                    }
                }
            } else {
                items(filteredAccounts, key = { it.id }) { account ->
                    AccountCard(
                        account = account,
                        onConfigureProxy = { selectedAccountForProxy = account },
                        onDeleteAccount = { onDeleteAccount(account.id) }
                    )
                }
            }
        }
    }

    // Add Single Account Dialog
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

    // Bulk Import CSV Dialog
    if (showBulkImportDialog) {
        BulkImportCsvDialog(
            groups = groups,
            onDismiss = { showBulkImportDialog = false },
            onImportComplete = {
                onBulkImportAccounts(it)
                showBulkImportDialog = false
            }
        )
    }

    // Proxy Config Dialog
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
fun AccountCard(
    account: EmailAccount,
    onConfigureProxy: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(account.colorHex))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = account.displayName.ifBlank { account.email },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = account.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "IMAP: ${account.imapHost}:${account.imapPort} | SMTP: ${account.smtpHost}:${account.smtpPort}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }

                StatusIndicator(status = account.status)
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onDeleteAccount, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Hapus Akun",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(8.dp))

            // Protocol & Proxy Info
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
                                "IMAP/SMTP (${if (account.useSsl) "SSL" else "TLS"})",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                    if (account.unreadCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                "${account.unreadCount} belum dibaca",
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
    val coroutineScope = rememberCoroutineScope()

    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var selectedGroupId by remember { mutableStateOf(groups.firstOrNull()?.id ?: "") }

    var imapHost by remember { mutableStateOf("") }
    var imapPort by remember { mutableStateOf("993") }
    var smtpHost by remember { mutableStateOf("") }
    var smtpPort by remember { mutableStateOf("465") }
    var useSsl by remember { mutableStateOf(true) }

    var showAdvancedServer by remember { mutableStateOf(false) }

    var isTestingConnection by remember { mutableStateOf(false) }
    var testSuccess by remember { mutableStateOf<Boolean?>(null) }
    var testErrorMessage by remember { mutableStateOf<String?>(null) }

    // Auto-detect server settings based on domain
    LaunchedEffect(emailInput) {
        val serverConfig = EmailService.autoDetectServer(emailInput)
        imapHost = serverConfig.imapHost
        imapPort = serverConfig.imapPort.toString()
        smtpHost = serverConfig.smtpHost
        smtpPort = serverConfig.smtpPort.toString()
        useSsl = serverConfig.useSsl
        testSuccess = null
        testErrorMessage = null
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text("Tambah Akun Email Sungguhan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Mendukung Gmail, Outlook, Yahoo, Zoho, dan cPanel/Custom IMAP",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Helpful Info Tip
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(18.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Untuk Gmail & Yahoo: Aktifkan 2-Step Verification di akun Anda, lalu gunakan Sandi Aplikasi (App Password 16 karakter), bukan sandi akun biasa.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text("Alamat Email Lengkap") },
                    placeholder = { Text("contoh@gmail.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = {
                        passwordInput = it
                        testSuccess = null
                    },
                    label = { Text("Password / App Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Test Connection Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = { showAdvancedServer = !showAdvancedServer }) {
                        Text(if (showAdvancedServer) "Sembunyikan Server" else "Pengaturan Server Lanjutan")
                    }

                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                isTestingConnection = true
                                testSuccess = null
                                testErrorMessage = null
                                val testAcc = EmailAccount(
                                    id = "test",
                                    email = emailInput.trim(),
                                    displayName = emailInput.substringBefore("@"),
                                    authType = AuthType.IMAP_SMTP_MANUAL,
                                    workspaceGroupId = selectedGroupId,
                                    colorHex = 0xFF3B82F6,
                                    password = passwordInput,
                                    imapHost = imapHost,
                                    imapPort = imapPort.toIntOrNull() ?: 993,
                                    smtpHost = smtpHost,
                                    smtpPort = smtpPort.toIntOrNull() ?: 465,
                                    useSsl = useSsl
                                )
                                val result = EmailService.testConnection(testAcc)
                                isTestingConnection = false
                                if (result.isSuccess) {
                                    testSuccess = true
                                } else {
                                    testSuccess = false
                                    testErrorMessage = result.exceptionOrNull()?.message ?: "Gagal terhubung"
                                }
                            }
                        },
                        enabled = emailInput.isNotBlank() && passwordInput.isNotBlank() && !isTestingConnection,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isTestingConnection) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Menguji...", fontSize = 12.sp)
                        } else {
                            Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tes Koneksi", fontSize = 12.sp)
                        }
                    }
                }

                // Test Connection Results
                if (testSuccess == true) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Koneksi IMAP Berhasil! Akun siap digunakan.", color = Color(0xFF1B5E20), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                } else if (testSuccess == false) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Gagal: ${testErrorMessage ?: "Periksa password / email"}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text("Pastikan menggunakan Sandi Aplikasi (bukan sandi biasa).", color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // Advanced Server Config Collapsible
                AnimatedVisibility(visible = showAdvancedServer) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = imapHost,
                                onValueChange = { imapHost = it },
                                label = { Text("IMAP Host") },
                                singleLine = true,
                                modifier = Modifier.weight(2f)
                            )
                            OutlinedTextField(
                                value = imapPort,
                                onValueChange = { imapPort = it },
                                label = { Text("Port") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = smtpHost,
                                onValueChange = { smtpHost = it },
                                label = { Text("SMTP Host") },
                                singleLine = true,
                                modifier = Modifier.weight(2f)
                            )
                            OutlinedTextField(
                                value = smtpPort,
                                onValueChange = { smtpPort = it },
                                label = { Text("Port") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

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
                                        email = emailInput.trim(),
                                        displayName = emailInput.substringBefore("@"),
                                        authType = AuthType.IMAP_SMTP_MANUAL,
                                        workspaceGroupId = selectedGroupId,
                                        colorHex = 0xFF3B82F6,
                                        password = passwordInput,
                                        imapHost = imapHost.ifBlank { "imap.${emailInput.substringAfter("@")}" },
                                        imapPort = imapPort.toIntOrNull() ?: 993,
                                        smtpHost = smtpHost.ifBlank { "smtp.${emailInput.substringAfter("@")}" },
                                        smtpPort = smtpPort.toIntOrNull() ?: 465,
                                        useSsl = useSsl
                                    )
                                )
                            }
                        },
                        enabled = emailInput.isNotBlank() && passwordInput.isNotBlank()
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
    val coroutineScope = rememberCoroutineScope()
    var csvText by remember { mutableStateOf("") }
    var selectedGroupId by remember { mutableStateOf(groups.firstOrNull()?.id ?: "") }
    var isImporting by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var statusText by remember { mutableStateOf("Siap memproses akun") }
    var verifiedAccounts by remember { mutableStateOf(listOf<EmailAccount>()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Bulk Account Import (CSV)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Format tiap baris: email,password (atau email,password,imap_host,imap_port,smtp_host,smtp_port)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = csvText,
                    onValueChange = { csvText = it },
                    label = { Text("Tempel Data CSV Akun di Sini") },
                    placeholder = {
                        Text("user1@gmail.com,sandi_aplikasi_1\nuser2@outlook.com,sandi_2\nuser3@domain.com,sandi_3,mail.domain.com,993,mail.domain.com,465")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        csvText = "agent1@perusahaan.com,sandi_agent1\nagent2@perusahaan.com,sandi_agent2\ncs@toko.com,sandi_cs"
                    }) {
                        Text("Contoh Template", style = MaterialTheme.typography.labelSmall)
                    }

                    val detectedCount = csvText.lines().count { it.contains("@") }
                    Text("$detectedCount akun terdeteksi", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }

                if (isImporting) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(statusText, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss, enabled = !isImporting) { Text("Batal") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isImporting = true
                                statusText = "Membaca data akun..."
                                progress = 0.3f
                                delay(300)

                                val parsed = EmailService.parseCsvAccounts(csvText, selectedGroupId)
                                progress = 0.7f
                                statusText = "${parsed.size} akun berhasil diparsing..."
                                delay(300)

                                progress = 1.0f
                                isImporting = false
                                onImportComplete(parsed)
                            }
                        },
                        enabled = csvText.isNotBlank() && !isImporting
                    ) {
                        Text(if (isImporting) "Memproses..." else "Impor Akun")
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
    var username by remember { mutableStateOf(account.proxyConfig?.username ?: "") }
    var password by remember { mutableStateOf(account.proxyConfig?.password ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Konfigurasi Proxy Akun", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Setiap akun email dapat merutekan koneksi IMAP/SMTP melalui IP Proxy sendiri",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Gunakan Proxy untuk Akun ini", fontWeight = FontWeight.Medium)
                    Switch(
                        checked = isProxyEnabled,
                        onCheckedChange = { isProxyEnabled = it }
                    )
                }

                if (isProxyEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Tipe Proxy", style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProxyType.values().forEach { type ->
                            FilterChip(
                                selected = proxyType == type,
                                onClick = { proxyType = type },
                                label = { Text(type.name) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = host,
                            onValueChange = { host = it },
                            label = { Text("Host / IP Proxy") },
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
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username Proxy (Opsional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password Proxy (Opsional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Batal") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val config = if (isProxyEnabled) {
                                ProxyConfig(
                                    type = proxyType,
                                    host = host,
                                    port = port.toIntOrNull() ?: 1080,
                                    username = username,
                                    password = password,
                                    enabled = true
                                )
                            } else null
                            onSaveProxy(config)
                        }
                    ) {
                        Text("Simpan Proxy")
                    }
                }
            }
        }
    }
}
