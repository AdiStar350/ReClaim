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

    @Delete
    void delete(PendingReportEntity entity);

    @Query("SELECT COUNT(*) FROM pending_reports")
    int count();
}
