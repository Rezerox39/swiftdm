import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  base: './',
  plugins: [react()],
  css: {
    postcss: './postcss.config.js',
  },
  build: {
    // The WebViewAssetLoader serves app/src/main/assets/web through
    // https://appassets.androidplatform.net/. Keep the Vite build in one
    // hashed output tree so the Android bridge and CI always agree.
    outDir: '../app/src/main/assets/web',
    emptyOutDir: true,
  },
  server: {
    host: '127.0.0.1',
    port: 5173,
  },
});
