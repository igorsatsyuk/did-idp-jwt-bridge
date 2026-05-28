import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Wallet } from 'ethers';

import { Register } from './register';

describe('Register', () => {
  const MOCK_WALLET = {
    address: '0x1111111111111111111111111111111111111111',
    privateKey: '0xaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',
    signingKey: {
      publicKey:
        '0x040102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e2f303132333435363738393a3b3c3d3e3f40'
    }
  } as unknown as ReturnType<typeof Wallet.createRandom>;
  let component: Register;
  let fixture: ComponentFixture<Register>;
  let httpMock: HttpTestingController;
  let sessionStorageSetItemSpy: ReturnType<typeof vi.spyOn>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Register],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();

    fixture = TestBed.createComponent(Register);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    sessionStorageSetItemSpy = vi.spyOn(Storage.prototype, 'setItem');
    vi.spyOn(Wallet, 'createRandom').mockReturnValue(MOCK_WALLET);
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('generates key pair and pre-fills DID and public key', () => {
    component.generateKeyPair();

    const formValue = component.registerForm.getRawValue();
    expect(formValue.did).toMatch(/^did:ethr:0x[a-f0-9]{40}$/);
    expect(formValue.publicKey).toMatch(/^0x04[a-f0-9]+$/);
    expect(formValue.publicKey.length).toBe(132);
    expect(formValue.backupConfirmed).toBe(false);
    expect(component.generatedPrivateKey).toBe(MOCK_WALLET.privateKey);
    expect(sessionStorageSetItemSpy).toHaveBeenCalledTimes(1);
  });

  it('posts DID registration and shows success feedback', () => {
    component.generateKeyPair();
    component.registerForm.controls.backupConfirmed.setValue(true);

    component.submitRegistration();

    const request = httpMock.expectOne('/did/register');
    expect(request.request.method).toBe('POST');
    expect(request.request.body.did).toBe(component.registerForm.controls.did.value);
    expect(request.request.body.publicKey).toBe(component.registerForm.controls.publicKey.value);

    request.flush({
      did: request.request.body.did,
      publicKey: request.request.body.publicKey,
      status: 'ACTIVE',
      createdAt: '2026-05-01T00:00:00Z',
      updatedAt: '2026-05-01T00:00:00Z'
    });

    expect(component.successMessage).toContain('has been registered');
    expect(component.errorMessage).toBeNull();
    expect(component.registeredDid?.status).toBe('ACTIVE');
  });

  it('shows API error feedback when registration fails', async () => {
    component.generateKeyPair();
    component.registerForm.controls.backupConfirmed.setValue(true);

    component.submitRegistration();

    const request = httpMock.expectOne('/did/register');
    request.flush({ message: 'DID already registered' }, { status: 409, statusText: 'Conflict' });

    await fixture.whenStable();
    fixture.detectChanges();
    expect(component.errorMessage).toBe('DID already registered');
    expect(component.successMessage).toBeNull();
    expect(component.registeredDid).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('DID already registered');
  });

  it('does not submit when form is invalid', async () => {
    component.submitRegistration();
    await fixture.whenStable();
    fixture.detectChanges();

    httpMock.expectNone('/did/register');
    expect(component.registerForm.controls.did.touched).toBe(true);
    expect(component.registerForm.controls.publicKey.touched).toBe(true);
    expect(component.registerForm.controls.backupConfirmed.touched).toBe(true);
    expect(fixture.nativeElement.textContent).toContain('DID is required.');
    expect(fixture.nativeElement.textContent).toContain('Public key is required.');
    expect(fixture.nativeElement.textContent).toContain('Confirm private key backup before registration.');
  });

  it('does not submit until private key backup is confirmed', async () => {
    component.generateKeyPair();
    component.submitRegistration();
    await fixture.whenStable();
    fixture.detectChanges();

    httpMock.expectNone('/did/register');
    expect(component.registerForm.controls.backupConfirmed.invalid).toBe(true);
  });

  afterEach(() => {
    httpMock.verify();
    vi.restoreAllMocks();
  });
});
