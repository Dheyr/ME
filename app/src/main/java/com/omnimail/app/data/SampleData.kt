package com.omnimail.app.data

import com.omnimail.app.model.*

object SampleData {

    val sampleGroups = listOf(
        WorkspaceGroup(
            id = "group_agency",
            name = "Agensi & Bisnis Utama",
            colorHex = 0xFF6750A4, // Primary Purple
            accountIds = listOf("acc_1", "acc_2", "acc_3")
        ),
        WorkspaceGroup(
            id = "group_support",
            name = "Customer Support",
            colorHex = 0xFF006D77, // Teal
            accountIds = listOf("acc_4", "acc_5", "acc_6")
        ),
        WorkspaceGroup(
            id = "group_marketing",
            name = "Cold Outreach / Marketing",
            colorHex = 0xFFD84A1B, // Burnt Orange
            accountIds = listOf("acc_7", "acc_8", "acc_9", "acc_10")
        ),
        WorkspaceGroup(
            id = "group_personal",
            name = "Personal & Portofolio",
            colorHex = 0xFF2E7D32, // Forest Green
            accountIds = listOf("acc_11")
        )
    )

    val sampleAccounts = listOf(
        EmailAccount(
            id = "acc_1",
            email = "ceo@agencycorp.com",
            displayName = "Alex Carter (CEO)",
            authType = AuthType.OAUTH_GOOGLE,
            workspaceGroupId = "group_agency",
            colorHex = 0xFF6750A4,
            unreadCount = 3,
            status = AccountStatus.ONLINE,
            isPushEnabled = true,
            signature = "Best regards,\nAlex Carter | CEO, AgencyCorp"
        ),
        EmailAccount(
            id = "acc_2",
            email = "operations@agencycorp.com",
            displayName = "Operations Team",
            authType = AuthType.OAUTH_GOOGLE,
            workspaceGroupId = "group_agency",
            colorHex = 0xFF8338EC,
            unreadCount = 1,
            status = AccountStatus.ONLINE,
            signature = "Operations Dept.\nAgencyCorp"
        ),
        EmailAccount(
            id = "acc_3",
            email = "billing@agencycorp.com",
            displayName = "Finance & Billing",
            authType = AuthType.OAUTH_MICROSOFT,
            workspaceGroupId = "group_agency",
            colorHex = 0xFF3A86FF,
            unreadCount = 0,
            status = AccountStatus.ONLINE,
            signature = "Finance Desk - AgencyCorp"
        ),
        EmailAccount(
            id = "acc_4",
            email = "helpdesk@clientcare.io",
            displayName = "Tier 1 Helpdesk",
            authType = AuthType.IMAP_SMTP_MANUAL,
            workspaceGroupId = "group_support",
            colorHex = 0xFF006D77,
            unreadCount = 14,
            status = AccountStatus.SYNCING,
            proxyConfig = ProxyConfig(type = ProxyType.SOCKS5, host = "192.168.10.45", port = 1080, enabled = true),
            signature = "ClientCare Support Team"
        ),
        EmailAccount(
            id = "acc_5",
            email = "urgent-tickets@clientcare.io",
            displayName = "Urgent Escalations",
            authType = AuthType.IMAP_SMTP_MANUAL,
            workspaceGroupId = "group_support",
            colorHex = 0xFFE63946,
            unreadCount = 2,
            status = AccountStatus.ONLINE,
            isPushEnabled = true,
            signature = "Urgent Response Unit"
        ),
        EmailAccount(
            id = "acc_6",
            email = "cs-indo@onlinestore.id",
            displayName = "CS Regional ID",
            authType = AuthType.IMAP_SMTP_MANUAL,
            workspaceGroupId = "group_support",
            colorHex = 0xFF2A9D8F,
            unreadCount = 5,
            status = AccountStatus.ONLINE,
            signature = "Salam hangat,\nCustomer Success OnlineStore ID"
        ),
        EmailAccount(
            id = "acc_7",
            email = "sarah.lead@outreachflow.co",
            displayName = "Sarah - SDR",
            authType = AuthType.IMAP_SMTP_MANUAL,
            workspaceGroupId = "group_marketing",
            colorHex = 0xFFD84A1B,
            unreadCount = 4,
            status = AccountStatus.ONLINE,
            proxyConfig = ProxyConfig(type = ProxyType.HTTP, host = "proxy-us.outreach.net", port = 8080, enabled = true),
            signature = "Sarah Jenkins\nSales Development Rep"
        ),
        EmailAccount(
            id = "acc_8",
            email = "david.b2b@outreachflow.co",
            displayName = "David - Enterprise",
            authType = AuthType.IMAP_SMTP_MANUAL,
            workspaceGroupId = "group_marketing",
            colorHex = 0xFFF77F00,
            unreadCount = 0,
            status = AccountStatus.ONLINE,
            proxyConfig = ProxyConfig(type = ProxyType.HTTP, host = "proxy-uk.outreach.net", port = 8080, enabled = true),
            signature = "David Vance | B2B Specialist"
        ),
        EmailAccount(
            id = "acc_9",
            email = "newsletter@globalpromo.net",
            displayName = "Broadcast System",
            authType = AuthType.IMAP_SMTP_MANUAL,
            workspaceGroupId = "group_marketing",
            colorHex = 0xFFBC6C25,
            unreadCount = 0,
            status = AccountStatus.PAUSED,
            signature = "GlobalPromo Weekly"
        ),
        EmailAccount(
            id = "acc_10",
            email = "partnerships@growthhub.agency",
            displayName = "Partner Relations",
            authType = AuthType.OAUTH_GOOGLE,
            workspaceGroupId = "group_marketing",
            colorHex = 0xFF9B5DE5,
            unreadCount = 1,
            status = AccountStatus.ONLINE,
            signature = "Partnerships GrowthHub"
        ),
        EmailAccount(
            id = "acc_11",
            email = "alex.personal@domain.com",
            displayName = "Alex Personal",
            authType = AuthType.OAUTH_GOOGLE,
            workspaceGroupId = "group_personal",
            colorHex = 0xFF2E7D32,
            unreadCount = 2,
            status = AccountStatus.ONLINE,
            signature = "Alex"
        )
    )

    val sampleEmails = listOf(
        EmailMessage(
            id = "msg_1",
            accountId = "acc_1",
            accountEmail = "ceo@agencycorp.com",
            accountColorHex = 0xFF6750A4,
            senderName = "Jonathan Ward (Vanguard VC)",
            senderEmail = "jward@vanguardvc.com",
            recipients = listOf("ceo@agencycorp.com"),
            subject = "Follow up: Term Sheet Discussion & Q3 Expansion",
            snippet = "Hi Alex, great meeting yesterday. Attached is the revised valuation matrix for your consideration before next Monday...",
            bodyText = "Hi Alex,\n\nIt was great catching up yesterday regarding your expansion plans and the enterprise SaaS pipeline. The metrics on customer acquisition cost and LTV you presented are very compelling.\n\nI have reviewed the term sheet internally with our partners, and we have updated clause 4.2 to accommodate your IP retention preferences. Please find the revised draft attached.\n\nLet me know if 2:00 PM tomorrow works for a quick 15-minute sync.\n\nBest,\nJonathan Ward\nGeneral Partner | Vanguard Capital",
            timestamp = System.currentTimeMillis() - 1000 * 60 * 18, // 18 mins ago
            formattedTime = "10:42 AM",
            isRead = false,
            isStarred = true,
            hasAttachments = true,
            attachments = listOf(
                EmailAttachment("att_1", "Term_Sheet_v3_Final.pdf", 2_450_000, "application/pdf"),
                EmailAttachment("att_2", "Q3_CapTable_Model.xlsx", 890_000, "application/vnd.ms-excel")
            ),
            folder = EmailFolder.INBOX,
            threadId = "thread_1"
        ),
        EmailMessage(
            id = "msg_2",
            accountId = "acc_4",
            accountEmail = "helpdesk@clientcare.io",
            accountColorHex = 0xFF006D77,
            senderName = "Tech Support - SLA Alert",
            senderEmail = "alerts@monitoring-node.org",
            recipients = listOf("helpdesk@clientcare.io"),
            subject = "[CRITICAL] High latency spike on US-East API Cluster #04",
            snippet = "Warning: P99 response time crossed 1,450ms for 3 consecutive intervals. Automatic failover initiated...",
            bodyText = "Automated Infrastructure Alert:\n\nSeverity: HIGH\nResource: Cluster us-east-04 (API Gateway)\nTrigger: P99 Response Time > 1200ms for 5 minutes\nCurrent P99: 1,452ms\n\nFailover status: Redirecting 30% traffic to Secondary Node us-east-05.\nPlease verify database pool connectivity immediately.",
            timestamp = System.currentTimeMillis() - 1000 * 60 * 45, // 45 mins ago
            formattedTime = "10:15 AM",
            isRead = false,
            isStarred = false,
            hasAttachments = false,
            folder = EmailFolder.INBOX,
            threadId = "thread_2"
        ),
        EmailMessage(
            id = "msg_3",
            accountId = "acc_7",
            accountEmail = "sarah.lead@outreachflow.co",
            accountColorHex = 0xFFD84A1B,
            senderName = "Marcus Brody",
            senderEmail = "mbrody@nexustech.io",
            recipients = listOf("sarah.lead@outreachflow.co"),
            subject = "Re: Automating your inbound multi-channel lead funnels",
            snippet = "Thanks for reaching out Sarah. We actually evaluated this last month and need a solution by next month. Can we schedule a demo?",
            bodyText = "Hi Sarah,\n\nThanks for following up. Your email came at the right time—our marketing team is currently restructuring our SDR pipelines, and the multi-account dispatch feature you mentioned would solve our biggest bottleneck.\n\nCould we hop on a 20-minute Zoom call on Thursday at 3 PM CET? Please send an invite if that works.\n\nRegards,\nMarcus Brody\nVP of Growth, NexusTech",
            timestamp = System.currentTimeMillis() - 1000 * 60 * 120, // 2 hours ago
            formattedTime = "08:50 AM",
            isRead = false,
            isStarred = true,
            hasAttachments = false,
            folder = EmailFolder.INBOX,
            threadId = "thread_3"
        ),
        EmailMessage(
            id = "msg_4",
            accountId = "acc_5",
            accountEmail = "urgent-tickets@clientcare.io",
            accountColorHex = 0xFFE63946,
            senderName = "Enterprise Client (Acme Corp)",
            senderEmail = "cfo@acmecorp.com",
            recipients = listOf("urgent-tickets@clientcare.io"),
            subject = "Urgent: Billing discrepancy on Invoice #INV-2026-904",
            snippet = "Hello team, we noticed a double charge on our recurring monthly subscription for 500 seat licenses...",
            bodyText = "Dear Support Team,\n\nDuring our routine monthly reconciliation, our accounts department noticed that Invoice #INV-2026-904 was processed twice through the automated gateway.\n\nAttached is the bank receipt showing both transactions. Kindly review and issue a refund or credit note at your earliest convenience.\n\nThank you,\nFinance Department | Acme Corp",
            timestamp = System.currentTimeMillis() - 1000 * 60 * 240, // 4 hours ago
            formattedTime = "06:30 AM",
            isRead = false,
            isStarred = false,
            hasAttachments = true,
            attachments = listOf(
                EmailAttachment("att_3", "Bank_Receipt_Double_Charge.pdf", 450_000, "application/pdf")
            ),
            folder = EmailFolder.INBOX,
            threadId = "thread_4"
        ),
        EmailMessage(
            id = "msg_5",
            accountId = "acc_2",
            accountEmail = "operations@agencycorp.com",
            accountColorHex = 0xFF8338EC,
            senderName = "Google Cloud Platform",
            senderEmail = "billing-noreply@google.com",
            recipients = listOf("operations@agencycorp.com"),
            subject = "Monthly Billing Summary for Organization OmniHoldings",
            snippet = "Your Google Cloud invoice for the period August 2026 is now available to download from the Cloud Console...",
            bodyText = "Hello OmniHoldings Admin,\n\nYour monthly statement for Google Cloud usage has been generated.\n\nTotal Due: $3,412.80\nPayment Method: Automatic charge to Visa ending in 4920\nStatement Period: Aug 1 - Aug 31, 2026\n\nYou can view breakdown by service and project in the Google Cloud Billing console.",
            timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 12, // 12 hours ago
            formattedTime = "Yesterday",
            isRead = true,
            isStarred = false,
            hasAttachments = true,
            attachments = listOf(
                EmailAttachment("att_4", "GCP_Invoice_AUG2026.pdf", 185_000, "application/pdf")
            ),
            folder = EmailFolder.INBOX,
            threadId = "thread_5"
        ),
        EmailMessage(
            id = "msg_6",
            accountId = "acc_6",
            accountEmail = "cs-indo@onlinestore.id",
            accountColorHex = 0xFF2A9D8F,
            senderName = "Budi Pratama (Customer #8921)",
            senderEmail = "budi.pratama@gmail.com",
            recipients = listOf("cs-indo@onlinestore.id"),
            subject = "Konfirmasi Penerimaan Barang Pesanan #ORD-77123",
            snippet = "Selamat sore admin, paket pesanan saya sudah sampai dengan aman dan packing sangat rapi...",
            bodyText = "Selamat sore admin CS,\n\nSaya ingin konfirmasi bahwa pesanan #ORD-77123 berisi 5 unit router enterprise sudah kami terima di kantor kami di Surabaya.\n\nSemua unit berfungsi dengan baik. Terima kasih atas respon cepatnya!\n\nSalam,\nBudi Pratama",
            timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 24, // 1 day ago
            formattedTime = "Yesterday",
            isRead = true,
            isStarred = false,
            hasAttachments = false,
            folder = EmailFolder.INBOX,
            threadId = "thread_6"
        ),
        EmailMessage(
            id = "msg_7",
            accountId = "acc_11",
            accountEmail = "alex.personal@domain.com",
            accountColorHex = 0xFF2E7D32,
            senderName = "Garuda Indonesia Reservations",
            senderEmail = "e-ticket@garuda-indonesia.com",
            recipients = listOf("alex.personal@domain.com"),
            subject = "Electronic Ticket Receipt - CGK to DPS (Flight GA408)",
            snippet = "Dear Mr. Alex Carter, your flight booking is confirmed. Departure 14:15 WIB from Soekarno-Hatta Terminal 3...",
            bodyText = "Dear Mr. Alex Carter,\n\nThank you for choosing Garuda Indonesia. Here is your electronic ticket details:\n\nBooking Code: ZQ89KL\nFlight: GA 408 | Jakarta (CGK) -> Bali (DPS)\nDate: 28 September 2026\nDeparture: 14:15 WIB | Terminal 3\nBaggage: 30 KG Allowance\n\nPlease check in online 24 hours prior to departure.",
            timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 36,
            formattedTime = "23 Sep",
            isRead = true,
            isStarred = true,
            hasAttachments = true,
            attachments = listOf(
                EmailAttachment("att_5", "ETicket_GA408_AlexCarter.pdf", 320_000, "application/pdf")
            ),
            folder = EmailFolder.INBOX,
            threadId = "thread_7"
        )
    )
}
