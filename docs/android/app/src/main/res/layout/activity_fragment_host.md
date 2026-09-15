```xml
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/fragment_host"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/md_theme_background" />
```

What that function does: I am an empty full-screen container for one fragment.
What the function is responsible for: I am the host layout for Search, My Items, and Profile as standalone activities.
Where it works: SearchActivity, MyItemsActivity, and ProfileActivity set me as content and replace R.id.fragment_host.
Why I chose to write the code of the function the way its written: A FrameLayout is enough. The fragment brings its own toolbar.
More: I use md_theme_background so the gap behind the fragment is not white flash.
