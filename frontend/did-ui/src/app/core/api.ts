import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface RegisterDidRequest {
  did: string;
  publicKey: string;
}

export interface DidDocument {
  did: string;
  publicKey: string;
  status: 'ACTIVE' | 'REVOKED';
  createdAt: string;
  updatedAt: string;
}

export interface AuthTokenRequest {
  did: string;
  challenge: string;
  signature: string;
}

export interface AuthTokenResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface ProfileResponse {
  did: string;
  claims: Record<string, unknown>;
}

@Injectable({ providedIn: 'root' })
export class Api {
  constructor(private readonly http: HttpClient) {}

  registerDid(payload: RegisterDidRequest): Observable<DidDocument> {
    return this.http.post<DidDocument>('/did/register', payload);
  }

  getChallenge(): Observable<string> {
    return this.http.get('/auth/challenge', { responseType: 'text' });
  }

  exchangeToken(payload: AuthTokenRequest): Observable<AuthTokenResponse> {
    return this.http.post<AuthTokenResponse>('/auth/token', payload);
  }

  getProfile(token: string): Observable<ProfileResponse> {
    return this.http.get<ProfileResponse>('/api/me', {
      headers: { Authorization: `Bearer ${token}` }
    });
  }
}
