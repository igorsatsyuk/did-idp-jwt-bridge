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
    });

    const request = httpMock.expectOne('/did/register');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);
    request.flush({ did: payload.did, publicKey: payload.publicKey, active: true });
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
      expect(response.token).toBe('jwt-token');
    });

    const request = httpMock.expectOne('/auth/token');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);
    request.flush({ token: 'jwt-token' });
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
