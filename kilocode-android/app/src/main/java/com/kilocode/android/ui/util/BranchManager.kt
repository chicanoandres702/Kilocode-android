package com.kilocode.android.ui.util

object BranchManager {
    fun generateBranchName(featureName: String, issueNumber: Int, issueTitle: String): String {
        val sanitizedFeatureName = featureName.replace(" ", "-").lowercase()
        val sanitizedIssueTitle = issueTitle.replace(" ", "-").lowercase()
        return "$sanitizedFeatureName/$issueNumber-$sanitizedIssueTitle"
    }
}
