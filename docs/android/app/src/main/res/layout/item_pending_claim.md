# item_pending_claim.xml

## Overview
This layout is one pending-claim card: item thumbnail, title, claimant name, their
verification answer, and Approve / Reject buttons. It is inflated once per claim by
`PendingClaimAdapter` through `ItemPendingClaimBinding`. The adapter is used in
`PendingClaimsSheet` (the bottom sheet from `HomeFragment`).

Key idea: an **Adapter** inflates this layout once per list item and a **ViewHolder**
holds its views so the list can scroll smoothly. See `docs/GLOSSARY.md`.

## Concepts used here
- **MaterialCardView** — the rounded card that is the row's root.
- **LinearLayout** — stacks thumbnail/title, answer, and buttons. See `docs/GLOSSARY.md`.
- **Glide** — loads the item photo into the thumbnail. See `docs/GLOSSARY.md`.
- **ViewBinding** — `ItemPendingClaimBinding`. See `docs/GLOSSARY.md`.

## Screen structure

```
MaterialCardView (root)
└── LinearLayout (vertical)
    ├── View (accent bar — no id)
    └── LinearLayout (padded content)
        ├── LinearLayout (horizontal): thumbnail card + title/claimant
        ├── MaterialTextView text_validation_answer
        └── LinearLayout (horizontal): btn_reject_claim + btn_approve_claim
```

The root is a `MaterialCardView` because each claim is a self-contained card with
corners, elevation, and a stroke.

## Components

### Thumbnail and titles
```xml
<com.google.android.material.card.MaterialCardView ...>
    <ImageView android:id="@+id/image_item_thumbnail" android:scaleType="centerCrop" ... />
</com.google.android.material.card.MaterialCardView>

<MaterialTextView android:id="@+id/text_item_title" android:maxLines="2" ... />
<MaterialTextView android:id="@+id/text_claimant" ... />
```
- **What it shows:** a square photo of the reported item, the item title, and who is
  claiming it.
- **Why it's set up this way:** a nested card gives the thumbnail rounded corners; the
  title uses `maxLines="2"` so long names don't blow up the row height.
- **How the code uses it:** the adapter loads the image with Glide into
  `binding.imageItemThumbnail`, and sets `binding.textItemTitle` and
  `binding.textClaimant` from `PendingClaimResponse`.

### Validation answer
```xml
<com.google.android.material.textview.MaterialTextView
    android:id="@+id/text_validation_answer"
    android:textAppearance="?attr/textAppearanceBodyMedium"
    android:textColor="@color/brand_slate" />
```
- **What it shows:** the claimant's answer to the owner's verification question.
- **Why it's set up this way:** slate body text sits under the header so the owner can
  read the answer before deciding.
- **How the code uses it:** the adapter sets `binding.textValidationAnswer` with
  `R.string.label_claim_answer` and the answer string.

### Approve / Reject buttons
```xml
<com.google.android.material.button.MaterialButton
    android:id="@+id/btn_reject_claim"
    style="@style/Widget.Material3.Button.OutlinedButton"
    android:text="@string/btn_reject_claim" />

<com.google.android.material.button.MaterialButton
    android:id="@+id/btn_approve_claim"
    style="@style/Widget.ReClaim.Button.Primary"
    android:text="@string/btn_approve_claim" />
```
- **What it shows:** side-by-side Reject (outlined, lost-red stroke) and Approve
  (primary filled) actions.
- **Why it's set up this way:** equal `layout_weight="1"` keeps the buttons balanced;
  outlined vs primary makes Approve the stronger choice.
- **How the code uses it:** the adapter wires
  `binding.btnApproveClaim` / `binding.btnRejectClaim` to the
  `ReviewListener`, and disables both while a review request is in flight
  (`setBusyClaimId`).
