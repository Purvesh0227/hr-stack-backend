# HR-Stack Frontend

The React frontend for **HR-Stack**, an employee management application. It includes an admin dashboard, employee and attendance tools, finance features, and a progressive web app (PWA).


**Live app:** [purvesh0227.github.io/hr-stack-frontend](https://purvesh0227.github.io/hr-stack-frontend/)

## Features

- Employee and administrator management
- Attendance management, including QR-code attendance
- Finance tools for salary structures and salary slips
- Announcements and app releases
- Login, registration, email verification, and password recovery
- Responsive dashboard, notifications, and PWA installation support

## Tech stack

- React 19, React Router, and Vite
- Axios for API requests
- Vitest and Testing Library for unit and component tests
- Playwright for end-to-end tests
- MSW for API mocking in tests
- Vite PWA plugin and Workbox

## Getting started

### Prerequisites

- Node.js 22 or later
- npm
- The HR-Stack backend, running locally or available at a configured API URL

### Install dependencies

```bash
npm install
```

### Configure environment variables

Create a `.env.local` file in the project root:

```dotenv
VITE_API_BASE_URL=http://localhost:8080
VITE_VAPID_PUBLIC_KEY=<your-vapid-public-key>
VITE_GOOGLE_CLIENT_ID=<your-google-client-id>
VITE_GOOGLE_DESKTOP_CLIENT_ID=<your-desktop-client-id>
VITE_GOOGLE_DESKTOP_CLIENT_SECRET=<your-desktop-client-secret>
```

`VITE_API_BASE_URL` defaults to `http://localhost:8080` when omitted. Google credentials are needed for the corresponding Google authentication flows; the VAPID public key is used for push notifications.

> **Security:** Vite embeds `VITE_*` values in client-side assets. Do not commit `.env.local` or put confidential credentials in client-exposed variables. In particular, a client secret included in a frontend build is not private; use a backend-mediated OAuth flow for confidential credentials.

### Start the development server

```bash
npm run dev
```

Open [http://localhost:5173/hr-stack-frontend/](http://localhost:5173/hr-stack-frontend/).

## Available scripts

| Command | Description |
| --- | --- |
| `npm run dev` | Start the Vite development server |
| `npm run build` | Create the production build in `dist/` |
| `npm run preview` | Preview the production build locally |
| `npm run lint` | Run ESLint |
| `npm test` | Run unit and component tests once |
| `npm run test:watch` | Run Vitest in watch mode |
| `npm run test:coverage` | Run unit tests with coverage |
| `npm run e2e` | Run Playwright end-to-end tests |
| `npm run e2e:ui` | Open Playwright's interactive test UI |
| `npm run e2e:report` | View the latest Playwright HTML report |

## End-to-end tests

Playwright tests use a test admin account to authenticate against the backend. Create a `.env.test` file in the project root:

```dotenv
E2E_BASE_URL=http://localhost:5173/hr-stack-frontend/
E2E_ADMIN_EMAIL=<test-admin-email>
E2E_ADMIN_PASSWORD=<test-admin-password>
```

Make sure the backend is running and the test account exists, then run:

```bash
npm run e2e
```

Playwright starts the frontend development server automatically. The configured admin credentials are used to create an authenticated browser state for the tests.

## Deployment

The app is hosted on [GitHub Pages](https://purvesh0227.github.io/hr-stack-frontend/). The deployment workflow builds and deploys on pushes to the `prod` branch; the frontend CI workflow runs linting, unit tests, and a production build on `main` and `prod`.

The Vite base path is configured for `/hr-stack-frontend/`. If deploying under a different URL path, update `base` in `vite.config.js` and the PWA manifest paths to match.

## Project structure

```text
src/
├── components/   Reusable UI and dashboard components
├── contexts/     Theme and notification state
├── hooks/        Reusable React hooks
├── pages/        Login, dashboard, and feature pages
├── services/     API and notification services
├── test/         Test setup and MSW handlers
└── utils/        Shared helpers and utilities
e2e/              Playwright end-to-end tests
public/           Static assets, icons, and PWA resources
```
