```java
public ClaimRequest(String itemId, String validationAnswer) {
    this.itemId = itemId;
    this.validationAnswer = validationAnswer;
}
```

What that function does: I store the item id and the answer to the check question.
What the function is responsible for: I am the JSON body for POST /api/claims.
Where it works: I am built in ItemDetailsActivity and passed to ReClaimApiService.submitClaim.
Why I chose to write the code of the function the way its written: I kept the two fields the claim route needs.
More: I use Gson @SerializedName on itemId and validationAnswer.

```java
public String getItemId() {
    return itemId;
}
```

What that function does: I return the item id I was built with.
What the function is responsible for: I let callers read the item id.
Where it works: I did not find an app caller of this getter. Gson reads the field.
Why I chose to write the code of the function the way its written: I kept a simple getter next to the field.
More: I return the final itemId field.

```java
public String getValidationAnswer() {
    return validationAnswer;
}
```

What that function does: I return the check-question answer I was built with.
What the function is responsible for: I let callers read the answer.
Where it works: I did not find an app caller of this getter. Gson reads the field.
Why I chose to write the code of the function the way its written: I kept a simple getter next to the field.
More: I return the final validationAnswer field.
