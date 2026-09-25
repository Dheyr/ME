package com.omnimail.app.ui.screens.accounts

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.omnimail.app.model.*
import com.omnimail.app.service.EmailService
import com.omnimail.app.ui.components.StatusIndicator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountManagementScreen(
    accounts: List<EmailAccount>,
    onAddSingleAccount: (EmailAccount, List<EmailMessage>) -> Unit,
    onUpdateAccountProxy: (accountId: String, ProxyConfig?) -> Unit,
    onDeleteAccount: (accountId: String) -> Unit = {},
    onUpdateAccountPasswords: (accountId: String, appPassword: String, originalPassword: String) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    var showMethodPicker by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showWebLoginDialog by remember { mutableStateOf(false) }
    var selectedAccountForProxy by remember { mutableStateOf<EmailAccount?>(null) }
    var selectedAccountForPasswords by remember { mutableStateOf<EmailAccount?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Kelola Akun Email (${accounts.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Sandi asli & sandi aplikasi digabung dalam satu akun", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = { showMethodPicker = true }) {
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
                        text = "Daftar Akun Terhubung (${accounts.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { showMethodPicker = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tambah Akun", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            if (accounts.isEmpty()) {
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
                            Text("Belum Ada Akun Terhubung", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "Tambahkan akun email Anda (Gmail, Google Workspace kampus, Outlook, Yahoo, atau Webmail). Bisa login banyak akun sekaligus!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(onClick = { showMethodPicker = true }, shape = RoundedCornerShape(8.dp)) {
                                Text("Tambah Akun Email Pertama")
                            }
                        }
                    }
                }
            } else {
                items(accounts, key = { it.id }) { account ->
                    AccountCard(
                        account = account,
                        onConfigureProxy = { selectedAccountForProxy = account },
                        onEditPasswords = { selectedAccountForPasswords = account },
                        onDeleteAccount = { onDeleteAccount(account.id) }
                    )
                }
            }
        }
    }

    // Method Selection Dialog (Web Login vs Manual IMAP)
    if (showMethodPicker) {
        MethodPickerDialog(
            onDismiss = { showMethodPicker = false },
            onSelectWebLogin = {
                showMethodPicker = false
                showWebLoginDialog = true
            },
            onSelectManualImap = {
                showMethodPicker = false
                showAddDialog = true
            }
        )
    }

    // Web Login Dialog (Full Screen in-app web login)
    if (showWebLoginDialog) {
        WebLoginDialog(
            onDismiss = { showWebLoginDialog = false },
            onAccountAttached = { newAcc, fetchedEmails ->
                onAddSingleAccount(newAcc, fetchedEmails)
                showWebLoginDialog = false
            }
        )
    }

    // Add Single Account Dialog (Manual IMAP/SMTP with unified passwords)
    if (showAddDialog) {
        AddSingleAccountDialog(
            onDismiss = { showAddDialog = false },
            onSwitchToWebLogin = {
                showAddDialog = false
                showWebLoginDialog = true
            },
            onAddAccount = { acc, fetchedEmails ->
                onAddSingleAccount(acc, fetchedEmails)
                showAddDialog = false
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

    // Edit Passwords Dialog (Unified Sandi Asli & Sandi Aplikasi)
    selectedAccountForPasswords?.let { account ->
        EditAccountPasswordsDialog(
            account = account,
            onDismiss = { selectedAccountForPasswords = null },
            onSave = { appPass, origPass ->
                onUpdateAccountPasswords(account.id, appPass, origPass)
                selectedAccountForPasswords = null
            }
        )
    }
}

@Composable
fun AccountCard(
    account: EmailAccount,
    onConfigureProxy: () -> Unit,
    onEditPasswords: () -> Unit,
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

            // Protocol & Credentials Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (account.password.isNotBlank()) {
                        AssistChip(
                            onClick = onEditPasswords,
                            leadingIcon = {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF2E7D32))
                            },
                            label = {
                                Text(
                                    "IMAP Native Aktif",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        )
                    } else {
                        AssistChip(
                            onClick = onEditPasswords,
                            leadingIcon = {
                                Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFB45309))
                            },
                            label = {
                                Text(
                                    "+ Sandi Aplikasi IMAP",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFB45309),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        )
                    }

                    AssistChip(
                        onClick = onEditPasswords,
                        leadingIcon = {
                            Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        },
                        label = {
                            Text(
                                "Webmail",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    )

                    if (account.unreadCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                "${account.unreadCount} baru",
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
fun EditAccountPasswordsDialog(
    account: EmailAccount,
    onDismiss: () -> Unit,
    onSave: (appPassword: String, originalPassword: String) -> Unit
) {
    val context = LocalContext.current
    var appPassword by remember { mutableStateOf(account.password) }
    var originalPassword by remember { mutableStateOf(account.originalPassword) }
    val isGoogle = remember(account.email, account.imapHost) {
        account.email.contains("gmail") || account.email.contains("google") ||
        account.email.endsWith(".ac.id") || account.email.endsWith(".edu") ||
        account.imapHost.contains("gmail")
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Kelola Sandi Akun", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    account.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    "Sandi Asli (Webmail) dan Sandi Aplikasi (IMAP Native) digabung dalam akun ini.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = appPassword,
                    onValueChange = { appPassword = it },
                    label = { Text(if (isGoogle) "Sandi Aplikasi IMAP (16 Karakter)" else "Kata Sandi Email (IMAP)") },
                    placeholder = { Text("Untuk sync inbox & kirim email native") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isGoogle) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://myaccount.google.com/apppasswords"))
                                context.startActivity(intent)
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Buka Pembuat Sandi Aplikasi Google", fontSize = 11.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = originalPassword,
                    onValueChange = { originalPassword = it },
                    label = { Text("Sandi Asli (Webmail & 2FA)") },
                    placeholder = { Text("Opsional") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Batal") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onSave(appPassword.trim(), originalPassword.trim()) }) {
                        Text("Simpan Sandi")
                    }
                }
            }
        }
    }
}

@Composable
fun AddSingleAccountDialog(
    onDismiss: () -> Unit,
    onSwitchToWebLogin: (() -> Unit)? = null,
    onAddAccount: (EmailAccount, List<EmailMessage>) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var originalPasswordInput by remember { mutableStateOf("") }

    var imapHost by remember { mutableStateOf("") }
    var imapPort by remember { mutableStateOf("993") }
    var smtpHost by remember { mutableStateOf("") }
    var smtpPort by remember { mutableStateOf("465") }
    var useSsl by remember { mutableStateOf(true) }

    var showAdvancedServer by remember { mutableStateOf(false) }
    var isLoggingIn by remember { mutableStateOf(false) }
    var loginErrorMessage by remember { mutableStateOf<String?>(null) }

    // Auto-detect server settings based on domain
    LaunchedEffect(emailInput) {
        val serverConfig = EmailService.autoDetectServer(emailInput)
        imapHost = serverConfig.imapHost
        imapPort = serverConfig.imapPort.toString()
        smtpHost = serverConfig.smtpHost
        smtpPort = serverConfig.smtpPort.toString()
        useSsl = serverConfig.useSsl
        loginErrorMessage = null

        // Asynchronously check MX if custom domain
        if (emailInput.contains("@") && emailInput.substringAfter("@").isNotBlank()) {
            val resolved = EmailService.resolveMxServerConfig(emailInput)
            imapHost = resolved.imapHost
            imapPort = resolved.imapPort.toString()
            smtpHost = resolved.smtpHost
            smtpPort = resolved.smtpPort.toString()
            useSsl = resolved.useSsl
        }
    }

    val isGoogleDomain = remember(emailInput, imapHost) {
        emailInput.contains("gmail", ignoreCase = true) ||
        emailInput.contains("google", ignoreCase = true) ||
        emailInput.contains("uniba", ignoreCase = true) ||
        emailInput.endsWith(".ac.id", ignoreCase = true) ||
        imapHost.contains("gmail", ignoreCase = true)
    }

    Dialog(onDismissRequest = { if (!isLoggingIn) onDismiss() }) {
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
                Text("Tambah Akun Email", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Sandi Asli & Sandi Aplikasi kini digabung dalam satu akun",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (onSwitchToWebLogin != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onSwitchToWebLogin,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Atau Login Cepat via Web Browser", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

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
                            Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(18.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (isGoogleDomain)
                                "Akun Google: Masukkan Sandi Aplikasi (16 karakter) agar inbox native bisa memperbarui email. Masukkan juga Sandi Asli jika ingin membuka webmail."
                            else
                                "Masukkan email dan kata sandi akun Anda. Kotak masuk native dan webmail akan terhubung secara otomatis.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it; loginErrorMessage = null },
                    label = { Text("Alamat Email Lengkap") },
                    placeholder = { Text("contoh@domain.com") },
                    singleLine = true,
                    enabled = !isLoggingIn,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it; loginErrorMessage = null },
                    label = { Text(if (isGoogleDomain) "Sandi Aplikasi IMAP (16 Karakter)" else "Kata Sandi Email") },
                    placeholder = { Text(if (isGoogleDomain) "16 karakter dari Google" else "Kata sandi email") },
                    singleLine = true,
                    enabled = !isLoggingIn,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = originalPasswordInput,
                    onValueChange = { originalPasswordInput = it },
                    label = { Text("Sandi Asli (Webmail & 2FA - Opsional)") },
                    placeholder = { Text("Untuk login webmail") },
                    singleLine = true,
                    enabled = !isLoggingIn,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                if (isGoogleDomain) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://myaccount.google.com/apppasswords"))
                                context.startActivity(intent)
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Buka Pembuat Sandi Aplikasi Google", fontSize = 11.sp)
                        }
                    }
                }

                // Toggle advanced settings
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = { showAdvancedServer = !showAdvancedServer }) {
                        Text(if (showAdvancedServer) "Sembunyikan Pengaturan Server" else "Pengaturan Server Lanjutan (Host/Port)")
                    }
                }

                // Error feedback if login failed
                if (loginErrorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "Gagal Terhubung: $loginErrorMessage",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (onSwitchToWebLogin != null) {
                                Button(
                                    onClick = onSwitchToWebLogin,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Beralih: Login via Web (Sandi Asli & 2FA)", fontSize = 12.sp)
                                }
                            }
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
                                enabled = !isLoggingIn,
                                modifier = Modifier.weight(2f)
                            )
                            OutlinedTextField(
                                value = imapPort,
                                onValueChange = { imapPort = it },
                                label = { Text("Port") },
                                singleLine = true,
                                enabled = !isLoggingIn,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = smtpHost,
                                onValueChange = { smtpHost = it },
                                label = { Text("SMTP Host") },
                                singleLine = true,
                                enabled = !isLoggingIn,
                                modifier = Modifier.weight(2f)
                            )
                            OutlinedTextField(
                                value = smtpPort,
                                onValueChange = { smtpPort = it },
                                label = { Text("Port") },
                                singleLine = true,
                                enabled = !isLoggingIn,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss, enabled = !isLoggingIn) { Text("Batal") }
                    Spacer(modifier = Modifier.width(8.dp))

                    if (loginErrorMessage != null) {
                        OutlinedButton(
                            onClick = {
                                val targetMailUrl = if (emailInput.contains("gmail") || emailInput.contains("google") || emailInput.endsWith(".ac.id") || emailInput.endsWith(".edu")) {
                                    "https://mail.google.com/mail/u/?authuser=${emailInput.trim()}"
                                } else "https://mail.google.com/mail/u/0/"

                                val acc = EmailAccount(
                                    id = "acc_${System.currentTimeMillis()}",
                                    email = emailInput.trim(),
                                    displayName = emailInput.substringBefore("@"),
                                    authType = AuthType.IMAP_SMTP_MANUAL,
                                    colorHex = 0xFF3B82F6,
                                    password = passwordInput.trim(),
                                    originalPassword = originalPasswordInput.trim(),
                                    imapHost = imapHost.ifBlank { "imap.${emailInput.substringAfter("@")}" },
                                    imapPort = imapPort.toIntOrNull() ?: 993,
                                    smtpHost = smtpHost.ifBlank { "smtp.${emailInput.substringAfter("@")}" },
                                    smtpPort = smtpPort.toIntOrNull() ?: 465,
                                    useSsl = useSsl,
                                    webLoginUrl = targetMailUrl
                                )
                                onAddAccount(acc, emptyList())
                            },
                            enabled = !isLoggingIn
                        ) {
                            Text("Tetap Simpan")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Button(
                        onClick = {
                            if (emailInput.isNotBlank() && passwordInput.isNotBlank()) {
                                coroutineScope.launch {
                                    isLoggingIn = true
                                    loginErrorMessage = null
                                    val targetMailUrl = if (emailInput.contains("gmail") || emailInput.contains("google") || emailInput.endsWith(".ac.id") || emailInput.endsWith(".edu")) {
                                        "https://mail.google.com/mail/u/?authuser=${emailInput.trim()}"
                                    } else "https://mail.google.com/mail/u/0/"

                                    val acc = EmailAccount(
                                        id = "acc_${System.currentTimeMillis()}",
                                        email = emailInput.trim(),
                                        displayName = emailInput.substringBefore("@"),
                                        authType = AuthType.IMAP_SMTP_MANUAL,
                                        colorHex = 0xFF3B82F6,
                                        password = passwordInput.trim(),
                                        originalPassword = originalPasswordInput.trim(),
                                        imapHost = imapHost.ifBlank { "imap.${emailInput.substringAfter("@")}" },
                                        imapPort = imapPort.toIntOrNull() ?: 993,
                                        smtpHost = smtpHost.ifBlank { "smtp.${emailInput.substringAfter("@")}" },
                                        smtpPort = smtpPort.toIntOrNull() ?: 465,
                                        useSsl = useSsl,
                                        webLoginUrl = targetMailUrl
                                    )
                                    val result = EmailService.loginAndFetchInbox(acc, limit = 30)
                                    isLoggingIn = false
                                    if (result.isSuccess) {
                                        val (workingAcc, fetchedEmails) = result.getOrThrow()
                                        onAddAccount(workingAcc, fetchedEmails)
                                    } else {
                                        loginErrorMessage = result.exceptionOrNull()?.message ?: "Gagal terhubung ke server email"
                                    }
                                }
                            }
                        },
                        enabled = emailInput.isNotBlank() && passwordInput.isNotBlank() && !isLoggingIn
                    ) {
                        if (isLoggingIn) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Menghubungkan...")
                        } else {
                            Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Login & Baca Inbox")
                        }
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

@Composable
fun MethodPickerDialog(
    onDismiss: () -> Unit,
    onSelectWebLogin: () -> Unit,
    onSelectManualImap: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Pilih Cara Hubungkan Email", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Sandi Asli & Sandi Aplikasi tersimpan bersama dalam satu akun",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Option 1: Web Login (Multi-Account Supported)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectWebLogin() },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Language, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Login Web Browser (Banyak Akun)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        "REKOMENDASI",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "Masuk dengan Sandi Asli & 2FA langsung di situs web Gmail, Outlook, Yahoo, atau Webmail. Bisa tambah banyak akun Gmail sekaligus!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Option 2: Manual IMAP/SMTP
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectManualImap() },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Dns, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(22.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Login Manual IMAP / SMTP", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "Koneksi protokol IMAP native dengan Sandi Aplikasi & Sandi Asli tergabung.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}
