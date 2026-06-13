<div align="center">

# Cursed Auth

### A dedicated JWT authentication service for every service that needs identity

[![Java](https://img.shields.io/badge/Java-25-fd5c63?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.2-6db33f?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![JWT](https://img.shields.io/badge/JWT-RS512-111111?style=for-the-badge&logo=jsonwebtokens&logoColor=white)](https://jwt.io/)
[![MongoDB](https://img.shields.io/badge/MongoDB-Users-47a248?style=for-the-badge&logo=mongodb&logoColor=white)](https://www.mongodb.com/)
[![Redis](https://img.shields.io/badge/Redis-OTP%20Cache-dc382d?style=for-the-badge&logo=redis&logoColor=white)](https://redis.io/)
[![Brevo](https://img.shields.io/badge/Brevo-Email%20OTP-0092ff?style=for-the-badge)](https://www.brevo.com/)

<br />

**Cursed Auth** is an in-house authentication API built around RSA-signed JWTs, email OTP verification, MongoDB-backed users, Redis-backed short-lived verification codes, and Spring Security stateless authorization.

</div>

---

## Why This Exists

Your ecosystem should not re-implement login, token signing, password hashing, email verification, or user lookup in every service. Cursed Auth centralizes that work behind one clean boundary:

```text
Client app -> Cursed Auth -> JWT
Other services -> verify JWT signature with public key -> trust role/user claims
```

Keep the private key inside this service. Share only the public key with the services that need to verify tokens.

---

## System Flow

```mermaid
flowchart LR
    A[Client] -->|register| B[Cursed Auth API]
    B -->|hash password| C[(MongoDB users)]
    B -->|store OTP, 10 min TTL| D[(Redis)]
    B -->|send email OTP| E[Brevo]
    A -->|verify OTP| B
    B -->|issue RS512 JWT| A
    A -->|Bearer token| F[Protected services]
    F -->|verify signature| G[JWT public key]

    classDef auth fill:#151515,stroke:#fd5c63,color:#ffffff,stroke-width:2px;
    classDef data fill:#102a43,stroke:#38bdf8,color:#ffffff;
    classDef edge fill:#132a13,stroke:#6db33f,color:#ffffff;
    class B auth;
    class C,D data;
    class F,G edge;
```

---

## Feature Set

| Capability | Status | Notes |
| --- | --- | --- |
| Register users | Built in | Validates email, username, password, first name, and role |
| Password hashing | Built in | Uses `BCryptPasswordEncoder` |
| Email OTP verification | Built in | Generates a 6-character alphanumeric OTP |
| OTP cache | Built in | Stores verification records in Redis for 10 minutes |
| JWT signing | Built in | Signs tokens with RSA private key using `RS512` |
| JWT validation | Built in | Verifies signatures with RSA public key |
| RBAC-ready users | Built in | Current roles: `VIEWER`, `SUPERUSER` |
| Stateless API security | Built in | Spring Security with bearer-token authentication |
| OpenAPI docs | Built in | Swagger UI plus raw OpenAPI JSON/YAML |
| Health endpoints | Built in | Public `/api/health/ok` and test failure endpoint |
| Email delivery | Built in | Sends OTP emails through Brevo |

---

## Tech Stack

| Layer | Choice |
| --- | --- |
| Runtime | Java 25 |
| Framework | Spring Boot 4.0.2 |
| Security | Spring Security, stateless filter chain |
| Token library | JJWT `0.13.0` |
| Token algorithm | `RS512` |
| API documentation | Springdoc OpenAPI, Swagger UI |
| Database | MongoDB |
| Cache | Redis |
| Email provider | Brevo REST API |
| Build tool | Maven Wrapper |

---

## API Contract

Base URL:

```text
https://api.auth.sudox1.com
```

Docs:

| UI / Spec | URL |
| --- | --- |
| Swagger UI | `https://api.auth.sudox1.com/swagger-ui.html` |
| OpenAPI JSON | `https://api.auth.sudox1.com/v3/api-docs` |
| OpenAPI YAML | `https://api.auth.sudox1.com/v3/api-docs.yaml` |

Every API response is wrapped in:

```json
{
  "success": true,
  "error": null,
  "data": {}
}
```

Error responses use:

```json
{
  "success": false,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "Authentication required",
    "details": "...",
    "timestamp": "2026-05-16T12:00:00"
  },
  "data": null
}
```

### Public Routes

| Method | Route | Purpose |
| --- | --- | --- |
| `GET` | `/api/health/ok` | Service health check |
| `GET` | `/api/health/fail` | Intentional failure endpoint for testing |
| `POST` | `/api/auth/register` | Create a user and send verification OTP |
| `POST` | `/api/auth/verify-otp` | Verify OTP and issue an access token |
| `POST` | `/api/auth/login` | Login a verified active user |

### Protected Routes

| Method | Route | Purpose |
| --- | --- | --- |
| `GET` | `/api/auth/all` | List all users |
| `GET` | `/api/auth/{email}` | Get a user by email |

Protected routes require:

```http
Authorization: Bearer <access_token>
```

---

## Request Examples

### Register

```bash
curl --request POST https://api.auth.sudox1.com/api/auth/register \
  --header "Content-Type: application/json" \
  --data '{
    "email": "gojo@sudox1.com",
    "username": "gojo",
    "password": "domain-expansion",
    "firstName": "Satoru",
    "middleName": "",
    "lastName": "Gojo",
    "role": "SUPERUSER"
  }'
```

Response:

```json
{
  "success": true,
  "error": null,
  "data": {
    "email": "gojo@sudox1.com",
    "id": "9f2f59f7-4c48-40ef-9834-1f089fb34e67"
  }
}
```

### Verify OTP

```bash
curl --request POST https://api.auth.sudox1.com/api/auth/verify-otp \
  --header "Content-Type: application/json" \
  --data '{
    "email": "gojo@sudox1.com",
    "otp": "A1B2C3"
  }'
```

Returns a login-style token payload:

```json
{
  "data": {
    "accessToken": "<jwt>"
  }
}
```

### Login

```bash
curl --request POST https://api.auth.sudox1.com/api/auth/login \
  --header "Content-Type: application/json" \
  --data '{
    "email": "gojo@sudox1.com",
    "password": "domain-expansion"
  }'
```

Response:

```json
{
  "success": true,
  "error": null,
  "data": {
    "accessToken": "<jwt>"
  }
}
```

### Call a Protected Endpoint

```bash
curl https://api.auth.sudox1.com/api/auth/all \
  --header "Authorization: Bearer <jwt>"
```

---

## JWT Shape

Cursed Auth signs access tokens with the configured RSA private key.

```json
{
  "sub": "gojo@sudox1.com",
  "role": "SUPERUSER",
  "userId": "9f2f59f7-4c48-40ef-9834-1f089fb34e67",
  "iat": 1778913000,
  "exp": 1778916600
}
```

Downstream services should:

1. Read the `Authorization: Bearer <token>` header.
2. Verify the token signature with the shared RSA public key.
3. Check `exp`.
4. Use `sub`, `userId`, and `role` for request identity and authorization.

---

## Local Setup

### 1. Requirements

- Java 25
- Maven Wrapper from this repository
- MongoDB
- Redis
- Brevo API key
- RSA keypair for JWT signing

### 2. Generate JWT Keys

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out private_key.pem
openssl rsa -pubout -in private_key.pem -out public_key.pem
```

### 3. Create Local Configuration

Create `src/main/resources/application.yml`. This file is ignored by git, so secrets stay local.

```yaml
server:
  port: 8080

spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/cursed-auth
    redis:
      host: localhost
      port: 6379

jwt-config:
  JWT_EXPIRY: 3600000
  JWT_PRIVATE_KEY: |
    -----BEGIN PRIVATE KEY-----
    paste-private-key-here
    -----END PRIVATE KEY-----
  JWT_PUBLIC_KEY: |
    -----BEGIN PUBLIC KEY-----
    paste-public-key-here
    -----END PUBLIC KEY-----

brevo-config:
  BREVO_URL: https://api.brevo.com/v3
  BREVO_API_KEY: your-brevo-api-key
```

### 4. Start Dependencies

With local Docker:

```bash
docker run --name cursed-auth-mongo -p 27017:27017 -d mongo:latest
docker run --name cursed-auth-redis -p 6379:6379 -d redis:latest
```

### 5. Run the API

```bash
./mvnw spring-boot:run
```

Health check:

```bash
curl http://localhost:8080/api/health/ok
```

Expected response:

```json
{
  "status": "ok"
}
```

---

## Integration Pattern For Other Services

Use Cursed Auth as the issuer and make every other service a verifier.

```text
AUTH_SERVICE_PRIVATE_KEY -> only Cursed Auth
AUTH_SERVICE_PUBLIC_KEY  -> every service that validates tokens
```

Recommended service-to-service contract:

| Claim | Meaning | Example |
| --- | --- | --- |
| `sub` | User email | `gojo@sudox1.com` |
| `userId` | Internal user id | `9f2f59f7-4c48-40ef-9834-1f089fb34e67` |
| `role` | Role for RBAC | `SUPERUSER` |
| `exp` | Token expiration | Unix timestamp |

For Spring services, mirror the validation logic from `JwtUtils`: parse the JWT with the public key, reject invalid signatures, reject expired tokens, then map `role` into your service's authorities.

---

## Project Structure

```text
src/main/java/com/cursed/auth
|-- clients
|   `-- BrevoEmailClient.java
|-- config
|   |-- Beans.java
|   |-- OpenApiConfig.java
|   |-- RedisConfig.java
|   `-- SecurityConfig.java
|-- controllers
|   |-- AuthController.java
|   `-- HealthController.java
|-- dtos
|   |-- LoginDTO.java
|   |-- RegisterDTO.java
|   |-- VerifyOTPDTO.java
|   `-- response
|-- entities
|   |-- BaseEntity.java
|   |-- User.java
|   `-- enums/Role.java
|-- filters
|   `-- JwtFilter.java
|-- repository
|   `-- UserRepository.java
|-- services
|   |-- RedisService.java
|   |-- UserService.java
|   `-- UserServiceImpl.java
`-- utils
    |-- CommonUtils.java
    `-- JwtUtils.java
```

---

## Security Notes

- Store the JWT private key only in this service.
- Share the JWT public key with consumers that need local verification.
- Keep `BREVO_API_KEY` outside git.
- Restrict CORS origins before production. The current security config allows all origins.
- Use HTTPS in every deployed environment.
- Rotate JWT keys on a planned schedule and keep a key rollover strategy for active tokens.
- Treat Redis OTP data as sensitive, even with a short TTL.

---

## Response And Error Behavior

| Scenario | Behavior |
| --- | --- |
| Unknown login email | Returns `Invalid email/password` |
| Wrong password | Returns `Invalid email/password` |
| Unverified user login | Returns `Email not verified` |
| Blocked user login | Returns `User is blocked` |
| Duplicate email registration | Returns `User already exists` |
| Duplicate username registration | Returns `This username is taken by another user` |
| Missing authentication | Returns `401` with `UNAUTHORIZED` |
| Forbidden access | Returns `403` with `FORBIDDEN` |
| Unhandled exception | Returns `500` with `INTERNAL_SERVER_ERROR` |

---

## Production Checklist

- [ ] Add deployment-specific `application.yml` or environment-backed config.
- [ ] Lock CORS to trusted frontend and service origins.
- [ ] Move secrets to a secret manager.
- [ ] Add rate limiting for login, register, and OTP verification.
- [ ] Add resend OTP and forgot password flows if needed.
- [ ] Add refresh-token implementation or remove the unused response field.
- [ ] Add tests for auth flows, JWT validation, and error responses.
- [ ] Add observability around login failures, OTP delivery, and token issuance.

---

<div align="center">

Built to be the auth spine for the rest of your services.

</div>
