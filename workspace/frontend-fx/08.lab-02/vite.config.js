import { defineConfig, transformWithOxc } from 'vite'
import react, { reactCompilerPreset } from '@vitejs/plugin-react'
import babel from '@rolldown/plugin-babel'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    {
      name: 'treat-js-files-as-jsx',
      enforce: 'pre',
      async transform(code, id) {
        const normalizedId = id.replace(/\\/g, '/');
        if (!normalizedId.includes('/src/') || !normalizedId.split('?')[0].endsWith('.js')) {
          return null;
        }
        return transformWithOxc(code, id, {
          lang: 'jsx'
        });
      },
    },
    react(),
    babel({
      presets: [reactCompilerPreset()],
      include: /\.(jsx|tsx)$/
    })
  ],
})
