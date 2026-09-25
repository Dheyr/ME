package com.omnimail.app.ui.screens.accounts

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.*
import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.omnimail.app.model.*

enum class WebProvider(val title: String, val loginUrl: String, val successKeywords: List<String>, val defaultColor: Long) {
    GMAIL(
        title = "Gmail (Google)",
        loginUrl = "https://accounts.google.com/ServiceLogin?service=mail",
        successKeywords = listOf("mail.google.com", "myaccount.google.com", "inbox"),
        defaultColor = 0xFFEA4335
    ),
    OUTLOOK(
        title = "Outlook / Microsoft",
        loginUrl = "https://login.live.com",
        successKeywords = listOf("outlook.live.com", "outlook.office.com", "mail"),
        defaultColor = 0xFF0078D4
    ),
    YAHOO(
        title = "Yahoo Mail",
        loginUrl = "https://login.yahoo.com",
        successKeywords = listOf("mail.yahoo.com"),
        defaultColor = 0xFF6001D2
    ),
    CUSTOM(
        title = "Webmail Custom / cPanel",
        loginUrl = "",
        successKeywords = listOf("webmail", "roundcube", "horde", "mailbox"),
        defaultColor = 0xFF00897B
    )
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebLoginDialog(
    groups: List<WorkspaceGroup>,
    onDismiss: () -> Unit,
    onAccountAttached: (EmailAccount, List<EmailMessage>) -> Unit
) {
    var selectedProvider by remember { mutableStateOf(WebProvider.GMAIL) }
    var currentUrl by remember { mutableStateOf(WebProvider.GMAIL.loginUrl) }
    var customUrlInput by remember { mutableStateOf("https://webmail.") }

    var emailInput by remember { mutableStateOf("") }
    var selectedGroupId by remember { mutableStateOf(groups.firstOrNull()?.id ?: "") }
    var isLoading by remember { mutableStateOf(false) }
    var isLoginDetected by remember { mutableStateOf(false) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Login Web & Tempelkan Native", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Masuk via web, akun otomatis tersimpan di aplikasi", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup")
                        }
                    },
                    actions = {
                        IconButton(onClick = { webViewInstance?.reload() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Muat Ulang")
                        }
                    }
                )
            },
            bottomBar = {
                Surface(
                    tonalElevation = 6.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        AnimatedVisibility(visible = isLoginDetected) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Login web berhasil terdeteksi! Masukkan alamat email untuk menempelkannya ke aplikasi.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF1B5E20),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = { emailInput = it },
                                label = { Text("Konfirmasi Alamat Email") },
                                placeholder = { Text("contoh@${if (selectedProvider == WebProvider.GMAIL) "gmail.com" else "domain.com"}") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )

                            Button(
                                onClick = {
                                    val finalEmail = emailInput.trim()
                                    if (finalEmail.isNotBlank()) {
                                        val cookies = CookieManager.getInstance().getCookie(currentUrl) ?: ""
                                        val newAccount = EmailAccount(
                                            id = "web_acc_${System.currentTimeMillis()}",
                                            email = finalEmail,
                                            displayName = finalEmail.substringBefore("@"),
                                            authType = AuthType.WEB_SESSION,
                                            workspaceGroupId = selectedGroupId,
                                            colorHex = selectedProvider.defaultColor,
                                            status = AccountStatus.ONLINE,
                                            unreadCount = 1,
                                            webLoginUrl = currentUrl,
                                            webCookies = cookies
                                        )

                                        val welcomeEmail = EmailMessage(
                                            id = "msg_${System.currentTimeMillis()}",
                                            accountId = newAccount.id,
                                            accountEmail = newAccount.email,
                                            accountColorHex = newAccount.colorHex,
                                            senderName = "${selectedProvider.title}",
                                            senderEmail = "no-reply@${finalEmail.substringAfter("@")}",
                                            recipients = listOf(finalEmail),
                                            subject = "Akun ${newAccount.email} berhasil terhubung",
                                            snippet = "Akun email Anda telah berhasil dihubungkan ke OmniMail secara native.",
                                            bodyText = "Selamat! Akun email ${newAccount.email} berhasil dihubungkan secara native ke OmniMail melalui sesi web browser. Sesi Anda aktif dan siap digunakan.",
                                            timestamp = System.currentTimeMillis(),
                                            formattedTime = "Baru saja",
                                            isRead = false,
                                            folder = EmailFolder.INBOX
                                        )

                                        onAccountAttached(newAccount, listOf(welcomeEmail))
                                    }
                                },
                                enabled = emailInput.isNotBlank(),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(56.dp)
                            ) {
                                Icon(Icons.Default.DownloadDone, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Tempelkan Akun")
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Provider Selector Chips
                ScrollableTabRow(
                    selectedTabIndex = selectedProvider.ordinal,
                    edgePadding = 12.dp,
                    divider = {}
                ) {
                    WebProvider.values().forEach { provider ->
                        Tab(
                            selected = selectedProvider == provider,
                            onClick = {
                                selectedProvider = provider
                                isLoginDetected = false
                                if (provider != WebProvider.CUSTOM) {
                                    currentUrl = provider.loginUrl
                                    webViewInstance?.loadUrl(provider.loginUrl)
                                }
                            },
                            text = { Text(provider.title) }
                        )
                    }
                }

                // Custom URL bar if custom webmail
                if (selectedProvider == WebProvider.CUSTOM) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customUrlInput,
                            onValueChange = { customUrlInput = it },
                            placeholder = { Text("https://webmail.domainanda.com") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                var target = customUrlInput.trim()
                                if (!target.startsWith("http://") && !target.startsWith("https://")) {
                                    target = "https://$target"
                                }
                                currentUrl = target
                                webViewInstance?.loadUrl(target)
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Buka")
                        }
                    }
                }

                if (isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                // In-App WebView
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            CookieManager.getInstance().setAcceptCookie(true)
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                setSupportZoom(true)
                                builtInZoomControls = true
                                displayZoomControls = false
                                userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    isLoading = true
                                    url?.let {
                                        currentUrl = it
                                        checkIfLoggedIn(it, selectedProvider)
                                    }
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    isLoading = false
                                    url?.let {
                                        currentUrl = it
                                        checkIfLoggedIn(it, selectedProvider)
                                    }
                                }

                                private fun checkIfLoggedIn(url: String, provider: WebProvider) {
                                    val lower = url.lowercase()
                                    val matches = provider.successKeywords.any { lower.contains(it) }
                                    if (matches) {
                                        isLoginDetected = true
                                    }
                                }
                            }

                            loadUrl(currentUrl)
                            webViewInstance = this
                        }
                    },
                    update = { webView ->
                        webViewInstance = webView
                    }
                )
            }
        }
    }
}
