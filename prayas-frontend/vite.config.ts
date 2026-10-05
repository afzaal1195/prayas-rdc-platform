import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'
import { VitePWA } from 'vite-plugin-pwa'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react(),
    VitePWA({
      // Updates the cached app silently in the background on the next
      // visit, rather than interrupting the person with a "reload?"
      // prompt -- the right tradeoff for a simple public form.
      registerType: 'autoUpdate',
      // The installed app is the STAFF app. Schools use the public request
      // form in an ordinary browser tab, so the app opens straight at /staff
      // and is scoped to the staff area (which keeps "open in app" from
      // grabbing a school's tracking link on a staff laptop).
      manifest: {
        id: '/staff',
        name: 'PRAYAS Staff — IIT Hyderabad',
        short_name: 'PRAYAS Staff',
        description:
          'Staff dashboard for the PRAYAS campus tour programme at IIT Hyderabad: review requests, plan visits and manage approvals.',
        start_url: '/staff',
        scope: '/staff',
        display: 'standalone',
        background_color: '#FBF8F3',
        theme_color: '#1B2A41',
        icons: [
          { src: '/icon-192.png', sizes: '192x192', type: 'image/png' },
          { src: '/icon-512.png', sizes: '512x512', type: 'image/png' },
          {
            src: '/icon-maskable-512.png',
            sizes: '512x512',
            type: 'image/png',
            purpose: 'maskable',
          },
        ],
      },
      workbox: {
        // Precache the built app shell (JS/CSS/HTML/fonts/images) so the
        // app loads instantly on repeat visits. Actual form submission
        // still needs a live connection -- this isn't an offline-queueing
        // system, just a faster, installable shell.
        globPatterns: ['**/*.{js,css,html,svg,png,woff2}'],
        // By default the service worker answers EVERY page navigation with
        // the app shell, so the React app can load offline. These paths are
        // not React pages: they're served by the backend (Google sign-in,
        // sign-out, the API). Without this, clicking "Sign in with Google"
        // or "Log out" in a built/installed copy is swallowed by the shell
        // and ends up on the home page instead of reaching the backend.
        navigateFallbackDenylist: [/^\/oauth2/, /^\/login/, /^\/logout/, /^\/api/],
      },
    }),
  ],
  server: {
    proxy: {
      // Proxying keeps the frontend and backend on the same origin during
      // dev (both look like localhost:5173 to the browser), so the staff
      // session cookie is same-site -- avoids the SameSite/Secure
      // cross-origin cookie fragility that comes with calling a genuinely
      // different origin (localhost:8080) directly.
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
      '/oauth2': { target: 'http://localhost:8080', changeOrigin: true },
      '/login': { target: 'http://localhost:8080', changeOrigin: true },
      '/logout': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
})