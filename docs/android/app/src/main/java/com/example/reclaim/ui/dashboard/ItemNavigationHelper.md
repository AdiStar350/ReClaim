# ItemNavigationHelper.java

## Overview
This is a small **helper class** — a class of static methods with no state, grouped for reuse (see `docs/GLOSSARY.md`). Its one job is to build the `Intent` that opens `ItemDetailsActivity` for a given item, copying all of the item's fields into the intent as extras. Several screens (home, search, match lists) reuse it so the details screen always opens the same way.

## Concepts used here
- **Helper class** — static methods, private constructor, no instances. See `docs/GLOSSARY.md`.
- **Intent + extras** — a message that starts another Activity and carries data as key/value "extras". See `docs/GLOSSARY.md`.

## Libraries and imports
- **Android framework**: `Context` is needed to build an `Intent`; `Intent` is the message that opens the details screen.
- **Project classes**: `Item` (the data object being opened) and `ItemDetailsActivity` (the target screen, whose `EXTRA_*` keys are used).

## Functions

### `public static Intent createDetailsIntent(Context context, Item item)`
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
- **What it does:** Creates and returns an `Intent` aimed at `ItemDetailsActivity`, with every field of the `item` attached as an extra so the details screen can show the item right away.
- **Where it's called:** `HomeFragment` (nearby preview taps, targeted-found card, and each match row) and `SearchFragment` (list taps and map marker taps). Each caller passes the intent to `startActivity(...)`.
- **Why it's written this way:** Centralizing the intent-building means the extras are always packed the same way in one place, so the details screen never receives a partially filled intent. Passing the fields as extras lets the details screen render instantly before it re-fetches the latest copy from the server.
- **Step by step:**
  1. Create an `Intent` from the given `context` to `ItemDetailsActivity`.
  2. Put each item field under the matching `EXTRA_*` key (id, title, description, location, status, category, image URL, type, verification question, owner id, reported-at, owner name, owner photo, and the "already interacted" flag).
  3. Return the ready-to-start intent.
