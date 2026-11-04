// Importação direta do Playwright original, sem dependência da Lovable
import { test as base, expect } from "@playwright/test";

// Re-exportando para manter a compatibilidade com seus arquivos de teste
export { expect };
export const test = base;