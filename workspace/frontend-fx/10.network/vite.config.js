import { defineConfig } from 'vite'
import react, { reactCompilerPreset } from '@vitejs/plugin-react'
import babel from '@rolldown/plugin-babel'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react(),
    babel({ presets: [reactCompilerPreset()] })
  ],
  // proxy 서버 설정 : 자바스크립트로 naver의 open api 호출하기 위해
  server: {
    proxy: {
      '/naver': {
        target: 'https://openapi.naver.com',
        changeOrigin: true,
        rewrite: path => path.replace(/^\/naver/, '') // /naver -> ''
      }
    }
  }
})
