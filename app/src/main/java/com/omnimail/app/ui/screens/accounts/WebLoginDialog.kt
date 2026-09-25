package com.omnimail.app.ui.screens.accounts

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.*
import androidx.compose.animation.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.omnimail.app.model.*
import com.omnimail.app.service.EmailService
import com.omnimail.app.service.WebMailExtractor
import kotlinx.coroutines.launch

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
    onDismiss: () -> Unit,
    onAccountAttached: (EmailAccount, List<EmailMessage>) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedProvider by remember { mutableStateOf(WebProvider.GMAIL) }
    var currentUrl by remember { mutableStateOf(WebProvider.GMAIL.loginUrl) }
    var customUrlInput by remember { mutableStateOf("https://webmail.") }

    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var isAttaching by remember { mutableStateOf(false) }
    var isLoginDetected by remember { mutableStateOf(false) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var attachMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = { if (!isAttaching) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Login Web & Tempelkan Akun", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Bisa login banyak akun Gmail sekaligus", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss, enabled = !isAttaching) {
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
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AnimatedVisibility(visible = isLoginDetected) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Login web berhasil terdeteksi! Masukkan email Anda di bawah dan simpan akun.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF1B5E20),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        if (attachMessage != null) {
                            Text(
                                text = attachMessage ?: "",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Alamat Email
                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text("Alamat Email Lengkap") },
                            placeholder = { Text("contoh@gmail.com") },
                            singleLine = true,
                            enabled = !isAttaching,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Kata Sandi Email (Password Asli)
                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text("Kata Sandi Email (Password Asli)") },
                            placeholder = { Text("Opsional - untuk sinkronisasi otomatis") },
                            singleLine = true,
                            enabled = !isAttaching,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                val finalEmail = emailInput.trim()
                                val finalPassword = passwordInput.trim()

                                if (finalEmail.isNotBlank()) {
                                    coroutineScope.launch {
                                        isAttaching = true
                                        attachMessage = "Menyimpan akun & mengambil email kotak masuk..."
                                        val cookies = CookieManager.getInstance().getCookie(currentUrl) ?: ""
                                        val serverConfig = EmailService.autoDetectServer(finalEmail)
                                        
                                        // Target exact Gmail authuser URL for multi-account isolation
                                        val targetMailUrl = if (finalEmail.contains("gmail") || finalEmail.contains("google") || finalEmail.endsWith(".ac.id") || finalEmail.endsWith(".edu")) {
                                            "https://mail.google.com/mail/u/?authuser=$finalEmail"
                                        } else if (currentUrl.contains("mail.google.com")) {
                                            "https://mail.google.com/mail/u/?authuser=$finalEmail"
                                        } else {
                                            currentUrl
                                        }

                                        val newAccount = EmailAccount(
                                            id = "acc_${System.currentTimeMillis()}",
                                            email = finalEmail,
                                            displayName = finalEmail.substringBefore("@"),
                                            authType = AuthType.WEB_SESSION,
                                            colorHex = selectedProvider.defaultColor,
                                            status = AccountStatus.ONLINE,
                                            password = finalPassword,
                                            originalPassword = finalPassword,
                                            imapHost = serverConfig.imapHost,
                                            imapPort = serverConfig.imapPort,
                                            smtpHost = serverConfig.smtpHost,
                                            smtpPort = serverConfig.smtpPort,
                                            useSsl = serverConfig.useSsl,
                                            webLoginUrl = targetMailUrl,
                                            webCookies = cookies
                                        )

                                        // Extract emails from active WebView DOM and authenticated web session
                                        var fetchedEmails = emptyList<EmailMessage>()
                                        if (webViewInstance != null) {
                                            val domEmails = WebMailExtractor.extractFromWebView(webViewInstance!!, newAccount)
                                            if (domEmails.isNotEmpty()) {
                                                fetchedEmails = domEmails
                                            }
                                        }

                                        if (fetchedEmails.isEmpty()) {
                                            val feedEmails = WebMailExtractor.fetchSessionFeed(newAccount)
                                            if (feedEmails.isNotEmpty()) {
                                                fetchedEmails = feedEmails
                                            }
                                        }

                                        if (fetchedEmails.isEmpty() && finalPassword.isNotBlank()) {
                                            val fetchRes = EmailService.loginAndFetchInbox(newAccount, limit = 30)
                                            if (fetchRes.isSuccess) {
                                                fetchedEmails = fetchRes.getOrDefault(Pair(newAccount, emptyList())).second
                                            }
                                        }

                                        isAttaching = false
                                        onAccountAttached(newAccount, fetchedEmails)
                                    }
                                }
                            },
                            enabled = emailInput.isNotBlank() && !isAttaching,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            if (isAttaching) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Menyimpan & Menghubungkan...")
                            } else {
                                Icon(Icons.Default.DownloadDone, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Tempelkan Akun & Buka Kotak Masuk")
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

                // Multi-Account Google Controls (Tambah Akun Lain / Pilih Akun / Logout Sesi)
                if (selectedProvider == WebProvider.GMAIL) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SuggestionChip(
                            onClick = {
                                val addSessionUrl = "https://accounts.google.com/AddSession?service=mail&continue=https://mail.google.com/mail/"
                                currentUrl = addSessionUrl
                                webViewInstance?.loadUrl(addSessionUrl)
                            },
                            icon = { Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary) },
                            label = { Text("➕ Tambah Akun Google Lain", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                        )

                        SuggestionChip(
                            onClick = {
                                val chooserUrl = "https://accounts.google.com/AccountChooser?service=mail&continue=https://mail.google.com/mail/"
                                currentUrl = chooserUrl
                                webViewInstance?.loadUrl(chooserUrl)
                            },
                            icon = { Icon(Icons.Default.SwitchAccount, contentDescription = null, modifier = Modifier.size(15.dp)) },
                            label = { Text("Pilih / Ganti Akun", fontSize = 11.sp) }
                        )

                        SuggestionChip(
                            onClick = {
                                CookieManager.getInstance().removeAllCookies(null)
                                CookieManager.getInstance().flush()
                                val cleanLoginUrl = "https://accounts.google.com/ServiceLogin?service=mail"
                                currentUrl = cleanLoginUrl
                                webViewInstance?.loadUrl(cleanLoginUrl)
                            },
                            icon = { Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(15.dp)) },
                            label = { Text("Sesi Bersih (Logout)", fontSize = 11.sp) }
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
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp))
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
                                        checkIfLoggedIn(it, selectedProvider, view)
                                    }
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    isLoading = false
                                    url?.let {
                                        currentUrl = it
                                        checkIfLoggedIn(it, selectedProvider, view)
                                    }
                                }

                                private fun checkIfLoggedIn(url: String, provider: WebProvider, webView: WebView?) {
                                    val lower = url.lowercase()
                                    val matches = provider.successKeywords.any { lower.contains(it) }
                                    if (matches) {
                                        isLoginDetected = true
                                    }

                                    // Auto-detect email from URL parameters
                                    try {
                                        val uri = Uri.parse(url)
                                        val authUser = uri.getQueryParameter("authuser")
                                        if (!authUser.isNullOrBlank() && authUser.contains("@") && emailInput.isBlank()) {
                                            emailInput = authUser.trim()
                                        }
                                        val emailParam = uri.getQueryParameter("Email") ?: uri.getQueryParameter("email")
                                        if (!emailParam.isNullOrBlank() && emailParam.contains("@") && emailInput.isBlank()) {
                                            emailInput = emailParam.trim()
                                        }
                                    } catch (_: Exception) {}

                                    // Auto-detect email from webpage DOM if empty
                                    if (emailInput.isBlank() && webView != null) {
                                        webView.evaluateJavascript("""
                                            (function() {
                                                try {
                                                    var el = document.querySelector('[data-identifier]') || document.querySelector('div[data-email]') || document.querySelector('a[aria-label*="@"]');
                                                    if (el) {
                                                        return el.getAttribute('data-identifier') || el.getAttribute('data-email') || el.getAttribute('aria-label') || '';
                                                    }
                                                    var titleMatch = document.title.match(/([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,})/);
                                                    return titleMatch ? titleMatch[0] : '';
                                                } catch(e) { return ''; }
                                            })()
                                        """.trimIndent()) { jsRes ->
                                            val clean = jsRes?.replace("\"", "")?.trim().orEmpty()
                                            val emailRegex = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
                                            val match = emailRegex.find(clean)?.value
                                            if (!match.isNullOrBlank() && emailInput.isBlank()) {
                                                emailInput = match
                                            }
                                        }
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
