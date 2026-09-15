```xml
<androidx.coordinatorlayout.widget.CoordinatorLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/md_theme_background">
```

What that function does: I am the home tab: greeting, notice cards, nearby, and matches.
What the function is responsible for: I am the root of fragment_home.
Where it works: HomeFragment inflates me through FragmentHomeBinding. DashboardActivity hosts the fragment.
Why I chose to write the code of the function the way its written: NestedScrollView lets nearby and matches share one scroll.
More: I use CoordinatorLayout.

```xml
            <FrameLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:background="@drawable/bg_dashboard_header"
                android:clipChildren="false"
                android:clipToPadding="false"
                android:paddingTop="@dimen/screen_padding_top"
                android:paddingBottom="@dimen/screen_padding_bottom"
                android:paddingEnd="@dimen/screen_padding_horizontal"
                android:paddingStart="@dimen/screen_padding_horizontal">
```

What that function does: I am the dark greeting header with the avatar.
What the function is responsible for: I am the home hero.
Where it works: HomeFragment sets text_greeting and image_home_avatar.
Why I chose to write the code of the function the way its written: clipChildren false lets the orb and avatar hang off the edge.
More: I use bg_dashboard_header.

```xml
                        <com.google.android.material.textview.MaterialTextView
                            android:id="@+id/text_greeting"
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:text="@string/dashboard_greeting"
                            android:textAppearance="@style/TextAppearance.ReClaim.DashboardTitle"
                            tools:text="Hello, Alex" />
```

What that function does: I show Hello, then the user's name when it loads.
What the function is responsible for: I am the greeting.
Where it works: HomeFragment.loadUserGreeting sets me.
Why I chose to write the code of the function the way its written: XML default keeps the header filled before the user call returns.
More: I use dashboard_greeting.

```xml
                    <com.google.android.material.card.MaterialCardView
                        android:layout_width="@dimen/space_82"
                        android:layout_height="@dimen/space_82"
                        android:layout_marginStart="@dimen/space_16"
                        android:layout_marginEnd="@dimen/space_neg_10"
                        android:layout_marginTop="@dimen/space_neg_10"
                        android:layout_marginBottom="@dimen/space_neg_10"
                        app:cardBackgroundColor="#28F7F5F0"
                        app:cardCornerRadius="@dimen/space_41"
                        app:cardElevation="0dp"
                        app:strokeColor="#40F7F5F0"
                        app:strokeWidth="@dimen/space_1_5">

                        <ImageView
                            android:id="@+id/image_home_avatar"
                            android:layout_width="match_parent"
                            android:layout_height="match_parent"
                            android:contentDescription="@string/profile_title"
                            android:padding="@dimen/space_12"
                            android:scaleType="centerCrop"
                            android:src="@drawable/ic_settings_person"
                            app:tint="@color/brand_white" />
                    </com.google.android.material.card.MaterialCardView>
```

What that function does: I am the round home avatar.
What the function is responsible for: I show the profile photo.
Where it works: HomeFragment.loadAvatar loads into image_home_avatar.
Why I chose to write the code of the function the way its written: Java clears padding and tint before a real photo so the icon style does not stay on.
More: I use a translucent stroke on the dark header.

```xml
            <com.google.android.material.card.MaterialCardView
                android:id="@+id/card_claim_decision"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginStart="@dimen/screen_padding_horizontal"
                android:layout_marginEnd="@dimen/screen_padding_horizontal"
                android:layout_marginTop="@dimen/space_20"
                android:clickable="true"
                android:focusable="true"
                android:foreground="?attr/selectableItemBackground"
                android:visibility="gone"
                app:cardBackgroundColor="@color/brand_dark"
                app:cardCornerRadius="@dimen/space_18"
                app:cardElevation="@dimen/space_4"
                app:strokeWidth="0dp">
```

What that function does: I am the approved/declined notice. Start gone.
What the function is responsible for: I open the decision dialog.
Where it works: HomeFragment.renderDecisionCard shows me. cardClaimDecision click opens the dialog.
Why I chose to write the code of the function the way its written: Dark fill stands apart from the other home cards.
More: I use text_claim_decision_title and text_claim_decision_subtitle inside.

```xml
            <com.google.android.material.card.MaterialCardView
                android:id="@+id/card_targeted_found"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginStart="@dimen/screen_padding_horizontal"
                android:layout_marginEnd="@dimen/screen_padding_horizontal"
                android:layout_marginTop="@dimen/space_20"
                android:clickable="true"
                android:focusable="true"
                android:foreground="?attr/selectableItemBackground"
                android:visibility="gone"
                app:cardBackgroundColor="@color/brand_accent"
                app:cardCornerRadius="@dimen/space_18"
                app:cardElevation="@dimen/space_4"
                app:strokeWidth="0dp">
```

What that function does: I am the private found-report notice. Start gone.
What the function is responsible for: I open the latest targeted found item.
Where it works: HomeFragment.renderTargetedFoundCard shows me. Click opens ItemDetailsActivity.
Why I chose to write the code of the function the way its written: Accent fill marks this as a match aimed at me.
More: I use text_targeted_found_subtitle.

```xml
            <com.google.android.material.card.MaterialCardView
                android:id="@+id/card_pending_claims"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginStart="@dimen/screen_padding_horizontal"
                android:layout_marginEnd="@dimen/screen_padding_horizontal"
                android:layout_marginTop="@dimen/space_20"
                android:clickable="true"
                android:focusable="true"
                android:foreground="?attr/selectableItemBackground"
                android:visibility="gone"
                app:cardBackgroundColor="@color/md_theme_surfaceContainerLowest"
                app:cardCornerRadius="@dimen/space_18"
                app:cardElevation="@dimen/space_2"
                app:strokeColor="@color/brand_accent"
                app:strokeWidth="@dimen/space_1">
```

What that function does: I am the pending-claims notice with a count. Start gone.
What the function is responsible for: I open PendingClaimsSheet.
Where it works: HomeFragment.renderClaimCards shows me. Click calls showPendingClaimsSheet.
Why I chose to write the code of the function the way its written: A light card with an accent stroke is quieter than the decision card.
More: I use text_pending_claims_count.

```xml
            <androidx.recyclerview.widget.RecyclerView
                android:id="@+id/recycler_nearby"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="@dimen/space_12"
                android:background="@color/md_theme_background"
                android:clipToPadding="false"
                android:fadingEdgeLength="@dimen/space_48"
                android:nestedScrollingEnabled="false"
                android:orientation="horizontal"
                android:requiresFadingEdge="horizontal"
                tools:itemCount="4"
                tools:listitem="@layout/item_card_preview" />
```

What that function does: I am the horizontal nearby row.
What the function is responsible for: I show up to four preview cards.
Where it works: HomeFragment sets PreviewItemAdapter. Cards use item_card_preview.xml.
Why I chose to write the code of the function the way its written: No side padding so the fade reaches the edge. Gutters come from ItemDecoration.
More: I use nestedScrollingEnabled false.

```xml
            <com.google.android.material.textview.MaterialTextView
                android:id="@+id/text_nearby_empty"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="@dimen/space_10"
                android:gravity="center"
                android:paddingTop="@dimen/space_16"
                android:paddingBottom="@dimen/space_16"
                android:text="@string/home_nearby_empty"
                android:textColor="@color/text_secondary"
                android:visibility="gone" />

            <com.google.android.material.button.MaterialButton
                android:id="@+id/btn_show_more"
                style="@style/Widget.ReClaim.Button.Secondary"
                android:layout_width="match_parent"
                android:layout_height="@dimen/button_height"
                android:layout_marginTop="@dimen/space_14"
                android:layout_marginStart="@dimen/space_24"
                android:layout_marginEnd="@dimen/space_24"
                android:text="@string/btn_show_more"
                android:visibility="visible" />
```

What that function does: I am nearby empty text and the show-more button.
What the function is responsible for: Empty hides the row. Show more jumps to Search.
Where it works: HomeFragment.renderNearby toggles these. btnShowMore uses DashboardNavigator.selectTab.
Why I chose to write the code of the function the way its written: Empty starts gone. Show more hides when there is nothing nearby.
More: I use Widget.ReClaim.Button.Secondary.

```xml
            <LinearLayout
                android:id="@+id/layout_matches"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="@dimen/space_12"
                android:paddingEnd="@dimen/screen_padding_horizontal"
                android:paddingStart="@dimen/screen_padding_horizontal"
                android:orientation="vertical" />

            <com.google.android.material.textview.MaterialTextView
                android:id="@+id/text_matches_empty"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="@dimen/space_10"
                android:gravity="center"
                android:paddingTop="@dimen/space_16"
                android:paddingBottom="@dimen/space_16"
                android:text="@string/home_matches_empty"
                android:textColor="@color/text_secondary"
                android:visibility="gone" />

            <ProgressBar
                android:id="@+id/progress_home"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_gravity="center_horizontal"
                android:layout_marginTop="@dimen/space_32"
                android:indeterminateTint="@color/brand_accent"
                android:visibility="gone" />
```

What that function does: I am the matches slot, empty text, and home spinner.
What the function is responsible for: Java adds headers and ItemAdapter lists into layout_matches.
Where it works: HomeFragment.renderMatches fills me. progress_home follows ItemsViewModel loading.
Why I chose to write the code of the function the way its written: Group count is not known in XML, so the slot starts empty.
More: Match cards use item_card.xml through ItemAdapter.
