# OmniMail Android - Antarmuka Native Jetpack Compose

Implementasi antarmuka klien email native Android menggunakan **Kotlin** dan **Jetpack Compose (Material Design 3 / Material You)** yang dirancang secara khusus untuk memenuhi spesifikasi [prd.md](file:///c:/Users/Entah~/Desktop/mlti.ag2/prd.md).

---

## 📱 Arsitektur & Struktur Kode

```
mlti.ag2/
├── prd.md                                # Dokumen Spesifikasi Kebutuhan Produk (PRD)
├── build.gradle.kts                      # Gradle root build script
├── settings.gradle.kts                   # Project settings
├── gradle/
│   └── libs.versions.toml                # Version Catalog dependencies
├── app/
│   ├── build.gradle.kts                  # Android app module config (Compose, Material 3)
│   └── src/main/
│       ├── AndroidManifest.xml           # Permissions (Network, Notification, Biometric, Doze)
│       └── java/com/omnimail/app/
│           ├── MainActivity.kt           # Host aplikasi: Navigation Drawer, TopBar, BottomBar, FAB
│           ├── model/
│           │   └── Models.kt             # Data models: Account, Message, Group, Proxy, Attachment
│           ├── data/
│           │   └── SampleData.kt         # Mock data realistis: 11 akun, 4 grup, email & attachment
│           ├── ui/
│           │   ├── theme/
│           │   │   ├── Color.kt          # Material 3 dark/light dynamic color palette
│           │   │   ├── Type.kt           # Tipografi OmniMail
│           │   │   └── Theme.kt          # OmniMailTheme dengan Dynamic Color & Dark Mode
│           │   ├── components/
│           │   │   ├── OmniComponents.kt # Badges warna akun, status dot, format toolbar, attachment card
│           │   │   ├── OmniDrawer.kt     # Side drawer navigasi folder & workspace switcher
│           │   │   └── OmniBars.kt       # TopAppBar filter dropdown & Bottom Navigation
│           │   └── screens/
│           │       ├── inbox/
│           │       │   └── UnifiedInboxScreen.kt # Kotak masuk terpadu, bulk action bar, lazy loading
│           │       ├── detail/
│           │       │   └── EmailDetailScreen.kt  # Detail email, thread percakapan, download attachment
│           │       ├── compose/
│           │       │   └── ComposeScreen.kt      # Multi-sender dropdown, Rich Text, 25MB check, signature
│           │       ├── search/
│           │       │   └── SearchScreen.kt       # Pencarian lokal instan & IMAP server search
│           │       ├── accounts/
│           │       │   └── AccountManagementScreen.kt # Workspaces, CSV Bulk Import, Proxy setting
│           │       └── settings/
│           │           └── SettingsScreen.kt     # Staggered sync, RAM budget, Keystore, App Lock
```

---

## 🎯 Pemetaan Fitur terhadap PRD

| Fitur di PRD | File Implementasi | Deskripsi & Acceptance Criteria |
| :--- | :--- | :--- |
| **FR 1.1 Penambahan Akun Otomatis/Manual** | [`AccountManagementScreen.kt`](file:///c:/Users/Entah~/Desktop/mlti.ag2/app/src/main/java/com/omnimail/app/ui/screens/accounts/AccountManagementScreen.kt) | Auto-detect host server IMAP/SMTP berdasarkan domain email & tombol Google/Microsoft OAuth. |
| **FR 1.2 Bulk Account Import (CSV)** | [`AccountManagementScreen.kt`](file:///c:/Users/Entah~/Desktop/mlti.ag2/app/src/main/java/com/omnimail/app/ui/screens/accounts/AccountManagementScreen.kt) | Modal dialog upload CSV dengan progress bar verifikasi dan daftar akun yang gagal login. |
| **FR 1.3 Pengelompokan Akun (Workspaces)** | [`OmniDrawer.kt`](file:///c:/Users/Entah~/Desktop/mlti.ag2/app/src/main/java/com/omnimail/app/ui/components/OmniDrawer.kt), [`Models.kt`](file:///c:/Users/Entah~/Desktop/mlti.ag2/app/src/main/java/com/omnimail/app/model/Models.kt) | Kategori akun dengan Color Tags (Agensi, Customer Support, Marketing Outreach, Personal). |
| **FR 1.4 Proxy per Akun / Grup** | [`AccountManagementScreen.kt`](file:///c:/Users/Entah~/Desktop/mlti.ag2/app/src/main/java/com/omnimail/app/ui/screens/accounts/AccountManagementScreen.kt) | Konfigurasi HTTP, HTTPS, dan SOCKS5 proxy khusus per akun untuk rotasi IP. |
| **FR 2.1 Unified Inbox** | [`UnifiedInboxScreen.kt`](file:///c:/Users/Entah~/Desktop/mlti.ag2/app/src/main/java/com/omnimail/app/ui/screens/inbox/UnifiedInboxScreen.kt) | Kotak masuk terpadu seluruh akun dengan badge warna identitas akun & filter dropdown header. |
| **FR 2.2 Threading & Conversation View** | [`EmailDetailScreen.kt`](file:///c:/Users/Entah~/Desktop/mlti.ag2/app/src/main/java/com/omnimail/app/ui/screens/detail/EmailDetailScreen.kt) | Pengelompokan percakapan email berdasarkan subject dan thread identifier. |
| **FR 2.3 Bulk Actions** | [`UnifiedInboxScreen.kt`](file:///c:/Users/Entah~/Desktop/mlti.ag2/app/src/main/java/com/omnimail/app/ui/screens/inbox/UnifiedInboxScreen.kt) | Seleksi multi-email via klik lama/checkbox dengan aksi Mark Read, Delete, dan Archive massal. |
| **FR 2.4 Offline Caching & Lazy Loading** | [`UnifiedInboxScreen.kt`](file:///c:/Users/Entah~/Desktop/mlti.ag2/app/src/main/java/com/omnimail/app/ui/screens/inbox/UnifiedInboxScreen.kt) | Cache 50 email lokal dan tombol pemuatan email lebih lama secara on-demand. |
| **FR 3.1 Multi-Sender Selection** | [`ComposeScreen.kt`](file:///c:/Users/Entah~/Desktop/mlti.ag2/app/src/main/java/com/omnimail/app/ui/screens/compose/ComposeScreen.kt) | Dropdown "Dari" (From) yang dapat dicari (*searchable modal*) dari ratusan akun aktif. |
| **FR 3.2 Rich Text Editor** | [`ComposeScreen.kt`](file:///c:/Users/Entah~/Desktop/mlti.ag2/app/src/main/java/com/omnimail/app/ui/screens/compose/ComposeScreen.kt) | Toolbar format: Bold, Italic, Underline, Bullet list, dan Hyperlink. |
| **FR 3.3 Manajemen Lampiran** | [`ComposeScreen.kt`](file:///c:/Users/Entah~/Desktop/mlti.ag2/app/src/main/java/com/omnimail/app/ui/screens/compose/ComposeScreen.kt) | Attachment preview card dan validasi peringatan otomatis jika ukuran > 25MB. |
| **FR 3.4 Signature per Akun** | [`ComposeScreen.kt`](file:///c:/Users/Entah~/Desktop/mlti.ag2/app/src/main/java/com/omnimail/app/ui/screens/compose/ComposeScreen.kt) | Penyisipan tanda tangan otomatis yang berubah menyesuaikan akun pengirim terpilih. |
| **FR 4.1 Global Search** | [`SearchScreen.kt`](file:///c:/Users/Entah~/Desktop/mlti.ag2/app/src/main/java/com/omnimail/app/ui/screens/search/SearchScreen.kt) | Pencarian instan lokal (sender, subject, keyword, body) serta opsi pencarian IMAP server. |
| **FR 5.1 Smart Background Sync (Staggered)** | [`SettingsScreen.kt`](file:///c:/Users/Entah~/Desktop/mlti.ag2/app/src/main/java/com/omnimail/app/ui/screens/settings/SettingsScreen.kt) | Pengaturan sync bertahap (15m, 30m, 1j, manual) & alokasi 5 akun IMAP IDLE push. |
| **4.1 Memory & Cache Management** | [`SettingsScreen.kt`](file:///c:/Users/Entah~/Desktop/mlti.ag2/app/src/main/java/com/omnimail/app/ui/screens/settings/SettingsScreen.kt) | Monitoring memori (target < 500MB) dan toggle fitur Auto-Clean Cache > 30 hari. |
| **4.2 Keamanan & Privasi** | [`SettingsScreen.kt`](file:///c:/Users/Entah~/Desktop/mlti.ag2/app/src/main/java/com/omnimail/app/ui/screens/settings/SettingsScreen.kt) | Indikator enkripsi AES-256 GCM Android Keystore, Zero-Middleware, dan App Lock biometrik. |
| **5. UI/UX Theming** | [`Theme.kt`](file:///c:/Users/Entah~/Desktop/mlti.ag2/app/src/main/java/com/omnimail/app/ui/theme/Theme.kt), [`MainActivity.kt`](file:///c:/Users/Entah~/Desktop/mlti.ag2/app/src/main/java/com/omnimail/app/MainActivity.kt) | Material Design 3, dynamic theme switching antara Dark Mode & Light Mode. |

---

## 🚀 Cara Menjalankan di Android Studio

1. Buka **Android Studio (Hedgehog / Iguana / Jellyfish atau yang lebih baru)**.
2. Pilih **Open** lalu arahkan ke folder project ini (`mlti.ag2`).
3. Tunggu proses **Gradle Sync** selesai.
4. Pilih emulator Android (API level 24 ke atas, disarankan API 33/34) atau perangkat fisik Android.
5. Klik tombol **Run 'app'** (`Shift + F10`).
