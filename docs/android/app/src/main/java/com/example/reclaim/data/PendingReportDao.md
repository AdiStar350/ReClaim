```java
@Insert
long insert(PendingReportEntity entity);
```

What that function does: I save a queued report row and return its id.
What the function is responsible for: I am the Room insert for pending_reports.
Where it works: I am called by PendingReportStore.enqueue.
Why I chose to write the code of the function the way its written: Room fills the SQL from @Insert.
More: I use androidx.room.Insert and return the row id.

```java
@Query("SELECT * FROM pending_reports ORDER BY createdAtEpochMs ASC")
List<PendingReportEntity> getAll();
```

What that function does: I load every queued report, oldest first.
What the function is responsible for: I am a full-table read.
Where it works: I did not find an app caller of getAll. Sync uses getByUserId.
Why I chose to write the code of the function the way its written: Oldest first matches send order.
More: I use Room @Query.

```java
@Query("SELECT * FROM pending_reports WHERE userId = :userId ORDER BY createdAtEpochMs ASC")
List<PendingReportEntity> getByUserId(String userId);
```

What that function does: I load queued reports for one user, oldest first.
What the function is responsible for: I give the sync worker this user's queue.
Where it works: I am called by ReportSyncWorker.doWork.
Why I chose to write the code of the function the way its written: I filter by userId so another account's drafts stay put.
More: I use Room @Query with a bind parameter.

```java
@Query("SELECT * FROM pending_reports WHERE userId IS NULL OR userId = ''")
List<PendingReportEntity> getUnassigned();
```

What that function does: I load queued reports that have no owner yet.
What the function is responsible for: I find guest drafts to claim after login.
Where it works: I am called by PendingReportStore.adoptUnassignedAndSync.
Why I chose to write the code of the function the way its written: I treat null and empty as unassigned.
More: I use Room @Query.

```java
@Query("UPDATE pending_reports SET userId = :userId WHERE userId IS NULL OR userId = ''")
int assignUnassigned(String userId);
```

What that function does: I give every guest report to this user and return how many rows changed.
What the function is responsible for: I attach drafts to the new session.
Where it works: I am called by PendingReportStore.adoptUnassignedAndSync and ReportSyncWorker.doWork.
Why I chose to write the code of the function the way its written: One update is safer than looping each row.
More: I use Room @Query UPDATE and return the change count.

```java
@Delete
void delete(PendingReportEntity entity);
```

What that function does: I remove one queued report row.
What the function is responsible for: I drop a row after it is sent or rejected.
Where it works: I am called by ReportSyncWorker.syncOne.
Why I chose to write the code of the function the way its written: Room matches the primary key from the entity.
More: I use androidx.room.Delete.

```java
@Query("SELECT COUNT(*) FROM pending_reports")
int count();
```

What that function does: I return how many queued reports exist.
What the function is responsible for: I am a full-table count.
Where it works: I did not find an app caller of count. Sync uses countForUser.
Why I chose to write the code of the function the way its written: COUNT(*) is enough for a size check.
More: I use Room @Query.

```java
@Query("SELECT COUNT(*) FROM pending_reports WHERE userId = :userId")
int countForUser(String userId);
```

What that function does: I return how many queued reports this user has.
What the function is responsible for: I decide if a send job should run after login.
Where it works: I am called by PendingReportStore.adoptUnassignedAndSync.
Why I chose to write the code of the function the way its written: I count one user so an empty queue does not start work.
More: I use Room @Query with userId.
