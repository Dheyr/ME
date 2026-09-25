package com.omnimail.app.model

enum class AuthType {
    OAUTH_GOOGLE,
    OAUTH_MICROSOFT,
    WEB_SESSION,
    IMAP_SMTP_MANUAL
}

enum class AccountStatus {
    ONLINE,
    SYNCING,
    ERROR,
    PAUSED
}

enum class NotificationLevel {
    ALL_SOUND,
    SILENT_BAR,
    MUTED
}

enum class ProxyType {
    HTTP,
    HTTPS,
    SOCKS5
}

enum class EmailFolder {
    INBOX,
    STARRED,
    SENT,
    DRAFTS,
    TRASH,
    SPAM
}

data class ProxyConfig(
    val type: ProxyType = ProxyType.SOCKS5,
    val host: String = "",
    val port: Int = 1080,
    val username: String = "",
    val password: String = "",
    val enabled: Boolean = false
)

data class EmailAttachment(
    val id: String,
    val fileName: String,
    val fileSizeBytes: Long,
    val mimeType: String
) {
    val formattedSize: String
        get() {
            val mb = fileSizeBytes / (1024.0 * 1024.0)
            return if (mb >= 1.0) {
                String.format("%.1f MB", mb)
            } else {
                String.format("%d KB", fileSizeBytes / 1024)
            }
        }
}

data class EmailAccount(
    val id: String,
    val email: String,
    val displayName: String,
    val authType: AuthType,
    val workspaceGroupId: String,
    val colorHex: Long, // ARGB representation
    val unreadCount: Int = 0,
    val status: AccountStatus = AccountStatus.ONLINE,
    val proxyConfig: ProxyConfig? = null,
    val signature: String = "Sent from OmniMail for Android",
    val isPushEnabled: Boolean = false,
    val notificationLevel: NotificationLevel = NotificationLevel.ALL_SOUND,
    val imapHost: String = "imap.provider.com",
    val imapPort: Int = 993,
    val smtpHost: String = "smtp.provider.com",
    val smtpPort: Int = 465,
    val password: String = "",
    val useSsl: Boolean = true,
    val webLoginUrl: String? = null,
    val webCookies: String? = null
)

data class WorkspaceGroup(
    val id: String,
    val name: String,
    val colorHex: Long,
    val accountIds: List<String> = emptyList(),
    val proxyConfig: ProxyConfig? = null
)

data class EmailMessage(
    val id: String,
    val accountId: String,
    val accountEmail: String,
    val accountColorHex: Long,
    val senderName: String,
    val senderEmail: String,
    val recipients: List<String>,
    val subject: String,
    val snippet: String,
    val bodyText: String,
    val timestamp: Long,
    val formattedTime: String,
    val isRead: Boolean = false,
    val isStarred: Boolean = false,
    val hasAttachments: Boolean = false,
    val attachments: List<EmailAttachment> = emptyList(),
    val folder: EmailFolder = EmailFolder.INBOX,
    val threadId: String = id
)

data class SyncSettings(
    val syncIntervalMinutes: Int = 15, // 0 = manual, 15, 30, 60
    val maxPushIdleAccounts: Int = 5,
    val autoCleanCacheDays: Int = 30,
    val isAppLockEnabled: Boolean = false,
    val isBiometricEnabled: Boolean = true,
    val isDarkTheme: Boolean? = null // null = system default
)
