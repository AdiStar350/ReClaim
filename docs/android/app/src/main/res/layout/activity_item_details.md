# activity_item_details.xml

## Overview
This layout is the item-details screen: a large photo header that collapses as you
scroll, then cards for the description, location/date, and who reported it, plus a
pinned "Claim" button at the bottom. It is inflated by `ItemDetailsActivity` through
`ActivityItemDetailsBinding`.

Key idea: this screen uses a **collapsing toolbar** — the photo shrinks into a normal
app bar as the content scrolls up. See `docs/GLOSSARY.md`.

## Concepts used here
- **CoordinatorLayout** — coordinates the scrolling header with the content and the
  pinned button. See `docs/GLOSSARY.md`.
- **AppBarLayout + CollapsingToolbarLayout + MaterialToolbar** — the shrinking photo
  header with a back arrow. See `docs/GLOSSARY.md`.
- **NestedScrollView** — the scrolling body under the header. See `docs/GLOSSARY.md`.
- **MaterialCardView** — rounded cards for grouped info. See `docs/GLOSSARY.md`.
- **Chip** — small pills for status and category. See `docs/GLOSSARY.md`.
- **MaterialDivider** — a thin line between rows in a card. See `docs/GLOSSARY.md`.
- **Material Button** — the bottom "Claim" button. See `docs/GLOSSARY.md`.
- **ViewBinding** — `ActivityItemDetailsBinding`. See `docs/GLOSSARY.md`.

## Screen structure
The root `CoordinatorLayout` wires three pieces together: the collapsing header, the
scrolling body, and the bottom button.

```
CoordinatorLayout (root)
├── AppBarLayout
│   └── CollapsingToolbarLayout (@dimen height, scroll|exitUntilCollapsed)
│       ├── FrameLayout (parallax): image_item + dark scrim View
│       └── MaterialToolbar (toolbar_item_details, pinned back arrow)
├── NestedScrollView (nested_scroll_content, layout_behavior = appbar scrolling)
│   └── LinearLayout (vertical)
│       ├── text_item_title
│       ├── chips row: chip_status + chip_category
│       ├── Card: description (label + text_description)
│       ├── Card: location row + divider + date-found row
│       ├── "Reported by" label
│       └── Card: reporter avatar + text_reported_by
└── FrameLayout (bottom_claim_container): btn_claim
```

A `CoordinatorLayout` is required because `AppBarLayout`, the scrolling body, and the
collapsing behavior only work together inside it.

## Components

### Collapsing photo header
```xml
<com.google.android.material.appbar.AppBarLayout android:id="@+id/app_bar_layout">
    <com.google.android.material.appbar.CollapsingToolbarLayout
        android:id="@+id/collapsing_toolbar"
        app:layout_scrollFlags="scroll|exitUntilCollapsed"
        app:titleCollapseMode="scale">

        <FrameLayout app:layout_collapseMode="parallax">
            <ImageView android:id="@+id/image_item" android:scaleType="centerCrop" ... />
            <View android:background="@drawable/bg_item_image_scrim" />
        </FrameLayout>

        <com.google.android.material.appbar.MaterialToolbar
            android:id="@+id/toolbar_item_details"
            app:layout_collapseMode="pin"
            app:navigationIcon="@drawable/ic_arrow_back_24" />
    </com.google.android.material.appbar.CollapsingToolbarLayout>
</com.google.android.material.appbar.AppBarLayout>
```
- **What it shows:** the item photo filling the header, darkened at the edges by a scrim
  so the back arrow and title stay readable; as you scroll up it collapses to a normal
  toolbar.
- **Why it's set up this way:** `scroll|exitUntilCollapsed` lets the header scroll away
  until only the toolbar remains; `layout_collapseMode="parallax"` makes the image drift
  slowly for a nice effect, while `pin` keeps the toolbar fixed on top. The scrim `View`
  is a gradient overlay so white icons don't get lost on a bright photo.
- **How the code uses it:** `ItemDetailsActivity` sets `binding.toolbarItemDetails` as
  the support action bar (for the back arrow), disables the collapsing title with
  `binding.collapsingToolbar.setTitleEnabled(false)`, and loads the item photo into
  `binding.imageItem` with Glide.

### Title and status/category chips
```xml
<com.google.android.material.textview.MaterialTextView android:id="@+id/text_item_title" ... />

<LinearLayout android:orientation="horizontal">
    <com.google.android.material.chip.Chip android:id="@+id/chip_status" ... />
    <com.google.android.material.chip.Chip android:id="@+id/chip_category" ... />
</LinearLayout>
```
- **What it shows:** the item's title, then a row with a status pill (Open/Closed) and a
  category pill.
- **Why it's set up this way:** chips are the Material way to show short labels; a
  horizontal `LinearLayout` keeps them side by side.
- **How the code uses it:** the code sets `binding.textItemTitle`, `binding.chipStatus`
  (text and open/closed styling), and `binding.chipCategory` from the item data.

### Description card
```xml
<com.google.android.material.card.MaterialCardView style="@style/Widget.ReClaim.Card">
    <LinearLayout android:orientation="vertical">
        <com.google.android.material.textview.MaterialTextView android:id="@+id/label_description" ... />
        <com.google.android.material.textview.MaterialTextView android:id="@+id/text_description" ... />
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```
- **What it shows:** a card with a "Description" label and the item's description text.
- **Why it's set up this way:** grouping the label and text in a rounded card visually
  separates this section from the others.
- **How the code uses it:** `binding.textDescription` is set from the item; the label is
  static.

### Location + date-found card
```xml
<com.google.android.material.card.MaterialCardView style="@style/Widget.ReClaim.Card">
    <LinearLayout android:orientation="vertical" android:padding="@dimen/space_18">
        <!-- location row: map icon + label + text_location -->
        <com.google.android.material.textview.MaterialTextView android:id="@+id/text_location" ... />
        <com.google.android.material.divider.MaterialDivider ... />
        <!-- date row: calendar icon + label + text_date_found -->
        <com.google.android.material.textview.MaterialTextView android:id="@+id/text_date_found" ... />
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```
- **What it shows:** one card with two rows — where the item was found and when — split
  by a thin divider, each row having an icon, a small label, and a value.
- **Why it's set up this way:** two related facts share one card to keep the screen
  tidy; the `MaterialDivider` separates them without the weight of a second card. Each
  row is a horizontal `LinearLayout` (icon + a vertical label/value stack that uses
  `layout_weight="1"` to take the remaining width).
- **How the code uses it:** the code sets `binding.textLocation` and
  `binding.textDateFound` (the date is formatted first). The labels/icons are static.

### Reporter card
```xml
<com.google.android.material.card.MaterialCardView android:id="@+id/card_reporter" ...>
    <LinearLayout android:orientation="horizontal">
        <com.google.android.material.card.MaterialCardView> <!-- round avatar -->
            <ImageView android:id="@+id/image_reporter_avatar" ... />
        </com.google.android.material.card.MaterialCardView>
        <com.google.android.material.textview.MaterialTextView android:id="@+id/text_reported_by" ... />
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```
- **What it shows:** a card with the reporter's round avatar and their name.
- **Why it's set up this way:** the avatar is a `MaterialCardView` with a half-size
  corner radius to make the square photo look circular; the name uses `layout_weight="1"`
  so it fills the rest of the row.
- **How the code uses it:** the code loads the reporter photo into
  `binding.imageReporterAvatar` and sets `binding.textReportedBy` with the name.

### Pinned "Claim" button
```xml
<FrameLayout
    android:id="@+id/bottom_claim_container"
    android:layout_gravity="bottom"
    android:elevation="@dimen/space_10">
    <com.google.android.material.button.MaterialButton
        android:id="@+id/btn_claim"
        style="@style/Widget.ReClaim.Button.Primary"
        android:text="@string/btn_claim" />
</FrameLayout>
```
- **What it shows:** a full-width action button pinned to the bottom of the screen (over
  the scrolling content).
- **Why it's set up this way:** wrapping the button in a `FrameLayout` with
  `layout_gravity="bottom"` and elevation keeps it fixed and visible while the body
  scrolls, so the main action is always reachable.
- **How the code uses it:** the code shows/hides `binding.bottomClaimContainer` and sets
  `binding.btnClaim`'s text, enabled state, and click action depending on the situation
  (claim this item, "I found this", already claimed, or submitting). Tapping it opens the
  claim dialog (see `dialog_claim_verification.xml`).
