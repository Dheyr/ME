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
            domain.contains("gmail") -> ServerConfig(
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
                imapHost = "imap.example.com",
                imapPort = 993,
                smtpHost = "smtp.example.com",
                smtpPort = 465,
                useSsl = true
            )
        }
    }

    private fun getImapStore(account: EmailAccount, port: Int, useSsl: Boolean): Pair<Session, Store> {
        val protocol = if (useSsl) "imaps" else "imap"
        val props = Properties().apply {
            put("mail.store.protocol", protocol)
            put("mail.$protocol.host", account.imapHost)
            put("mail.$protocol.port", port.toString())
            put("mail.$protocol.timeout", "10000")
            put("mail.$protocol.connectiontimeout", "10000")

            // Enable common auth methods for real passwords
            put("mail.$protocol.auth.plain.disable", "false")
            put("mail.$protocol.auth.login.disable", "false")

            // Trust all certificates (cPanel, self-signed, VPS, etc.)
            put("mail.$protocol.ssl.trust", "*")

            if (useSsl) {
                put("mail.$protocol.ssl.enable", "true")
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
        val hostsToTry = if (account.imapHost.startsWith("mail.") && domain.isNotBlank()) {
            listOf(account.imapHost, "imap.$domain")
        } else {
            listOf(account.imapHost)
        }

        var lastError: Exception? = null

        for (host in hostsToTry) {
            val acc = account.copy(imapHost = host)
            val attempts = listOf(
                Triple(acc.imapPort, acc.useSsl, acc.email),
                Triple(if (acc.imapPort == 993) 143 else 993, !acc.useSsl, acc.email),
                Triple(acc.imapPort, acc.useSsl, acc.email.substringBefore("@")),
                Triple(if (acc.imapPort == 993) 143 else 993, !acc.useSsl, acc.email.substringBefore("@"))
            )

            for ((port, ssl, user) in attempts) {
                try {
                    val (_, store) = getImapStore(acc, port, ssl)
                    store.connect(host, port, user, acc.password)
                    if (store.isConnected) {
                        return Pair(store, acc.copy(imapPort = port, useSsl = ssl))
                    }
                } catch (e: Exception) {
                    lastError = e
                }
            }
        }

        throw lastError ?: Exception("Tidak dapat terhubung ke server IMAP dengan kredensial yang diberikan.")
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

                val bodyText = extractText(msg)
                val snippet = bodyText.take(160).replace("\n", " ").trim()

                val emailId = "${workingAccount.id}_${msg.messageNumber}_${sentDate.time}"

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
                        hasAttachments = false,
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
        val res = loginAndFetchInbox(account, limit)
        if (res.isSuccess) {
            Result.success(res.getOrThrow().second)
        } else {
            Result.failure(res.exceptionOrNull() ?: Exception("Gagal mengambil email"))
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

        var lastError: Exception? = null

        for ((port, ssl) in attempts) {
            try {
                val protocol = if (ssl && port == 465) "smtps" else "smtp"
                val props = Properties().apply {
                    put("mail.$protocol.auth", "true")
                    put("mail.$protocol.host", fromAccount.smtpHost)
                    put("mail.$protocol.port", port.toString())
                    put("mail.$protocol.timeout", "15000")
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
                        return PasswordAuthentication(fromAccount.email, fromAccount.password)
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

    private fun extractText(part: Part): String {
        return try {
            if (part.isMimeType("text/plain")) {
                part.content?.toString() ?: ""
            } else if (part.isMimeType("text/html")) {
                val html = part.content?.toString() ?: ""
                Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString().trim()
            } else if (part.isMimeType("multipart/*")) {
                val multipart = part.content as? MimeMultipart ?: return ""
                val count = multipart.count
                var plainText = ""
                var htmlText = ""
                for (i in 0 until count) {
                    val bodyPart = multipart.getBodyPart(i)
                    if (bodyPart.isMimeType("text/plain")) {
                        plainText = bodyPart.content?.toString() ?: ""
                    } else if (bodyPart.isMimeType("text/html")) {
                        val h = bodyPart.content?.toString() ?: ""
                        htmlText = Html.fromHtml(h, Html.FROM_HTML_MODE_LEGACY).toString().trim()
                    } else if (bodyPart.isMimeType("multipart/*")) {
                        val nested = extractText(bodyPart)
                        if (nested.isNotBlank()) return nested
                    }
                }
                plainText.ifBlank { htmlText }
            } else {
                part.content?.toString() ?: ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    fun parseCsvAccounts(csvContent: String, defaultGroupId: String): List<EmailAccount> {
        val result = mutableListOf<EmailAccount>()
        val lines = csvContent.lines()
        val accountColors = listOf(
            0xFF6750A4, 0xFF006D77, 0xFFD84A1B, 0xFF2E7D32,
            0xFF8338EC, 0xFF3A86FF, 0xFFE63946, 0xFF2A9D8F
        )

        for ((index, line) in lines.withIndex()) {
            val trimmed = line.trim()
            if (trimmed.isBlank() || trimmed.startsWith("#") || trimmed.lowercase().startsWith("email")) {
                continue
            }

            val parts = trimmed.split(",").map { it.trim() }
            if (parts.isNotEmpty() && parts[0].contains("@")) {
                val email = parts[0]
                val password = if (parts.size > 1) parts[1] else ""
                val serverConfig = autoDetectServer(email)

                val imapHost = if (parts.size > 2 && parts[2].isNotBlank()) parts[2] else serverConfig.imapHost
                val imapPort = if (parts.size > 3 && parts[3].toIntOrNull() != null) parts[3].toInt() else serverConfig.imapPort
                val smtpHost = if (parts.size > 4 && parts[4].isNotBlank()) parts[4] else serverConfig.smtpHost
                val smtpPort = if (parts.size > 5 && parts[5].toIntOrNull() != null) parts[5].toInt() else serverConfig.smtpPort

                val color = accountColors[index % accountColors.size]
                val id = "acc_${System.currentTimeMillis()}_$index"

                result.add(
                    EmailAccount(
                        id = id,
                        email = email,
                        displayName = email.substringBefore("@"),
                        authType = AuthType.IMAP_SMTP_MANUAL,
                        workspaceGroupId = defaultGroupId,
                        colorHex = color,
                        password = password,
                        imapHost = imapHost,
                        imapPort = imapPort,
                        smtpHost = smtpHost,
                        smtpPort = smtpPort,
                        useSsl = serverConfig.useSsl,
                        status = AccountStatus.ONLINE
                    )
                )
            }
        }
        return result
    }
}
