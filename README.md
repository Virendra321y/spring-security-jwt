# Spring Boot JWT Security & Architecture Guide

Welcome to the **Spring Security JWT** reference repository. This project implements a production-ready, stateless authentication system using Spring Boot, Spring Security, JSON Web Tokens (JWT), Refresh Tokens, Lombok, and ModelMapper.

---

## 1. Spring Security Flow Made Easy

Spring Security is essentially a chain of servlet filters (the **Security Filter Chain**) that intercept HTTP requests before they reach your Controller.

### The Request Lifecycle
1. **HTTP Request** enters the servlet container.
2. **DelegatingFilterProxy** routes the request to Spring's `FilterChainProxy`.
3. The request passes through several default filters (CORS, CSRF, Session management) until it reaches our custom **`JwtAuthenticationFilter`**.
4. **Token Verification**:
   - The filter extracts the Bearer token from the `Authorization` header.
   - The `JwtService` verifies the signature and checks if the token has expired.
   - The token contains the user's database `userId` (stored in the subject claim).
5. **User Loading**:
   - The filter loads user details using `userRepository.findById(userId)`.
6. **Context Setup**:
   - A `UsernamePasswordAuthenticationToken` is created, passing the loaded `User` object as the **Principal**.
   - This authentication token is stored inside the **`SecurityContextHolder`**.
7. The request is forwarded. When it reaches the controller and service layer, the application retrieves the logged-in user details directly from the security context, completely bypassing any redundant database lookups.

---

## 2. JWT vs. Refresh Token Flow

We implement a dual-token mechanism to ensure both **security** and **smooth user experience**.

| Feature | Access Token (JWT) | Refresh Token |
| :--- | :--- | :--- |
| **Lifespan** | Short-lived (e.g., 1 hour) | Long-lived (e.g., 7 days) |
| **Storage** | Client-side memory/state (Stateless) | Saved in database (Stateful) |
| **Transmission** | Passed in every HTTP Header | Passed only to the `/refresh` endpoint |
| **Purpose** | Authorizes access to API endpoints | Re-generates a new Access Token |
| **Revocation** | Cannot be revoked until it naturally expires | Can be deleted/revoked immediately from the DB |

### The Revocation Pattern
If an attacker compromises an Access Token, they only have a maximum of 1 hour of access. The Refresh Token is kept in the database so that if a user logs out, or if malicious activity is detected, the Refresh Token can be deleted. This instantly blocks the attacker from generating any new access tokens.

---

## 3. Interview Q&A Reference (Perfect Interview Replies)

Prepare for system design and coding interviews with these standard interview questions based on this codebase:

### Q1: Why do we store the `userId` in the JWT subject instead of the `email`?
> **Answer**: 
> Email addresses are mutable (users can change their emails) and contain personally identifiable information (PII) that should not be exposed unnecessarily in JWT payloads. A database primary key (`userId`) is **immutable, unique, and anonymous**, making it the industry standard identifier for relational mapping, foreign keys, and session contexts. Looking up a user in database by primary key is also significantly faster because it leverages database indexes directly.

### Q2: How does stateless authentication verify the user on every request without maintaining sessions?
> **Answer**: 
> Each request includes the JWT in the `Authorization: Bearer <token>` header. The server uses a secret key (known only to the backend) to verify the cryptographic signature of the JWT. If the signature is valid, the server trusts the payload (e.g., the `userId`). The filter then loads the user details into the `SecurityContext` for the duration of that single request thread. Once the HTTP response is sent, the request thread terminates, and no session state remains on the server.

### Q3: Why do we write a legacy fallback lookup inside the `JwtAuthenticationFilter`?
> **Answer**: 
> Transitioning existing production databases to use numeric `userId` tokens instead of email-based tokens can invalidate active sessions of existing users. By implementing a fallback catch block (`NumberFormatException` fallback), our filter checks if the token subject is a numeric ID. If it is not, it queries by email instead. This prevents existing client sessions from breaking during the database migration.

### Q4: Why did we transition to `@Autowired` field injection instead of constructor injection?
> **Answer**: 
> Field injection using `@Autowired` makes the code cleaner, highly readable, and eliminates verbose constructor boilerplate, especially in service implementation files with multiple dependencies (such as `AuthServiceImpl` which has 6 dependencies). When combined with Lombok annotations, it keeps classes focused on business logic.

### Q5: How do `ModelMapper` and Lombok's `@Builder` optimize this codebase?
> **Answer**: 
> - **Lombok `@Builder`**: Implements the Builder design pattern, allowing us to build immutable entities and request payloads without complex constructors or long chains of setter methods.
> - **`ModelMapper`**: Automates class-to-class mappings (e.g., mapping `Employee` entity to `EmployeeDto` and vice versa). This removes manual assignment boilerplate (`dto.setName(entity.getName())`) and reduces human error during object mapping.

---

## 4. How to Run & Build

### Prerequisites
* Java 17
* Maven 3+
* MySQL Server (running on port 3306)

### Running the App
1. Update database credentials in [application.properties](file:///c:/my-local-directory-uses/project%20for%20dev/spring-security-jwt/src/main/resources/application.properties):
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/jwt_tct?createDatabaseIfNotExist=true
   spring.datasource.username=YOUR_MYSQL_USERNAME
   spring.datasource.password=YOUR_MYSQL_PASSWORD
   ```
2. Build and run:
   ```bash
   mvn clean spring-boot:run
   ```
