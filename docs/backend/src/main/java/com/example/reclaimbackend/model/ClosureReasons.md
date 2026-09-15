```java
private ClosureReasons() {
}
```

What that function does: I stop anyone from making an instance of this class. It only holds shared reason strings.
What the function is responsible for: I am a private constructor so the class is used as a bag of constants.
Where it works: I am unused except as the class constructor. CloseReportRequest uses OWNER_INITIATED_PATTERN. ClaimService uses OWNER_FOUND, CLAIM_APPROVED, and CLAIM_DECLINED.
Why I chose to write the code of the function the way its written: I left the body empty. A final class plus this constructor is enough to keep it from being built.
More: I do not import libraries here. The constants sit on the class so closing a report and reviewing a claim use the same words.
