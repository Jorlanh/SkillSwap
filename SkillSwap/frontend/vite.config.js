import { defineConfig } from "vite";
import react from "@vitejs/plugin-react-swc";
import path from "path";
import { fileURLToPath } from "url";
import { VitePWA } from "vite-plugin-pwa";
var projectRoot = path.dirname(fileURLToPath(import.meta.url));
export default defineConfig(function (_a) {
    var mode = _a.mode;
    return ({
        server: {
            host: "::",
            port: 8081,
            strictPort: true, // Garante que use sempre a 8081 ou falhe (evita conflitos de porta)
            hmr: {
                overlay: false,
            },
            headers: {
                // CORREÇÃO CRÍTICA: Resolve o erro "Cross-Origin-Opener-Policy" do Firebase Auth
                "Cross-Origin-Opener-Policy": "same-origin-allow-popups",
                "Cross-Origin-Embedder-Policy": "unsafe-none",
            },
        },
        plugins: [
            react(),
            VitePWA({
                registerType: "autoUpdate",
                includeAssets: ['favicon.ico', 'apple-touch-icon.png', 'mask-icon.svg'],
                workbox: {
                    navigateFallbackDenylist: [/^\/~oauth/, /^\/api/], // Evita que o PWA intercepte chamadas de API do Java
                    globPatterns: ["**/*.{js,css,html,ico,png,svg,jpg,jpeg,woff2}"],
                    maximumFileSizeToCacheInBytes: 3000000, // Aumenta limite de cache para 3MB
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
                            type: "image/x-icon"
                        },
                        // Adicione ícones 192 e 512 aqui futuramente para conformidade PWA total
                    ],
                },
            }),
        ].filter(Boolean),
        resolve: {
            alias: {
                "@": path.resolve(projectRoot, "src"),
            },
            // Garante que não existam múltiplas instâncias do React brigando no DOM
            dedupe: ["react", "react-dom"],
        },
        build: {
            chunkSizeWarningLimit: 1000,
            rollupOptions: {
                output: {
                    manualChunks: {
                        vendor: ['react', 'react-dom', 'react-router-dom'],
                    },
                },
            },
        },
    });
});
