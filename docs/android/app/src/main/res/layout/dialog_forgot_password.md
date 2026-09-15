```xml
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:paddingStart="@dimen/space_8"
    android:paddingTop="@dimen/space_4"
    android:paddingEnd="@dimen/space_8"
    android:paddingBottom="@dimen/space_4">
```

What that function does: I am the dialog body that asks for an email to send a reset code.
What the function is responsible for: I am the forgot-password dialog layout.
Where it works: LoginActivity.showForgotPasswordDialog inflates me as R.layout.dialog_forgot_password.
Why I chose to write the code of the function the way its written: The title and buttons live on MaterialAlertDialogBuilder. I only hold the message and the field.
More: I use a vertical LinearLayout.

```xml
    <com.google.android.material.textview.MaterialTextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginBottom="@dimen/space_12"
        android:text="@string/forgot_password_dialog_message"
        android:textAppearance="?attr/textAppearanceBodyMedium"
        android:textColor="@color/text_secondary" />
```

What that function does: I show the short "enter your email" message.
What the function is responsible for: I explain the dialog before the field.
Where it works: LoginActivity inflates this layout into the forgot-password dialog.
Why I chose to write the code of the function the way its written: Body medium and secondary color keep it quieter than the title.
More: I use forgot_password_dialog_message.

```xml
    <com.google.android.material.textfield.TextInputLayout
        android:id="@+id/input_layout_reset_email"
        style="@style/Widget.ReClaim.TextInput"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="@string/hint_email"
        app:startIconDrawable="@android:drawable/ic_dialog_email">

        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/edit_reset_email"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:imeOptions="actionDone"
            android:inputType="textEmailAddress" />
    </com.google.android.material.textfield.TextInputLayout>
```

What that function does: I am the email field for the reset code.
What the function is responsible for: I take the address LoginActivity will send to sendPasswordResetOtp.
Where it works: LoginActivity finds input_layout_reset_email and edit_reset_email after inflate.
Why I chose to write the code of the function the way its written: TextInputLayout holds the error. inputType email brings the email keyboard.
More: I use Widget.ReClaim.TextInput and hint_email.
