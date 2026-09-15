```xml
<com.google.android.material.textfield.TextInputLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/input_layout_claim_answer"
    style="@style/Widget.Material3.TextInputLayout.OutlinedBox"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginStart="@dimen/space_24"
    android:layout_marginTop="@dimen/space_16"
    android:layout_marginEnd="@dimen/space_24"
    android:hint="@string/hint_claim_answer"
    app:counterEnabled="true"
    app:counterMaxLength="500">

    <com.google.android.material.textfield.TextInputEditText
        android:id="@+id/edit_claim_answer"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:inputType="textMultiLine"
        android:maxLines="4"
        android:minLines="2" />

</com.google.android.material.textfield.TextInputLayout>
```

What that function does: I am the claim-answer field with a 500 character counter.
What the function is responsible for: I take the check-question answer when someone claims an item.
Where it works: ItemDetailsActivity inflates me as dialog_claim_verification.xml into the claim dialog.
Why I chose to write the code of the function the way its written: A small outlined box is enough. The question text is set as the dialog message in Java. Counter 500 matches a long answer without a huge box.
More: I use counterMaxLength 500 and textMultiLine.
