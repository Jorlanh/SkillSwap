
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react-swc";
import path from "path";
import { fileURLToPath } from "url";
import { VitePWA } from "vite-plugin-pwa";

const projectRoot = path.dirname(fileURLToPath(import.meta.url));

export default defineConfig(({ mode }) => ({
  server: {
    host: "::",
    port: 8081,
    strictPort: true,
    hmr: {
      overlay: false,
    },
    headers: {
      "Cross-Origin-Opener-Policy": "same-origin-allow-popups",
      "Cross-Origin-Embedder-Policy": "unsafe-none",
    },
  },

  plugins: [
    react(),

    VitePWA({
      registerType: "autoUpdate",

      includeAssets: [
        "favicon.ico",
        "apple-touch-icon.png",
        "mask-icon.svg",
      ],

      workbox: {
        navigateFallbackDenylist: [
          /^\/.*\/~oauth/,
          /^\/api(?:\/|$)/,
        ],
        globPatterns: [
          "**/*.{js,css,html,ico,png,svg,jpg,jpeg,woff2}",
        ],
        maximumFileSizeToCacheInBytes: 3000000,
      },

      manifest: {
        name: "SkillSwap HUB",
        short_name: "SkillSwap",
        description: "Plataforma de troca de habilidades e conhecimento",
        start_url: "/",
        display: "standalone",
        background_color: "#ffffff",
        theme_color: "#2a9d8f",

        icons: [
          {
            src: "/favicon.ico",
            sizes: "64x64",
            type: "image/x-icon",
          },
        ],
      },
    }),
  ],

  resolve: {
    alias: {
      "@": path.resolve(projectRoot, "src"),
    },
    dedupe: ["react", "react-dom"],
  },

  build: {
    chunkSizeWarningLimit: 1000,

    rollupOptions: {
      output: {
        manualChunks: {
          vendor: ["react", "react-dom", "react-router-dom"],
        },
      },
    },
  },
}));
