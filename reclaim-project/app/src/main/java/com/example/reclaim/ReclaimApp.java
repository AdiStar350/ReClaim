package com.example.reclaim;

import android.app.Application;

import com.example.reclaim.notifications.AppNotifications;
import com.example.reclaim.notifications.FcmTokenRegistrar;
import com.example.reclaim.sync.NetworkMonitor;
import com.example.reclaim.sync.OfflineProtocol;
import com.example.reclaim.sync.ReportSyncScheduler;

/**
 * App start. Sets up channels, network watch, report sync, and the push token.
 */
public class ReclaimApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AppNotifications.createChannels(this);
        OfflineProtocol.attach(this);
        NetworkMonitor.register(this);
        ReportSyncScheduler.enqueue(this);
        FcmTokenRegistrar.register(this);
    }
}
