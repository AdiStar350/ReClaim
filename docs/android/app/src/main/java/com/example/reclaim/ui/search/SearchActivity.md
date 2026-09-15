```java
@Override
protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_fragment_host);

    if (savedInstanceState == null) {
        Fragment fragment = new SearchFragment();

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_host, fragment)
                .commit();
    }
}
```

What that function does: I host SearchFragment in a simple container layout.
What the function is responsible for: I am the standalone search screen.
Where it works: I inflate activity_fragment_host.xml. I am started when search is opened as its own activity.
Why I chose to write the code of the function the way its written: I only add the fragment when savedInstanceState is null so rotate does not add a second copy.
More: I use FragmentManager.replace on R.id.fragment_host.
