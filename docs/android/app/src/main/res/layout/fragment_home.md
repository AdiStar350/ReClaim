# fragment_home.xml

## Overview
This layout is the Home tab: a greeting header with avatar, a few optional highlight
cards (a claim decision, a targeted found item, and pending claims), a horizontal
"Nearby" list, and a "Possible matches" section. It is inflated by `HomeFragment`
through `FragmentHomeBinding` and shown inside the dashboard's fragment container.

Key idea: a **Fragment** is a page inside an Activity; this one shows a scrolling
dashboard of cards and lists. See `docs/GLOSSARY.md`.

## Concepts used here
- **CoordinatorLayout** — the root container for this scrolling page. See `docs/GLOSSARY.md`.
- **NestedScrollView** — makes the whole page scroll. See `docs/GLOSSARY.md`.
- **LinearLayout / FrameLayout** — stack and overlay the header pieces. See `docs/GLOSSARY.md`.
- **MaterialCardView** — the highlight cards and the round avatar. See `docs/GLOSSARY.md`.
- **RecyclerView** — the horizontal "Nearby" list. See `docs/GLOSSARY.md`.
- **Material Button** — the "Show more" button. See `docs/GLOSSARY.md`.
- **ProgressBar** — the loading spinner. See `docs/GLOSSARY.md`.
- **ViewBinding** — `FragmentHomeBinding`. See `docs/GLOSSARY.md`.

## Screen structure

```
CoordinatorLayout (root)
└── NestedScrollView
    └── LinearLayout (vertical)
        ├── FrameLayout (header: greeting + subtitle + avatar, decorative orb)
        ├── Card card_claim_decision (hidden until there is a decision)
        ├── Card card_targeted_found (hidden unless a found report targets you)
        ├── Card card_pending_claims (hidden unless you have claims to review)
        ├── "Nearby" section title + subtitle
        ├── RecyclerView recycler_nearby (horizontal) + text_nearby_empty
        ├── "Show more" button
        ├── "Possible matches" section title + subtitle
        ├── LinearLayout layout_matches (filled in code) + text_matches_empty
        └── ProgressBar progress_home
```

The root is a `CoordinatorLayout` (a common choice for scrolling dashboard fragments),
with a `NestedScrollView` doing the actual scrolling so it cooperates with the
dashboard's bottom bar.

## Components

### Header (greeting + avatar)
```xml
<FrameLayout android:background="@drawable/bg_dashboard_header">
    <View android:background="@drawable/bg_header_orb" ... /> <!-- decorative -->
    <LinearLayout android:orientation="horizontal">
        <LinearLayout android:layout_weight="1" android:orientation="vertical">
            <com.google.android.material.textview.MaterialTextView android:id="@+id/text_greeting" ... />
            <com.google.android.material.textview.MaterialTextView android:id="@+id/text_dashboard_subtitle" ... />
        </LinearLayout>
        <com.google.android.material.card.MaterialCardView> <!-- round avatar -->
            <ImageView android:id="@+id/image_home_avatar" ... />
        </com.google.android.material.card.MaterialCardView>
    </LinearLayout>
</FrameLayout>
```
- **What it shows:** a colored header with a personalized greeting and subtitle on the
  left and a round profile avatar on the right, plus a soft decorative "orb" behind it.
- **Why it's set up this way:** a `FrameLayout` lets the orb sit behind the text/avatar
  row. The text block uses `layout_weight="1"` to take the remaining width so the avatar
  stays pinned to the right. The avatar is a `MaterialCardView` circle.
- **How the code uses it:** the code sets `binding.textGreeting` with the user's name and
  loads the photo into `binding.imageHomeAvatar` with Glide. The subtitle is static.

### Highlight cards (claim decision / targeted found / pending claims)
```xml
<com.google.android.material.card.MaterialCardView
    android:id="@+id/card_claim_decision"
    android:visibility="gone" ...>
    <!-- icon + title (text_claim_decision_title) + subtitle (text_claim_decision_subtitle) + chevron -->
</com.google.android.material.card.MaterialCardView>

<com.google.android.material.card.MaterialCardView android:id="@+id/card_targeted_found" android:visibility="gone" ...>
    <!-- icon + title + subtitle (text_targeted_found_subtitle) + chevron -->
</com.google.android.material.card.MaterialCardView>

<com.google.android.material.card.MaterialCardView android:id="@+id/card_pending_claims" android:visibility="gone" ...>
    <!-- icon + title + count (text_pending_claims_count) + chevron -->
</com.google.android.material.card.MaterialCardView>
```
- **What it shows:** up to three tappable notice cards — a decision on your claim, a
  found item aimed at your lost report, and a count of claims awaiting your review. Each
  has an icon, a title/subtitle, and a right chevron.
- **Why it's set up this way:** all three start `visibility="gone"` and are turned on
  only when relevant, so the home screen stays clean. Each card is `clickable`/`focusable`
  with a `foreground` ripple so the whole card feels like a button. They share the same
  icon-left / text-middle (`layout_weight="1"`) / chevron-right layout.
- **How the code uses it:** the code shows/hides `binding.cardClaimDecision`,
  `binding.cardTargetedFound`, and `binding.cardPendingClaims`, fills
  `binding.textClaimDecisionTitle`/`textClaimDecisionSubtitle`,
  `binding.textTargetedFoundSubtitle`, and `binding.textPendingClaimsCount`, and sets
  click listeners that navigate or open the pending-claims sheet.

### "Nearby" list, empty text, and Show more
```xml
<androidx.recyclerview.widget.RecyclerView
    android:id="@+id/recycler_nearby"
    android:orientation="horizontal"
    android:fadingEdgeLength="@dimen/space_48"
    android:requiresFadingEdge="horizontal"
    tools:listitem="@layout/item_card_preview" />

<com.google.android.material.textview.MaterialTextView android:id="@+id/text_nearby_empty" android:visibility="gone" ... />
<com.google.android.material.button.MaterialButton android:id="@+id/btn_show_more" ... />
```
- **What it shows:** a horizontally scrolling row of small item cards that fade at the
  edges, an empty message when there's nothing nearby, and a "Show more" button.
- **Why it's set up this way:** the list is horizontal with fading edges to hint there's
  more to scroll; it uses no side padding so the fade sits at the screen edge (spacing
  between cards is added in code via an item decoration). Each row uses
  `item_card_preview.xml` (see that doc).
- **How the code uses it:** the code sets a horizontal `LinearLayoutManager`, an item
  decoration, and the preview adapter on `binding.recyclerNearby`. It toggles
  `binding.textNearbyEmpty`/`binding.btnShowMore` based on whether the list is empty, and
  `binding.btnShowMore` opens the full search list.

### "Possible matches" and progress
```xml
<LinearLayout android:id="@+id/layout_matches" android:orientation="vertical" />
<com.google.android.material.textview.MaterialTextView android:id="@+id/text_matches_empty" android:visibility="gone" ... />
<ProgressBar android:id="@+id/progress_home" android:visibility="gone" />
```
- **What it shows:** a vertical area where suggested matches for your lost items appear,
  an empty message when there are none, and a centered loading spinner.
- **Why it's set up this way:** `layout_matches` is an empty `LinearLayout` that the code
  fills at runtime (each match is added as a child view), which is simpler than a second
  RecyclerView for a short, built-in-code list.
- **How the code uses it:** the code clears and re-adds child views to
  `binding.layoutMatches` (`removeAllViews()` then `addView(...)`), shows/hides
  `binding.textMatchesEmpty`, and toggles `binding.progressHome` while loading.
