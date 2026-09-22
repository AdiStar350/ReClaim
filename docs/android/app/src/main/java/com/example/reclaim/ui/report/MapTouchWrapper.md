# MapTouchWrapper.java

## Overview
This is a custom `FrameLayout` that wraps the map on the report screen. When the user drags on the map, it tells the parent scroll view not to steal that touch, so the map can pan inside a scrolling page.

## Concepts used here
- **View / ViewGroup** — `FrameLayout` is a ViewGroup that stacks children; this class extends it. See `docs/GLOSSARY.md`.
- **Touch dispatch** — `dispatchTouchEvent` sees every touch before children; `requestDisallowInterceptTouchEvent` stops the parent from taking the gesture.
- **AttributeSet** — XML attributes when the view is inflated from a layout file.

## Libraries and imports
- **Android framework**: `Context`, `AttributeSet`, `MotionEvent`, `FrameLayout` for the custom view.
- **AndroidX annotations**: `@NonNull` / `@Nullable` on constructors.

## Functions

### `public MapTouchWrapper(Context context)` / `(Context, AttributeSet)` / `(Context, AttributeSet, int)`
```java
public MapTouchWrapper(@NonNull Context context) {
    super(context);
}

public MapTouchWrapper(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
}

public MapTouchWrapper(@NonNull Context context, @Nullable AttributeSet attrs,
                       int defStyleAttr) {
    super(context, attrs, defStyleAttr);
}
```
- **What it does:** Standard View constructors so the class can be created in code or inflated from XML.
- **Where it's called:** Android inflates the two-arg form from `activity_report.xml`, which wraps the map fragment in this class. The other constructors exist for completeness / code creation.
- **Why it's written this way:** Custom views used in XML must provide the `Context` + `AttributeSet` constructor or inflation fails.
- **Step by step:**
  1. Each constructor forwards to the matching `FrameLayout` superclass constructor.
  2. No extra field setup is needed.

### `public boolean dispatchTouchEvent(MotionEvent ev)`
```java
@Override
public boolean dispatchTouchEvent(MotionEvent ev) {
    switch (ev.getActionMasked()) {
        case MotionEvent.ACTION_DOWN:
            getParent().requestDisallowInterceptTouchEvent(true);
            break;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
            getParent().requestDisallowInterceptTouchEvent(false);
            break;
        default:
            break;
    }
    return super.dispatchTouchEvent(ev);
}
```
- **What it does:** On finger-down, blocks the parent from intercepting touches; on finger-up/cancel, allows the parent again; then passes the event to the normal `FrameLayout` handling.
- **Where it's called:** By the Android view system whenever a touch hits this wrapper (or is dispatched through it) on the report map.
- **Why it's written this way:** Without this, a `ScrollView` parent grabs vertical drags and the map cannot pan. Disallowing intercept only while the finger is down keeps page scrolling working elsewhere.
- **Step by step:**
  1. Look at `ev.getActionMasked()`.
  2. On `ACTION_DOWN`, call `getParent().requestDisallowInterceptTouchEvent(true)`.
  3. On `ACTION_UP` or `ACTION_CANCEL`, call it with `false`.
  4. Return `super.dispatchTouchEvent(ev)` so children (the map) still receive the event.
