import { HttpErrorResponse } from '@angular/common/http';

export function formatHttpErrorMessage(error: HttpErrorResponse, fallback: string): string {
  if (typeof error.error === 'string' && error.error.trim().length > 0) {
    const maybeJsonMessage = extractMessageFromJson(error.error);
    if (maybeJsonMessage !== null) {
      return maybeJsonMessage;
    }
    return error.error;
  }

  if (
    typeof error.error === 'object' &&
    error.error !== null &&
    'message' in error.error &&
    typeof error.error.message === 'string' &&
    error.error.message.trim().length > 0
  ) {
    return error.error.message;
  }

  return `${fallback} (HTTP ${error.status || 'unknown'})`;
}

function extractMessageFromJson(value: string): string | null {
  try {
    const parsed = JSON.parse(value);
    if (
      typeof parsed === 'object' &&
      parsed !== null &&
      'message' in parsed &&
      typeof parsed.message === 'string' &&
      parsed.message.trim().length > 0
    ) {
      return parsed.message;
    }
  } catch {
    return null;
  }
  return null;
}
