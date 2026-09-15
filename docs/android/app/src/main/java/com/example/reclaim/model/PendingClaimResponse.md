```java
public String getClaimId() {
    return claimId;
}
```

What that function does: I return the claim id.
What the function is responsible for: I give review and contact calls the claim to act on.
Where it works: I am read by PendingClaimsActivity, PendingClaimsSheet, and PendingClaimAdapter.
Why I chose to write the code of the function the way its written: The server uses claimId, not id, so I kept that name.
More: I use Gson @SerializedName("claimId").

```java
public String getItemId() {
    return itemId;
}
```

What that function does: I return the item this claim is about.
What the function is responsible for: I let the UI know which report was claimed.
Where it works: I am used on pending-claim screens with the rest of this object.
Why I chose to write the code of the function the way its written: I kept a simple getter.
More: I use Gson @SerializedName("itemId").

```java
public String getItemTitle() {
    return itemTitle;
}
```

What that function does: I return the item title.
What the function is responsible for: I show which report the claim is for.
Where it works: I am read by PendingClaimAdapter.bind.
Why I chose to write the code of the function the way its written: I keep the title on the claim so the list does not need a second fetch.
More: I use Gson @SerializedName("itemTitle").

```java
public String getItemImageUrl() {
    return itemImageUrl;
}
```

What that function does: I return the item photo URL.
What the function is responsible for: I show a thumbnail on the pending-claim row.
Where it works: I am read by PendingClaimAdapter.bind.
Why I chose to write the code of the function the way its written: I keep the image URL on the claim for the list.
More: I use Gson @SerializedName("itemImageUrl").

```java
public String getClaimantId() {
    return claimantId;
}
```

What that function does: I return who made the claim.
What the function is responsible for: I identify the claimant.
Where it works: Gson fills this field. The row shows claimantName instead.
Why I chose to write the code of the function the way its written: I kept the id even if the UI mostly shows the name.
More: I use Gson @SerializedName("claimantId").

```java
public String getClaimantName() {
    return claimantName;
}
```

What that function does: I return the claimant's display name.
What the function is responsible for: I show who is claiming on the pending-claim row.
Where it works: I am read by PendingClaimAdapter.bind.
Why I chose to write the code of the function the way its written: A name is easier to show than an id.
More: I use Gson @SerializedName("claimantName").

```java
public String getValidationAnswer() {
    return validationAnswer;
}
```

What that function does: I return the answer to the check question.
What the function is responsible for: I let the owner judge the claim.
Where it works: I am read by PendingClaimAdapter.bind and the review dialogs.
Why I chose to write the code of the function the way its written: I keep the answer on this object so the owner can read it in the list.
More: I use Gson @SerializedName("validationAnswer").

```java
public String getStatus() {
    return status;
}
```

What that function does: I return the claim status.
What the function is responsible for: I tell the UI if the claim is still pending.
Where it works: I am used on pending-claim screens with this object.
Why I chose to write the code of the function the way its written: I kept a simple getter.
More: I use Gson @SerializedName("status").

```java
public String getVerificationQuestion() {
    return verificationQuestion;
}
```

What that function does: I return the check question that was asked.
What the function is responsible for: I show the question next to the answer.
Where it works: I am read by PendingClaimAdapter.bind and the review dialogs.
Why I chose to write the code of the function the way its written: I keep question and answer together so the owner can compare them.
More: I use Gson @SerializedName("verificationQuestion").
