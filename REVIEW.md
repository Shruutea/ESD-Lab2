# REVIEW.md — AI Output Review Log

Complete **one review entry per slice** before moving to the next.  


---

## Slice 1 — Signup (S1)

### A. Bucket Check
List every class and method the AI produced. Tag each one:  
`Controller` / `Service` / `Repository` / `DTO` / `Security` / `Config` / `Exception`

| File / Method | Tag | In Allowed List? |
|---|---|---|
| `SignupRequest` (validation annotations) | DTO | Yes |
| `UserResponse` (already provided, verified only) | DTO | Yes |
| `AuthService.signup()` | Service | Yes |
| `AuthService.toResponse()` | Service | Yes |
| `AuthController.signup()` | Controller | Yes |

> **Important:** Anything **not** in the allowed list → **reject, re-prompt**. Don't keep it because it works.

### B. Contract Check
Compare against API contract row A1 line by line.

| Check | Pass / Fail |
|---|---|
| Path is `POST /api/auth/signup` | Pass |
| Returns **201** | Pass |
| Response shape: `{ id, email, fullName, role, createdAt }` | Pass |
| No `passwordHash` field in response | Pass |
| Role in response is always `STUDENT` | Pass |

### C. Validation & Errors

| Check | Pass / Fail |
|---|---|
| `@Valid` on controller parameter | Pass |
| `@Email`, `@NotBlank`, `@Size` on DTO fields | Pass |
| Duplicate email throws `DuplicateResourceException` | Pass |
| No new error/response wrapper class invented | Pass |
| No stack trace or exception message leaked to client | Pass |

### D. Security Specifics

| Check | Pass / Fail |
|---|---|
| Password hashed via injected `PasswordEncoder` (not `new BCryptPasswordEncoder()`) | Pass |
| `SignupRequest` has no `role` field, or it is ignored | Pass |
| Duplicate check uses normalised (lowercased/trimmed) email | Pass |
| AI did **not** touch `SecurityConfig`, add JWT class, or add `UserDetailsService` | Pass |

### E. Understanding Questions
*Answer in your own words (1–2 lines each)*

1. Where exactly is the password hashed, and why is the same password stored as a different string for two users?

   > It is hashed in `AuthService.signup()` using `passwordEncoder.encode()`, where the encoder is the BCrypt bean from `SecurityConfig`. BCrypt generates a random salt for every hash and stores it inside the hash, so the same password gives a different string for each user.

2. What would happen if `SignupRequest` accepted a `role` field and the AI used it?

   > Any user could send `"role": "ADMIN"` during signup and give themselves admin access (privilege escalation). That's why the service always hardcodes `Role.STUDENT`.


3. Which class turns `DuplicateResourceException` into a 409 response?

   > `GlobalExceptionHandler` (a `@RestControllerAdvice`). Its `handleDuplicate()` method catches the exception and returns an `ApiError` body with status 409 Conflict.

### F. Decision

| `SignupRequest.java` | Accepted | |
| `AuthService.java` | Accepted | |
| `AuthController.java` | Accepted | |
---

## Slice 2 — Login & JWT (S2)

### A. Bucket Check

| File / Method | Tag | In Allowed List? |
|---|---|---|
| `LoginRequest` | DTO | Yes |
| `LoginResponse` | DTO | Yes |
| `JwtService` (constructor + `generateToken()` + `getExpirationSeconds()`) | Security | Yes |
| `AuthService.login()` | Service | Yes |
| `AuthController.login()` | Controller | Yes |

### B. Contract Check

| Check | Pass / Fail |
|---|---|
| Path is `POST /api/auth/login` | Pass |
| Returns **200** | Pass |
| Response: `{ accessToken, tokenType: "Bearer", expiresIn }` | Pass |
| `expiresIn` matches configured value (not hardcoded) | Pass |


### C. Validation & Errors

| Check | Pass / Fail |
|---|---|
| Same 401 message for unknown email and wrong password | Pass |
| No `try/catch` swallowing exceptions silently | Pass |

### D. Security Specifics

| Check | Pass / Fail |
|---|---|
| JWT secret comes from `@Value`, no string literal secret | Pass |
| Uses jjwt 0.12 API: `.subject()`, `.claim()`, `.id()`, `.issuedAt()`, `.expiration()`, `.signWith(key)` | Pass |
| Rejects deprecated: `setSubject`, `setExpiration`, `signWith(SignatureAlgorithm.HS256, secret)`, `Jwts.parserBuilder()` | Pass |
| `jti` is a random UUID (not the user id) | Pass |
| No password or hash in token claims | Pass |
| Parse/validate method **not** written yet (that is S3) | Pass |

### E. Understanding Questions

1. Why must the error message be identical for "no such email" and "wrong password"?

   > If the messages were different, an attacker could use the login endpoint to find out which emails are registered (user enumeration) and then target only those accounts.


2. Who can read the claims inside your JWT? Who can change them?

   > Anyone who has the token can read the claims, because the payload is only Base64-encoded, not encrypted. Only the server can change them, because any change breaks the HMAC signature, which needs the secret key to recreate.


3. What does `jti` give us that `sub` doesn't? (Think about S5.)

   > `sub` identifies the user, so it is the same for all of that user's tokens. `jti` is a unique ID for one specific token, so on logout we can revoke just that token without blocking the user's other tokens.


### F. Decision

| File | Decision | Reason |
|---|---|---|
| `LoginRequest.java` | Accepted | |
| `LoginResponse.java` | Accepted | |
| `JwtService.java` | Accepted | Uses `signWith(key, Jwts.SIG.HS256)` to pin the algorithm to HS256 as per the contract (still the jjwt 0.12 API). |
| `AuthService.java` | Accepted | |
| `AuthController.java` | Accepted | |

---

## Slice 3 — JWT Filter & Protected API (S3)

### A. Bucket Check

| File / Method | Tag | In Allowed List? |
|---|---|---|
| `JwtService.parseToken()` | Security | Yes |
| `JwtAuthenticationFilter.doFilterInternal()` | Security | Yes |
| `SecurityConfig.filterChain()` | Config | Yes |
| `UserController.me()` | Controller | Yes |
| `AuthService.getCurrentUser()` | Service | Yes |

### B. Contract Check

| Check | Pass / Fail |
|---|---|
| `GET /api/courses` works **without** token | Pass |
| `GET /api/users/me` returns **401** with no token | Pass |
| `GET /api/users/me` returns **200** with valid token | Pass |
| 401 body is an `ApiError` JSON object | Pass |

### C. Validation & Errors

| Check | Pass / Fail |
|---|---|
| Filter does not write a response on error — continues chain | Pass |
| Filter clears SecurityContext on bad token | Pass |


### D. Security Specifics

| Check | Pass / Fail |
|---|---|
| Spring Security 6 style: lambda DSL, `requestMatchers`, `authorizeHttpRequests` | Pass |
| Rejected: `WebSecurityConfigurerAdapter`, `antMatchers`, `authorizeRequests`, `.and()` | Pass |
| `permitAll` covers only the three specific public endpoints | Pass |
| `GET /api/courses` is public but `POST /api/courses` is NOT (watch for broad matcher) | Pass |
| Signature + expiry verified with `Jwts.parser().verifyWith(key).build().parseSignedClaims(token)` | Pass |
| Authorities use `ROLE_` prefix | Pass |
| Entry point and access-denied handler wired via `exceptionHandling(...)` | Pass |
| No `UserDetailsService`, `AuthenticationManager`, sessions or CORS added | Pass |
| `GET /api/users/me` returns `UserResponse`, not the entity | Pass |

### E. Understanding Questions

1. Why is CSRF protection disabled here, and when would disabling it be wrong?

   > CSRF attacks work because the browser automatically sends cookies with every request. Our API uses a token in the `Authorization` header and no session cookie, so a malicious site can't make the browser send it. Disabling CSRF would be wrong if we used cookie-based sessions or stored the JWT in a cookie.

2. Walk through what happens to a request with an expired token, step by step, until the client sees 401.

   > The request reaches `JwtAuthenticationFilter`, which reads the Bearer token and calls `parseToken()`. jjwt sees the `exp` is in the past and throws `ExpiredJwtException`. The filter catches it, clears the SecurityContext, and continues the chain as anonymous. The `anyRequest().authenticated()` rule then rejects the anonymous request, so Spring Security calls `JsonAuthenticationEntryPoint`, which writes the 401 `ApiError` JSON.

3. Why don't 401/403 errors from the filter chain reach `GlobalExceptionHandler`?

   > `GlobalExceptionHandler` only handles exceptions thrown from controllers (inside the DispatcherServlet). Security filters run before the request reaches any controller, so their 401/403 must be written by `JsonAuthenticationEntryPoint` and `JsonAccessDeniedHandler` instead.

### F. Decision

| File | Decision | Reason |
|---|---|---|
| `JwtService.java` | Accepted | |
| `JwtAuthenticationFilter.java` | Accepted | Filter is a `@Component` and also added with `addFilterBefore`; `OncePerRequestFilter` stops it from running twice in the same request. |
| `SecurityConfig.java` | Accepted | |
| `UserController.java` | Accepted | |
| `AuthService.java` | Accepted | Added `getCurrentUser()` to reuse the existing `toResponse()` mapper. |

---

## Slice 4 — Role Restriction (S4)

### A. Bucket Check

| File / Method | Tag | In Allowed List? |
|---|---|---|
| `SecurityConfig.filterChain()`: one `hasRole("ADMIN")` rule | Config | Yes |

### B. Contract Check

| Check | Pass / Fail |
|---|---|
| `POST /api/courses` with no token → **401** | Pass |
| `POST /api/courses` with STUDENT token → **403** | Pass |
| `POST /api/courses` with ADMIN token → **201** | Pass |
| 403 body is an `ApiError` JSON object | Pass |

### D. Security Specifics

| Check | Pass / Fail |
|---|---|
| Role naming consistent: `hasRole("ADMIN")` ↔ authority `ROLE_ADMIN` | Pass |
| If `@PreAuthorize` used: `@EnableMethodSecurity` is present | N/A (rule is in SecurityConfig) |
| Rule placed **before** `anyRequest().authenticated()` if in SecurityConfig | Pass |
| Diff is small — AI did not rewrite the whole config | Pass |

### E. Understanding Questions

1. A STUDENT calls `POST /api/courses`. Why does the server return 403 and not 401?

   > 401 means "we don't know who you are". The student has a valid token, so they are authenticated. 403 means "we know who you are, but you don't have permission", because they have `ROLE_STUDENT` and the rule needs `ROLE_ADMIN`.

2. Where does the role in the JWT come from, and can the client change it?

   > `JwtService.generateToken()` takes it from the user's row in the database at login. The client can't change it, because editing the payload breaks the signature, and `parseToken()` would reject the token.
### F. Decision

| File | Decision | Reason |
|---|---|---|
| `SecurityConfig.java` | Accepted | First STUDENT test returned 201 because the app had not been restarted after the edit; after restarting it correctly returned 403. |

---

## Slice 5 — Logout (S5) *(stretch)*

### A. Bucket Check

| File / Method | Tag | In Allowed List? |
|---|---|---|
| `TokenDenylist` (`revoke()`, `isRevoked()`, `cleanExpired()`) | Security | Yes |
| `AuthController.logout()` | Controller | Yes |
| `JwtAuthenticationFilter.doFilterInternal()`: denylist check | Security | Yes |

### B. Contract Check

| Check | Pass / Fail |
|---|---|
| `POST /api/auth/logout` returns **204** | Pass |
| Same token returns **401** after logout | Pass |
| Fresh login after logout returns a new working token | Pass |

### D. Security Specifics

| Check | Pass / Fail |
|---|---|
| Logout adds entry to server-side denylist (not just tells client to delete token) | Pass |
| Denylist keyed by **jti**, not email | Pass |
| Map is `ConcurrentHashMap` (thread-safe) | Pass |
| Logout endpoint is NOT under `permitAll` | Pass |
| Filter checks the denylist and treats denylisted jti as invalid | Pass |
| (Bonus) Expired entries are removed from the map | Pass |

### E. Understanding Questions

1. Why does logout need server-side state if JWT is "stateless"?

   > *Your answer:* A JWT stays valid until its expiry, and the server normally keeps no record of it. Deleting it on the client doesn't stop a stolen copy from working, so the server has to remember revoked tokens to really log the user out.

2. What happens to the denylist when the server restarts? What if there are two server instances?

   > *Your answer:* It is stored in memory, so a restart clears it and logged-out tokens that haven't expired work again. With two instances, each has its own map, so a token revoked on one still works on the other. A shared store like Redis or a database table would fix both problems.

3. Why store the expiry time with each jti?

   > *Your answer:* After a token expires, `parseToken()` rejects it anyway, so we don't need to remember it. Storing the expiry lets `cleanExpired()` remove those entries so the map doesn't keep growing.

### F. Decision

| File | Decision | Reason |
|---|---|---|
| `TokenDenylist.java` | Accepted | `cleanExpired()` is called inside `revoke()`, so no scheduler is needed. |
| `AuthController.java` | Accepted | |
| `JwtAuthenticationFilter.java` | Accepted | |

**Explanation (your own words):**
**Files changed:**

| File | What changed and why |
|---|---|
| `dto/SignupRequest.java` | Added `@NotBlank`, `@Email`, `@Size` so invalid signup input is rejected with 400 before reaching the service. |
| `dto/LoginRequest.java` (new) | Login input with `@NotBlank` and `@Email`. |
| `dto/LoginResponse.java` (new) | Returns `accessToken`, `tokenType` ("Bearer") and `expiresIn`. |
| `service/AuthService.java` | `signup()` normalises email, checks duplicates (409), hashes password with the injected `PasswordEncoder`, always sets `Role.STUDENT`. `login()` checks password with `matches()` and returns the same 401 for unknown email and wrong password. `getCurrentUser()` returns the logged-in user as `UserResponse`. |
| `controller/AuthController.java` | Added `POST /signup` (201), `POST /login` (200) and `POST /logout` (204). |
| `security/JwtService.java` (new) | Creates HS256-signed tokens (`sub`, `role`, random `jti`, `iat`, `exp`) using the secret from `@Value`, and `parseToken()` verifies signature and expiry. |
| `security/JwtAuthenticationFilter.java` (new) | Reads the Bearer token on every request, verifies it, and sets the user with authority `ROLE_<role>` in the SecurityContext. Bad, expired or revoked tokens are treated as anonymous. |
| `config/SecurityConfig.java` | Replaced the temporary permit-all config: stateless, CSRF disabled, only signup, login and `GET /api/courses` public, `POST /api/courses` ADMIN-only, everything else needs login, JSON 401/403 handlers wired, JWT filter added. |
| `controller/UserController.java` (new) | `GET /api/users/me` returns the current user's `UserResponse`. |
| `security/TokenDenylist.java` (new) | Thread-safe `ConcurrentHashMap` of revoked `jti`s with expiry, so logged-out tokens are rejected; expired entries are cleaned up. |

**Explanation :**
> I added stateless JWT authentication to the app. Signup stores users with a BCrypt-hashed password and always gives them the STUDENT role. Login checks the password and returns a signed JWT containing the user's email, role and a unique `jti`. On every request, `JwtAuthenticationFilter` verifies the token's signature and expiry and tells Spring Security who the user is and what role they have. `SecurityConfig` then decides access: public endpoints are open, `/api/users/me` needs any valid token (401 without one), and creating a course needs `ROLE_ADMIN` (403 for students). Logout adds the token's `jti` to a server-side denylist, so that exact token is rejected afterwards even though it hasn't expired. This works because the token can't be changed without the secret key, so the role inside it can be trusted.
