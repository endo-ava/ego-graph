import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';
import path from 'path';

export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),  // Tailwind v4 Viteプラグイン
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    host: '0.0.0.0',
    port: 5174,
    allowedHosts: [
      'dev-server',
      'localhost',
    ],
  },
  build: {
    outDir: 'dist',
    sourcemap: true,
  },
});
