# MenuPilot (Club Menu Pre-Order)

Spring Boot starter for a $5/mo micro-SaaS: clubs post menus, members pre-order, kitchen prints prep lists.

## Stack
- Spring Boot 3.3 (Web, Data JPA, Security, Mail, Actuator)
- PostgreSQL + Flyway
- Thymeleaf
- Stripe (Checkout + Billing Portal)
- Session-based magic-link auth
- Role guards via `HandlerInterceptor`

## Quick start
1) Postgres + env vars:
```
export DB_URL=jdbc:postgresql://localhost:5432/menupilot
export DB_USER=postgres
export DB_PASS=postgres

export STRIPE_SECRET=sk_test_xxx
export STRIPE_PRICE_ID=price_xxx   # recurring price for $5/mo
export STRIPE_WEBHOOK_SECRET=whsec_xxx
export APP_BASE_URL=http://localhost:8080
```
2) Run app:
```
./mvnw spring-boot:run
```
3) In another shell (for dev webhooks):
```
stripe listen --forward-to localhost:8080/stripe/webhook
```
4) Go to `/auth/login` → sign in (magic link prints in console). The first user becomes **ADMIN**.
5) Create an event in `/admin/events`, add items, place a pre-order, view `/staff/events/{id}/prep-list`.
6) Open `/billing` to start Stripe Checkout or the Portal.

## Roles & guards
- **ADMIN**: `/admin/**`
- **STAFF**: `/staff/**`
- Sign-in required for `/events/**` and `/billing/**`.
Guards enforced by `RoleGuard` interceptor using session attributes (`role`, `userEmail`).

## Notes
- Email sending is a dev stub (`EmailService` prints to console). Plug in JavaMail in prod.
- SecurityConfig is permissive for MVP. Lock down before production.


## Caps, cutoff, and plan enforcement
- **Caps:** Event menu items can define `capQty`. The preorder form shows **Remaining** and enforces the cap across all users.
- **Cutoff:** If `cutoffAt` is set and passed, the preorder form is disabled and attempts to submit redirect with an error.
- **Active plan required:** Access to `/admin/**` and `/staff/**` requires an **active** subscription. `/billing/**` always allowed so you can subscribe/manage.


## Branding & Emails
- Upload event **cover image** and set **accent color** at `/admin/events/{id}/design`. PDFs (menu/tickets) render logo + cover and use your accent for table headers.
- **Receipt-style** tickets at `/export/events/{id}/tickets-receipt.pdf` for 80mm printers.
- **Users CSV**: Export `/export/users.csv`. Preview import at `/export/users/import/preview` with headers `email,role,active`.
- **HTML emails** via Thymeleaf templates (`templates/email/*`). Dev prints HTML to console; plug in JavaMailSender in prod.

## CI/CD + SSL

### GitHub Actions → GHCR → Fly.io (optional auto-deploy)
- Workflow: `.github/workflows/ci-cd.yml`
  - Build & test with Maven
  - Build multi-arch Docker image and push to GHCR at `ghcr.io/<org>/menupilot`
  - If `FLY_API_TOKEN` secret is set, deploys using `fly.toml`

**Secrets to add (Settings → Secrets → Actions):**
- `FLY_API_TOKEN` (from `fly auth token`)
- (Optional SMTP) `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASS`, `MAIL_FROM`
- (Stripe & DB) `STRIPE_SECRET`, `STRIPE_PRICE_ID`, `STRIPE_WEBHOOK_SECRET`, `DB_URL`, `DB_USER`, `DB_PASS`, `APP_BASE_URL`

### Render.com
- `render.yaml` provided. Connect repo → Render detects and builds Dockerfile.
- TLS is auto-managed; `APP_FORCE_HTTPS=true` enforces HTTPS redirects behind the proxy.

### SSL options
- **Proxy termination (recommended):** Platform handles TLS; app trusts `X-Forwarded-*` and enforces HTTPS via `APP_FORCE_HTTPS=true`.
- **Direct TLS in Spring Boot:** set
  ```
  SERVER_SSL_ENABLED=true
  SERVER_SSL_KEY_STORE=/path/to/keystore.p12
  SERVER_SSL_KEY_STORE_PASSWORD=********
  SERVER_SSL_KEY_STORE_TYPE=PKCS12
  SERVER_SSL_KEY_ALIAS=tomcat
  ```
