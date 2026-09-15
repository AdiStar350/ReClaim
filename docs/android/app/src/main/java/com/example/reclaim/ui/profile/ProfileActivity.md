```java
@Override
protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_fragment_host);

    if (savedInstanceState == null) {
        Fragment fragment = new ProfileSettingsFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_host, fragment)
                .commit();
    }
}
```

What that function does: I host ProfileSettingsFragment in a simple container layout.
What the function is responsible for: I am the standalone profile screen.
Where it works: I inflate activity_fragment_host.xml. Dashboard can also host the fragment in a tab.
Why I chose to write the code of the function the way its written: I only add the fragment on first create so rotate does not add a second copy.
More: I use FragmentManager.replace on R.id.fragment_host.
