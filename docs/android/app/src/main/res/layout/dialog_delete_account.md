```xml
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="@dimen/space_8">
```

What that function does: I am the delete-account dialog body.
What the function is responsible for: I collect confirm name and password.
Where it works: ProfileSettingsFragment.showDeleteAccountDialog inflates me as dialog_delete_account.xml.
Why I chose to write the code of the function the way its written: Title and buttons live on MaterialAlertDialogBuilder.
More: I use a vertical LinearLayout.

```xml
    <com.google.android.material.textview.MaterialTextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginBottom="@dimen/space_12"
        android:text="@string/delete_account_dialog_message"
        android:textColor="@color/brand_slate" />
```

What that function does: I explain that name and password are needed to delete.
What the function is responsible for: I am the warning text.
Where it works: ProfileSettingsFragment shows me inside the dialog.
Why I chose to write the code of the function the way its written: A short message is enough. The title already says delete.
More: I use delete_account_dialog_message.

```xml
    <com.google.android.material.textfield.TextInputLayout
        android:id="@+id/input_layout_confirm_name"
        style="@style/Widget.ReClaim.TextInput"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="@string/hint_confirm_name">

        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/edit_confirm_name"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:inputType="textPersonName" />
    </com.google.android.material.textfield.TextInputLayout>
```

What that function does: I am the confirm-name field.
What the function is responsible for: I take the name the server will check.
Where it works: ProfileSettingsFragment reads edit_confirm_name.
Why I chose to write the code of the function the way its written: The layout holds the empty-name error.
More: I use hint_confirm_name.

```xml
    <com.google.android.material.textfield.TextInputLayout
        android:id="@+id/input_layout_confirm_password"
        style="@style/Widget.ReClaim.TextInput"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="@dimen/space_12"
        android:hint="@string/hint_password"
        app:endIconMode="password_toggle">

        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/edit_confirm_password"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:inputType="textPassword" />
    </com.google.android.material.textfield.TextInputLayout>
```

What that function does: I am the confirm-password field with a show/hide eye.
What the function is responsible for: I take the password the server will check.
Where it works: ProfileSettingsFragment reads edit_confirm_password.
Why I chose to write the code of the function the way its written: password_toggle helps the user check the value before delete.
More: I use endIconMode password_toggle.
