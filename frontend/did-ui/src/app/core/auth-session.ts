export const ACCESS_TOKEN_STORAGE_KEY = 'did-ui.access-token';

export function saveAccessToken(token: string): void {
  sessionStorage.setItem(ACCESS_TOKEN_STORAGE_KEY, token);
}

export function readAccessToken(): string | null {
  const token = sessionStorage.getItem(ACCESS_TOKEN_STORAGE_KEY);
  if (token === null) {
    return null;
  }

  const normalizedToken = token.trim();
  return normalizedToken.length > 0 ? normalizedToken : null;
}
