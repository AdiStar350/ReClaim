```xml
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/brand_deep"
    android:fitsSystemWindows="true"
    android:orientation="vertical">
```

What that function does: I am the login screen: dark hero on top, form below.
What the function is responsible for: I am the root of activity_login.
Where it works: LoginActivity inflates me through ActivityLoginBinding.
Why I chose to write the code of the function the way its written: fitsSystemWindows keeps the hero off the status bar. brand_deep is the auth look.
More: I use a vertical LinearLayout.

```xml
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="@dimen/auth_hero_height"
        android:gravity="center"
        android:orientation="vertical"
        android:paddingTop="@dimen/space_24"
        android:paddingBottom="@dimen/space_16">

        <ImageView
            android:id="@+id/image_logo"
            android:layout_width="@dimen/space_72"
            android:layout_height="@dimen/space_72"
            android:contentDescription="@string/app_name"
            android:scaleType="centerInside"
            android:src="@mipmap/ic_launcher" />

        <com.google.android.material.textview.MaterialTextView
            android:id="@+id/text_login_title"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="@dimen/space_12"
            android:text="@string/app_name"
            android:textAppearance="@style/TextAppearance.ReClaim.AuthHero" />

        <com.google.android.material.textview.MaterialTextView
            android:id="@+id/text_login_subtitle"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="@dimen/space_6"
            android:text="@string/dashboard_subtitle"
            android:textAppearance="@style/TextAppearance.ReClaim.HeroSubtitle" />
    </LinearLayout>
```

What that function does: I show the logo, app name, and a short subtitle.
What the function is responsible for: I am the login hero.
Where it works: LoginActivity shows me at the top of activity_login.
Why I chose to write the code of the function the way its written: A fixed hero height matches register. The launcher icon is enough branding.
More: I use AuthHero and HeroSubtitle text styles.

```xml
    <androidx.core.widget.NestedScrollView
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:background="@drawable/bg_auth_form"
        android:fillViewport="true">
```

What that function does: I hold the light form sheet and let it scroll on small screens.
What the function is responsible for: I am the form container.
Where it works: LoginActivity's fields sit inside me.
Why I chose to write the code of the function the way its written: layout_weight 1 fills the rest under the hero. bg_auth_form rounds the top.
More: I use NestedScrollView.

```xml
            <com.google.android.material.textfield.TextInputLayout
                android:id="@+id/input_layout_email"
                style="@style/Widget.ReClaim.TextInput"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="@dimen/space_24"
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
What the function is responsible for: I take the login email. Errors go on the layout.
Where it works: LoginActivity.attemptLogin reads edit_email.
Why I chose to write the code of the function the way its written: inputType email brings the email keyboard.
More: I use Widget.ReClaim.TextInput.

```xml
            <com.google.android.material.textfield.TextInputLayout
                android:id="@+id/input_layout_password"
                style="@style/Widget.ReClaim.TextInput"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="@dimen/space_16"
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
What the function is responsible for: I take the login password.
Where it works: LoginActivity.attemptLogin reads edit_password.
Why I chose to write the code of the function the way its written: password_toggle lets the user check what they typed.
More: I use endIconMode password_toggle.

```xml
            <com.google.android.material.button.MaterialButton
                android:id="@+id/btn_forgot_password"
                style="@style/Widget.Material3.Button.TextButton"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_gravity="end"
                android:text="@string/btn_forgot_password"
                android:textColor="@color/brand_accent" />

            <com.google.android.material.button.MaterialButton
                android:id="@+id/btn_login"
                style="@style/Widget.ReClaim.Button.Primary"
                android:layout_width="match_parent"
                android:layout_height="@dimen/button_height"
                android:layout_marginTop="@dimen/space_8"
                android:text="@string/btn_login" />
```

What that function does: I am forgot-password and the main login button.
What the function is responsible for: I start reset or sign-in.
Where it works: LoginActivity.setupClickListeners wires both.
Why I chose to write the code of the function the way its written: Forgot sits as a text button on the right so it stays secondary.
More: I use Widget.ReClaim.Button.Primary for login.

```xml
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="@dimen/space_12"
                android:gravity="center"
                android:orientation="horizontal">

                <com.google.android.material.textview.MaterialTextView
                    android:id="@+id/text_no_account"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="@string/login_no_account"
                    android:textAppearance="?attr/textAppearanceBodyMedium"
                    android:textColor="?attr/colorOnSurfaceVariant" />

                <com.google.android.material.button.MaterialButton
                    android:id="@+id/btn_go_register"
                    style="@style/Widget.Material3.Button.TextButton"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="@string/btn_go_register"
                    android:textColor="@color/brand_accent" />
            </LinearLayout>
```

What that function does: I offer a path to sign-up.
What the function is responsible for: I am the "no account? register" row.
Where it works: LoginActivity starts RegisterActivity from btn_go_register.
Why I chose to write the code of the function the way its written: A text button next to the label is quieter than a second primary button.
More: I use btn_go_register.
