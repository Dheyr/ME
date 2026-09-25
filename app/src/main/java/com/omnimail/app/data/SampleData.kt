package com.omnimail.app.data

import com.omnimail.app.model.*

object SampleData {
    // Zero dummy data: Starts clean for real multi-account login
    val sampleGroups: List<WorkspaceGroup> = OmniStorage.defaultGroups
    val sampleAccounts: List<EmailAccount> = emptyList()
    val sampleEmails: List<EmailMessage> = emptyList()
}
