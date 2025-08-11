import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      // apunta a clients/scss-shared
      '@shared-scss': path.resolve(__dirname, '../scss-shared'),
    },
  },
  css: {
    preprocessorOptions: {
      scss: {
        // se inyecta en TODOS los .scss automáticamente
        additionalData: `@use "@shared-scss/variables" as *;`,
      },
    },
  },
});
