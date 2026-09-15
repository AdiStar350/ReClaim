```xml
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:paddingStart="@dimen/space_24"
    android:paddingTop="@dimen/space_8"
    android:paddingEnd="@dimen/space_16"
    android:paddingBottom="@dimen/space_4">
```

What that function does: I am the dialog body that shows name, email, and phone.
What the function is responsible for: I am the contact-info dialog layout.
Where it works: ContactInfoDialog inflates me through DialogContactInfoBinding.
Why I chose to write the code of the function the way its written: The dialog title and OK button are set in Java. I only hold the rows.
More: I use a vertical LinearLayout.

```xml
    <com.google.android.material.textview.MaterialTextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/contact_info_label_name"
        android:textAppearance="?attr/textAppearanceLabelMedium"
        android:textColor="@color/text_secondary" />

    <com.google.android.material.textview.MaterialTextView
        android:id="@+id/text_contact_name"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="@dimen/space_4"
        android:textAppearance="?attr/textAppearanceBodyLarge"
        android:textColor="@color/brand_dark"
        tools:text="Ada Lovelace" />
```

What that function does: I show the other person's name under a small label.
What the function is responsible for: I am the name row.
Where it works: ContactInfoDialog.bindContact sets text_contact_name.
Why I chose to write the code of the function the way its written: Name has no copy button because people copy email and phone more.
More: I use contact_info_label_name.

```xml
    <com.google.android.material.textview.MaterialTextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="@dimen/space_16"
        android:text="@string/contact_info_label_email"
        android:textAppearance="?attr/textAppearanceLabelMedium"
        android:textColor="@color/text_secondary" />

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:gravity="center_vertical"
        android:orientation="horizontal">

        <com.google.android.material.textview.MaterialTextView
            android:id="@+id/text_contact_email"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:textAppearance="?attr/textAppearanceBodyLarge"
            android:textColor="@color/brand_dark"
            android:textIsSelectable="true"
            tools:text="ada@example.com" />

        <ImageButton
            android:id="@+id/btn_copy_email"
            android:layout_width="@dimen/space_36"
            android:layout_height="@dimen/space_36"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:contentDescription="@string/cd_copy_email"
            android:padding="@dimen/space_8"
            android:scaleType="centerInside"
            android:src="@drawable/ic_copy"
            app:tint="@color/brand_accent" />
    </LinearLayout>
```

What that function does: I show the email and a copy button beside it.
What the function is responsible for: I am the email row.
Where it works: ContactInfoDialog.bindContact and bindCopyButton fill text_contact_email and btn_copy_email.
Why I chose to write the code of the function the way its written: textIsSelectable lets long-press copy too. The button is hidden in Java if email is empty.
More: I use ic_copy and cd_copy_email.

```xml
    <com.google.android.material.textview.MaterialTextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="@dimen/space_16"
        android:text="@string/contact_info_label_phone"
        android:textAppearance="?attr/textAppearanceLabelMedium"
        android:textColor="@color/text_secondary" />

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:gravity="center_vertical"
        android:orientation="horizontal">

        <com.google.android.material.textview.MaterialTextView
            android:id="@+id/text_contact_phone"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:textAppearance="?attr/textAppearanceBodyLarge"
            android:textColor="@color/brand_dark"
            android:textIsSelectable="true"
            tools:text="+1 555 0100" />

        <ImageButton
            android:id="@+id/btn_copy_phone"
            android:layout_width="@dimen/space_36"
            android:layout_height="@dimen/space_36"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:contentDescription="@string/cd_copy_phone"
            android:padding="@dimen/space_8"
            android:scaleType="centerInside"
            android:src="@drawable/ic_copy"
            app:tint="@color/brand_accent" />
    </LinearLayout>
```

What that function does: I show the phone and a copy button beside it.
What the function is responsible for: I am the phone row.
Where it works: ContactInfoDialog.bindContact and bindCopyButton fill text_contact_phone and btn_copy_phone.
Why I chose to write the code of the function the way its written: Same pattern as email so both rows feel the same.
More: I use ic_copy and cd_copy_phone.
