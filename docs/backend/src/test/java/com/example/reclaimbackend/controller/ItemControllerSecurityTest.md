# ItemControllerSecurityTest.java

## Overview
This is a **slice test** (`@WebMvcTest`) for `ItemController` security: it boots only the web layer for that controller and checks that creating an item without a login token is rejected (see `docs/GLOSSARY.md`). `ItemService` and `JwtAuthenticationFilter` are mocked so the test focuses on HTTP security, not real DB or JWT parsing.

## Concepts used here
- **Controller / endpoint** — `POST /api/items` under test; see `docs/GLOSSARY.md`.
- **Authentication** — requests without a valid identity must not create items; see `docs/GLOSSARY.md`.
- **WebMvcTest** — Spring Boot test that loads only the MVC slice for one controller.
- **MockMvc** — fake HTTP client that calls the controller without a real server.
- **MockBean** — replaces real beans with mocks in the test context.

## Libraries and imports
- **Spring Boot Test** (`@WebMvcTest`, `@MockBean`, `MockMvc`): slice test and HTTP simulation.
- **JUnit 5** (`@Test`): marks the test method.
- **Jackson** (`ObjectMapper`): turns an `Item` into JSON for the request body.
- **Mockito / Spring**: mock `ItemService` and `JwtAuthenticationFilter`.
- **Project**: `ItemController` (under test), `Item` model.

## Fields
- `mockMvc` (`MockMvc`) — performs the HTTP request in-process.
- `objectMapper` (`ObjectMapper`) — serializes the sample item to JSON.
- `itemService` (`ItemService`) — mocked so no real create runs.
- `jwtAuthenticationFilter` (`JwtAuthenticationFilter`) — mocked filter bean for the security slice.

## Functions

### `@Test void createItem_withoutToken_returnsUnauthorized()`
```java
@Test
void createItem_withoutToken_returnsUnauthorized() throws Exception {
    Item item = new Item();
    item.setTitle("Wallet");
    item.setDescription("Black leather wallet");
    item.setCategory("Wallets");
    item.setLocation("Tel Aviv");
    item.setType("Found");

    mockMvc.perform(post("/api/items")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(item)))
            .andExpect(status().isForbidden());
}
```
- **What it does:** posts a new item JSON to `/api/items` with no `Authorization` header and expects the response status to be **forbidden** (`403`).
- **Where it's called:** by the JUnit test runner.
- **Why it's written this way:** proves production security: anonymous callers cannot create reports even if they send a valid-looking body. The method name says “Unauthorized,” but the assertion matches what Spring Security returns here (`403 Forbidden`).
- **Step by step:**
  1. Build a sample `Item` and serialize it to JSON.
  2. `POST /api/items` without a token.
  3. Assert HTTP status is forbidden.
