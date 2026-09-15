```xml
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/brand_deep"
    android:fitsSystemWindows="true"
    android:orientation="vertical">
```

What that function does: I am the sign-up screen: dark hero on top, form below.
What the function is responsible for: I am the root of activity_register.
Where it works: RegisterActivity inflates me through ActivityRegisterBinding.
Why I chose to write the code of the function the way its written: Same split as login so the two screens feel like a pair.
More: I use a vertical LinearLayout.

```xml
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="@dimen/space_200"
        android:gravity="center"
        android:orientation="vertical"
        android:paddingTop="@dimen/space_20"
        android:paddingBottom="@dimen/space_12">

        <ImageView
            android:id="@+id/image_logo"
            android:layout_width="@dimen/space_64"
            android:layout_height="@dimen/space_64"
            android:contentDescription="@string/app_name"
            android:scaleType="centerInside"
            android:src="@mipmap/ic_launcher" />

        <com.google.android.material.textview.MaterialTextView
            android:id="@+id/text_register_title"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="@dimen/space_10"
            android:text="@string/app_name"
            android:textAppearance="@style/TextAppearance.ReClaim.AuthHero" />

        <com.google.android.material.textview.MaterialTextView
            android:id="@+id/text_register_subtitle"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="@dimen/space_4"
            android:text="@string/register_subtitle"
            android:textAppearance="@style/TextAppearance.ReClaim.HeroSubtitle" />
    </LinearLayout>
```

What that function does: I show the logo, app name, and a short sign-up line.
What the function is responsible for: I am the register hero.
Where it works: RegisterActivity shows me at the top.
Why I chose to write the code of the function the way its written: A shorter hero than login leaves more room for extra fields.
More: I use AuthHero and HeroSubtitle.

```xml
    <androidx.core.widget.NestedScrollView
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:background="@drawable/bg_auth_form"
        android:fillViewport="true">
```

What that function does: I hold the light form sheet and let it scroll.
What the function is responsible for: I am the form container.
Where it works: RegisterActivity's fields sit inside me.
Why I chose to write the code of the function the way its written: Name, email, phone, two passwords, and a photo need scroll on a small screen.
More: I use NestedScrollView.

```xml
            <FrameLayout
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_gravity="center_horizontal"
                android:layout_marginTop="@dimen/space_18">

                <com.google.android.material.card.MaterialCardView
                    android:id="@+id/card_avatar"
                    android:layout_width="@dimen/space_96"
                    android:layout_height="@dimen/space_96"
                    android:clickable="true"
                    android:focusable="true"
                    app:cardBackgroundColor="@color/md_theme_primaryContainer"
                    app:cardCornerRadius="@dimen/space_48"
                    app:cardElevation="0dp"
                    app:strokeWidth="0dp">

                    <ImageView
                        android:id="@+id/image_profile_avatar"
                        android:layout_width="match_parent"
                        android:layout_height="match_parent"
                        android:contentDescription="@string/cd_profile_avatar"
                        android:padding="@dimen/space_26"
                        android:scaleType="centerCrop"
                        android:src="@drawable/ic_settings_person"
                        app:tint="@color/brand_dark" />
                </com.google.android.material.card.MaterialCardView>

                <com.google.android.material.card.MaterialCardView
                    android:layout_width="@dimen/space_32"
                    android:layout_height="@dimen/space_32"
                    android:layout_gravity="bottom|end"
                    android:clickable="false"
                    app:cardBackgroundColor="@color/brand_accent"
                    app:cardCornerRadius="@dimen/space_16"
                    app:cardElevation="@dimen/space_2"
                    app:strokeColor="@color/brand_white"
                    app:strokeWidth="@dimen/space_2">

                    <ImageView
                        android:layout_width="match_parent"
                        android:layout_height="match_parent"
                        android:contentDescription="@string/register_add_photo"
                        android:padding="@dimen/space_7"
                        android:src="@android:drawable/ic_menu_camera"
                        app:tint="@color/text_on_accent" />
                </com.google.android.material.card.MaterialCardView>
            </FrameLayout>

            <com.google.android.material.button.MaterialButton
                android:id="@+id/btn_add_photo"
                style="@style/Widget.Material3.Button.TextButton"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_gravity="center_horizontal"
                android:layout_marginTop="@dimen/space_2"
                android:text="@string/register_add_photo"
                android:textColor="@color/brand_accent" />
```

What that function does: I am the avatar picker with a camera badge and an add-photo button.
What the function is responsible for: I take an optional profile picture.
Where it works: RegisterActivity wires card_avatar and btn_add_photo.
Why I chose to write the code of the function the way its written: The small camera badge is only a hint. The card and text button both open the picker.
More: I use MaterialCardView for a round avatar.

```xml
            <com.google.android.material.textfield.TextInputLayout
                android:id="@+id/input_layout_full_name"
                style="@style/Widget.ReClaim.TextInput"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="@dimen/space_20"
                android:hint="@string/hint_full_name"
                app:startIconDrawable="@android:drawable/ic_menu_myplaces">

                <com.google.android.material.textfield.TextInputEditText
                    android:id="@+id/edit_full_name"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:inputType="textPersonName" />
            </com.google.android.material.textfield.TextInputLayout>
```

What that function does: I am the full-name field.
What the function is responsible for: I take the name sent with the OTP signup.
Where it works: RegisterActivity reads edit_full_name.
Why I chose to write the code of the function the way its written: inputType person name helps the keyboard. Errors go on the layout.
More: I use Widget.ReClaim.TextInput.

```xml
            <com.google.android.material.textfield.TextInputLayout
                android:id="@+id/input_layout_email"
                style="@style/Widget.ReClaim.TextInput"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="@dimen/space_14"
                android:hint="@string/hint_email"
                app:startIconDrawable="@android:drawable/ic_dialog_email">

                <com.google.android.material.textfield.TextInputEditText
                    android:id="@+id/edit_email"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:inputType="textEmailAddress" />
            </com.google.android.material.textfield.TextInputLayout>
```

What that function does: I am the email field.
What the function is responsible for: I take the signup email.
Where it works: RegisterActivity reads edit_email.
Why I chose to write the code of the function the way its written: Email input type brings the email keyboard.
More: I use startIconDrawable email.

```xml
            <com.google.android.material.textfield.TextInputLayout
                android:id="@+id/input_layout_phone"
                style="@style/Widget.ReClaim.TextInput"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="@dimen/space_14"
                android:hint="@string/hint_phone"
                app:startIconDrawable="@android:drawable/ic_menu_call">

                <com.google.android.material.textfield.TextInputEditText
                    android:id="@+id/edit_phone"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:inputType="phone" />
            </com.google.android.material.textfield.TextInputLayout>
```

What that function does: I am the phone field.
What the function is responsible for: I take the signup phone.
Where it works: RegisterActivity reads edit_phone.
Why I chose to write the code of the function the way its written: inputType phone brings the number pad.
More: I use ic_menu_call.

```xml
            <com.google.android.material.textfield.TextInputLayout
                android:id="@+id/input_layout_password"
                style="@style/Widget.ReClaim.TextInput"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="@dimen/space_14"
                android:hint="@string/hint_password"
                app:endIconMode="password_toggle">

                <com.google.android.material.textfield.TextInputEditText
                    android:id="@+id/edit_password"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:inputType="textPassword" />
            </com.google.android.material.textfield.TextInputLayout>
```

What that function does: I am the password field with a show/hide eye.
What the function is responsible for: I take the signup password.
Where it works: RegisterActivity reads edit_password.
Why I chose to write the code of the function the way its written: password_toggle lets the user check the value.
More: I use endIconMode password_toggle.

```xml
            <com.google.android.material.textfield.TextInputLayout
                android:id="@+id/input_layout_confirm_password"
                style="@style/Widget.ReClaim.TextInput"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="@dimen/space_14"
                android:hint="@string/hint_confirm_password"
                app:endIconMode="password_toggle">

                <com.google.android.material.textfield.TextInputEditText
                    android:id="@+id/edit_confirm_password"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:inputType="textPassword" />
            </com.google.android.material.textfield.TextInputLayout>
```

What that function does: I am the confirm-password field.
What the function is responsible for: I catch a mismatch before the OTP call.
Where it works: RegisterActivity reads edit_confirm_password.
Why I chose to write the code of the function the way its written: A second field is simpler than asking the user to retype later.
More: I use password_toggle.

```xml
            <com.google.android.material.button.MaterialButton
                android:id="@+id/btn_register"
                style="@style/Widget.ReClaim.Button.Primary"
                android:layout_width="match_parent"
                android:layout_height="@dimen/button_height"
                android:layout_marginTop="@dimen/space_20"
                android:text="@string/btn_register" />
```

What that function does: I am the main sign-up button.
What the function is responsible for: I start the OTP send.
Where it works: RegisterActivity wires btn_register.
Why I chose to write the code of the function the way its written: Full width primary matches login.
More: I use Widget.ReClaim.Button.Primary.

```xml
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="@dimen/space_18"
                android:gravity="center"
                android:orientation="horizontal">

                <com.google.android.material.textview.MaterialTextView
                    android:id="@+id/text_has_account"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="@string/register_has_account"
                    android:textAppearance="?attr/textAppearanceBodyMedium"
                    android:textColor="?attr/colorOnSurfaceVariant" />

                <com.google.android.material.button.MaterialButton
                    android:id="@+id/btn_go_login"
                    style="@style/Widget.Material3.Button.TextButton"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="@string/btn_go_login"
                    android:textColor="@color/brand_accent" />
            </LinearLayout>
```

What that function does: I offer a path back to login.
What the function is responsible for: I am the already-have-account row.
Where it works: RegisterActivity starts LoginActivity from btn_go_login.
Why I chose to write the code of the function the way its written: A text button stays quieter than a second primary.
More: I use btn_go_login.
