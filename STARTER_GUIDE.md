# Mindlog Web Starter Guide

## Scope (Phase 1)
- Login (web + app handover)
- Home
- Diary CRUD
- Turbo Drive + Stimulus baseline

## Auth Contract
- `GET /auth/login`
- `GET /auth/login/{provider}`
- `GET /auth/callback`
- `GET /auth/exchange`

Native app flow:
1. Open `/auth/login?source=app` in external browser.
2. Social login redirect returns to `/auth/callback?source=app&v=...`.
3. Server renders deep link page (`mindlog://auth/callback?token=...`).
4. App opens `/auth/exchange?token=...` in WebView to create session.

## Environment Profiles
- `local`: HTTP web development
- `local,local-https`: HTTPS for Turbo Native local testing
- `prod`: production

## Required Environment Variables
See `.env.example`.

## Local HTTPS Setup (for mobile)
```bash
./scripts/setup-local-https-cert.sh
SSL_KEYSTORE_PASSWORD=changeit ./gradlew bootRun --args='--spring.profiles.active=local,local-https'
```

## Run / Test
```bash
./gradlew bootRun
./gradlew test
npm run build-css
```

## Implementation Rules
- Turbo form success must use 303 redirect.
- Turbo form validation failures must render 422.
- Keep Turbo/Stimulus global hooks minimal.
