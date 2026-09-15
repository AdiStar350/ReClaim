```java
private BackendAvailability() {
}
```

What that function does: I stop anyone from making an instance of this helper.
What the function is responsible for: I keep reachability as static state.
Where it works: I am not called. Java uses me when someone tries `new BackendAvailability()`.
Why I chose to write the code of the function the way its written: I made the constructor private so one flag is shared.
More: I use a private constructor. The class is final.

```java
public static boolean isReachable() {
    return reachable;
}
```

What that function does: I return whether the last API call reached the server.
What the function is responsible for: I am the read for the offline dialog and ConnectionToasts.
Where it works: I am called by OfflineProtocol.showIfNeeded and NetworkMonitor.isBackendAvailable.
Why I chose to write the code of the function the way its written: A volatile boolean is enough for a last-known flag.
More: I return the static reachable field.

```java
public static void markReachable() {
    reachable = true;
    OfflineProtocol.resetOutageAck();
}
```

What that function does: I mark the API as up and clear the "user already dismissed outage" flag.
What the function is responsible for: I restore online state after a good reply.
Where it works: I am called by RetrofitClient.interceptBackendAvailability on a reachable status.
Why I chose to write the code of the function the way its written: I reset the outage ack so a later outage can show the dialog again.
More: I set reachable and call OfflineProtocol.resetOutageAck.

```java
public static void markUnreachable() {
    reachable = false;
}
```

What that function does: I mark the API as down.
What the function is responsible for: I store that the last call did not reach the server.
Where it works: I am called by RetrofitClient.interceptBackendAvailability on 502-style codes or IOException.
Why I chose to write the code of the function the way its written: I only flip the flag. The interceptor opens the dialog.
More: I set reachable to false.

```java
public static boolean isUnreachableStatus(int code) {
    return code == 502 || code == 503 || code == 504
            || code == 521 || code == 522 || code == 523;
}
```

What that function does: I treat gateway and tunnel timeout codes as "API did not answer".
What the function is responsible for: I tell the interceptor when HTTP finished but the backend was still down.
Where it works: I am called by RetrofitClient.interceptBackendAvailability.
Why I chose to write the code of the function the way its written: 521-523 are ngrok/Cloudflare down codes I have seen on the tunnel.
More: I compare the status int only.
