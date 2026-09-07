package com.example.reclaim.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PendingReportDao {

    @Insert
    long insert(PendingReportEntity entity);

    @Query("SELECT * FROM pending_reports ORDER BY createdAtEpochMs ASC")
    List<PendingReportEntity> getAll();

    @Query("SELECT * FROM pending_reports WHERE userId = :userId ORDER BY createdAtEpochMs ASC")
    List<PendingReportEntity> getByUserId(String userId);

    @Query("SELECT * FROM pending_reports WHERE userId IS NULL OR userId = ''")
    List<PendingReportEntity> getUnassigned();

    /**
     * Gives every guest report to {@code userId}.
     * Returns how many rows changed.
     */
    @Query("UPDATE pending_reports SET userId = :userId WHERE userId IS NULL OR userId = ''")
    int assignUnassigned(String userId);

    @Delete
    void delete(PendingReportEntity entity);

    @Query("SELECT COUNT(*) FROM pending_reports")
    int count();

    @Query("SELECT COUNT(*) FROM pending_reports WHERE userId = :userId")
    int countForUser(String userId);
}
