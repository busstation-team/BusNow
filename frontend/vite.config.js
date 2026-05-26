import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),   // Tailwind CSS v4 Vite 플러그인
  ],
  server: {
    port: 5173,
    // ✅ 백엔드 API 프록시: 개발 시 CORS 없이 /api/** 요청을 Spring Boot로 전달
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
        cookieDomainRewrite: 'localhost', // ✅ 쿠키 도메인 강제 매칭
        cookiePathRewrite: {
          '^/api': '/', // ✅ 쿠키 경로 보정
          '*': '/'
        }
      },
    },
  },
})
