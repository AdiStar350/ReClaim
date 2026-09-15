```java
public abstract PendingReportDao pendingReportDao();
```

What that function does: I return the Room DAO for queued reports.
What the function is responsible for: I am the access point to pending_reports.
Where it works: I am called by PendingReportStore and ReportSyncWorker through getInstance.
Why I chose to write the code of the function the way its written: Room implements this abstract method.
More: I use @Database entities PendingReportEntity, version 2.

```java
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
```

What that function does: I build one Room database the first time, then reuse it.
What the function is responsible for: I am the shared reclaim.db handle.
Where it works: I am called by PendingReportStore and ReportSyncWorker.
Why I chose to write the code of the function the way its written: Double-checked locking with volatile instance avoids two databases. Destructive migration is fine for a local queue.
More: I use Room.databaseBuilder, applicationContext, and fallbackToDestructiveMigration.
