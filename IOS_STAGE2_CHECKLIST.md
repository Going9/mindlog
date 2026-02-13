# iOS Stage 2 Checklist

## Goal
Apply the same auth/navigation contract used by web + Android starter.

## Required Contracts
- Deep link scheme: `mindlog://auth/callback?token=...`
- Session exchange endpoint: `GET /auth/exchange?token=...`
- Login entry for app flow: `/auth/login?source=app`

## Path Configuration Rules
- Default routes in app WebView.
- `/auth/login.*` routes open externally.

## Validation Scenarios
1. Open app and navigate to login.
2. Browser login succeeds and returns deep link.
3. App receives token and routes to `/auth/exchange`.
4. WebView session is created and home page is authenticated.
5. Session expiry redirects to `/auth/login?source=app&error=session_expired`.

## Non-goals (Stage 2)
- No custom native animation stack.
- No provider-specific SDK login in app WebView.
