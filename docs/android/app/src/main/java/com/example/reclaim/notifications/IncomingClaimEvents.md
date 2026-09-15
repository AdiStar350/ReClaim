```java
private IncomingClaimEvents() {
}
```

What that function does: I stop anyone from making an instance of this helper.
What the function is responsible for: I keep the pending-claim signal static.
Where it works: I am not called. Java uses me when someone tries `new IncomingClaimEvents()`.
Why I chose to write the code of the function the way its written: I made the constructor private so HomeFragment observes live().
More: I use a private constructor. The class is final.

```java
public static LiveData<Long> live() {
    return SIGNAL;
}
```

What that function does: I return the shared CLAIM_PENDING clock.
What the function is responsible for: I let home observe incoming claim pushes.
Where it works: I am observed by HomeFragment.
Why I chose to write the code of the function the way its written: I return LiveData so only this class can post.
More: I use MutableLiveData SIGNAL.

```java
public static void notifyReceived() {
    SIGNAL.postValue(System.currentTimeMillis());
}
```

What that function does: I tick that a CLAIM_PENDING push arrived.
What the function is responsible for: I wake home so the claim card can show without waiting for onResume.
Where it works: I am called by ReclaimMessagingService.onMessageReceived.
Why I chose to write the code of the function the way its written: postValue is safe from Firebase's background thread.
More: I use LiveData.postValue with the current time.
