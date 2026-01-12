package com.example.birthdayapp;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class BirthdayWorker extends Worker {
    public BirthdayWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull @Override
    public Result doWork() {
        Log.d("MY_APP", "сканування завершено!");
        return Result.success();
    }
}