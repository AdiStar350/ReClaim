package com.example.reclaim.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {PendingReportEntity.class}, version = 1, exportSchema = false)
public abstract class ReclaimDatabase extends RoomDatabase {

    private static final String DB_NAME = "reclaim.db";
    private static volatile ReclaimDatabase instance;

    public abstract PendingReportDao pendingReportDao();

    @NonNull
    public static ReclaimDatabase getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (ReclaimDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    ReclaimDatabase.class,
                                    DB_NAME)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }
}
