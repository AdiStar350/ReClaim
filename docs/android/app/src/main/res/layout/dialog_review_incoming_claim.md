```xml
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:paddingStart="@dimen/space_24"
    android:paddingTop="@dimen/space_8"
    android:paddingEnd="@dimen/space_24"
    android:paddingBottom="@dimen/space_4">
```

What that function does: I am a dialog body that shows item title, check question, and answer.
What the function is responsible for: I am the incoming-claim review layout.
Where it works: I did not find a Java inflate of this file. Pending claim review uses item_pending_claim.xml and PendingClaimsSheet instead.
Why I chose to write the code of the function the way its written: Title and buttons would live on the dialog builder. I only hold the three text blocks.
More: I use a vertical LinearLayout.

```xml
    <com.google.android.material.textview.MaterialTextView
        android:id="@+id/text_review_item"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:textAppearance="?attr/textAppearanceTitleMedium"
        android:textColor="@color/brand_dark" />
```

What that function does: I show which item the claim is for.
What the function is responsible for: I am the item title in the review dialog.
Where it works: I am text_review_item in this unused-or-legacy layout.
Why I chose to write the code of the function the way its written: Title medium stands out from the labels below.
More: I use brand_dark.

```xml
    <com.google.android.material.textview.MaterialTextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="@dimen/space_16"
        android:text="@string/claim_review_question_label"
        android:textAppearance="?attr/textAppearanceLabelMedium"
        android:textColor="@color/text_secondary" />

    <com.google.android.material.textview.MaterialTextView
        android:id="@+id/text_review_question"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="@dimen/space_4"
        android:textAppearance="?attr/textAppearanceBodyLarge"
        android:textColor="@color/brand_dark" />
```

What that function does: I show the check question under a small label.
What the function is responsible for: I am the question block.
Where it works: I am text_review_question in this layout.
Why I chose to write the code of the function the way its written: Label then value matches the contact dialog.
More: I use claim_review_question_label.

```xml
    <com.google.android.material.textview.MaterialTextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="@dimen/space_16"
        android:text="@string/claim_review_answer_label"
        android:textAppearance="?attr/textAppearanceLabelMedium"
        android:textColor="@color/text_secondary" />

    <com.google.android.material.textview.MaterialTextView
        android:id="@+id/text_review_answer"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="@dimen/space_4"
        android:textAppearance="?attr/textAppearanceBodyLarge"
        android:textColor="@color/brand_dark" />
```

What that function does: I show the claimant's answer under a small label.
What the function is responsible for: I am the answer block.
Where it works: I am text_review_answer in this layout.
Why I chose to write the code of the function the way its written: Question and answer stay as two labeled blocks so the owner can compare them.
More: I use claim_review_answer_label.
