```xml
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/brand_deep"
    android:fitsSystemWindows="true"
    android:orientation="vertical">
```

What that function does: I am the new-password screen after a reset token.
What the function is responsible for: I am the root of activity_reset_password.
Where it works: ResetPasswordActivity inflates me through ActivityResetPasswordBinding.
Why I chose to write the code of the function the way its written: Same auth split as OTP.
More: I use a vertical LinearLayout.

```xml
    <com.google.android.material.appbar.MaterialToolbar
        android:id="@+id/toolbar_reset"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:background="@android:color/transparent"
        app:navigationIcon="@drawable/ic_arrow_back_24"
        app:navigationIconTint="@color/brand_white" />
```

What that function does: I am a transparent back bar on the dark hero.
What the function is responsible for: I let the user leave reset.
Where it works: ResetPasswordActivity uses toolbar_reset for up navigation.
Why I chose to write the code of the function the way its written: Transparent keeps the dark hero unbroken.
More: I use ic_arrow_back_24.

```xml
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="@dimen/space_150"
        android:gravity="center"
        android:orientation="vertical"
        android:paddingBottom="@dimen/space_12">

        <ImageView
            android:layout_width="@dimen/space_56"
            android:layout_height="@dimen/space_56"
            android:contentDescription="@string/app_name"
            android:scaleType="centerInside"
            android:src="@mipmap/ic_launcher" />

        <com.google.android.material.textview.MaterialTextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="@dimen/space_10"
            android:text="@string/reset_password_title"
            android:textAppearance="@style/TextAppearance.ReClaim.AuthHero" />
    </LinearLayout>
```

What that function does: I show the logo and reset title.
What the function is responsible for: I am the reset hero.
Where it works: ResetPasswordActivity shows me under the toolbar.
Why I chose to write the code of the function the way its written: A short hero leaves room for two password fields.
More: I use AuthHero.

```xml
    <androidx.core.widget.NestedScrollView
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:background="@drawable/bg_auth_form"
        android:fillViewport="true">
```

What that function does: I hold the light form sheet.
What the function is responsible for: I am the reset form container.
Where it works: The password fields sit in me.
Why I chose to write the code of the function the way its written: fillViewport keeps the sheet tall.
More: I use NestedScrollView.

```xml
            <com.google.android.material.textview.MaterialTextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="@string/reset_password_subtitle"
                android:textAppearance="?attr/textAppearanceBodyLarge"
                android:textColor="@color/brand_slate" />
```

What that function does: I explain that a new password is needed.
What the function is responsible for: I am the reset subtitle.
Where it works: ResetPasswordActivity shows me at the top of the sheet.
Why I chose to write the code of the function the way its written: Static XML text is enough. The token is not shown.
More: I use reset_password_subtitle.

```xml
            <com.google.android.material.textfield.TextInputLayout
                android:id="@+id/input_layout_new_password"
                style="@style/Widget.ReClaim.TextInput"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="@dimen/space_24"
                android:hint="@string/hint_new_password"
                app:endIconMode="password_toggle">

                <com.google.android.material.textfield.TextInputEditText
                    android:id="@+id/edit_new_password"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:inputType="textPassword" />
            </com.google.android.material.textfield.TextInputLayout>
```

What that function does: I am the new-password field with a show/hide eye.
What the function is responsible for: I take the new password.
Where it works: ResetPasswordActivity reads edit_new_password.
Why I chose to write the code of the function the way its written: password_toggle helps the user check the value.
More: I use Widget.ReClaim.TextInput.

```xml
            <com.google.android.material.textfield.TextInputLayout
                android:id="@+id/input_layout_confirm_new_password"
                style="@style/Widget.ReClaim.TextInput"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="@dimen/space_14"
                android:hint="@string/hint_confirm_new_password"
                app:endIconMode="password_toggle">

                <com.google.android.material.textfield.TextInputEditText
                    android:id="@+id/edit_confirm_new_password"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:inputType="textPassword" />
            </com.google.android.material.textfield.TextInputLayout>
```

What that function does: I am the confirm-new-password field.
What the function is responsible for: I catch a mismatch before the reset call.
Where it works: ResetPasswordActivity reads edit_confirm_new_password.
Why I chose to write the code of the function the way its written: A second field is simpler than a later error from the server.
More: I use password_toggle.

```xml
            <com.google.android.material.button.MaterialButton
                android:id="@+id/btn_update_password"
                style="@style/Widget.ReClaim.Button.Primary"
                android:layout_width="match_parent"
                android:layout_height="@dimen/button_height"
                android:layout_marginTop="@dimen/space_20"
                android:text="@string/btn_update_password" />

            <ProgressBar
                android:id="@+id/progress"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_gravity="center_horizontal"
                android:layout_marginTop="@dimen/space_16"
                android:indeterminateTint="@color/brand_accent"
                android:visibility="gone" />
```

What that function does: I am update-password and a spinner while the call runs.
What the function is responsible for: I submit the new password.
Where it works: ResetPasswordActivity wires btn_update_password and shows progress.
Why I chose to write the code of the function the way its written: The spinner starts gone.
More: I use Widget.ReClaim.Button.Primary.
