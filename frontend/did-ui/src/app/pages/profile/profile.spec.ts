import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { provideRouter } from '@angular/router';

import { ACCESS_TOKEN_STORAGE_KEY } from '../../core/auth-session';
import { Profile } from './profile';

describe('Profile', () => {
  const DID = 'did:ethr:0x1111111111111111111111111111111111111111';
  const TOKEN = 'jwt-token';
  let component: Profile;
  let fixture: ComponentFixture<Profile>;
  let httpMock: HttpTestingController;
  let router: Router;
  let navigateSpy: ReturnType<typeof vi.spyOn>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Profile],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(Profile);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('loads protected profile with stored JWT on init', () => {
    sessionStorage.setItem(ACCESS_TOKEN_STORAGE_KEY, TOKEN);
    navigateSpy.mockClear();

    component.ngOnInit();

    const request = httpMock.expectOne('/api/me');
    expect(request.request.method).toBe('GET');
    expect(request.request.headers.get('Authorization')).toBe(`Bearer ${TOKEN}`);
    request.flush({
      did: DID,
      claims: { role: 'user' }
    });

    expect(component.profile?.did).toBe(DID);
    expect(component.profile?.claims['role']).toBe('user');
    expect(component.errorMessage).toBeNull();
    expect(component.successMessage).toContain('loaded successfully');
    expect(navigateSpy).not.toHaveBeenCalled();
  });

  it('redirects to auth when JWT is missing', () => {
    component.loadProfile();

    httpMock.expectNone('/api/me');
    expect(component.errorMessage).toContain('JWT not found');
    expect(navigateSpy).toHaveBeenCalledWith(['/auth']);
  });

  it('redirects to auth when API returns 401', () => {
    sessionStorage.setItem(ACCESS_TOKEN_STORAGE_KEY, TOKEN);

    component.loadProfile();

    const request = httpMock.expectOne('/api/me');
    request.flush({ message: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });

    expect(component.errorMessage).toContain('JWT is missing or expired');
    expect(navigateSpy).toHaveBeenCalledWith(['/auth']);
  });

  it('shows API error message for non-401 responses', () => {
    sessionStorage.setItem(ACCESS_TOKEN_STORAGE_KEY, TOKEN);

    component.loadProfile();

    const request = httpMock.expectOne('/api/me');
    request.flush({ message: 'Resource API unavailable' }, { status: 503, statusText: 'Service Unavailable' });

    expect(component.errorMessage).toBe('Resource API unavailable');
    expect(component.profile).toBeNull();
  });

  afterEach(() => {
    httpMock.verify();
    sessionStorage.clear();
    vi.restoreAllMocks();
  });
});
