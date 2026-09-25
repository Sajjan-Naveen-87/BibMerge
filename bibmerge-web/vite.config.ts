import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// In dev, forward /api to the Spring Boot backend (bibmerge-api).
// Override with BIBMERGE_API=http://localhost:8091 npm run dev
const apiTarget = process.env.BIBMERGE_API ?? 'http://localhost:8080'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': apiTarget,
    },
  },
})
