# DashboardNavigator.java

## Overview
This is a small **interface** — a contract that lists methods a class promises to provide, without any code of its own (see `docs/GLOSSARY.md`). It lets a Fragment ask its host screen to switch the bottom tab without knowing the exact class of that screen. `DashboardActivity` implements it, and `HomeFragment` calls it to jump to the Search tab.

## Concepts used here
- **Interface** — a list of method names a class must define; here just one, `selectTab`. See `docs/GLOSSARY.md`.
- **Loose coupling** — a Fragment talks to "whatever navigator hosts me" instead of naming `DashboardActivity` directly, so the two are less tangled.

## Libraries and imports
This file imports nothing. It only declares the interface and its single method.

## Functions

### `void selectTab(int menuItemId)`
```java
public interface DashboardNavigator {

    void selectTab(int menuItemId);
}
```
- **What it does:** Declares one method: switch to the bottom-navigation tab whose menu id is passed in. There is no body here; the implementing class provides it.
- **Where it's called:** `HomeFragment` calls `navigator.selectTab(R.id.nav_search)` from its "show more" button (after checking the host is a `DashboardNavigator`). `DashboardActivity` implements the method and does the real work of selecting and showing the tab.
- **Why it's written this way:** Declaring the ability as an interface means any host screen can offer tab switching, and Fragments stay independent of the concrete Activity class.
- **Step by step:**
  1. The interface names a single method, `selectTab`, taking a menu item id.
  2. Classes that implement `DashboardNavigator` must supply the actual tab-switching logic.
