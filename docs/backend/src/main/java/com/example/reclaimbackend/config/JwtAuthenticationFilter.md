```java
@Override
protected void doFilterInternal(HttpServletRequest request,
                                HttpServletResponse response,
                                FilterChain filterChain)
        throws ServletException, IOException {

    String token = extractTokenFromHeader(request);

    if (token != null && jwtUtil.validateToken(token)) {
        String userId = jwtUtil.getUserIdFromToken(token);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userId,       // the user id
                        null,         // no password here
                        Collections.emptyList()  // no roles
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    filterChain.doFilter(request, response);
}
```

What that function does: I read the Bearer token on each request and, if it is good, set the user id on the security context.
What the function is responsible for: I am the JWT filter. I do not block the request myself if the token is missing.
Where it works: I run on each request because SecurityConfig adds me before UsernamePasswordAuthenticationFilter. ItemControllerSecurityTest mocks me.
Why I chose to write the code of the function the way its written: I always call filterChain.doFilter so public routes still work. SecurityConfig then requires auth on the rest.
More: I use OncePerRequestFilter, JwtUtil, UsernamePasswordAuthenticationToken, SecurityContextHolder, and Collections.emptyList because this app does not use roles.

```java
private String extractTokenFromHeader(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");
    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
        return bearerToken.substring(7);
    }
    return null;
}
```

What that function does: I pull the token from the Authorization header, or return null if it is missing.
What the function is responsible for: I strip the Bearer prefix.
Where it works: I am called by doFilterInternal. I am unused except internally.
Why I chose to write the code of the function the way its written: I check the prefix before substring(7) so a short header cannot throw.
More: I use HttpServletRequest.getHeader and Spring StringUtils.hasText.
