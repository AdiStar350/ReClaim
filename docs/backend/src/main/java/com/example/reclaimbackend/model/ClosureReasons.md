# ClosureReasons.java

## Overview
This file is a **constants holder**: a small `final` class whose only job is to give names to fixed text values, so the same strings aren't retyped (and mistyped) all over the code. It lists the reasons a report or `Claim` can be closed. It is not a Controller/Service/Model/DTO — it stores no data and has no behavior; it just centralizes wording. (For those general roles see `docs/GLOSSARY.md`.)

## Concepts used here
- **Constant (`static final`)** — a value fixed at compile time and shared by the whole program; naming it once avoids typos.
- **Utility/constants class** — a class you never create an object of; you use its `static` members directly (`ClosureReasons.OWNER_FOUND`).

## Libraries and imports
None. This file has no imports — it uses only plain Java `String` constants.

## Fields
```java
public static final String OWNER_FOUND = "owner found";
public static final String OWNER_NOT_FOUND = "owner not found";
public static final String CLAIM_DECLINED = "claim declined";
public static final String CLAIM_APPROVED = "claim approved";

/** Allowed reasons when the owner closes a report. */
public static final String OWNER_INITIATED_PATTERN = "owner found|owner not found";
```
- `OWNER_FOUND` / `OWNER_NOT_FOUND` — reasons the reporter picks when closing a report by hand.
- `CLAIM_DECLINED` / `CLAIM_APPROVED` — reasons set automatically when a claim is reviewed.
- `OWNER_INITIATED_PATTERN` — a regular-expression string (`"owner found|owner not found"`) listing the only reasons allowed when the owner closes a report. `CloseReportRequest` uses this in its `@Pattern` validation, so the DTO and the model can't drift apart.

These constants are used by `ClaimService` (setting a claim's/item's `closureReason` on approve/decline) and by `CloseReportRequest` (validation), and the values are asserted in `ClaimServiceTest`.

## Functions

### `private ClosureReasons()`
```java
private ClosureReasons() {
}
```
- **What it does:** an empty, private constructor that does nothing.
- **Where it's called:** nowhere — that is the point. Making the constructor `private` stops any other code from writing `new ClosureReasons()`.
- **Why it's written this way:** this class only holds `static` constants, so there is never a reason to create an instance. A private constructor makes that intent clear and prevents accidental objects. Combined with the class being `final`, no one can create or subclass it.
- **Step by step:**
  1. The constructor takes no arguments and has an empty body.
  2. Because it is `private`, only code inside `ClosureReasons` could call it, and none does.
