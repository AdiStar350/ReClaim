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

What that function does: I post a new item without a token and expect the request to be blocked.
What the function is responsible for: I test that POST /api/items is not public. SecurityConfig requires a signed-in user. I assert forbidden.
Where it works: I run under JUnit through MockMvc against ItemController. I do not call ItemService.createItem because the filter should stop first.
Why I chose to write the code of the function the way its written: I wrote it this way because webMvcTest plus a mocked JWT filter is enough to show an unauthenticated post is rejected. The method name says unauthorized; the assertion is isForbidden because that is what this setup returns.
More: I use MockMvc, ObjectMapper, MediaType.APPLICATION_JSON, and status().isForbidden. JwtAuthenticationFilter and ItemService are @MockBean.
