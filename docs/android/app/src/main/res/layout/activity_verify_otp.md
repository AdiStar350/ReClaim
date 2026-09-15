```xml
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/brand_deep"
    android:fitsSystemWindows="true"
    android:orientation="vertical">
```

What that function does: I am the OTP screen: back bar, hero, then the code form.
What the function is responsible for: I am the root of activity_verify_otp.
Where it works: VerifyOtpActivity inflates me through ActivityVerifyOtpBinding.
Why I chose to write the code of the function the way its written: Same auth split as login and register.
More: I use a vertical LinearLayout.

```xml
    <com.google.android.material.appbar.MaterialToolbar
        android:id="@+id/toolbar_verify"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:background="@android:color/transparent"
        app:navigationIcon="@drawable/ic_arrow_back_24"
        app:navigationIconTint="@color/brand_white" />
```

What that function does: I am a transparent back bar on the dark hero.
What the function is responsible for: I let the user leave OTP.
Where it works: VerifyOtpActivity uses toolbar_verify for up navigation.
Why I chose to write the code of the function the way its written: Transparent keeps the dark hero unbroken.
More: I use ic_arrow_back_24.

```xml
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="@dimen/space_160"
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
            android:text="@string/verify_title"
            android:textAppearance="@style/TextAppearance.ReClaim.AuthHero" />
    </LinearLayout>
```

What that function does: I show the logo and Verify title.
What the function is responsible for: I am the OTP hero.
Where it works: VerifyOtpActivity shows me under the toolbar.
Why I chose to write the code of the function the way its written: Shorter than login because the form is small.
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
What the function is responsible for: I am the OTP form container.
Where it works: The code field and buttons sit in me.
Why I chose to write the code of the function the way its written: fillViewport keeps the sheet tall even with few fields.
More: I use NestedScrollView.

```xml
            <com.google.android.material.textview.MaterialTextView
                android:id="@+id/text_verify_subtitle"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="@string/verify_title"
                android:textAppearance="?attr/textAppearanceBodyLarge"
                android:textColor="@color/brand_slate" />
```

What that function does: I explain where the code was sent.
What the function is responsible for: I am the OTP subtitle.
Where it works: VerifyOtpActivity sets my text from the email extra.
Why I chose to write the code of the function the way its written: XML starts with the title string; Java replaces it with the email line.
More: I use text_verify_subtitle.

```xml
            <com.google.android.material.textfield.TextInputLayout
                android:id="@+id/input_layout_otp"
                style="@style/Widget.ReClaim.TextInput"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="@dimen/space_24"
                android:hint="@string/hint_otp_code"
                app:startIconDrawable="@android:drawable/ic_lock_lock">

                <com.google.android.material.textfield.TextInputEditText
                    android:id="@+id/edit_otp"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:inputType="number"
                    android:letterSpacing="0.3"
                    android:maxLength="6"
                    android:textSize="@dimen/text_size_20" />
            </com.google.android.material.textfield.TextInputLayout>
```

What that function does: I am the six-digit code field.
What the function is responsible for: I take the email OTP.
Where it works: VerifyOtpActivity reads edit_otp.
Why I chose to write the code of the function the way its written: number keyboard and maxLength 6 match a short code. Wider letter spacing makes digits easier to check.
More: I use inputType number.

```xml
            <com.google.android.material.button.MaterialButton
                android:id="@+id/btn_verify"
                style="@style/Widget.ReClaim.Button.Primary"
                android:layout_width="match_parent"
                android:layout_height="@dimen/button_height"
                android:layout_marginTop="@dimen/space_20"
                android:text="@string/btn_verify" />

            <ProgressBar
                android:id="@+id/progress"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_gravity="center_horizontal"
                android:layout_marginTop="@dimen/space_16"
                android:indeterminateTint="@color/brand_accent"
                android:visibility="gone" />
```

What that function does: I am verify and a spinner while the call runs.
What the function is responsible for: I submit the code.
Where it works: VerifyOtpActivity wires btn_verify and shows progress.
Why I chose to write the code of the function the way its written: The spinner starts gone so the form stays calm.
More: I use Widget.ReClaim.Button.Primary.

```xml
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="@dimen/space_12"
                android:gravity="center"
                android:orientation="horizontal">

                <com.google.android.material.textview.MaterialTextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="@string/verify_no_code"
                    android:textAppearance="?attr/textAppearanceBodyMedium"
                    android:textColor="?attr/colorOnSurfaceVariant" />

                <com.google.android.material.button.MaterialButton
                    android:id="@+id/btn_resend_code"
                    style="@style/Widget.Material3.Button.TextButton"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="@string/btn_resend_code"
                    android:textColor="@color/brand_accent" />
            </LinearLayout>
```

What that function does: I offer a resend if the code did not arrive.
What the function is responsible for: I am the resend row.
Where it works: VerifyOtpActivity wires btn_resend_code.
Why I chose to write the code of the function the way its written: A text button next to the label matches login's secondary action.
More: I use btn_resend_code.
