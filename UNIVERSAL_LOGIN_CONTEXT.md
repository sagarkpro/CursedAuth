# Build Spec — CursedAuth "Universal Login" App

You are building the **hosted login page** for an Auth0-style Identity Provider (IdP) called **CursedAuth**. This is a standalone React SPA (its own repo/project) that plays the exact role Auth0's "Universal Login" plays. The IdP backend is already built, running, and verified. **Your app only renders the login UI and talks to ONE backend endpoint.** It never touches OAuth codes, PKCE, or tokens — those are handled by the backend and by the client app's Auth0 SDK.

---

## 1. The big picture (where your app sits)

A client web app uses `@auth0/auth0-react` pointed at the IdP. When the user clicks "Log in":

```
Client SPA (e.g. http://localhost:3000)
  │  auth0-react loginWithRedirect()  → browser navigates to the IdP
  ▼
IdP  GET /authorize?...PKCE...     (http://localhost:7772)
  │  validates client + PKCE, stores a login transaction, 302 →
  ▼
IdP  GET /login?login_id=ABC       302 → your app
  ▼
┌──────────────────────────────────────────────────────────────┐
│  YOUR APP:  http://localhost:7704/login?login_id=ABC          │
│  1. read login_id from the URL query string                   │
│  2. show email + password form                                │
│  3. POST { email, password, loginId } → IdP /api/auth/login   │
│  4. on success: window.location.assign(data.redirectUri)      │
└──────────────────────────────────────────────────────────────┘
  │  redirectUri = http://localhost:3000/callback?code=...&state=...
  ▼
Client SPA callback → auth0-react exchanges the code at /oauth/token → logged in
```

Your app's responsibility **begins** when the browser lands on `/login?login_id=...` and **ends** when you navigate to the `redirectUri` the backend returns. That's it.

---

## 2. Hard rules

**MUST**

- Run on **`http://localhost:7704`** and serve a **`/login`** route (the backend redirects here; the origin/port is already allowlisted in the backend's CORS + redirect config).
- Read **`login_id`** from the URL query string and send it back as the JSON field **`loginId`** (note: snake_case in the URL, **camelCase in the JSON body**).
- POST credentials as **`application/json`** to the IdP and, on success, do a **full-page navigation** (`window.location.assign(redirectUri)`) — not a client-side router push, not a `fetch` follow. The browser must actually navigate to the client app's callback URL.
- Treat the returned `redirectUri` as trusted and navigate to it verbatim (the backend already validated it against the registered client's redirect URIs).

**MUST NOT**

- Do **not** implement OAuth/OIDC/PKCE, do **not** generate `code_challenge`, do **not** call `/oauth/token`, do **not** parse or store any `access_token` / `id_token` / `refresh_token`. None of that belongs in the login page.
- Do **not** send cookies/credentials on the fetch (`credentials: 'include'` is wrong — backend CORS has `allowCredentials=false`). Plain CORS fetch only.
- Do **not** hardcode the client app's URL. The `redirectUri` is provided by the backend per request.
- Do **not** log the password or the full `redirectUri` (it contains a one-time authorization code).

---

## 3. The contract — the only required endpoint

### `POST {IDP_BASE_URL}/api/auth/login`  (cross-origin, JSON)

`IDP_BASE_URL` = `http://localhost:7772` in dev (make it an env var; prod will be `https://api.auth.sudox1.com`).

**Request headers:** `Content-Type: application/json`
**Request body:**

```json
{
  "email": "user@example.com",
  "password": "the-password",
  "loginId": "ABC...the value from ?login_id="
}
```

**Success → HTTP 200:**

```json
{
  "success": true,
  "data": {
    "redirectUri": "http://localhost:3000/callback?code=wtDSy...Epn40&state=xyz123"
  }
}
```

→ Action: `window.location.assign(data.data.redirectUri)`.

**Failure → HTTP 400:**

```json
{
  "success": false,
  "error": { "code": "invalid_credentials", "message": "Invalid email/password" }
}
```

→ Action: show `error.message` to the user (see the error table below).

> Field-name reminder: the URL query param is **`login_id`** (snake_case), but the JSON request field is **`loginId`** (camelCase). They carry the same value; your app reads the former and sends the latter.

---

## 4. Error handling (this is most of the real work)

| HTTP | `error.code` | `error.message` (examples) | What it means | Recommended UX |
|---|---|---|---|---|
| 400 | `invalid_credentials` | "Invalid email/password" | Wrong email or password | Inline form error; let them retry on the same page (the `login_id` is still valid). |
| 400 | `invalid_credentials` | "Email not verified" | Account exists but email isn't verified | Show message; optionally offer "verify email" (see §7 optional). |
| 400 | `invalid_credentials` | "User is blocked" | Account deactivated | Show message; no retry path. |
| 400 | `invalid_login` | "Login session expired or invalid. Please restart sign-in." | The `login_id` transaction is missing or expired (server TTL is **10 minutes**) | The login flow **cannot** be retried from this page — the user must restart from the client app. Show a clear "Your sign-in session expired — please return to the app and try again" message. Do **not** keep POSTing. |
| — | (no `login_id` in URL) | — | User hit `/login` directly with no active request | Render a neutral "No active sign-in request" state; do not POST. |
| network / 5xx | — | — | Backend down / CORS / unexpected | Generic "Something went wrong, please try again" + a retry button. |

Notes:

- Always read the JSON body to get `error.message`; don't rely on HTTP status text.
- Keep the email field populated on `invalid_credentials` retries; clear the password.
- Disable the submit button and show a spinner while the request is in flight; never double-submit.

---

## 5. CORS (so you don't fight it)

The backend already allows your origin. For reference, it permits:

- **Origin:** `http://localhost:7704` (your app) — set via backend config `sso.cors.allowed-origins`.
- **Methods:** `GET, POST, OPTIONS`  •  **Headers:** `Authorization, Content-Type, Accept`  •  **Credentials:** `false`.

Because you send `Content-Type: application/json`, the browser will fire a preflight `OPTIONS` — the backend handles it. Just use a normal `fetch`/`axios` POST **without** `credentials: 'include'`. If you add new request headers, they must be within the allowed set (or the backend CORS config must be expanded).

---

## 6. Reference implementation (core logic — adapt to your stack)

Minimal, dependency-light. Works with Vite + React (Router optional; reading `login_id` from `window.location` is enough).

```tsx
// LoginPage.tsx
import { useState, useEffect } from "react";

const IDP_BASE_URL = import.meta.env.VITE_IDP_BASE_URL ?? "http://localhost:7772";

export default function LoginPage() {
  const [loginId, setLoginId] = useState<string | null>(null);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    const id = new URLSearchParams(window.location.search).get("login_id");
    setLoginId(id);
    if (!id) setError("No active sign-in request. Please start from the application.");
  }, []);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!loginId) return;
    setSubmitting(true);
    setError(null);
    try {
      const res = await fetch(`${IDP_BASE_URL}/api/auth/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password, loginId }),
      });
      const body = await res.json();
      if (res.ok && body.success && body.data?.redirectUri) {
        window.location.assign(body.data.redirectUri); // hand control back to the client app
        return;
      }
      // failure: show the server message; allow retry unless the session expired
      setError(body?.error?.message ?? "Sign-in failed. Please try again.");
      setPassword("");
    } catch {
      setError("Cannot reach the sign-in service. Please try again.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={onSubmit}>
      {/* email + password inputs, error banner, submit button disabled while submitting */}
    </form>
  );
}
```

Routing: ensure `http://localhost:7704/login` renders this page and that the dev server has SPA history fallback so a direct hit on `/login?login_id=...` works.

---

## 7. Optional (only if you want a full auth UI — confirm scope first)

Auth0's Universal Login also has Sign-up and (here) email-OTP verification. These hit other JSON endpoints. **Not required for the core login flow** — include only if asked. These are plain JSON APIs (no `login_id` involved); after a successful signup+verify, the user returns to the normal login form.

- **Sign up** — `POST {IDP_BASE_URL}/api/auth/register`

  ```json
  { "email": "u@x.com", "username": "uniquehandle", "password": "Secret123!",
    "firstName": "Jane", "lastName": "Doe", "role": "VIEWER" }
  ```

  `role` ∈ `"VIEWER" | "SUPERUSER"`. `lastName`/`middleName` optional. Success: `{ success, data: { email, id } }`. An email OTP is sent.
  ⚠️ In the current dev environment the email provider (Brevo) is IP-allowlisted, so register may return **HTTP 500 even though the user is saved**. Treat 500 here as "registered but email may not have sent."

- **Verify OTP** — `POST {IDP_BASE_URL}/api/auth/verify-otp`

  ```json
  { "email": "u@x.com", "otp": "ABC123" }
  ```

  Marks the user `verified=true`. After this, the user can log in normally.

If you add these, present them as tabs/links alongside Login. A user who hits "Email not verified" on login should be routed to the OTP step.

---

## 8. Out of scope (handled elsewhere — do not build)

- PKCE / `code_challenge` generation, the `/authorize` request, the `/oauth/token` exchange, refresh, logout — all done by the **client app's** `@auth0/auth0-react` SDK and the **IdP backend**.
- Token storage / session management — the client SPA owns that.
- Social / federated login, MFA — not implemented in the IdP.

---

## 9. Config / environment

| Setting | Dev value | Notes |
|---|---|---|
| This app's URL | `http://localhost:7704` | Must match the backend's `sso.login.frontend-url` (`…/login`) and `sso.cors.allowed-origins`. If you change the port, the backend config must change too. |
| Login route | `/login` | Receives `?login_id=...`. |
| `VITE_IDP_BASE_URL` | `http://localhost:7772` | IdP backend base. Prod: `https://api.auth.sudox1.com`. |

---

## 10. How to test end-to-end

1. **Backend running** on `:7772` with Mongo + Redis up (the IdP team handles this).
2. **A registered client + a verified user** must exist (the IdP team can seed these; a public client with `authorization_code`+`refresh_token`, auth method `none`, PKCE on, and a redirect URI like `http://localhost:3000/callback`).
3. **Generate a real `login_id`** to drive your page without a full client app: call the IdP authorize endpoint and read the redirect (it 302s to `/login?login_id=...`). Example:

   ```bash
   curl -s -D - -o /dev/null -G 'http://localhost:7772/authorize' \
     --data-urlencode 'response_type=code' --data-urlencode 'client_id=spa-test' \
     --data-urlencode 'redirect_uri=http://localhost:3000/callback' \
     --data-urlencode 'scope=openid profile email offline_access' \
     --data-urlencode 'state=xyz' --data-urlencode 'nonce=abc' \
     --data-urlencode 'code_challenge=E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM' \
     --data-urlencode 'code_challenge_method=S256' | grep -i location
   # → Location: /login?login_id=<USE-THIS>
   ```

   Open `http://localhost:7704/login?login_id=<USE-THIS>` in the browser, log in, and confirm you get bounced to `http://localhost:3000/callback?code=...&state=xyz`.
4. The IdP ships a **Postman collection** (`CursedAuth.postman_collection.json`) that exercises the exact `/authorize → /api/auth/login → redirectUri` shapes your page depends on — use it to confirm request/response formats and error bodies.

**Acceptance:** Given a valid `login_id` and correct credentials, the page POSTs once and the browser ends up at the client callback with `code` + `state`. Wrong password shows an inline error and allows retry. A stale/absent `login_id` shows the "session expired / no active request" state and does not loop.

---

## 11. Backend references (for the curious — not needed to build)

The contract above is everything you need, but if you want to read the source: the login endpoint logic is in the IdP's `AuthController` (`/api/auth/login`, branches on `loginId`), the redirect-target is built there, and the `/authorize` → `/login` redirect lives in `AuthorizeController` + `LoginRedirectController`. Error codes (`invalid_login`, `invalid_credentials`) and messages come from `AuthController` + `UserServiceImpl.authenticate`.
