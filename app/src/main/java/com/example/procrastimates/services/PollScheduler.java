package com.example.procrastimates.services;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.procrastimates.MyApplication;
import com.example.procrastimates.models.Poll;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class PollScheduler {
    public static void schedulePollChecks() {
        // Create a worker that runs periodically to check polls
        WorkManager workManager = WorkManager.getInstance(MyApplication.getAppContext());

        PeriodicWorkRequest pollCheckRequest = new PeriodicWorkRequest.Builder(
                PollCheckWorker.class,
                30, TimeUnit.MINUTES)
                .build();

        workManager.enqueueUniquePeriodicWork(
                "poll_check_work",
                ExistingPeriodicWorkPolicy.REPLACE,
                pollCheckRequest);
    }

    public static class PollCheckWorker extends Worker {
        private PollProcessor pollProcessor;

        public PollCheckWorker(@NonNull Context context, @NonNull WorkerParameters params) {
            super(context, params);
            pollProcessor = new PollProcessor();
        }

        @NonNull
        @Override
        public Result doWork() {
            checkAndClosePendingPolls();
            return Result.success();
        }

        private void checkAndClosePendingPolls() {
            pollProcessor.findActivePolls(polls -> {
                for (Poll poll : polls) {
                    pollProcessor.getCircleMembers(poll.getCircleId(), members -> {
                        int totalMembers = members.size();
                        int votesSoFar = poll.getVotes() != null ? poll.getVotes().size() : 0;

                        boolean allVoted = votesSoFar >= totalMembers;
                        boolean pollExpired = poll.getEndTime().toDate().before(new Date());

                        if (allVoted || pollExpired) {
                            pollProcessor.closePoll(poll);
                        }
                    });
                }
            });
        }
    }
}
