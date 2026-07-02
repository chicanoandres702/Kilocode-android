package com.kilocode.android.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.kilocode.android.ui.util.BranchManager

class BranchWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val issueNumber = inputData.getInt(EXTRA_ISSUE_NUMBER, 0)
        val featureTitle = inputData.getString(EXTRA_FEATURE_TITLE) ?: return Result.failure()
        
        // In a real implementation, we would call the backend to create the branch.
        // For now, we simulate the branch creation.
        val branchName = BranchManager.generateBranchName("feature", issueNumber, featureTitle)
        
        // TODO: Call API to create branch
        
        return Result.success()
    }

    companion object {
        const val EXTRA_ISSUE_NUMBER = "issue_number"
        const val EXTRA_FEATURE_TITLE = "feature_title"
        
        fun enqueue(context: Context, issueNumber: Int, featureTitle: String) {
            val data = workDataOf(
                EXTRA_ISSUE_NUMBER to issueNumber,
                EXTRA_FEATURE_TITLE to featureTitle
            )
            val request = OneTimeWorkRequestBuilder<BranchWorker>()
                .setInputData(data)
                .addTag("branch_creation")
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
