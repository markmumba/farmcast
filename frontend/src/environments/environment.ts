declare global {
  interface Window {
    __farmcastEnv?: {
      backendUrl?: string;
    };
  }
}

const backendUrl = window.__farmcastEnv?.backendUrl?.replace(/\/+$/, '') || 'http://localhost:8080';

export const environment = {
  backendUrl
};
