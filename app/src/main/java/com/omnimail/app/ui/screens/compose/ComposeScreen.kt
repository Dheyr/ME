package com.omnimail.app.ui.screens.compose

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.omnimail.app.model.EmailAccount
import com.omnimail.app.model.EmailAttachment
import com.omnimail.app.ui.components.AccountBadge
import com.omnimail.app.ui.components.AttachmentCard
import com.omnimail.app.ui.components.RichTextFormattingBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeEmailScreen(
    accounts: List<EmailAccount>,
    initialSenderId: String? = null,
    initialRecipient: String = "",
    initialSubject: String = "",
    onDiscard: () -> Unit,
    onSend: (senderAccountId: String, to: String, subject: String, body: String, attachments: List<EmailAttachment>) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedAccount by remember {
        mutableStateOf(accounts.find { it.id == initialSenderId } ?: accounts.firstOrNull() ?: EmailAccount(
            id = "default",
            email = "you@example.com",
            displayName = "Default",
            authType = com.omnimail.app.model.AuthType.IMAP_SMTP_MANUAL,
            workspaceGroupId = "",
            colorHex = 0xFF6366F1
        ))
    }

    var toText by remember { mutableStateOf(initialRecipient) }
    var ccText by remember { mutableStateOf("") }
    var bccText by remember { mutableStateOf("") }
    var showCcBcc by remember { mutableStateOf(false) }

    var subjectText by remember { mutableStateOf(initialSubject) }
    var bodyText by remember { mutableStateOf("\n\n--\n${selectedAccount.signature}") }

    // Rich Text State
    var isBold by remember { mutableStateOf(false) }
    var isItalic by remember { mutableStateOf(false) }
    var isUnderline by remember { mutableStateOf(false) }
    var isBullet by remember { mutableStateOf(false) }

    // Attachments
    var attachments by remember { mutableStateOf(listOf<EmailAttachment>()) }
    var showSenderDialog by remember { mutableStateOf(false) }

    // Check 25MB attachment limit
    val totalAttachmentSizeBytes = attachments.sumOf { it.fileSizeBytes }
    val isOverSizeLimit = totalAttachmentSizeBytes > 25 * 1024 * 1024

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Tulis Email Baru", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onDiscard) {
                        Icon(Icons.Default.Close, contentDescription = "Discard")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        // Mock add attachment
                        val newAtt = EmailAttachment(
                            id = "att_${System.currentTimeMillis()}",
                            fileName = "Document_Attachment_${attachments.size + 1}.pdf",
                            fileSizeBytes = 3_200_000,
                            mimeType = "application/pdf"
                        )
                        attachments = attachments + newAtt
                    }) {
                        Icon(Icons.Outlined.AttachFile, contentDescription = "Lampirkan File")
                    }

                    IconButton(
                        onClick = {
                            if (toText.isNotBlank()) {
                                onSend(selectedAccount.id, toText, subjectText, bodyText, attachments)
                            }
                        },
                        enabled = toText.isNotBlank() && !isOverSizeLimit
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Kirim",
                            tint = if (toText.isNotBlank() && !isOverSizeLimit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // FR 3.1 Multi-Sender Dropdown Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showSenderDialog = true }
                    .padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Dari:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    AccountBadge(
                        accountEmail = selectedAccount.email,
                        colorHex = selectedAccount.colorHex,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = "Ganti Akun Pengirim",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // To Field
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Kepada:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(60.dp)
                )
                TextField(
                    value = toText,
                    onValueChange = { toText = it },
                    placeholder = { Text("email@tujuan.com") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                IconButton(onClick = { showCcBcc = !showCcBcc }) {
                    Icon(
                        if (showCcBcc) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle CC/BCC",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Expandable CC / BCC
            AnimatedVisibility(visible = showCcBcc) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Cc:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(60.dp)
                        )
                        TextField(
                            value = ccText,
                            onValueChange = { ccText = it },
                            placeholder = { Text("cc@domain.com") },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Bcc:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(60.dp)
                        )
                        TextField(
                            value = bccText,
                            onValueChange = { bccText = it },
                            placeholder = { Text("bcc@domain.com") },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Subject Field
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Subjek:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(60.dp)
                )
                TextField(
                    value = subjectText,
                    onValueChange = { subjectText = it },
                    placeholder = { Text("Judul email...") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Attachments Preview & Size Warning (FR 3.3)
            if (attachments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                if (isOverSizeLimit) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Peringatan: Total lampiran melebihi batas 25MB!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(attachments) { attachment ->
                        AttachmentCard(
                            attachment = attachment,
                            onRemove = {
                                attachments = attachments - attachment
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // FR 3.2 Rich Text Formatting Toolbar
            RichTextFormattingBar(
                isBold = isBold,
                isItalic = isItalic,
                isUnderline = isUnderline,
                isBullet = isBullet,
                onToggleBold = { isBold = !isBold },
                onToggleItalic = { isItalic = !isItalic },
                onToggleUnderline = { isUnderline = !isUnderline },
                onToggleBullet = { isBullet = !isBullet },
                onInsertLink = { },
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Body TextField with auto signature
            TextField(
                value = bodyText,
                onValueChange = { bodyText = it },
                placeholder = { Text("Ketik pesan email Anda di sini...") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }

    // Modal Searchable Multi-Sender Picker (FR 3.1)
    if (showSenderDialog) {
        SenderSelectionModal(
            accounts = accounts,
            selectedAccount = selectedAccount,
            onSelectAccount = { account ->
                selectedAccount = account
                // FR 3.4 Auto-apply signature for chosen sender
                if (!bodyText.contains(account.signature)) {
                    bodyText = bodyText.substringBefore("\n\n--\n") + "\n\n--\n${account.signature}"
                }
                showSenderDialog = false
            },
            onDismiss = { showSenderDialog = false }
        )
    }
}

@Composable
private fun SenderSelectionModal(
    accounts: List<EmailAccount>,
    selectedAccount: EmailAccount,
    onSelectAccount: (EmailAccount) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = accounts.filter {
        it.email.contains(searchQuery, ignoreCase = true) ||
        it.displayName.contains(searchQuery, ignoreCase = true)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Pilih Akun Pengirim",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${accounts.size} akun terdaftar",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar in Modal
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari akun atau nama...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filtered) { account ->
                        val isCurrent = account.id == selectedAccount.id
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectAccount(account) }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(account.colorHex))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = account.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = account.email,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isCurrent) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }
                }
            }
        }
    }
}
