## Slice 2 — Login & JWT (S2)

**Prompt 1:**

```
CONTEXT
Spring Boot 3.x, Spring Security 6, Java 17+, jjwt 0.12.x. No other libraries.
Package layout:
  com.esd.lab2
  ├── config/       SecurityConfig.java, AdminSeeder.java
  ├── controller/   AuthController.java, CourseController.java
  ├── service/      AuthService.java, CourseService.java
  ├── dto/          SignupRequest.java, UserResponse.java, CourseRequest.java, CourseResponse.java
  ├── entity/       User.java, Role.java (enum), Course.java
  ├── repository/   UserRepository.java, CourseRepository.java
  ├── exception/    ApiError.java, GlobalExceptionHandler.java,
  │                 DuplicateResourceException.java, InvalidCredentialsException.java,
  │                 ResourceNotFoundException.java
  └── security/     JsonAuthenticationEntryPoint.java, JsonAccessDeniedHandler.java
Existing code you must use (do not rewrite it):
AuthService (with signup), UserRepository.findByEmail, PasswordEncoder bean,
InvalidCredentialsException (401). application.yml has jwt.secret (Base64) and
jwt.expiration-seconds: 3600.

TASK
Implement only: POST /api/auth/login
Contract: A2 | POST /api/auth/login | Public | { email, password } | 200 LoginResponse | 400, 401
LoginRequest  { email: required, valid email; password: required }
LoginResponse { accessToken, tokenType: "Bearer", expiresIn }
Normalise email (trim + lowercase). Use the same 401 message for unknown email and
wrong password. Token: HS256, claims sub = email, role, jti = random UUID, iat, exp.
expiresIn must come from config, not be hardcoded. Do not write token parsing yet.

ALLOWED TO CREATE OR CHANGE
dto/LoginRequest.java, dto/LoginResponse.java, security/JwtService.java (token generation only),
service/AuthService.java (login method only), controller/AuthController.java (login endpoint only)

RULES
- Do not create or modify any file not listed above.
- Use the existing exceptions in com.esd.lab2.exception. Do not create new error classes.
- Use DTOs for request and response. Never return an entity from a controller.
- Use jakarta.validation annotations for input validation.
- Use only APIs that exist in Spring Security 6 and jjwt 0.12. No deprecated methods.
- If something is unclear, add a TODO comment instead of guessing.

OUTPUT
Each file in full, with its path. Then a list of anything you assumed.
```

**Follow-up (if any):**

```
None
```

---

## Slice 3 — JWT Filter & Protected API (S3)

**Prompt 1:**

```
CONTEXT
Spring Boot 3.x, Spring Security 6, Java 17+, jjwt 0.12.x. No other libraries.
Package layout: same as S2, plus dto/LoginRequest.java, dto/LoginResponse.java,
security/JwtService.java
Existing code you must use (do not rewrite it):
JwtService (generateToken, SecretKey from jwt.secret), AuthService.toResponse,
UserRepository.findByEmail, JsonAuthenticationEntryPoint, JsonAccessDeniedHandler,
ResourceNotFoundException (404).

TASK
Implement only: JWT authentication filter, security rules, and GET /api/users/me
Contract:
P1 | GET /api/courses | Public | 200 list
P2 | GET /api/users/me | Any logged-in user | 200 UserResponse | 401
- JwtService.parseToken using Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload()
- Filter extends OncePerRequestFilter, reads "Authorization: Bearer <token>", sets
  UsernamePasswordAuthenticationToken with authority "ROLE_" + role. On a bad token,
  clear the context and continue the chain. The filter must not write a response.
- SecurityConfig: CSRF disabled, STATELESS, permitAll only POST /api/auth/signup,
  POST /api/auth/login, GET /api/courses; anyRequest().authenticated(); wire entry
  point and access-denied handler via exceptionHandling; add filter before
  UsernamePasswordAuthenticationFilter.

ALLOWED TO CREATE OR CHANGE
security/JwtService.java (parseToken only), security/JwtAuthenticationFilter.java,
config/SecurityConfig.java (filterChain only), controller/UserController.java,
service/AuthService.java (getCurrentUser method only)

RULES
- Do not create or modify any file not listed above.
- Use the existing exceptions in com.esd.lab2.exception. Do not create new error classes.
- Use DTOs for request and response. Never return an entity from a controller.
- Use jakarta.validation annotations for input validation.
- Use only APIs that exist in Spring Security 6 and jjwt 0.12. No deprecated methods.
- Spring Security 6 lambda DSL only: no WebSecurityConfigurerAdapter, antMatchers,
  authorizeRequests or .and(). No UserDetailsService, AuthenticationManager, sessions or CORS.
- If something is unclear, add a TODO comment instead of guessing.

OUTPUT
Each file in full, with its path. Then a list of anything you assumed.
```

**Follow-up (if any):**

```
None
```

---

## Slice 4 — Role Restriction (S4)

**Prompt 1:**

```
CONTEXT
Spring Boot 3.x, Spring Security 6, Java 17+, jjwt 0.12.x. No other libraries.
Package layout: same as S3, plus security/JwtAuthenticationFilter.java,
controller/UserController.java
Existing code you must use (do not rewrite it):
SecurityConfig.filterChain from S3. The filter sets authority "ROLE_" + role.
CourseController POST /api/courses, JsonAccessDeniedHandler (403).

TASK
Implement only: ADMIN-only course creation
Contract: P3 | POST /api/courses | ADMIN only | { code, title, description } | 201 course | 400, 401, 403, 409
Add .requestMatchers(HttpMethod.POST, "/api/courses").hasRole("ADMIN") immediately
before .anyRequest().authenticated(). GET /api/courses must stay public.

ALLOWED TO CREATE OR CHANGE
config/SecurityConfig.java (one rule in filterChain only)

RULES
- Do not create or modify any file not listed above.
- Keep the diff minimal. Do not rewrite the rest of the config.
- Use the existing exceptions in com.esd.lab2.exception. Do not create new error classes.
- Use only APIs that exist in Spring Security 6 and jjwt 0.12. No deprecated methods.
- If something is unclear, add a TODO comment instead of guessing.

OUTPUT
The changed lines with the file path. Then a list of anything you assumed.
```

**Follow-up (if any):**

```
STUDENT token on POST /api/courses returned 201 instead of 403. Why is the
hasRole("ADMIN") rule not being applied?
(Answer: the app had not been restarted after editing SecurityConfig. After a
restart it returned 403. No code change needed.)
```

---

## Slice 5 — Logout *(stretch, S5)*

**Prompt 1:**

```
CONTEXT
Spring Boot 3.x, Spring Security 6, Java 17+, jjwt 0.12.x. No other libraries.
Package layout: same as S4.
Existing code you must use (do not rewrite it):
JwtService.parseToken, JwtAuthenticationFilter, AuthController. Tokens already carry
a random UUID jti and an exp claim.

TASK
Implement only: POST /api/auth/logout with a server-side token denylist
Contract: A3 | POST /api/auth/logout | Logged-in | none | 204 | 401
- TokenDenylist: @Component with ConcurrentHashMap<String, Instant>; revoke(jti, expiry),
  isRevoked(jti), cleanExpired().
- Logout: read the Bearer token, parse it, revoke its jti with its expiry, return 204.
  The endpoint must NOT be in permitAll.
- Filter: after parsing, if the jti is revoked, clear the context and continue, treating
  it the same as an expired token (client gets 401).

ALLOWED TO CREATE OR CHANGE
security/TokenDenylist.java, controller/AuthController.java (logout endpoint only),
security/JwtAuthenticationFilter.java (denylist check only)

RULES
- Do not create or modify any file not listed above.
- Use the existing exceptions in com.esd.lab2.exception. Do not create new error classes.
- Denylist must be keyed by jti, not email.
- Use only APIs that exist in Spring Security 6 and jjwt 0.12. No deprecated methods.
- If something is unclear, add a TODO comment instead of guessing.

OUTPUT
Each file in full, with its path. Then a list of anything you assumed.
```

**Follow-up (if any):**

```
None
```