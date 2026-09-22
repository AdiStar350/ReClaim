# CloseReportRequest.java

## Overview
This is a **DTO (Data Transfer Object)** — a small class used only to carry data in or out of the API, separate from the database model (see `docs/GLOSSARY.md`). `CloseReportRequest` is the JSON body a reporter sends when closing their own report by hand. It carries one value, `closureReason`, which must be either `"owner found"` or `"owner not found"`.

## Concepts used here
- **DTO** — a data-carrier class for the API; see `docs/GLOSSARY.md`.
- **Validation** — annotations reject bad input before our code runs; see `docs/GLOSSARY.md`.
- **Lombok** — generates getters, setters, constructors, and a builder; see `docs/GLOSSARY.md`.
- **Shared constant** — the allowed values come from `ClosureReasons`, so the rule lives in one place.

## Libraries and imports
- **Jakarta Bean Validation** (`NotBlank`, `Pattern`): require a value and force it to match an allowed pattern.
- **Project class** (`com.example.reclaimbackend.model.ClosureReasons`): provides `ClosureReasons.OWNER_INITIATED_PATTERN`, the regex `"owner found|owner not found"`. Using this constant means the DTO and the model agree on the wording.
- **Lombok** (`Getter`, `Setter`, `NoArgsConstructor`, `AllArgsConstructor`, `Builder`): generate accessors and constructors so the JSON library can build the object.

## Fields
```java
@NotBlank(message = "Closure reason is required")
@Pattern(regexp = ClosureReasons.OWNER_INITIATED_PATTERN,
        message = "Closure reason must be 'owner found' or 'owner not found'")
private String closureReason;
```
- `closureReason` — why the owner is closing the report. Only the two owner-initiated reasons are allowed here (claims close reports through a different path with their own reasons).

**Annotations used and why:**
- `@NotBlank` — the value must be present and non-empty.
- `@Pattern(regexp = ClosureReasons.OWNER_INITIATED_PATTERN)` — restricts the text to the two allowed reasons; anything else returns `400 Bad Request`.
- `@Getter`/`@Setter` — create `getClosureReason()`/`setClosureReason(...)`.
- `@NoArgsConstructor` — empty constructor the JSON library uses.
- `@AllArgsConstructor` / `@Builder` — build the object easily in code and tests.

Spring builds this object from the JSON body of `PUT /api/items/{id}/close` in `ItemController.closeReport`, which reads `getClosureReason()` and passes it to `ItemService.closeReport`.
