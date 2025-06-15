package com.example.procrastimates.services;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.example.procrastimates.models.Poll;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PollScheduler {
    public static void schedulePollChecks(Context context) {
        WorkManager workManager = WorkManager.getInstance(context);

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                PollCheckWorker.class,
                30, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build();
        workManager.enqueueUniquePeriodicWork(
                "poll_check_work",
                ExistingPeriodicWorkPolicy.REPLACE,
                request
        );
    }

    public static class PollCheckWorker extends Worker {
        private PollProcessor processor;

        public PollCheckWorker(@NonNull Context ctx, @NonNull WorkerParameters params) {
            super(ctx, params);
            processor = new PollProcessor();
        }

        @NonNull
        @Override
        public Result doWork() {
            try {
                List<Poll> polls = processor.findActivePollsSync();
                Date now = new Date();
                for (Poll poll : polls) {
                    List<String> members = processor.getCircleMembersSync(poll.getCircleId());
                    int total = members.size();
                    int votes = poll.getVotes() != null ? poll.getVotes().size() : 0;

                    boolean allVoted = votes >= total;
                    boolean expired = poll.getEndTime().toDate().before(now);

                    if (allVoted || expired) {
                        processor.closePollSync(poll);
                    }
                }

                return Result.success();
            } catch (Exception e) {
                // Retry on any failure
                return Result.retry();
            }
        }
    }
}