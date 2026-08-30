import '@angular/compiler';
import 'jest-preset-angular/setup-jest';

// jsdom < 22 não expõe structuredClone global — usado pelo Dexie/fake-indexeddb.
if (typeof (globalThis as { structuredClone?: unknown }).structuredClone === 'undefined') {
  (globalThis as unknown as { structuredClone: (v: unknown) => unknown }).structuredClone = (
    v: unknown,
  ) => JSON.parse(JSON.stringify(v));
}
