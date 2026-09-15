```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactResponse {

    private String name;
    private String email;
    private String phoneNumber;

}
```

What that function does: I hold a name, email, and phone so I can send contact details after a claim is approved or for a lost report.
What the function is responsible for: I am the reply shape for contact info. I do not contain extra user fields.
Where it works: I am built in ItemService.getLostItemOwnerContact and ClaimService.getContactForApprovedClaim, then returned from GET /api/items/{id}/contact and GET /api/claims/{claimId}/contact. ClaimServiceTest also reads me.
Why I chose to write the code of the function the way its written: I used a simple data class with no methods of my own. Lombok fills in getters, setters, and a builder so I stay short.
More: I use Lombok Getter, Setter, NoArgsConstructor, AllArgsConstructor, and Builder so Jackson and my services can make and fill me without handwritten code.
