package com.example.reclaim.ui.dashboard;

import android.content.Context;
import android.content.Intent;

import com.example.reclaim.model.Item;
import com.example.reclaim.ui.details.ItemDetailsActivity;

/**
 * Opens the item details screen.
 */
public final class ItemNavigationHelper {

    private ItemNavigationHelper() {
    }

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
}
