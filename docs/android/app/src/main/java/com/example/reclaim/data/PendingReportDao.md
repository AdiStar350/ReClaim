# PendingReportDao.java

## Overview
This file is a **Room DAO (Data Access Object)**: an interface where each method is one
database operation (insert, query, update, delete). You write the method signature plus
an annotation with the SQL, and Room generates the code that runs it; see Room in
`docs/GLOSSARY.md`. This DAO manages the `pending_reports` table — the offline reports
waiting to be sent.

## Concepts used here
- **Room DAO.** An interface of database operations; Room writes the implementation;
  see `docs/GLOSSARY.md`.
- **`@Dao`.** Marks the interface as a DAO.
- **`@Insert` / `@Delete`.** Generate insert and delete statements from the entity
  argument, no SQL needed.
- **`@Query`.** Runs the SQL string you provide; `:name` in the SQL binds to the
  method argument of the same name.
- **Blocking calls.** These methods return plain values (not `LiveData`), so they must
  be called off the main thread (the callers use a background executor or a Worker).

## Libraries and imports
- **AndroidX Room (`androidx.room.Dao`, `Insert`, `Delete`, `Query`).** The annotations
  that define each operation.
- **Java standard (`java.util.List`).** Return type for the "get many rows" queries.

## Functions
This is an interface, so there are no bodies. Each method is one database operation.

### `long insert(PendingReportEntity entity)`
```java
@Insert
long insert(PendingReportEntity entity);
```
- **What it does:** inserts one report row and returns its new auto-generated id.
- **Where it's called:** `PendingReportStore.enqueue` when saving a report that could
  not be sent online.
- **Why it's written this way:** `@Insert` needs no SQL; returning the new `long` id
  confirms the row was stored.

### `List<PendingReportEntity> getAll()`
```java
@Query("SELECT * FROM pending_reports ORDER BY createdAtEpochMs ASC")
List<PendingReportEntity> getAll();
```
- **What it does:** returns every queued report, oldest first.
- **Where it's called:** not called from app code right now; it is a general-purpose
  read kept for completeness/debugging.
- **Why it's written this way:** ordering by creation time keeps reports sent in the
  order they were made.

### `List<PendingReportEntity> getByUserId(String userId)`
```java
@Query("SELECT * FROM pending_reports WHERE userId = :userId ORDER BY createdAtEpochMs ASC")
List<PendingReportEntity> getByUserId(String userId);
```
- **What it does:** returns queued reports that belong to one user, oldest first.
- **Where it's called:** `ReportSyncWorker.doWork`, to sync only the signed-in user's
  reports.
- **Why it's written this way:** the `:userId` bind keeps the query safe and reusable;
  filtering by owner avoids sending someone else's queued report.

### `List<PendingReportEntity> getUnassigned()`
```java
@Query("SELECT * FROM pending_reports WHERE userId IS NULL OR userId = ''")
List<PendingReportEntity> getUnassigned();
```
- **What it does:** returns reports created before login (no owner yet).
- **Where it's called:** `PendingReportStore.adoptUnassignedAndSync`, to find guest
  reports to hand to the user who just signed in.
- **Why it's written this way:** guest reports are stored with an empty/null `userId`;
  this query finds them so they can be adopted.

### `int assignUnassigned(String userId)`
```java
@Query("UPDATE pending_reports SET userId = :userId WHERE userId IS NULL OR userId = ''")
int assignUnassigned(String userId);
```
- **What it does:** sets the owner on all guest reports and returns how many rows
  changed.
- **Where it's called:** `PendingReportStore.adoptUnassignedAndSync` and
  `ReportSyncWorker.doWork`.
- **Why it's written this way:** one bulk `UPDATE` claims all guest reports at once
  when the user logs in, so they get synced under the right account.

### `void delete(PendingReportEntity entity)`
```java
@Delete
void delete(PendingReportEntity entity);
```
- **What it does:** deletes one report row.
- **Where it's called:** `ReportSyncWorker.syncOne` after a report is successfully sent
  (or permanently rejected), so it is not retried forever.
- **Why it's written this way:** `@Delete` matches the row by primary key from the
  passed entity; no SQL needed.

### `int count()`
```java
@Query("SELECT COUNT(*) FROM pending_reports")
int count();
```
- **What it does:** returns the total number of queued reports.
- **Where it's called:** not called from app code right now; a general count kept for
  completeness/debugging.
- **Why it's written this way:** a lightweight count without loading rows.

### `int countForUser(String userId)`
```java
@Query("SELECT COUNT(*) FROM pending_reports WHERE userId = :userId")
int countForUser(String userId);
```
- **What it does:** returns how many queued reports belong to one user.
- **Where it's called:** `PendingReportStore.adoptUnassignedAndSync`, to decide whether
  a sync is worth scheduling.
- **Why it's written this way:** counting first avoids scheduling background work when
  there is nothing to send.
