package com.example.reclaim.sync;

/**
 * Whether the API last answered.
 * The device can have internet while the server is down.
 */
public final class BackendAvailability {

    private static volatile boolean reachable = true;

    private BackendAvailability() {
    }

    public static boolean isReachable() {
        return reachable;
    }

    public static void markReachable() {
        reachable = true;
        OfflineProtocol.resetOutageAck();
    }

    public static void markUnreachable() {
        reachable = false;
    }

    /**
     * Status codes that mean the API did not answer,
     * even though HTTP finished.
     */
    public static boolean isUnreachableStatus(int code) {
        return code == 502 || code == 503 || code == 504
                || code == 521 || code == 522 || code == 523;
    }
}
