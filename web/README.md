# ARMz Web

React 18 + TypeScript + Tailwind UI for the ARMz Android WebView shell.

```bash
npm ci
npm run dev     # optional browser preview with the mock bridge
npm run build   # writes ../app/src/main/assets/web/
npm run test    # Vitest
```

Production bridge methods are exposed by Kotlin as `window.ARMz` and accept or
return JSON strings. Native events arrive through `window.__armz_emit` and the
perf tier through `window.__armz_perf`.

React Bits components are used under MIT + Commons Clause. See
`../THIRD_PARTY_NOTICES.md`.
