# PRAYAS Campus Tours — Frontend

React + TypeScript + Vite frontend for the public campus tour request form.

## Setup

1. `npm install`
2. (Optional) copy `.env.example` to `.env.local` if your backend isn't on `http://localhost:8080`.
3. `npm run dev` — opens on `http://localhost:5173`.

## Backend requirement

This expects the Spring Boot backend (`prayas-backend`) running locally on port 8080,
with CORS configured to allow `http://localhost:5173` (see `SecurityConfig.java` /
`application.yml`'s `prayas.frontend.origin` property — already set up as of this
milestone).

## What's here

- `/` — the public multi-step campus tour request form (school details, visit details,
  venue picker, logistics, review & submit).
- Staff dashboard routes are not built yet — next milestone.

## Structure

- `src/api/` — typed API client, mirroring the backend's DTOs exactly.
- `src/pages/RequestTourPage.tsx` — owns the multi-step form's state and step flow.
- `src/pages/formState.ts` — form state shape, validation, and conversion to the API payload.
- `src/components/steps/` — one component per form step.
- `src/styles/form.css` — all form-specific styling (design tokens live in `src/index.css`).

## Known stub

The captcha token sent on submit is currently a hardcoded placeholder string
(`'local-dev-placeholder'`) — see the `TODO` in `RequestTourPage.tsx`. This works because
the backend's `NoOpCaptchaVerifier` (`@Profile("local")`) accepts any non-blank string.
A real hCaptcha widget needs to replace this before this goes anywhere near production.
