import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';
import axios from 'axios';

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
    resolve: {
      alias: {
        '@': '/src',
      },
    },
    configureServer(server) {
      server.middlewares.use(async (req, res, next) => {
        if (
          req.url &&
          req.method === 'GET' &&
          req.url.startsWith('/api/v1/auth/verify-email')
        ) {
          const queryIndex = req.url.indexOf('?');
          const query = queryIndex !== -1 ? req.url.substring(queryIndex) : '';
          const token = new URLSearchParams(query).get('token');

          if (token) {
            try {
              const response = await axios.post('http://localhost:8080/api/v1/auth/verify-email', { token });
              if (response.status === 200) {
                res.writeHead(302, { Location: '/login' });
              } else {
                res.writeHead(302, { Location: '/verify-email?error=verification_failed' });
              }
            } catch (error) {
              console.error('Email verification error:', error);
              res.writeHead(302, { Location: '/verify-email?error=server_error' });
            }
            res.end();
          } else {
            res.writeHead(302, { Location: '/verify-email?error=missing_token' });
            res.end();
          }
        } else {
          next();
        }
      });
    },
  },
});
