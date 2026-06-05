package com.example.reclaim.ui.dashboard;

import android.content.Context;
import android.content.Intent;

import com.example.reclaim.model.Item;
import com.example.reclaim.ui.details.ItemDetailsActivity;

/**
 * Builds navigation intents for the item details screen.
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
        return intent;
    }
}
