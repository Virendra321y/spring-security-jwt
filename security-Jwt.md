# Spring Boot Security & JWT Implementation Guide

This guide provides a comprehensive overview of how Spring Security, JSON Web Tokens (JWT), and Refresh Tokens are implemented and structured in this application.

---

## 1. Folder Structure

The project follows a standard layered Spring Boot package structure:

```
src/main/java/com/example/jwt/
├── SpringSecurityJwtApplication.java      # Application bootstrap class
├── config/
│   ├── JwtAuthenticationEntryPoint.java   # Handles unauthorized API access exceptions
│   ├── JwtAuthenticationFilter.java       # Intercepts requests, validates JWT, sets security context principal
│   ├── ModelMapperConfig.java             # Exposes ModelMapper bean for object-to-dto conversions
│   └── SecurityConfig.java                # Main Security filter chain, endpoints authorization rules
├── controller/
│   ├── AuthController.java                # Signup, Signin, and Token Refresh endpoints
│   └── EmployeeController.java            # Secure employee management resource endpoints
├── dto/
│   ├── EmployeeDto.java                   # DTO mapping for Employee resource
│   ├── RefreshTokenRequest.java           # DTO containing the refresh token string
│   ├── SignInRequest.java                 # Login request containing email and password
│   ├── SignUpRequest.java                 # Register request containing user details
│   ├── TokenResponse.java                 # Auth success response with access token, refresh token, and user info
│   └── UserDto.java                       # DTO representing User profile details
├── entity/
│   ├── Employee.java                      # Employee JPA Entity (linked to User)
│   ├── RefreshToken.java                  # Refresh Token JPA Entity (linked to User)
│   └── User.java                          # User JPA Entity (implements UserDetails)
├── repository/
│   ├── EmployeeRepository.java            # JPA repository interface for Employee database operations
│   ├── RefreshTokenRepository.java        # JPA repository interface for RefreshToken database operations
│   └── UserRepository.java                # JPA repository interface for User database operations
└── service/
    ├── AuthService.java                   # Interface for signin, signup, refresh token actions
    ├── EmployeeService.java               # Interface for Employee management actions
    ├── JwtService.java                    # Interface for generating/extracting JWTs
    ├── RefreshTokenService.java           # Interface for handling refresh token lifespan
    └── impl/
        ├── AuthServiceImpl.java           # Implementation of AuthService (uses Lombok Builders & ModelMapper)
        ├── EmployeeServiceImpl.java       # Implementation of EmployeeService (optimizes lookups using context principal)
        ├── JwtServiceImpl.java            # Implementation of JwtService (stores userId in subject)
        └── RefreshTokenServiceImpl.java   # Implementation of RefreshTokenService (manages DB-stored tokens)
```

---

## 2. Implementation Steps

The security architecture was built using these key steps:

1. **Model Definition**: Created `User` entity implementing Spring Security's `UserDetails` interface. Defined `Employee` and `RefreshToken` entities with foreign-key relationships pointing to `User`'s primary key (`id`).
2. **JWT Configuration**: Created `JwtServiceImpl` to encode JWTs. The user's database `id` (as a String) is stored in the JWT subject (`sub` claim) instead of their email. This aligns with standard practices for immutable user IDs.
3. **Request Filtering**: Built `JwtAuthenticationFilter` extending `OncePerRequestFilter`. On every incoming request:
   - Extract JWT from `Authorization: Bearer <token>` header.
   - Extract subject (the user's database ID) from the token.
   - Look up the user details by ID (`userRepository.findById(userId)`).
   - Set a `UsernamePasswordAuthenticationToken` in the `SecurityContextHolder`, containing the loaded `User` object as the principal.
   - Include a fallback check to verify legacy tokens containing email addresses in the subject field.
4. **Security Filter Chain configuration**: Setup `SecurityConfig` to:
   - Make authentication stateless (`SessionCreationPolicy.STATELESS`).
   - Allow public access to `/api/auth/**` (signup, signin, refresh).
   - Secure all other endpoints by requiring authentication.
   - Register `JwtAuthenticationFilter` before `UsernamePasswordAuthenticationFilter`.
5. **BOilerplate reduction (Lombok)**: Refactored all model/DTO files with Lombok annotations (`@Getter`, `@Setter`, `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`) to eliminate manual boilerplate code.
6. **Object Mappings (ModelMapper)**: Registered a `ModelMapper` bean to automatically map DTOs to entities and vice-versa, avoiding manual mapping helper methods.
7. **Clean Dependency Injection**: Swapped out constructors for clean `@Autowired` field injection in all controller, service, configuration, and filter components.

---

## 3. How Security Works Under the Hood

### Authentication Flow (Sign In)
```mermaid
sequenceDiagram
    participant Client
    participant AuthController
    participant AuthenticationManager
    participant UserDetailsService
    participant JwtService
    participant RefreshTokenService
    participant Database

    Client->>AuthController: POST /api/auth/signin (email + password)
    AuthController->>AuthenticationManager: authenticate()
    AuthenticationManager->>UserDetailsService: loadUserByUsername(email)
    UserDetailsService->>Database: Fetch user by email
    Database-->>UserDetailsService: User entity
    UserDetailsService-->>AuthenticationManager: UserDetails object
    AuthenticationManager-->>AuthController: Authenticated
    AuthController->>JwtService: generateToken(User) (uses user.id as subject)
    JwtService-->>AuthController: Access Token (JWT)
    AuthController->>RefreshTokenService: createRefreshToken(user.id)
    RefreshTokenService->>Database: Save generated Refresh Token
    RefreshTokenService-->>AuthController: RefreshToken entity
    AuthController-->>Client: TokenResponse (accessToken, refreshToken, userId, email)
```

### Authorization Flow (Secure Requests)
```mermaid
sequenceDiagram
    participant Client
    participant JwtAuthenticationFilter
    participant UserRepository
    participant SecurityContext
    participant Controller/Service

    Client->>JwtAuthenticationFilter: GET /api/employees with Bearer <token>
    JwtAuthenticationFilter->>JwtAuthenticationFilter: Extract JWT & subject (userId)
    alt Subject is numeric (userId)
        JwtAuthenticationFilter->>UserRepository: findById(userId)
    else Subject is email (Legacy fallback)
        JwtAuthenticationFilter->>UserRepository: findByEmail(email)
    end
    UserRepository-->>JwtAuthenticationFilter: User entity
    JwtAuthenticationFilter->>SecurityContext: Set Authentication (Principal = User object)
    JwtAuthenticationFilter->>Controller/Service: Forward Request
    Controller/Service->>SecurityContext: getPrincipal() (directly cast to User)
    Controller/Service-->>Client: Secure data response
```

---

## 4. API Reference & cURL Examples

All API requests assume the server is running on `http://localhost:8080`.

### 1. User Sign Up
Registers a new user account.

* **Endpoint**: `POST /api/auth/signup`
* **Request**:
```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Jane Doe",
    "email": "jane.doe@example.com",
    "password": "securepassword123",
    "address": "123 Main Street, NY",
    "salary": 75000.00
  }'
```
* **Response (Status 201 Created)**:
```json
{
  "id": 1,
  "name": "Jane Doe",
  "email": "jane.doe@example.com",
  "address": "123 Main Street, NY",
  "salary": 75000.0,
  "role": "ROLE_USER"
}
```

---

### 2. User Sign In
Authenticates credentials and returns access and refresh tokens.

* **Endpoint**: `POST /api/auth/signin`
* **Request**:
```bash
curl -X POST http://localhost:8080/api/auth/signin \
  -H "Content-Type: application/json" \
  -d '{
    "email": "jane.doe@example.com",
    "password": "securepassword123"
  }'
```
* **Response (Status 200 OK)**:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwiaWF0IjoxNzE2ODg5MDk1LCJleHAiOjE3MTY4OTI2OTV9.signature",
  "refreshToken": "d55c7a4c-eb99-4c12-9c1c-0b810d7a0494",
  "tokenType": "Bearer",
  "userId": 1,
  "email": "jane.doe@example.com"
}
```

---

### 3. Refresh Access Token
Obtains a new Access Token using a valid Refresh Token.

* **Endpoint**: `POST /api/auth/refresh`
* **Request**:
```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "d55c7a4c-eb99-4c12-9c1c-0b810d7a0494"
  }'
```
* **Response (Status 200 OK)**:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwiaWF0IjoxNzE2ODkwMDAwLCJleHAiOjE3MTY4OTM2MDB9.signature",
  "refreshToken": "e66d8b5d-fc00-4d23-ad2d-1c920e8b15a5",
  "tokenType": "Bearer",
  "userId": 1,
  "email": "jane.doe@example.com"
}
```

---

### 4. Create Employee (Secure)
Creates an employee associated with the authenticated user context.

* **Endpoint**: `POST /api/employees`
* **Request**:
```bash
curl -X POST http://localhost:8080/api/employees \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwiaWF0IjoxNzE2ODg5MDk1LCJleHAiOjE3MTY4OTI2OTV9.signature" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Smith",
    "email": "john.smith@company.com",
    "address": "456 Oak Ave, CA",
    "salary": 60000.00
  }'
```
* **Response (Status 201 Created)**:
```json
{
  "id": 1,
  "name": "John Smith",
  "email": "john.smith@company.com",
  "address": "456 Oak Ave, CA",
  "salary": 60000.0
}
```

---

### 5. Get All Employees (Secure)
Retrieves all employees associated with the authenticated user context.

* **Endpoint**: `GET /api/employees`
* **Request**:
```bash
curl -X GET http://localhost:8080/api/employees \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwiaWF0IjoxNzE2ODg5MDk1LCJleHAiOjE3MTY4OTI2OTV9.signature"
```
* **Response (Status 200 OK)**:
```json
[
  {
    "id": 1,
    "name": "John Smith",
    "email": "john.smith@company.com",
    "address": "456 Oak Ave, CA",
    "salary": 60000.0
  }
]
```
