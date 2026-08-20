package com.example.reclaim;

import android.app.Application;

import com.example.reclaim.notifications.AppNotifications;
import com.example.reclaim.notifications.FcmTokenRegistrar;
import com.example.reclaim.sync.NetworkMonitor;
import com.example.reclaim.sync.ReportSyncScheduler;

/**
 * Application entry point. Creates notification channels, registers
 * connectivity monitoring, schedules background sync of locally cached
 * reports, and keeps the FCM token registered with the backend.
 */
public class ReclaimApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AppNotifications.createChannels(this);
        NetworkMonitor.register(this);
        ReportSyncScheduler.enqueue(this);
        FcmTokenRegistrar.register(this);
    }
}
