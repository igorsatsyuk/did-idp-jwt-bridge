import { HttpErrorResponse } from '@angular/common/http';

import { formatHttpErrorMessage } from './http-error';

describe('formatHttpErrorMessage', () => {
  it('returns plain string response body as error message', () => {
    const error = new HttpErrorResponse({
      error: 'Service unavailable',
      status: 503
    });

    expect(formatHttpErrorMessage(error, 'Fallback')).toBe('Service unavailable');
  });

  it('returns message from JSON string response body', () => {
    const error = new HttpErrorResponse({
      error: JSON.stringify({ message: 'Readable error' }),
      status: 400
    });

    expect(formatHttpErrorMessage(error, 'Fallback')).toBe('Readable error');
  });

  it('returns message from object response body', () => {
    const error = new HttpErrorResponse({
      error: { message: 'Object error' },
      status: 409
    });

    expect(formatHttpErrorMessage(error, 'Fallback')).toBe('Object error');
  });

  it('returns fallback with status when body has no message', () => {
    const error = new HttpErrorResponse({
      error: {},
      status: 500
    });

    expect(formatHttpErrorMessage(error, 'Could not process request')).toBe(
      'Could not process request (HTTP 500)'
    );
  });
});
