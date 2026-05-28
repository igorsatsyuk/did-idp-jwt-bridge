import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Wallet } from 'ethers';

import { Auth } from './auth';

describe('Auth', () => {
  const PRIVATE_KEY = '0x59c6995e998f97a5a0044966f094538a31f5f9df74a0f4f5b8f7d6b47a5c3a2a';
  const DID = 'did:ethr:0x1111111111111111111111111111111111111111';
  const CHALLENGE = 'nonce-123';
  const JWT_TOKEN = createJwt({ sub: DID, role: 'USER', exp: 1999999999 });
  let component: Auth;
  let fixture: ComponentFixture<Auth>;
  let httpMock: HttpTestingController;
  let sessionStorageSetItemSpy: ReturnType<typeof vi.spyOn>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Auth],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();

    fixture = TestBed.createComponent(Auth);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    sessionStorageSetItemSpy = vi.spyOn(Storage.prototype, 'setItem');
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('fetches challenge from API', () => {
    component.requestChallenge();

    const request = httpMock.expectOne('/auth/challenge');
    expect(request.request.method).toBe('GET');
    request.flush(CHALLENGE);

    expect(component.challenge).toBe(CHALLENGE);
    expect(component.successMessage).toContain('Challenge received');
  });

  it('shows readable error when challenge request fails', () => {
    component.requestChallenge();

    const request = httpMock.expectOne('/auth/challenge');
    request.flush({ message: 'Challenge service unavailable' }, { status: 503, statusText: 'Service Unavailable' });

    expect(component.errorMessage).toBe('Challenge service unavailable');
  });

  it('marks form touched when sign is requested with invalid form', () => {
    component.signChallenge();

    expect(component.authForm.controls.did.touched).toBe(true);
    expect(component.authForm.controls.privateKey.touched).toBe(true);
  });

  it('requires challenge before signing', () => {
    component.authForm.patchValue({ did: DID, privateKey: PRIVATE_KEY });

    component.signChallenge();

    expect(component.errorMessage).toBe('Fetch challenge before signing.');
    expect(component.isSigningChallenge).toBe(false);
  });

  it('signs challenge with private key', async () => {
    component.authForm.patchValue({ did: DID, privateKey: PRIVATE_KEY });
    component.challenge = CHALLENGE;
    const signMessageSpy = vi.spyOn(Wallet.prototype, 'signMessage').mockResolvedValue('0xsigned');

    component.signChallenge();
    await fixture.whenStable();

    expect(signMessageSpy).toHaveBeenCalledWith(CHALLENGE);
    expect(component.signature).toBe('0xsigned');
    expect(component.successMessage).toContain('Challenge signed');
  });

  it('handles invalid private key without getting stuck in signing state', () => {
    component.authForm.patchValue({ did: DID, privateKey: 'invalid-private-key' });
    component.challenge = CHALLENGE;

    component.signChallenge();

    expect(component.isSigningChallenge).toBe(false);
    expect(component.signature).toBeNull();
    expect(component.errorMessage).toBeTruthy();
  });

  it('shows sign error when wallet signing fails', async () => {
    component.authForm.patchValue({ did: DID, privateKey: PRIVATE_KEY });
    component.challenge = CHALLENGE;
    vi.spyOn(Wallet.prototype, 'signMessage').mockRejectedValue('boom');

    component.signChallenge();
    await fixture.whenStable();

    expect(component.errorMessage).toBe('Failed to sign challenge');
    expect(component.isSigningChallenge).toBe(false);
  });

  it('exchanges token, stores JWT in sessionStorage, and decodes claims', () => {
    component.authForm.patchValue({ did: DID, privateKey: PRIVATE_KEY });
    component.challenge = CHALLENGE;
    component.signature = '0xsigned';

    component.exchangeToken();

    const request = httpMock.expectOne('/auth/token');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      did: DID,
      challenge: CHALLENGE,
      signature: '0xsigned'
    });
    request.flush({
      accessToken: JWT_TOKEN,
      tokenType: 'Bearer',
      expiresIn: 3600
    });

    expect(sessionStorageSetItemSpy).toHaveBeenCalledWith('did-ui.access-token', JWT_TOKEN);
    expect(component.jwtClaims?.['sub']).toBe(DID);
    expect(component.jwtClaims?.['role']).toBe('USER');
    expect(component.successMessage).toContain('JWT received');
  });

  it('keeps claims null when JWT payload is not decodable', () => {
    component.authForm.patchValue({ did: DID, privateKey: PRIVATE_KEY });
    component.challenge = CHALLENGE;
    component.signature = '0xsigned';

    component.exchangeToken();

    const request = httpMock.expectOne('/auth/token');
    request.flush({
      accessToken: 'invalid-token',
      tokenType: 'Bearer',
      expiresIn: 3600
    });

    expect(component.jwtClaims).toBeNull();
  });

  it('marks form touched when token exchange requested with invalid form', () => {
    component.exchangeToken();

    expect(component.authForm.controls.did.touched).toBe(true);
    expect(component.authForm.controls.privateKey.touched).toBe(true);
  });

  it('does not request token without challenge', () => {
    component.authForm.patchValue({ did: DID, privateKey: PRIVATE_KEY });
    component.signature = '0xsigned';

    component.exchangeToken();

    httpMock.expectNone('/auth/token');
    expect(component.errorMessage).toBe('Fetch challenge before requesting JWT.');
  });

  it('does not request token without signature', () => {
    component.authForm.patchValue({ did: DID, privateKey: PRIVATE_KEY });
    component.challenge = CHALLENGE;

    component.exchangeToken();

    httpMock.expectNone('/auth/token');
    expect(component.errorMessage).toBe('Sign challenge before requesting JWT.');
  });

  it('shows fallback message when token exchange fails without payload message', () => {
    component.authForm.patchValue({ did: DID, privateKey: PRIVATE_KEY });
    component.challenge = CHALLENGE;
    component.signature = '0xsigned';

    component.exchangeToken();

    const request = httpMock.expectOne('/auth/token');
    request.flush({}, { status: 500, statusText: 'Server Error' });

    expect(component.errorMessage).toBe('Could not exchange token (HTTP 500)');
  });

  afterEach(() => {
    httpMock.verify();
    vi.restoreAllMocks();
  });
});

function createJwt(payload: Record<string, unknown>): string {
  const header = { alg: 'HS256', typ: 'JWT' };
  const encode = (value: unknown): string =>
    btoa(JSON.stringify(value)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '');
  return `${encode(header)}.${encode(payload)}.signature`;
}
