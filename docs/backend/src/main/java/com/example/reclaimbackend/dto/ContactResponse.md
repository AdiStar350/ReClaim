# ContactResponse.java

## Overview
This is a **DTO (Data Transfer Object)** — a small class used only to carry data in or out of the API, kept separate from the database model (see `docs/GLOSSARY.md`). `ContactResponse` is an **output** DTO: it is the JSON the server sends back with another person's contact details. It is returned only after two people are allowed to talk (an approved claim, or a finder reaching a lost-item reporter), so contact info is never exposed by accident.

## Concepts used here
- **DTO** — a data-carrier class for the API; see `docs/GLOSSARY.md`.
- **Lombok** — generates getters, setters, constructors, and a builder; see `docs/GLOSSARY.md`.

## Libraries and imports
- **Lombok** (`Getter`, `Setter`, `NoArgsConstructor`, `AllArgsConstructor`, `Builder`): generate accessors, an empty constructor, an all-fields constructor, and a `builder()` used by the services to fill this object one field at a time. There is no validation here because this DTO is only ever sent out, never received.

## Fields
```java
private String name;
private String email;
private String phoneNumber;
```
- `name` — the other person's display name.
- `email` — the other person's email address.
- `phoneNumber` — the other person's phone number.

**Annotations used and why:**
- `@Getter`/`@Setter` — create getters/setters; the getters are what the JSON library reads to build the response.
- `@Builder` — lets services write `ContactResponse.builder().name(...).email(...).phoneNumber(...).build()`.
- `@NoArgsConstructor` / `@AllArgsConstructor` — provide empty and all-fields constructors.

This object is built by `ClaimService.getContactForApprovedClaim` and by `ItemService.getLostItemOwnerContact`, then returned as the body of `GET /api/claims/{claimId}/contact` (via `ClaimController.getClaimContact`) and `GET /api/items/{id}/contact` (via `ItemController.getLostItemOwnerContact`).
