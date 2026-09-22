# ReclaimDatabase.java

## Overview
This file is a **Room database** class. Room is Android's library for storing data in a
local SQLite database on the phone using plain Java classes; see Room in
`docs/GLOSSARY.md`. `ReclaimDatabase` is the single entry point to that local database.
It is declared `abstract` — you never write its body; Room generates the real
implementation at compile time from the annotations. This app uses it to hold reports
that were created while offline until they can be sent.

## Concepts used here
- **Room database.** The typed front door to a local SQLite database; see
  `docs/GLOSSARY.md`.
- **`@Database`.** Lists which entity (table) classes belong to this database and its
  version number.
- **DAO accessor.** An abstract method returning a DAO (Data Access Object), the
  interface that holds the queries.
- **Singleton with double-checked locking.** One database instance is built and shared,
  guarded so two threads cannot build two.
- **`volatile`.** Marks the shared field so all threads see the latest value.

## Libraries and imports
- **AndroidX Room (`androidx.room.Database`, `Room`, `RoomDatabase`).**
  `@Database(...)` configures the database; `Room.databaseBuilder(...)` builds it; the
  class extends `RoomDatabase`.
- **Android (`android.content.Context`).** Needed by the builder to reach app storage;
  the application context is used so the database outlives any one screen.
- **AndroidX annotations (`androidx.annotation.NonNull`).** Documents non-null
  parameters and return.

## Fields
- `private static final String DB_NAME = "reclaim.db"` — the file name of the SQLite
  database on disk.
- `private static volatile ReclaimDatabase instance` — the shared, lazily built
  instance; `volatile` so its visibility is correct across threads.

## Class annotation
`@Database(entities = {PendingReportEntity.class}, version = 2, exportSchema = false)`
tells Room this database has one table (from `PendingReportEntity`), is at schema
version 2, and should not export its schema to a file.

## Functions

### `public abstract PendingReportDao pendingReportDao()`
```java
public abstract PendingReportDao pendingReportDao();
```
- **What it does:** returns the DAO used to read and write the pending-reports table.
  It has no body because Room generates one.
- **Where it's called:** by `PendingReportStore` and `ReportSyncWorker` (via
  `getInstance(context).pendingReportDao()`) to run queries.
- **Why it's written this way:** declaring the DAO as an abstract method is how Room
  connects the database to its query interface; the generated code returns a working
  instance.
- **Step by step:** 1) declared abstract; 2) at build time Room implements it; 3)
  callers get a `PendingReportDao` to run inserts, queries, and deletes.

### `public static ReclaimDatabase getInstance(@NonNull Context context)`
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
- **What it does:** returns the one shared database, building it on first use.
- **Where it's called:** `PendingReportStore.enqueue`,
  `PendingReportStore.adoptUnassignedAndSync`, and `ReportSyncWorker.doWork`.
- **Why it's written this way:** it uses the classic "double-checked locking"
  singleton: a quick null check, then a `synchronized` block with a second null check,
  so only one instance is ever built even under concurrent access. It uses the
  application context so the database is not tied to a screen's lifecycle.
  `fallbackToDestructiveMigration()` means that if the schema version changes without a
  written migration, Room rebuilds the table from scratch — acceptable here because the
  table only holds temporary offline reports.
- **Step by step:**
  1. If `instance` already exists, skip to the return.
  2. Otherwise enter a `synchronized` block on the class.
  3. Check `instance` again (another thread may have built it while waiting).
  4. If still null, build the database with the app context, this class, and the file
     name, allowing destructive migration.
  5. Store it and return it.
