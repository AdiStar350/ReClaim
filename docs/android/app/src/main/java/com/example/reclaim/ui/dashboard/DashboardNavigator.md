```java
void selectTab(int menuItemId);
```

What that function does: I ask the dashboard to switch the bottom tab.
What the function is responsible for: I am the tab-switch hook fragments can call.
Where it works: DashboardActivity implements me. HomeFragment calls me when the host is a DashboardNavigator.
Why I chose to write the code of the function the way its written: An interface keeps HomeFragment from depending on DashboardActivity by name.
More: I take a menu item id from the bottom navigation.
