package com.omnimail.app.service

import android.webkit.CookieManager
import android.webkit.WebView
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.omnimail.app.model.EmailFolder
import com.omnimail.app.model.EmailMessage
import com.omnimail.app.model.EmailAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

object WebMailExtractor {

    private val gson = Gson()

    data class ExtractedItem(
        val id: String? = null,
        val sender: String? = null,
        val senderEmail: String? = null,
        val subject: String? = null,
        val snippet: String? = null,
        val date: String? = null,
        val isUnread: Boolean = false
    )

    const val EXTRACTION_JS = """
        (function() {
            var results = [];
            try {
                var rows = document.querySelectorAll('tr.zA, div[role="row"], table.F.cf tr');
                if (rows && rows.length > 0) {
                    for (var i = 0; i < Math.min(rows.length, 40); i++) {
                        var r = rows[i];
                        var isUnread = r.classList.contains('zE') || r.querySelector('.zE') != null;
                        var senderEl = r.querySelector('.yW span, .bA4, td.yX, span[email]');
                        var sender = senderEl ? (senderEl.innerText || senderEl.textContent) : 'Pengirim';
                        var sEmail = senderEl ? (senderEl.getAttribute('email') || '') : '';
                        var subjectEl = r.querySelector('.bog span, span.bqe, [data-thread-id]');
                        var subject = subjectEl ? (subjectEl.innerText || subjectEl.textContent) : '(Tanpa Subjek)';
                        var snippetEl = r.querySelector('.y2');
                        var snippet = snippetEl ? (snippetEl.innerText || snippetEl.textContent) : '';
                        var dateEl = r.querySelector('.xW span, td.xW, span.bq3');
                        var date = dateEl ? (dateEl.innerText || dateEl.textContent) : '';
                        var tid = r.getAttribute('data-thread-id') || r.getAttribute('data-legacy-thread-id') || ('dom_' + i + '_' + Date.now());
                        
                        if (subject.length > 0 || snippet.length > 0 || sender.length > 0) {
                            results.push({
                                id: tid,
                                sender: sender.trim(),
                                senderEmail: sEmail.trim(),
                                subject: subject.trim(),
                                snippet: snippet.trim().replace(/^[\s\-–—]+/, ''),
                                date: date.trim(),
                                isUnread: isUnread
                            });
                        }
                    }
                }
                if (results.length === 0) {
                    var cards = document.querySelectorAll('.t, .z, div[data-item-id], table.th tr');
                    for (var j = 0; j < Math.min(cards.length, 30); j++) {
                        var c = cards[j];
                        var mSender = c.querySelector('.f, .sender, td:first-child') ? c.querySelector('.f, .sender, td:first-child').innerText : 'Pengirim';
                        var mSubj = c.querySelector('.h, .subject, td:nth-child(2)') ? c.querySelector('.h, .subject, td:nth-child(2)').innerText : '(Tanpa Subjek)';
                        var mDate = c.querySelector('.d, .date, td:nth-child(3)') ? c.querySelector('.d, .date, td:nth-child(3)').innerText : '';
                        if (mSubj.length > 0 || mSender.length > 0) {
                            results.push({
                                id: 'm_' + j + '_' + Date.now(),
                                sender: mSender.trim(),
                                senderEmail: '',
                                subject: mSubj.trim(),
                                snippet: '',
                                date: mDate.trim(),
                                isUnread: false
                            });
                        }
                    }
                }
            } catch(e) {}
            return JSON.stringify(results);
        })()
    """

    fun parseExtractedJson(rawJson: String, account: EmailAccount): List<EmailMessage> {
        return try {
            val cleanJson = if (rawJson.startsWith("\"") && rawJson.endsWith("\"")) {
                JsonParser.parseString(rawJson).asString
            } else {
                rawJson
            }

            if (cleanJson.isBlank() || cleanJson == "[]" || cleanJson == "null") {
                return emptyList()
            }

            val type = object : TypeToken<List<ExtractedItem>>() {}.type
            val items: List<ExtractedItem> = gson.fromJson(cleanJson, type) ?: emptyList()

            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val now = System.currentTimeMillis()

            items.mapIndexed { index, item ->
                val msgId = item.id?.ifBlank { null } ?: "web_${account.id}_${now}_$index"
                EmailMessage(
                    id = msgId,
                    accountId = account.id,
                    accountEmail = account.email,
                    accountColorHex = account.colorHex,
                    senderName = item.sender?.ifBlank { "Pengirim" } ?: "Pengirim",
                    senderEmail = item.senderEmail?.ifBlank { account.email } ?: account.email,
                    recipients = listOf(account.email),
                    subject = item.subject?.ifBlank { "(Tanpa Subjek)" } ?: "(Tanpa Subjek)",
                    snippet = item.snippet.orEmpty(),
                    bodyText = item.snippet.orEmpty(),
                    timestamp = now - (index * 60_000L),
                    formattedTime = item.date?.ifBlank { timeFormat.format(Date(now)) } ?: timeFormat.format(Date(now)),
                    isRead = !item.isUnread,
                    folder = EmailFolder.INBOX,
                    threadId = msgId
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun extractFromWebView(webView: WebView, account: EmailAccount): List<EmailMessage> =
        suspendCancellableCoroutine { cont ->
            webView.post {
                webView.evaluateJavascript(EXTRACTION_JS) { result ->
                    val emails = parseExtractedJson(result ?: "", account)
                    if (cont.isActive) {
                        cont.resume(emails)
                    }
                }
            }
        }

    suspend fun fetchSessionFeed(account: EmailAccount): List<EmailMessage> = withContext(Dispatchers.IO) {
        try {
            val cookieManager = CookieManager.getInstance()
            val cookies = cookieManager.getCookie("https://mail.google.com") ?: account.webCookies ?: ""
            if (cookies.isBlank()) return@withContext emptyList()

            val encodedEmail = try {
                URLEncoder.encode(account.email, "UTF-8")
            } catch (_: Exception) {
                account.email
            }

            val urlsToTry = listOf(
                "https://mail.google.com/mail/u/?authuser=$encodedEmail/feed/atom",
                "https://mail.google.com/mail/feed/atom"
            )

            for (u in urlsToTry) {
                try {
                    val conn = URL(u).openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.connectTimeout = 7000
                    conn.readTimeout = 7000
                    conn.setRequestProperty("Cookie", cookies)
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36")
                    conn.setRequestProperty("Accept", "application/atom+xml,application/xml,text/xml,*/*")

                    if (conn.responseCode == 200) {
                        val xmlContent = conn.inputStream.bufferedReader().use { it.readText() }
                        val messages = parseAtomXml(xmlContent, account)
                        if (messages.isNotEmpty()) {
                            return@withContext messages
                        }
                    }
                } catch (_: Exception) {}
            }
            emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun parseAtomXml(xml: String, account: EmailAccount): List<EmailMessage> {
        val messages = mutableListOf<EmailMessage>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            var eventType = parser.eventType
            var inEntry = false
            var currentTitle = ""
            var currentSummary = ""
            var currentName = ""
            var currentEmail = ""
            var currentId = ""
            var currentDate = ""
            var inAuthor = false

            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val dateFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tagName = parser.name?.lowercase() ?: ""
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (tagName) {
                            "entry" -> {
                                inEntry = true
                                currentTitle = ""
                                currentSummary = ""
                                currentName = ""
                                currentEmail = ""
                                currentId = ""
                                currentDate = ""
                            }
                            "author" -> if (inEntry) inAuthor = true
                            "title" -> if (inEntry) currentTitle = parser.nextText().orEmpty()
                            "summary" -> if (inEntry) currentSummary = parser.nextText().orEmpty()
                            "name" -> if (inAuthor) currentName = parser.nextText().orEmpty()
                            "email" -> if (inAuthor) currentEmail = parser.nextText().orEmpty()
                            "id" -> if (inEntry) currentId = parser.nextText().orEmpty()
                            "modified", "issued" -> if (inEntry && currentDate.isBlank()) currentDate = parser.nextText().orEmpty()
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        when (tagName) {
                            "author" -> inAuthor = false
                            "entry" -> {
                                inEntry = false
                                val parsedTimestamp = try {
                                    isoFormat.parse(currentDate)?.time ?: System.currentTimeMillis()
                                } catch (_: Exception) {
                                    System.currentTimeMillis()
                                }
                                val isToday = (System.currentTimeMillis() - parsedTimestamp) < 24 * 60 * 60 * 1000
                                val formattedTime = if (isToday) timeFormat.format(Date(parsedTimestamp)) else dateFormat.format(Date(parsedTimestamp))
                                val msgId = if (currentId.isNotBlank()) "atom_${currentId.hashCode()}" else "msg_${System.currentTimeMillis()}_${messages.size}"

                                messages.add(
                                    EmailMessage(
                                        id = msgId,
                                        accountId = account.id,
                                        accountEmail = account.email,
                                        accountColorHex = account.colorHex,
                                        senderName = currentName.ifBlank { currentEmail.ifBlank { "Pengirim" } },
                                        senderEmail = currentEmail.ifBlank { account.email },
                                        recipients = listOf(account.email),
                                        subject = currentTitle.ifBlank { "(Tanpa Subjek)" },
                                        snippet = currentSummary,
                                        bodyText = currentSummary,
                                        timestamp = parsedTimestamp,
                                        formattedTime = formattedTime,
                                        isRead = false,
                                        folder = EmailFolder.INBOX,
                                        threadId = msgId
                                    )
                                )
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {}
        return messages
    }
}
