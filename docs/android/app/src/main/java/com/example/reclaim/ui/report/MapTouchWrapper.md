```java
public MapTouchWrapper(@NonNull Context context) {
    super(context);
}
```

What that function does: I make the wrapper in code with only a context.
What the function is responsible for: I am the one-arg FrameLayout constructor.
Where it works: Android may use me if the view is created in Java. The report layout uses the XML constructors.
Why I chose to write the code of the function the way its written: FrameLayout needs this constructor chain.
More: I call super(context).

```java
public MapTouchWrapper(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
}
```

What that function does: I make the wrapper from XML attributes.
What the function is responsible for: I am the constructor the layout inflater uses.
Where it works: activity_report.xml inflates me around the map.
Why I chose to write the code of the function the way its written: Two-arg is the usual XML constructor.
More: I call super(context, attrs).

```java
public MapTouchWrapper(@NonNull Context context, @Nullable AttributeSet attrs,
                       int defStyleAttr) {
    super(context, attrs, defStyleAttr);
}
```

What that function does: I make the wrapper with a style attribute.
What the function is responsible for: I am the three-arg FrameLayout constructor.
Where it works: Android uses me if a style is set in XML.
Why I chose to write the code of the function the way its written: I keep the full constructor chain FrameLayout offers.
More: I call super(context, attrs, defStyleAttr).

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

What that function does: I tell the scrolling parent not to steal the drag while a finger is on the map.
What the function is responsible for: I let the map pan inside a NestedScrollView.
Where it works: I wrap the map in activity_report.xml. ReportActivity hosts that layout.
Why I chose to write the code of the function the way its written: DOWN blocks the parent. UP/CANCEL give the parent back so the page can scroll again.
More: I use requestDisallowInterceptTouchEvent and super.dispatchTouchEvent.
