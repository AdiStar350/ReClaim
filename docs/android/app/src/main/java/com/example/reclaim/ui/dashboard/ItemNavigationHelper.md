```java
private ItemNavigationHelper() {
}
```

What that function does: I stop anyone from making an instance of this helper.
What the function is responsible for: I keep details navigation as a static method.
Where it works: I am not called. Java uses me when someone tries `new ItemNavigationHelper()`.
Why I chose to write the code of the function the way its written: I made the constructor private so screens call createDetailsIntent.
More: I use a private constructor. The class is final.

```java
public static Intent createDetailsIntent(Context context, Item item) {
    Intent intent = new Intent(context, ItemDetailsActivity.class);
    intent.putExtra(ItemDetailsActivity.EXTRA_ITEM_ID, item.getId());
    intent.putExtra(ItemDetailsActivity.EXTRA_ITEM_TITLE, item.getTitle());
    intent.putExtra(ItemDetailsActivity.EXTRA_ITEM_DESCRIPTION, item.getDescription());
    intent.putExtra(ItemDetailsActivity.EXTRA_ITEM_LOCATION, item.getLocation());
    intent.putExtra(ItemDetailsActivity.EXTRA_ITEM_STATUS, item.getStatus());
    intent.putExtra(ItemDetailsActivity.EXTRA_ITEM_CATEGORY, item.getCategory());
    intent.putExtra(ItemDetailsActivity.EXTRA_ITEM_IMAGE_URL, item.getImageUrl());
    intent.putExtra(ItemDetailsActivity.EXTRA_ITEM_TYPE, item.getType());
    intent.putExtra(ItemDetailsActivity.EXTRA_ITEM_VERIFICATION_QUESTION,
            item.getVerificationQuestion());
    intent.putExtra(ItemDetailsActivity.EXTRA_ITEM_OWNER_ID, item.getOwnerId());
    intent.putExtra(ItemDetailsActivity.EXTRA_ITEM_REPORTED_AT, item.getReportedAt());
    intent.putExtra(ItemDetailsActivity.EXTRA_ITEM_OWNER_NAME, item.getOwnerName());
    intent.putExtra(ItemDetailsActivity.EXTRA_ITEM_OWNER_PROFILE_URL,
            item.getOwnerProfilePictureUrl());
    intent.putExtra(ItemDetailsActivity.EXTRA_HAS_USER_ALREADY_INTERACTED,
            item.hasUserAlreadyInteracted());
    return intent;
}
```

What that function does: I build an Intent that opens item details with the item fields as extras.
What the function is responsible for: I am the shared way to open ItemDetailsActivity.
Where it works: I am called by HomeFragment and SearchFragment.
Why I chose to write the code of the function the way its written: Putting extras here keeps the same keys in one place. Details can show immediately, then refresh from the server.
More: I use ItemDetailsActivity EXTRA_* keys and Item getters.
