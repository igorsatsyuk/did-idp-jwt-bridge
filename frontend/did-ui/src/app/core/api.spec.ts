import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { Api } from './api';

describe('Api', () => {
  let service: Api;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(Api);
    httpMock = TestBed.inject(HttpTestingController);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('posts DID registration request', () => {
    const payload = { did: 'did:example:alice', publicKey: '0xabc' };

    service.registerDid(payload).subscribe((response) => {
      expect(response.did).toBe(payload.did);
      expect(response.status).toBe('ACTIVE');
    });

    const request = httpMock.expectOne('/did/register');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);
    request.flush({
      did: payload.did,
      publicKey: payload.publicKey,
      status: 'ACTIVE',
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z'
    });
  });

  it('sends revoke DID request', () => {
    const did = 'did:ethr:0x1111111111111111111111111111111111111111';

    service.revokeDid(did).subscribe();

    const request = httpMock.expectOne(`/did/${did}/revoke`);
    expect(request.request.method).toBe('DELETE');
    request.flush(null);
  });

  it('gets challenge as text', () => {
    service.getChallenge().subscribe((challenge) => {
      expect(challenge).toBe('nonce-123');
    });

    const request = httpMock.expectOne('/auth/challenge');
    expect(request.request.method).toBe('GET');
    request.flush('nonce-123');
  });

  it('posts token exchange request', () => {
    const payload = { did: 'did:example:alice', challenge: 'nonce-123', signature: '0xsig' };

    service.exchangeToken(payload).subscribe((response) => {
      expect(response.accessToken).toBe('jwt-token');
      expect(response.tokenType).toBe('Bearer');
      expect(response.expiresIn).toBe(3600);
    });

    const request = httpMock.expectOne('/auth/token');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);
    request.flush({ accessToken: 'jwt-token', tokenType: 'Bearer', expiresIn: 3600 });
  });

  it('requests profile with bearer token', () => {
    service.getProfile('jwt-token').subscribe();

    const request = httpMock.expectOne('/api/me');
    expect(request.request.method).toBe('GET');
    expect(request.request.headers.get('Authorization')).toBe('Bearer jwt-token');
    request.flush({ did: 'did:example:alice' });
  });

  afterEach(() => {
    httpMock.verify();
  });
});
