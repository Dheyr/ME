package com.omnimail.app.service

import android.text.Html
import com.omnimail.app.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

data class ServerConfig(
    val imapHost: String,
    val imapPort: Int,
    val smtpHost: String,
    val smtpPort: Int,
    val useSsl: Boolean = true
)

object EmailService {

    fun autoDetectServer(email: String): ServerConfig {
        val domain = email.substringAfter("@", "").lowercase().trim()
        return when {
            domain.contains("gmail") || domain.contains("google") -> ServerConfig(
                imapHost = "imap.gmail.com",
                imapPort = 993,
                smtpHost = "smtp.gmail.com",
                smtpPort = 465,
                useSsl = true
            )
            domain.contains("outlook") || domain.contains("hotmail") || domain.contains("live.com") || domain.contains("office365") -> ServerConfig(
                imapHost = "outlook.office365.com",
                imapPort = 993,
                smtpHost = "smtp.office365.com",
                smtpPort = 587,
                useSsl = false // uses STARTTLS
            )
            domain.contains("yahoo") -> ServerConfig(
                imapHost = "imap.mail.yahoo.com",
                imapPort = 993,
                smtpHost = "smtp.mail.yahoo.com",
                smtpPort = 465,
                useSsl = true
            )
            domain.contains("zoho") -> ServerConfig(
                imapHost = "imap.zoho.com",
                imapPort = 993,
                smtpHost = "smtp.zoho.com",
                smtpPort = 465,
                useSsl = true
            )
            domain.contains("icloud") || domain.contains("me.com") -> ServerConfig(
                imapHost = "imap.mail.me.com",
                imapPort = 993,
                smtpHost = "smtp.mail.me.com",
                smtpPort = 587,
                useSsl = false
            )
            domain.isNotBlank() -> ServerConfig(
                imapHost = "mail.$domain",
                imapPort = 993,
                smtpHost = "mail.$domain",
                smtpPort = 465,
                useSsl = true
            )
            else -> ServerConfig(
                imapHost = "imap.gmail.com",
                imapPort = 993,
                smtpHost = "smtp.gmail.com",
                smtpPort = 465,
                useSsl = true
            )
        }
    }

    suspend fun resolveMxServerConfig(email: String): ServerConfig = withContext(Dispatchers.IO) {
        val domain = email.substringAfter("@", "").lowercase().trim()
        if (domain.isBlank()) return@withContext autoDetectServer(email)
        
        try {
            val url = java.net.URL("https://dns.google/resolve?name=$domain&type=MX")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 2500
            conn.readTimeout = 2500
            conn.requestMethod = "GET"
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                if (body.contains("google.com") || body.contains("googlemail.com") || body.contains("aspmx")) {
                    return@withContext ServerConfig("imap.gmail.com", 993, "smtp.gmail.com", 465, true)
                }
                if (body.contains("outlook.com") || body.contains("office365.com") || body.contains("pphosted")) {
                    return@withContext ServerConfig("outlook.office365.com", 993, "smtp.office365.com", 587, false)
                }
            }
        } catch (_: Exception) {}
        
        autoDetectServer(email)
    }

    private fun getImapStore(account: EmailAccount, port: Int, useSsl: Boolean): Pair<Session, Store> {
        val protocol = if (useSsl) "imaps" else "imap"
        val props = Properties().apply {
            put("mail.store.protocol", protocol)
            put("mail.$protocol.host", account.imapHost)
            put("mail.$protocol.port", port.toString())
            put("mail.$protocol.timeout", "25000")
            put("mail.$protocol.connectiontimeout", "15000")

            // Enable common auth methods for real passwords
            put("mail.$protocol.auth.plain.disable", "false")
            put("mail.$protocol.auth.login.disable", "false")

            // Trust all certificates (cPanel, self-signed, VPS, etc.)
            put("mail.$protocol.ssl.trust", "*")

            if (useSsl) {
                put("mail.$protocol.ssl.enable", "true")
                put("mail.$protocol.ssl.protocols", "TLSv1.2 TLSv1.3")
                put("mail.$protocol.ssl.checkserveridentity", "false")
            } else {
                put("mail.$protocol.starttls.enable", "true")
                put("mail.$protocol.starttls.required", "false")
            }

            // SOCKS5 Proxy support
            account.proxyConfig?.let { proxy ->
                if (proxy.enabled && proxy.host.isNotBlank()) {
                    put("mail.$protocol.socks.host", proxy.host)
                    put("mail.$protocol.socks.port", proxy.port.toString())
                }
            }
        }

        val session = Session.getInstance(props)
        val store = session.getStore(protocol)
        return Pair(session, store)
    }

    private fun connectToStore(account: EmailAccount): Pair<Store, EmailAccount> {
        val domain = account.email.substringAfter("@", "").lowercase().trim()
        val isGmail = domain == "gmail.com" || domain == "googlemail.com" || account.imapHost == "imap.gmail.com"
        val isWellKnownSsl = isGmail || domain.contains("yahoo") || account.imapHost.contains("yahoo") ||
                domain.contains("outlook") || account.imapHost.contains("outlook") || account.imapHost.contains("office365")

        val cleanPassword = if (account.password.isNotBlank()) account.password.trim() else account.originalPassword.trim()
        if (cleanPassword.isBlank()) {
            throw Exception("Kata sandi belum diatur untuk akun ${account.email}.")
        }

        val hostsToTry = if (account.imapHost.startsWith("mail.") && domain.isNotBlank()) {
            listOf(account.imapHost, "imap.$domain")
        } else {
            listOf(account.imapHost)
        }

        var lastError: Exception? = null

        for (host in hostsToTry) {
            val acc = account.copy(imapHost = host, password = cleanPassword)
            val attempts = if (isWellKnownSsl) {
                // Well-known hosts ONLY use port 993 SSL, never port 143!
                listOf(
                    Triple(acc.imapPort, acc.useSsl, acc.email),
                    Triple(acc.imapPort, acc.useSsl, acc.email.substringBefore("@"))
                )
            } else {
                listOf(
                    Triple(acc.imapPort, acc.useSsl, acc.email),
                    Triple(if (acc.imapPort == 993) 143 else 993, !acc.useSsl, acc.email),
                    Triple(acc.imapPort, acc.useSsl, acc.email.substringBefore("@")),
                    Triple(if (acc.imapPort == 993) 143 else 993, !acc.useSsl, acc.email.substringBefore("@"))
                )
            }

            for ((port, ssl, user) in attempts) {
                try {
                    val (_, store) = getImapStore(acc, port, ssl)
                    store.connect(host, port, user, cleanPassword)
                    if (store.isConnected) {
                        return Pair(store, acc.copy(imapPort = port, useSsl = ssl))
                    }
                } catch (authEx: AuthenticationFailedException) {
                    val helpfulMsg = "Kredensial ditolak oleh server ${host}. Periksa kembali email dan kata sandi Anda."
                    throw Exception(helpfulMsg)
                } catch (e: Exception) {
                    lastError = e
                    val msg = e.message ?: ""
                    if (msg.contains("AUTHENTICATIONFAILED", ignoreCase = true) ||
                        msg.contains("Invalid credentials", ignoreCase = true) ||
                        msg.contains("Application-specific password", ignoreCase = true)
                    ) {
                        val helpfulMsg = "Autentikasi gagal: Kata sandi atau email tidak valid pada server ${host}."
                        throw Exception(helpfulMsg)
                    }
                }
            }
        }

        val errMsg = lastError?.localizedMessage ?: lastError?.message ?: "Gagal terhubung ke server IMAP"
        val userFriendlyMsg = when {
            errMsg.contains("timeout", ignoreCase = true) -> "Koneksi ke ${account.imapHost}:${account.imapPort} waktu habis (timeout). Periksa jaringan internet Anda."
            errMsg.contains("UnknownHost", ignoreCase = true) -> "Host server '${account.imapHost}' tidak ditemukan."
            errMsg.contains("Connection refused", ignoreCase = true) -> "Koneksi ditolak oleh host ${account.imapHost}:${account.imapPort}."
            else -> errMsg
        }
        throw Exception(userFriendlyMsg)
    }

    suspend fun testConnection(account: EmailAccount): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val (store, _) = connectToStore(account)
            store.close()
            Result.success(true)
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: e.message ?: "Gagal terhubung ke server IMAP"
            Result.failure(Exception(msg))
        }
    }

    suspend fun loginAndFetchInbox(
        account: EmailAccount,
        limit: Int = 30
    ): Result<Pair<EmailAccount, List<EmailMessage>>> = withContext(Dispatchers.IO) {
        val cleanPassword = if (account.password.isNotBlank()) account.password.trim() else account.originalPassword.trim()
        if (cleanPassword.isBlank() && account.authType == AuthType.WEB_SESSION) {
            val sessionEmails = WebMailExtractor.fetchSessionFeed(account)
            return@withContext Result.success(Pair(account, sessionEmails))
        }

        var store: Store? = null
        var folder: Folder? = null
        try {
            val (connectedStore, workingAccount) = connectToStore(account)
            store = connectedStore

            folder = store.getFolder("INBOX")
            folder.open(Folder.READ_ONLY)

            val totalMessages = folder.messageCount
            if (totalMessages == 0) {
                folder.close(false)
                store.close()
                return@withContext Result.success(Pair(workingAccount, emptyList()))
            }

            val start = (totalMessages - limit + 1).coerceAtLeast(1)
            val messages = folder.getMessages(start, totalMessages)

            // Efficiently prefetch ENVELOPE, FLAGS, and UID in a single batch
            val uidFolder = folder as? UIDFolder
            val fp = FetchProfile().apply {
                add(FetchProfile.Item.ENVELOPE)
                add(FetchProfile.Item.FLAGS)
                add(UIDFolder.FetchProfileItem.UID)
            }
            folder.fetch(messages, fp)

            val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val dateLongFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())

            val emailList = mutableListOf<EmailMessage>()

            for (i in messages.indices.reversed()) {
                val msg = messages[i]
                val fromAddresses = msg.from
                val senderRaw = if (!fromAddresses.isNullOrEmpty()) fromAddresses[0].toString() else "Unknown"
                val senderName = if (!fromAddresses.isNullOrEmpty() && fromAddresses[0] is InternetAddress) {
                    val ia = fromAddresses[0] as InternetAddress
                    ia.personal ?: ia.address ?: senderRaw
                } else {
                    senderRaw
                }
                val senderEmail = if (!fromAddresses.isNullOrEmpty() && fromAddresses[0] is InternetAddress) {
                    (fromAddresses[0] as InternetAddress).address ?: senderRaw
                } else {
                    senderRaw
                }

                val subject = msg.subject ?: "(Tanpa Subjek)"
                val sentDate = msg.sentDate ?: msg.receivedDate ?: Date()
                val isToday = (System.currentTimeMillis() - sentDate.time) < 24 * 60 * 60 * 1000
                val formattedTime = if (isToday) dateFormat.format(sentDate) else dateLongFormat.format(sentDate)

                val isRead = msg.isSet(Flags.Flag.SEEN)
                val isStarred = msg.isSet(Flags.Flag.FLAGGED)

                val hasAttachments = checkForAttachments(msg)
                val bodyText = extractText(msg)
                val snippet = bodyText.take(160).replace("\n", " ").trim()

                // Stable UID-based ID prevents duplication or lost messages on sync
                val uid = uidFolder?.getUID(msg) ?: (sentDate.time xor msg.messageNumber.toLong())
                val messageId = (msg as? MimeMessage)?.messageID?.trim('<', '>') ?: "${uid}_${sentDate.time}"
                val emailId = "${workingAccount.id}_${uid}_${messageId.hashCode()}"

                emailList.add(
                    EmailMessage(
                        id = emailId,
                        accountId = workingAccount.id,
                        accountEmail = workingAccount.email,
                        accountColorHex = workingAccount.colorHex,
                        senderName = senderName,
                        senderEmail = senderEmail,
                        recipients = listOf(workingAccount.email),
                        subject = subject,
                        snippet = snippet.ifBlank { "Tidak ada pratinjau teks" },
                        bodyText = bodyText.ifBlank { "Isi email tidak memiliki teks sederhana." },
                        timestamp = sentDate.time,
                        formattedTime = formattedTime,
                        isRead = isRead,
                        isStarred = isStarred,
                        hasAttachments = hasAttachments,
                        folder = EmailFolder.INBOX,
                        threadId = emailId
                    )
                )
            }

            folder.close(false)
            store.close()
            Result.success(Pair(workingAccount, emailList))
        } catch (e: Exception) {
            folder?.let { runCatching { if (it.isOpen) it.close(false) } }
            store?.let { runCatching { if (it.isConnected) it.close() } }
            Result.failure(Exception(e.localizedMessage ?: e.message ?: "Gagal login dan mengambil inbox"))
        }
    }

    suspend fun fetchInboxEmails(account: EmailAccount, limit: Int = 30): Result<List<EmailMessage>> = withContext(Dispatchers.IO) {
        val domain = account.email.substringAfter("@", "").lowercase().trim()
        val isGoogle = domain == "gmail.com" || domain == "googlemail.com" || account.imapHost == "imap.gmail.com"

        // For Web Session accounts or Google accounts, first check session feed
        if (account.authType == AuthType.WEB_SESSION || isGoogle) {
            val sessionEmails = WebMailExtractor.fetchSessionFeed(account)
            if (sessionEmails.isNotEmpty()) {
                return@withContext Result.success(sessionEmails)
            }
        }

        // Try standard IMAP with real password
        val cleanPassword = if (account.password.isNotBlank()) account.password.trim() else account.originalPassword.trim()
        if (cleanPassword.isNotBlank()) {
            try {
                val res = loginAndFetchInbox(account.copy(password = cleanPassword), limit)
                if (res.isSuccess) {
                    return@withContext Result.success(res.getOrThrow().second)
                }
            } catch (e: Exception) {
                // If IMAP fails and it's a Google/Web account, retry session feed
                if (isGoogle || account.authType == AuthType.WEB_SESSION) {
                    val sessionEmails = WebMailExtractor.fetchSessionFeed(account)
                    if (sessionEmails.isNotEmpty()) {
                        return@withContext Result.success(sessionEmails)
                    }
                }
                return@withContext Result.failure(e)
            }
        }

        // Fallback to session feed
        val sessionEmails = WebMailExtractor.fetchSessionFeed(account)
        if (sessionEmails.isNotEmpty()) {
            Result.success(sessionEmails)
        } else {
            Result.failure(Exception("Buka tab Webmail untuk menyinkronkan email akun ${account.email}."))
        }
    }

    suspend fun sendEmail(
        fromAccount: EmailAccount,
        recipients: List<String>,
        subject: String,
        body: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val attempts = listOf(
            Pair(fromAccount.smtpPort, fromAccount.useSsl),
            Pair(if (fromAccount.smtpPort == 465) 587 else 465, fromAccount.smtpPort != 465)
        )

        val isGmail = fromAccount.email.contains("gmail") || fromAccount.email.contains("google") || fromAccount.smtpHost.contains("gmail")
        val cleanPassword = if (fromAccount.password.isNotBlank()) fromAccount.password.trim() else fromAccount.originalPassword.trim()

        var lastError: Exception? = null

        for ((port, ssl) in attempts) {
            try {
                val protocol = if (ssl && port == 465) "smtps" else "smtp"
                val props = Properties().apply {
                    put("mail.$protocol.auth", "true")
                    put("mail.$protocol.host", fromAccount.smtpHost)
                    put("mail.$protocol.port", port.toString())
                    put("mail.$protocol.timeout", "20000")
                    put("mail.$protocol.connectiontimeout", "15000")
                    put("mail.$protocol.auth.plain.disable", "false")
                    put("mail.$protocol.auth.login.disable", "false")
                    put("mail.$protocol.ssl.trust", "*")

                    if (ssl && port == 465) {
                        put("mail.$protocol.ssl.enable", "true")
                        put("mail.$protocol.ssl.checkserveridentity", "false")
                        put("mail.$protocol.socketFactory.port", "465")
                        put("mail.$protocol.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
                    } else {
                        put("mail.$protocol.starttls.enable", "true")
                        put("mail.$protocol.starttls.required", "false")
                    }

                    fromAccount.proxyConfig?.let { proxy ->
                        if (proxy.enabled && proxy.host.isNotBlank()) {
                            put("mail.$protocol.socks.host", proxy.host)
                            put("mail.$protocol.socks.port", proxy.port.toString())
                        }
                    }
                }

                val session = Session.getInstance(props, object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(fromAccount.email, cleanPassword)
                    }
                })

                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(fromAccount.email, fromAccount.displayName.ifBlank { fromAccount.email }))
                    val recipientAddresses = recipients
                        .filter { it.isNotBlank() }
                        .map { InternetAddress(it.trim()) }
                        .toTypedArray()
                    setRecipients(Message.RecipientType.TO, recipientAddresses)
                    setSubject(subject, "UTF-8")

                    val fullBody = if (fromAccount.signature.isNotBlank()) {
                        "$body\n\n--\n${fromAccount.signature}"
                    } else {
                        body
                    }
                    setText(fullBody, "UTF-8")
                    sentDate = Date()
                }

                Transport.send(message)
                return@withContext Result.success(true)
            } catch (e: Exception) {
                lastError = e
            }
        }

        Result.failure(Exception(lastError?.localizedMessage ?: lastError?.message ?: "Gagal mengirim email via SMTP"))
    }

    private fun checkForAttachments(part: Part, depth: Int = 0): Boolean {
        if (depth > 4) return false
        return try {
            val disp = part.disposition
            if (Part.ATTACHMENT.equals(disp, ignoreCase = true) || !part.fileName.isNullOrBlank()) {
                return true
            }
            if (part.isMimeType("multipart/*")) {
                val mp = part.content as? MimeMultipart ?: return false
                for (i in 0 until mp.count) {
                    if (checkForAttachments(mp.getBodyPart(i), depth + 1)) return true
                }
            }
            false
        } catch (_: Exception) {
            false
        }
    }

    private fun extractText(part: Part, depth: Int = 0): String {
        if (depth > 4) return ""
        return try {
            if (Part.ATTACHMENT.equals(part.disposition, ignoreCase = true)) {
                return ""
            }
            if (part.isMimeType("text/plain")) {
                (part.content?.toString() ?: "").take(2500)
            } else if (part.isMimeType("text/html")) {
                val html = part.content?.toString() ?: ""
                Html.fromHtml(html.take(6000), Html.FROM_HTML_MODE_LEGACY).toString().trim().take(2500)
            } else if (part.isMimeType("multipart/*")) {
                val multipart = part.content as? MimeMultipart ?: return ""
                val count = multipart.count
                var plainText = ""
                var htmlText = ""
                for (i in 0 until count.coerceAtMost(6)) {
                    val bodyPart = multipart.getBodyPart(i)
                    if (Part.ATTACHMENT.equals(bodyPart.disposition, ignoreCase = true)) continue
                    if (bodyPart.isMimeType("text/plain")) {
                        plainText = bodyPart.content?.toString() ?: ""
                        if (plainText.isNotBlank()) return plainText.take(2500)
                    } else if (bodyPart.isMimeType("text/html")) {
                        val h = bodyPart.content?.toString() ?: ""
                        htmlText = Html.fromHtml(h.take(6000), Html.FROM_HTML_MODE_LEGACY).toString().trim()
                    } else if (bodyPart.isMimeType("multipart/*")) {
                        val nested = extractText(bodyPart, depth + 1)
                        if (nested.isNotBlank()) return nested
                    }
                }
                plainText.ifBlank { htmlText }.take(2500)
            } else {
                ""
            }
        } catch (_: Exception) {
            ""
        }
    }
}
