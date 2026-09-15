```java
public JwtUtil(@Value("${jwt.secret}") String secret,
               @Value("${jwt.expiration}") long expiration) {
    this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.expirationMs = expiration;
}
```

What that function does: I load the signing key and how long tokens last from config.
What the function is responsible for: I am the constructor. I store the key and expiration for later token work.
Where it works: I am built by Spring as a @Component. AuthService and JwtAuthenticationFilter then use me.
Why I chose to write the code of the function the way its written: I turn the secret into an HMAC key once at startup so I do not rebuild it on every token.
More: I use jjwt Keys.hmacShaKeyFor and StandardCharsets.UTF_8. @Value reads jwt.secret and jwt.expiration.

```java
public String generateToken(String userId, String email) {
    Date now = new Date();
    Date expiry = new Date(now.getTime() + expirationMs);

    return Jwts.builder()
            .subject(userId)
            .claim("email", email)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(secretKey)
            .compact();
}
```

What that function does: I make a signed token with the user id as subject and email as a claim.
What the function is responsible for: I create login and post-signup tokens.
Where it works: I am called by AuthService.login and AuthService.verifyRegistration.
Why I chose to write the code of the function the way its written: I put userId in subject so the filter can set authentication from the token without a database lookup.
More: I use jjwt Jwts.builder, signWith, and compact. Date is issuedAt and expiration.

```java
public String getUserIdFromToken(String token) {
    return parseClaims(token).getSubject();
}
```

What that function does: I read the user id from a token's subject.
What the function is responsible for: I tell the filter who is signed in.
Where it works: I am called by JwtAuthenticationFilter.doFilterInternal after validateToken.
Why I chose to write the code of the function the way its written: I wrote it this way because subject is the user id I set in generateToken, so this is a one-line read.
More: I use jjwt Claims.getSubject after parseClaims checks the signature.

```java
public String getEmailFromToken(String token) {
    return parseClaims(token).get("email", String.class);
}
```

What that function does: I read the email claim from a token.
What the function is responsible for: I expose email stored on the token.
Where it works: I did not find callers in this backend.
Why I chose to write the code of the function the way its written: I kept it next to getUserIdFromToken in case a later caller needs email without loading the user.
More: I use Claims.get with String.class. parseClaims still checks the signature.

```java
public boolean validateToken(String token) {
    try {
        Claims claims = parseClaims(token);
        return !claims.getExpiration().before(new Date());
    } catch (Exception e) {
        return false;
    }
}
```

What that function does: I check the signature and that the token has not expired. Bad tokens return false.
What the function is responsible for: I decide if a Bearer token may sign the request in.
Where it works: I am called by JwtAuthenticationFilter.doFilterInternal.
Why I chose to write the code of the function the way its written: I catch any parse error and return false so a junk token does not blow up the filter.
More: I use Claims.getExpiration and Date. parseClaims does the signature check.

```java
private Claims parseClaims(String token) {
    return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
}
```

What that function does: I parse the token body after checking the signature.
What the function is responsible for: I am the shared parse used by the read and validate methods.
Where it works: I am called by getUserIdFromToken, getEmailFromToken, and validateToken. I am unused except internally.
Why I chose to write the code of the function the way its written: I wrote it this way because one parse path keeps verifyWith in one place.
More: I use jjwt Jwts.parser, verifyWith, parseSignedClaims, and getPayload.
